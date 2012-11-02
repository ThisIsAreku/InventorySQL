package fr.areku.InventorySQL.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import fr.areku.InventorySQL.Config;
import fr.areku.InventorySQL.InventorySQL;
import fr.areku.InventorySQL.PlayerManager;
import fr.areku.InventorySQL.database.SQLItemStack.Action;

public class SQLCheck implements Runnable {
	private CoreSQLProcess parent;
	private CoreSQLItem runThis = null;
	private boolean doGive = true;
	private boolean manualCheck = false;
	private String initiator = "";
	private JDCConnection conn = null;
	private long CURRENT_CHECK_EPOCH = 0;

	public SQLCheck(CoreSQLProcess parent, String initiator) {
		InventorySQL.d("New SQLCheck !");
		this.parent = parent;
		this.initiator = initiator;
	}

	public SQLCheck manualCheck(CoreSQLItem i, boolean doGive) {
		this.runThis = i;
		this.doGive = doGive;
		return manualCheck();
	}

	public SQLCheck manualCheck() {
		InventorySQL.d("SQLCheck->ManualCheck");
		manualCheck = true;
		return this;
	}

	private void updateConn() {
		if (!this.parent.isDatabaseReady()) {
			conn = null;
			return;
		}
		if (conn == null) {
			conn = this.parent.getConnection();
		} else {
			if (!conn.isValid())
				conn = this.parent.getConnection();
		}
	}

	@Override
	public void run() {
		CURRENT_CHECK_EPOCH = System.currentTimeMillis() / 1000L;
		try {
			if (manualCheck) {
				InventorySQL.d("Running manual check");
			} else {
				InventorySQL.d("Running scheduled check");
			}
			InventorySQL.d("id: " + this.hashCode());

			if (manualCheck && (runThis != null)) {
				if (runThis.hasPlayersData()) {
					checkPlayers(runThis);
				}
			} else {

				Player[] pList = this.parent.getOnlinePlayers();
				if (pList.length > 0) {
					checkPlayers(new CoreSQLItem(pList));
				} else {
					InventorySQL.d("No players to check");
				}
			}
		} catch (Exception ex) {
			if (parent.isDatabaseReady()) {
				InventorySQL.logException(ex,
						"exception in playerlogic - check all");
			}
		}

		if (manualCheck) {
			doGive = true;
			runThis = null;
			manualCheck = false;
		}
		if (conn != null)
			conn.close();
	}

	private void checkPlayers(CoreSQLItem i) throws SQLException {

		if (!i.hasPlayersData())
			return;
		updateConn();
		if (conn == null)
			return;

		StringBuilder players_to_remove_sb = new StringBuilder();

		for (Player p : i.getPlayers()) {
			if (InventorySQL.isUsingAuthenticator()) {
				if (!fr.areku.Authenticator.Authenticator.isPlayerLoggedIn(p)) {
					InventorySQL.d(this.hashCode() + " => checkPlayers:"
							+ p.getName() + " !UNAUTHORIZED");
					continue;
				}
			}
			InventorySQL.d(this.hashCode() + " => checkPlayers:" + p.getName());
			if ((Config.noCreative) && (p.getGameMode() == GameMode.CREATIVE)) {
				return;
			}
			String pName = p.getName();

			PreparedStatement sth = conn.prepareStatement("SELECT `id` FROM `"
					+ Config.dbTable_Users + "` WHERE `name` = ?");
			sth.setString(1, pName);
			ResultSet rs = sth.executeQuery();
			Integer userID = -1;
			if (rs.first()) {
				userID = rs.getInt(1);
			} else {
				InventorySQL.d(this.hashCode() + " => Creating entry for "
						+ pName);
				userID = executeInsert(conn.prepareStatement("INSERT INTO `"
						+ Config.dbTable_Users
						+ "`(`name`, `password`) VALUES ('" + pName + "','')",
						Statement.RETURN_GENERATED_KEYS));
				InventorySQL.getPlayerManager().get(pName).updateHash("");
			}
			rs.close();
			sth.close();

			InventorySQL.d(this.hashCode() + " => userID: " + userID);

			int added = 0;
			int removed = 0;

			if (Config.mirrorMode) {
				boolean fs = InventorySQL.getPlayerManager().get(pName)
						.isFirstSessionCheck();
				if (doMirroring(userID, p)) {
					if (fs)
						p.sendMessage(ChatColor.RED + "[InventorySQL] "
								+ InventorySQL.getMessage("mirror-latest"));
				} else {
					if (fs)
						p.sendMessage(ChatColor.RED + "[InventorySQL] "
								+ InventorySQL.getMessage("mirror-done"));
				}
			}

			if (doGive) {
				String q = "SELECT `pendings`.`id` AS `p_id`, `pendings`.`item` AS `item`, `pendings`.`data` AS `data`, `pendings`.`damage` AS `damage`, `pendings`.`count` AS `count`, `enchantments`.`ench` AS `ench`, `enchantments`.`level` AS `level`"
						+ " FROM `"
						+ Config.dbTable_Pendings
						+ "` as `pendings`"
						+ " LEFT JOIN `"
						+ Config.dbTable_Enchantments
						+ "` AS `enchantments` ON `pendings`.`id` = `enchantments`.`id`"
						+ " WHERE (`pendings`.`owner` = ?";
				if (Config.multiworld)
					q += " AND `pendings`.`world` = ?";
				q += ");";
				sth = conn.prepareStatement(q);
				sth.setInt(1, userID);
				if (Config.multiworld)
					sth.setString(2, p.getWorld().getName());

				rs = sth.executeQuery();

				if (rs.first()) {
					InventorySQL.d(this.hashCode() + " => pendings items for "
							+ pName);

					Map<String, SQLItemStack> stackList = new HashMap<String, SQLItemStack>();
					String latest_id = "";
					do {
						latest_id = rs.getString("p_id");
						if (stackList.containsKey(latest_id)) {
							stackList.get(latest_id).readEnch(rs);
						} else {
							stackList.put(latest_id, new SQLItemStack(rs,
									latest_id));
						}
					} while (rs.next());

					String donePendings = "";
					for (SQLItemStack stack : stackList.values()) {
						int left = 0;
						if (stack.getAction() == Action.ADD) {
							left = giveItem(stack.getItemStack(), p);
							if (left == 0) {
								donePendings += "'" + stack.getID() + "',";
								added++;
							}
						} else if (stack.getAction() == Action.REMOVE) {
							left = (-1) * removeItem(stack.getItemStack(), p);
							if (left == 0) {
								donePendings += "'" + stack.getID() + "',";
								removed++;
							}
						}
						if (left != 0)
							executeItemsLeft(stack.getID(), left);
					}

					InventorySQL.d(this.hashCode()
							+ " => checkPlayers:PendingsDone:+" + added + "/-"
							+ removed + " of " + stackList.size());
					if (donePendings.endsWith(","))
						donePendings = donePendings.substring(0,
								donePendings.length() - 1);

					if (donePendings != "") {
						InventorySQL
								.d(this.hashCode()
										+ " => checkPlayers:PendingsDone:RemovingEntries:"
										+ donePendings);
						conn.createStatement().executeUpdate(
								"DELETE FROM `" + Config.dbTable_Enchantments
										+ "` WHERE `id` IN (" + donePendings
										+ ");");
						conn.createStatement().executeUpdate(
								"DELETE FROM `" + Config.dbTable_Pendings
										+ "` WHERE `id` IN (" + donePendings
										+ ");");
					}
					i.sendMessage("[InventorySQL] "
							+ ChatColor.GREEN
							+ "("
							+ pName
							+ ") "
							+ InventorySQL.getMessage("modif", added, removed,
									stackList.size()));

				} else {
					i.sendMessage("[InventorySQL] " + ChatColor.GREEN + "("
							+ pName + ") "
							+ InventorySQL.getMessage("no-modif"));

				}
				rs.close();
				sth.close();
			}
			InventorySQL.getPlayerManager().get(pName)
					.updateEpoch(CURRENT_CHECK_EPOCH);
			String theInvHash = PlayerManager.computePlayerInventoryHash(p);
			if (InventorySQL.getPlayerManager().get(pName)
					.updateHash(theInvHash)) {
				InventorySQL.d(this.hashCode()
						+ " => checkPlayers:InventoryModified");
				/*
				 * String q = "DELETE `inventories`, `enchantments` FROM `" +
				 * Config.dbTable_Inventories + "` AS `inventories` LEFT JOIN `"
				 * + Config.dbTable_Enchantments +
				 * "` AS `enchantments` ON (`inventories`.`id` = `enchantments`.`id`"
				 * ;
				 * 
				 * if (Config.backup_enabled) q +=
				 * "AND `enchantments`.`is_backup` = 0";
				 * 
				 * q += ") WHERE (`inventories`.`owner` = ?"; if
				 * (Config.multiworld) q += " AND `inventories`.`world` = ?";
				 * 
				 * q += ");"; sth = conn.prepareStatement(q); sth.setInt(1,
				 * userID); if (Config.multiworld) sth.setString(2,
				 * p.getWorld().getName()); sth.executeUpdate(); sth.close();
				 */
				players_to_remove_sb.append(userID.toString() + ",").toString();

				for (Integer invSlotID = 0; invSlotID < 36; invSlotID++) {
					updateSQL(p, userID, null, invSlotID, conn);
				}
				updateSQL(p, userID, p.getInventory().getBoots(), 100, conn);
				updateSQL(p, userID, p.getInventory().getLeggings(), 101, conn);
				updateSQL(p, userID, p.getInventory().getChestplate(), 102,
						conn);
				updateSQL(p, userID, p.getInventory().getHelmet(), 103, conn);
				// Main.d(this.hashCode() + " => checkPlayers:Inventory:" + q);
			} else {
				InventorySQL.d(this.hashCode()
						+ " => checkPlayers:InventoryNotModified");
			}
			p.saveData();
		}

		String final_players_id = players_to_remove_sb.toString();
		if (final_players_id.length() > 0) {
			String q = "DELETE `inventories`, `enchantments` FROM `"
					+ Config.dbTable_Inventories
					+ "` AS `inventories` LEFT JOIN `"
					+ Config.dbTable_Enchantments
					+ "` AS `enchantments` ON (`inventories`.`id` = `enchantments`.`id`";

			if (Config.backup_enabled)
				q += " AND `enchantments`.`is_backup` = 0";

			q += ") WHERE (`inventories`.`owner` IN (" + final_players_id
					+ "0) AND `inventories`.`date` != ?";
			/*
			 * if (Config.multiworld) q += " AND `inventories`.`world` = ?";
			 */

			q += ")";

			PreparedStatement sth = conn.prepareStatement(q);
			sth.setLong(1, CURRENT_CHECK_EPOCH);
			// sth.setString(1, final_players_id);
			/*
			 * if (Config.multiworld) sth.setString(2, p.getWorld().getName());
			 */
			sth.executeUpdate();
			sth.close();
			InventorySQL.d(this.hashCode()
					+ " => Cleaning:OldRecordsOfinventories");
			InventorySQL.d(this.hashCode() + " => " + q + " - IDs: "
					+ final_players_id + " - time: " + CURRENT_CHECK_EPOCH);
		}
	}

	private boolean doMirroring(int userID, Player p) throws SQLException {
		InventorySQL.d(this.hashCode() + " => Mirroring:Start");
		boolean r = false;
		String q = "SELECT `inventories`.`id` AS `p_id`, `inventories`.`item` AS `item`, `inventories`.`data` AS `data`, `inventories`.`damage` AS `damage`, `inventories`.`count` AS `count`, `enchantments`.`ench` AS `ench`, `enchantments`.`level` AS `level`, `inventories`.`slot` AS `slot` FROM `"
				+ Config.dbTable_Inventories
				+ "` as `inventories` LEFT JOIN `"
				+ Config.dbTable_Enchantments
				+ "` AS `enchantments` ON `inventories`.`id` = `enchantments`.`id` WHERE `inventories`.`date` > ? AND (`inventories`.`owner` = ?";
		if (Config.multiworld)
			q += " AND `inventories`.`world` = ?";
		q += ");";
		PreparedStatement sth = conn.prepareStatement(q);
		sth.setLong(1, InventorySQL.getPlayerManager().get(p.getName())
				.getEpoch());
		sth.setInt(2, userID);
		if (Config.multiworld)
			sth.setString(3, p.getWorld().getName());

		ResultSet rs = sth.executeQuery();

		if (rs.first()) {
			InventorySQL.d(this.hashCode() + " => Mirroring:SQLMoreRecent");
			InventorySQL.d(this.hashCode()
					+ " => Mirroring:TODO:Fetch inv from SQL");
			if (doGive) {
				Map<String, SQLItemStack> stackList = new HashMap<String, SQLItemStack>();
				String latest_id = "";
				do {
					latest_id = rs.getString("p_id");
					if (stackList.containsKey(latest_id)) {
						stackList.get(latest_id).readEnch(rs);
					} else {
						stackList.put(latest_id, new SQLItemStack(rs,
								latest_id, rs.getInt("slot")));
					}
				} while (rs.next());
				System.out.println(stackList);
				p.getInventory().clear();
				for (SQLItemStack stack : stackList.values()) {
					switch (stack.getSlotID()) {
					case 100:
						p.getInventory().setBoots(stack.getItemStack());
						break;
					case 101:
						p.getInventory().setLeggings(stack.getItemStack());
						break;
					case 102:
						p.getInventory().setChestplate(stack.getItemStack());
						break;
					case 103:
						p.getInventory().setHelmet(stack.getItemStack());
						break;
					default:
						p.getInventory().setItem(stack.getSlotID(),
								stack.getItemStack());
					}
				}
			}
			updateSQLTime(userID, p);
			r = false;
		} else {
			InventorySQL.d(this.hashCode()
					+ " => Mirroring:LocalMoreRecentOrNoSQL");
			/*
			 * if (!invModified) { updateSQLTime(userID, p); }
			 */
			r = true;
		}
		rs.close();
		sth.close();
		InventorySQL.d(this.hashCode() + " => Mirroring:End");
		return r;
	}

	private void updateSQLTime(int userID, Player p) throws SQLException {
		String up = "UPDATE `"
				+ Config.dbTable_Inventories
				+ "` AS `inventories` SET `date`=?,`event`=? WHERE (`inventories`.`owner` = ?";
		if (Config.multiworld)
			up += " AND `inventories`.`world` = ?";
		up += ");";
		PreparedStatement sth2 = conn.prepareStatement(up);
		sth2.setLong(1, CURRENT_CHECK_EPOCH);
		sth2.setString(2, this.initiator);
		sth2.setInt(3, userID);
		if (Config.multiworld)
			sth2.setString(4, p.getWorld().getName());
		sth2.executeUpdate();
		InventorySQL.d(this.hashCode() + " => Mirroring:SQLTimeUpdated");
	}

	private void updateSQL(Player p, int userID, ItemStack stack, int slotID,
			JDCConnection conn) throws SQLException {
		String base_query = "INSERT INTO `%TABLE%`(`id`, `owner`, `world`, `item`, `data`,`damage`, `count`, `slot`, `event`, `date`, `suid`) VALUES ";
		ItemStack invSlotItem;
		try {
			invSlotItem = p.getInventory().getItem(slotID);
		} catch (Exception ex) {
			invSlotItem = stack;
		}

		if (invSlotItem != null) {
			String invSlotID = UUID.randomUUID().toString();
			String q_item = base_query + "('" + invSlotID + "', " + userID
					+ ", '" + p.getWorld().getName() + "', "
					+ invSlotItem.getTypeId() + ", "
					+ invSlotItem.getData().getData() + ", "
					+ invSlotItem.getDurability() + ", "
					+ invSlotItem.getAmount() + ", " + slotID + ", '"
					+ this.initiator + "', " + CURRENT_CHECK_EPOCH + ", '"
					+ Config.serverUID + "')";

			conn.prepareStatement(
					q_item.replace("%TABLE%", Config.dbTable_Inventories))
					.executeUpdate();
			if (Config.backup_enabled) {
				conn.prepareStatement(
						q_item.replace("%TABLE%", Config.dbTable_Backups))
						.executeUpdate();
			}
			InventorySQL.d(this.hashCode() + " => InsertItemFromInvCommand:"
					+ q_item);
			InventorySQL.d(this.hashCode() + " => InsertItemFromInv:"
					+ invSlotID);

			if (!invSlotItem.getEnchantments().isEmpty()) {

				String q_ench = "INSERT INTO `"
						+ Config.dbTable_Enchantments
						+ "`(`id`, `ench_index`, `ench`, `level`, `is_backup`) VALUES ";
				int i = 0;
				for (Entry<Enchantment, Integer> e : invSlotItem
						.getEnchantments().entrySet()) {
					q_ench += "('" + invSlotID + "', " + i + ", "
							+ e.getKey().getId() + ", " + e.getValue()
							+ ", %ISBACKUP%),";
					i++;
				}

				q_ench = q_ench.substring(0, q_ench.length() - 1);
				conn.prepareStatement(q_ench.replace("%ISBACKUP%", "0"))
						.executeUpdate();
				if (Config.backup_enabled) {
					conn.prepareStatement(q_ench.replace("%ISBACKUP%", "1"))
							.executeUpdate();
				}
			}
		}
	}

	private Integer executeInsert(PreparedStatement statement)
			throws SQLException {
		statement.executeUpdate();
		ResultSet rs = statement.getGeneratedKeys();
		if (rs.first()) {
			return rs.getInt(1);
		} else {
			return -1;
		}
	}

	private void executeItemsLeft(String item_id, int left) throws SQLException {
		PreparedStatement statement = conn.prepareStatement("UPDATE `"
				+ Config.dbTable_Pendings + "` SET `count`=? WHERE `id`=?");
		statement.setInt(1, left);
		statement.setString(2, item_id);
		statement.executeUpdate();
	}

	private int giveItem(final ItemStack item, final Player p) {
		InventorySQL.d(this.hashCode() + " => giveItem:" + item.toString());
		try {
			HashMap<Integer, ItemStack> m = this.parent.callSyncMethod(
					new Callable<HashMap<Integer, ItemStack>>() {
						@Override
						public HashMap<Integer, ItemStack> call()
								throws Exception {
							return p.getInventory().addItem(item);
						}
					}).get();
			return m.get(0).getAmount();
		} catch (Exception e) {
			return 0;
		}
	}

	private int removeItem(final ItemStack item, final Player p) {
		InventorySQL.d(this.hashCode() + " => removeItem:" + item.toString());
		try {
			HashMap<Integer, ItemStack> m = this.parent.callSyncMethod(
					new Callable<HashMap<Integer, ItemStack>>() {
						@Override
						public HashMap<Integer, ItemStack> call()
								throws Exception {
							return p.getInventory().removeItem(item);
						}
					}).get();
			return m.get(0).getAmount();
		} catch (Exception e) {
			return 0;
		}
	}

}
