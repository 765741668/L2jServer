/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.idfactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;

/**
 * This class ...
 *
 * @version $Revision: 1.3.2.1.2.7 $ $Date: 2005/04/11 10:06:12 $
 */
public abstract class IdFactory
{
	private static Logger _log = Logger.getLogger(IdFactory.class.getName());
	
	protected static final String[] ID_UPDATES = {
			"UPDATE items                 SET owner_id = ?    WHERE owner_id = ?",
			"UPDATE items                 SET object_id = ?   WHERE object_id = ?",
			"UPDATE character_quests      SET charId = ?     WHERE charId = ?",
			"UPDATE character_friends     SET charId = ?     WHERE charId = ?",
			"UPDATE character_friends     SET friendId = ?   WHERE friendId = ?",
			"UPDATE character_hennas      SET charId = ? WHERE charId = ?",
			"UPDATE character_recipebook  SET charId = ? WHERE charId = ?",
			"UPDATE character_shortcuts   SET charId = ? WHERE charId = ?",
			"UPDATE character_shortcuts   SET shortcut_id = ? WHERE shortcut_id = ? AND type = 1", // items
			"UPDATE character_macroses    SET charId = ? WHERE charId = ?", "UPDATE character_skills      SET charId = ? WHERE charId = ?",
			"UPDATE character_skills_save SET charId = ? WHERE charId = ?", "UPDATE character_subclasses  SET charId = ? WHERE charId = ?",
			"UPDATE characters            SET charId = ? WHERE charId = ?",
			"UPDATE characters            SET clanid = ?      WHERE clanid = ?",
			"UPDATE clan_data             SET clan_id = ?     WHERE clan_id = ?",
			"UPDATE siege_clans           SET clan_id = ?     WHERE clan_id = ?",
			"UPDATE clan_data             SET ally_id = ?     WHERE ally_id = ?",
			"UPDATE clan_data             SET leader_id = ?   WHERE leader_id = ?",
			"UPDATE pets                  SET item_obj_id = ? WHERE item_obj_id = ?",
			"UPDATE character_hennas     SET charId = ? WHERE charId = ?",
			"UPDATE itemsonground         SET object_id = ?   WHERE object_id = ?",
			"UPDATE auction_bid          SET bidderId = ?      WHERE bidderId = ?",
			"UPDATE auction_watch        SET charObjId = ?     WHERE charObjId = ?",
			"UPDATE clanhall             SET ownerId = ?       WHERE ownerId = ?"
	};
	
	protected static final String[] ID_CHECKS = {
			"SELECT owner_id    FROM items                 WHERE object_id >= ?   AND object_id < ?",
			"SELECT object_id   FROM items                 WHERE object_id >= ?   AND object_id < ?",
			"SELECT charId     FROM character_quests      WHERE charId >= ?     AND charId < ?",
			"SELECT charId     FROM character_friends     WHERE charId >= ?     AND charId < ?",
			"SELECT charId     FROM character_friends     WHERE friendId >= ?   AND friendId < ?",
			"SELECT charId     FROM character_hennas      WHERE charId >= ? AND charId < ?",
			"SELECT charId     FROM character_recipebook  WHERE charId >= ?     AND charId < ?",
			"SELECT charId     FROM character_shortcuts   WHERE charId >= ? AND charId < ?",
			"SELECT charId     FROM character_macroses    WHERE charId >= ? AND charId < ?",
			"SELECT charId     FROM character_skills      WHERE charId >= ? AND charId < ?",
			"SELECT charId     FROM character_skills_save WHERE charId >= ? AND charId < ?",
			"SELECT charId     FROM character_subclasses  WHERE charId >= ? AND charId < ?",
			"SELECT charId      FROM characters            WHERE charId >= ?      AND charId < ?",
			"SELECT clanid      FROM characters            WHERE clanid >= ?      AND clanid < ?",
			"SELECT clan_id     FROM clan_data             WHERE clan_id >= ?     AND clan_id < ?",
			"SELECT clan_id     FROM siege_clans           WHERE clan_id >= ?     AND clan_id < ?",
			"SELECT ally_id     FROM clan_data             WHERE ally_id >= ?     AND ally_id < ?",
			"SELECT leader_id   FROM clan_data             WHERE leader_id >= ?   AND leader_id < ?",
			"SELECT item_obj_id FROM pets                  WHERE item_obj_id >= ? AND item_obj_id < ?",
			"SELECT object_id   FROM itemsonground        WHERE object_id >= ?   AND object_id < ?"
	};
	
	protected boolean _initialized;
	
	public static final int FIRST_OID = 0x10000000;
	public static final int LAST_OID = 0x7FFFFFFF;
	public static final int FREE_OBJECT_ID_SIZE = LAST_OID - FIRST_OID;
	
	protected static final IdFactory _instance;
	
	protected IdFactory()
	{
		setAllCharacterOffline();
		cleanUpDB();
	}
	
	static
	{
		switch (Config.IDFACTORY_TYPE)
		{
			case Compaction:
				_instance = new CompactionIDFactory();
				break;
			case BitSet:
				_instance = new BitSetIDFactory();
				break;
			case Stack:
				_instance = new StackIDFactory();
				break;
			default:
				_instance = null;
				break;
		}
	}
	
	/**
	 * Sets all character offline
	 */
	private void setAllCharacterOffline()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			Statement statement = con.createStatement();
			statement.executeUpdate("UPDATE characters SET online = 0");
			statement.close();
			
			_log.info("Updated characters online status.");
		}
		catch (SQLException e)
		{
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	/**
	 * Cleans up Database
	 */
	private void cleanUpDB()
	{
		Connection con = null;
		try
		{
			int cleanCount = 0;
			con = L2DatabaseFactory.getInstance().getConnection();
			Statement stmt = con.createStatement();
			// Misc/Account Related
			// Please read the descriptions above each before uncommenting them. If you are still unsure of what exactly it does, leave it commented out. This is for those who know what they are doing. :)

			// Deletes only accounts that HAVE been logged into and have no characters associated with the account.
			// cleanCount += stmt.executeUpdate("DELETE FROM accounts WHERE accounts.lastactive > 0 AND accounts.login NOT IN (SELECT account_name FROM characters);");

			// Deletes any accounts that don't have characters. Whether or not the player has ever logged into the account.
			// cleanCount += stmt.executeUpdate("DELETE FROM accounts WHERE accounts.login NOT IN (SELECT account_name FROM characters);");

			// Deletes banned accounts that have not been logged into for xx amount of days (specified at the end of the script, default is set to 90 days). This prevents accounts from being deleted that were accidentally or temporarily banned.
			//cleanCount += stmt.executeUpdate("DELETE FROM accounts WHERE accounts.accessLevel < 0 AND DATEDIFF(CURRENT_DATE( ) , FROM_UNIXTIME(`lastactive`/1000)) > 90;");
			//cleanCount += stmt.executeUpdate("DELETE FROM characters WHERE characters.account_name NOT IN (SELECT login FROM accounts);");

			// If the character does not exist...
			cleanCount += stmt.executeUpdate("DELETE FROM character_friends WHERE character_friends.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_friends WHERE character_friends.friendId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_hennas WHERE character_hennas.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_macroses WHERE character_macroses.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_quests WHERE character_quests.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_recipebook WHERE character_recipebook.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_shortcuts WHERE character_shortcuts.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_skills WHERE character_skills.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_skills_save WHERE character_skills_save.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_subclasses WHERE character_subclasses.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_raid_points WHERE character_raid_points.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM items WHERE items.owner_id NOT IN (SELECT charId FROM characters) AND items.owner_id NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM item_attributes WHERE item_attributes.itemId NOT IN (SELECT object_id FROM items);");
			cleanCount += stmt.executeUpdate("DELETE FROM cursed_weapons WHERE cursed_weapons.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM heroes WHERE heroes.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM olympiad_nobles WHERE olympiad_nobles.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM olympiad_nobles_eom WHERE olympiad_nobles_eom.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM pets WHERE pets.item_obj_id NOT IN (SELECT object_id FROM items);");
			cleanCount += stmt.executeUpdate("DELETE FROM seven_signs WHERE seven_signs.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM merchant_lease WHERE merchant_lease.player_id NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_recommends WHERE character_recommends.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_recommends WHERE character_recommends.target_id NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM clan_data WHERE clan_data.leader_id NOT IN (SELECT charId FROM characters);");

			// If the clan does not exist...
			cleanCount += stmt.executeUpdate("DELETE FROM clan_privs WHERE clan_privs.clan_id NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM clan_skills WHERE clan_skills.clan_id NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM clan_subpledges WHERE clan_subpledges.clan_id NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM clan_wars WHERE clan_wars.clan1 NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM clan_wars WHERE clan_wars.clan2 NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM clanhall_functions WHERE clanhall_functions.hall_id NOT IN (SELECT id FROM clanhall WHERE ownerId <> 0);");
			cleanCount += stmt.executeUpdate("DELETE FROM siege_clans WHERE siege_clans.clan_id NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM clan_notices WHERE clan_notices.clan_id NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM clan_notices WHERE clan_notices.clan_id NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM auction_bid WHERE auction_bid.bidderId NOT IN (SELECT clan_id FROM clan_data);");
			//Untested, leaving commented out until confirmation that it's safe/works properly. Was initially removed because of a bug. Search for idfactory.java changes in the trac for further info.
			//cleanCount += stmt.executeUpdate("DELETE FROM auction WHERE auction.id IN (SELECT id FROM clanhall WHERE ownerId <> 0) AND auction.sellerId=0;");
			//cleanCount += stmt.executeUpdate("DELETE FROM auction_bid WHERE auctionId NOT IN (SELECT id FROM auction)");

			//Forum Related
			cleanCount += stmt.executeUpdate("DELETE FROM forums WHERE forums.forum_owner_id NOT IN (SELECT clan_id FROM clan_data) AND forums.forum_parent=2;");
			cleanCount += stmt.executeUpdate("DELETE FROM posts WHERE posts.post_forum_id NOT IN (SELECT forum_id FROM forums);");
			cleanCount += stmt.executeUpdate("DELETE FROM topic WHERE topic.topic_forum_id NOT IN (SELECT forum_id FROM forums);");

			// Update needed items after cleaning has taken place.
			stmt.executeUpdate("UPDATE clan_data SET auction_bid_at = 0 WHERE auction_bid_at NOT IN (SELECT auctionId FROM auction_bid);");
			stmt.executeUpdate("UPDATE clan_subpledges SET leader_id=0 WHERE clan_subpledges.leader_id NOT IN (SELECT charId FROM characters) AND leader_id > 0;");
			stmt.executeUpdate("UPDATE castle SET taxpercent=0 WHERE castle.id NOT IN (SELECT hasCastle FROM clan_data);");
			stmt.executeUpdate("UPDATE characters SET clanid=0, clan_privs=0, wantspeace=0, subpledge=0, lvl_joined_academy=0, apprentice=0, sponsor=0, clan_join_expiry_time=0, clan_create_expiry_time=0 WHERE characters.clanid NOT IN (SELECT clan_id FROM clan_data);");
			stmt.executeUpdate("UPDATE clanhall SET ownerId=0, paidUntil=0, paid=0 WHERE clanhall.ownerId NOT IN (SELECT clan_id FROM clan_data);");
			
			stmt.close();
			_log.info("Cleaned " + cleanCount + " elements from database.");
		}
		catch (SQLException e)
		{
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	/**
	 * @param con
	 * @return
	 * @throws SQLException
	 */
	protected int[] extractUsedObjectIDTable() throws SQLException
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			//create a temporary table
			Statement statement = con.createStatement();
			
			statement.executeUpdate("DROP TABLE IF EXISTS temporaryObjectTable");
			statement.executeUpdate("DELETE FROM itemsonground WHERE object_id IN (SELECT object_id FROM items)");
			statement.executeUpdate("CREATE TABLE temporaryObjectTable" + " (object_id int NOT NULL PRIMARY KEY)");
			
			statement.executeUpdate("INSERT INTO temporaryObjectTable (object_id)" + " SELECT charId FROM characters");
			statement.executeUpdate("INSERT INTO temporaryObjectTable (object_id)" + " SELECT object_id FROM items");
			statement.executeUpdate("INSERT INTO temporaryObjectTable (object_id)" + " SELECT clan_id FROM clan_data");
			//statement.executeUpdate("INSERT INTO temporaryObjectTable (object_id)" + " SELECT crest_id FROM clan_data WHERE crest_id > 0");
			statement.executeUpdate("INSERT INTO temporaryObjectTable (object_id)" + " SELECT object_id FROM itemsonground");
			
			ResultSet result = statement.executeQuery("SELECT COUNT(object_id) FROM temporaryObjectTable");
			
			result.next();
			int size = result.getInt(1);
			result.close();
			int[] tmp_obj_ids = new int[size];
			//_log.info("tmp table size: " + tmp_obj_ids.length);
			
			result = statement.executeQuery("SELECT object_id FROM temporaryObjectTable ORDER BY object_id");
			
			int idx = 0;
			while (result.next())
			{
				tmp_obj_ids[idx++] = result.getInt(1);
			}
			
			result.close();
			statement.close();
			
			return tmp_obj_ids;
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	public boolean isInitialized()
	{
		return _initialized;
	}
	
	public static IdFactory getInstance()
	{
		return _instance;
	}
	
	public abstract int getNextId();
	
	/**
	 * return a used Object ID back to the pool
	 * @param object ID
	 */
	public abstract void releaseId(int id);
	
	public abstract int size();
}
