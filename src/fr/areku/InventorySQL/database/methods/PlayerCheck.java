package fr.areku.InventorySQL.database.methods;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fr.areku.InventorySQL.Config;
import fr.areku.InventorySQL.InventorySQL;
import fr.areku.InventorySQL.PlayerManager;
import fr.areku.InventorySQL.database.CoreSQLProcess;
import fr.areku.InventorySQL.database.JDCConnection;
import fr.areku.InventorySQL.database.SQLItemStack;
import fr.areku.InventorySQL.database.SQLMethod;
import fr.areku.InventorySQL.database.SQLItemStack.Action;

public class PlayerCheck extends SQLMethod {
	private boolean doGive = true;

	public PlayerCheck(CoreSQLProcess parent, String initiator, CommandSender cs) {
		super(parent, initiator, cs);
	}

	public PlayerCheck setDoGive(boolean doGive) {
		this.doGive = doGive;
		return this;
	}

	@Override
	public void doAction(Object target, String initiator, CommandSender cs) {
		Player[] targetPlayers;
		if (target != null) {
			if (target instanceof Player) {
				targetPlayers = new Player[] { (Player) target };
			} else if (target instanceof Player[]) {
				targetPlayers = (Player[]) target;
			} else {
				InventorySQL.d(this.hashCode() + " => bad target");
				return;
			}
		} else {
			try {
				// assuming it's a full check
				targetPlayers = getCoreSQL().getOnlinePlayers();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		}

		StringBuilder players_to_remove_sb = new StringBuilder();

		for (Player p : targetPlayers) {

			try {
				if (InventorySQL.isUsingAuthenticator()) {
					if (!fr.areku.Authenticator.Authenticator
							.isPlayerLoggedIn(p)) {
						InventorySQL.d(this.hashCode() + " => checkPlayers:"
								+ p.getName() + " !UNAUTHORIZED");
						continue;
					}
				}
				InventorySQL.d(this.hashCode() + " => checkPlayers:"
						+ p.getName());
				if ((Config.noCreative)
						&& (p.getGameMode() == GameMode.CREATIVE)) {
					return;
				}
				String pName = p.getName();

				PreparedStatement sth = getConn().prepareStatement(
						"SELECT `id` FROM `" + Config.dbTable_Users
								+ "` WHERE `name` = ?");
				sth.setString(1, pName);
				ResultSet rs = sth.executeQuery();
				Integer userID = -1;
				if (rs.first()) {
					userID = rs.getInt(1);
				} else {
					InventorySQL.d(this.hashCode() + " => Creating entry for "
							+ pName);
					userID = executeInsert(getConn().prepareStatement(
							"INSERT INTO `" + Config.dbTable_Users
									+ "`(`name`, `password`) VALUES ('" + pName
									+ "','')", Statement.RETURN_GENERATED_KEYS));
					PlayerManager.getInstance().get(pName).updateHash("");
				}
				rs.close();
				sth.close();

				InventorySQL.d(this.hashCode() + " => userID: " + userID);

				int added = 0;
				int removed = 0;

				if (Config.mirrorMode) {
					boolean fs = PlayerManager.getInstance().get(pName)
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

				if (getCoreSQL().isDatabaseReady() && doGive) {
					String q = "SELECT `pendings`.`id` AS `p_id`, `pendings`.`item` AS `item`, `pendings`.`data` AS `data`, `pendings`.`damage` AS `damage`, `pendings`.`count` AS `count`, `enchantments`.`ench` AS `ench`, `enchantments`.`level` AS `level`, `meta`.`key` AS `meta_key`, `meta`.`value` AS `meta_value`"
							+ " FROM `"
							+ Config.dbTable_Pendings
							+ "` as `pendings`"
							+ " LEFT JOIN `"
							+ Config.dbTable_Enchantments
							+ "` AS `enchantments` ON `pendings`.`id` = `enchantments`.`id`";
					if (Config.backup_enabled)
						q += " AND `enchantments`.`is_backup` = 0";

					q += " LEFT JOIN `" + Config.dbTable_Meta
							+ "` AS `meta` ON `pendings`.`id` = `meta`.`id`";
					if (Config.backup_enabled)
						q += " AND `meta`.`is_backup` = 0";

					q += " WHERE (`pendings`.`owner` = ?";
					if (Config.multiworld)
						q += " AND `pendings`.`world` = ?";

					q += ");";

					InventorySQL.d(q);

					sth = getConn().prepareStatement(q);
					sth.setInt(1, userID);
					if (Config.multiworld)
						sth.setString(2, p.getWorld().getName());

					rs = sth.executeQuery();

					if (rs.first()) {
						InventorySQL.d(this.hashCode()
								+ " => pendings items for " + pName);

						Map<String, SQLItemStack> stackList = new HashMap<String, SQLItemStack>();
						String latest_id = "";
						do {
							latest_id = rs.getString("p_id");
							if (stackList.containsKey(latest_id)) {
								stackList.get(latest_id).readEnch(rs);
								stackList.get(latest_id).readMeta(rs);
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
								left = (-1)
										* removeItem(stack.getItemStack(), p);
								if (left == 0) {
									donePendings += "'" + stack.getID() + "',";
									removed++;
								}
							}
							if (left != 0)
								executeItemsLeft(stack.getID(), left);
						}

						InventorySQL.d(this.hashCode()
								+ " => checkPlayers:PendingsDone:+" + added
								+ "/-" + removed + " of " + stackList.size());
						if (donePendings.endsWith(","))
							donePendings = donePendings.substring(0,
									donePendings.length() - 1);

						if (donePendings != "") {
							InventorySQL
									.d(this.hashCode()
											+ " => checkPlayers:PendingsDone:RemovingEntries:"
											+ donePendings);
							getConn().createStatement().executeUpdate(
									"DELETE FROM `"
											+ Config.dbTable_Enchantments
											+ "` WHERE `id` IN ("
											+ donePendings + ");");
							getConn().createStatement().executeUpdate(
									"DELETE FROM `" + Config.dbTable_Pendings
											+ "` WHERE `id` IN ("
											+ donePendings + ");");
							getConn().createStatement().executeUpdate(
									"DELETE FROM `" + Config.dbTable_Meta
											+ "` WHERE `id` IN ("
											+ donePendings + ");");
						}
						sendMessage("[InventorySQL] "
								+ ChatColor.GREEN
								+ "("
								+ pName
								+ ") "
								+ InventorySQL.getMessage("modif", added,
										removed, stackList.size()));

					} else {
						sendMessage("[InventorySQL] " + ChatColor.GREEN + "("
								+ pName + ") "
								+ InventorySQL.getMessage("no-modif"));

					}
					rs.close();
					sth.close();
				}
				PlayerManager.getInstance().get(pName)
						.updateEpoch(getCurrentCheckEpoch());
				String theInvHash = PlayerManager.computePlayerInventoryHash(p);
				if (PlayerManager.getInstance().get(pName)
						.updateHash(theInvHash)) {
					InventorySQL.d(this.hashCode()
							+ " => checkPlayers:InventoryModified");

					players_to_remove_sb.append(userID.toString() + ",")
							.toString();

					// update standart slots
					for (Integer invSlotID = 0; invSlotID < 36; invSlotID++) {
						updateSQL(p, userID, null, invSlotID, getConn());
					}
					// moar slots !
					updateSQL(p, userID, p.getInventory().getBoots(), 100,
							getConn());
					updateSQL(p, userID, p.getInventory().getLeggings(), 101,
							getConn());
					updateSQL(p, userID, p.getInventory().getChestplate(), 102,
							getConn());
					updateSQL(p, userID, p.getInventory().getHelmet(), 103,
							getConn());
				} else {
					InventorySQL.d(this.hashCode()
							+ " => checkPlayers:InventoryNotModified");
				}
				p.saveData();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		String final_players_id = players_to_remove_sb.toString();
		if (final_players_id.length() > 0) {
			String q = "DELETE `inventories`, `enchantments`, `meta` FROM `"
					+ Config.dbTable_Inventories
					+ "` AS `inventories` LEFT JOIN `"
					+ Config.dbTable_Enchantments
					+ "` AS `enchantments` ON `inventories`.`id` = `enchantments`.`id`";
			if (Config.backup_enabled)
				q += " AND `enchantments`.`is_backup` = 0";

			q += " LEFT JOIN `" + Config.dbTable_Meta
					+ "` AS `meta` ON `inventories`.`id` = `meta`.`id`";
			if (Config.backup_enabled)
				q += " AND `meta`.`is_backup` = 0";

			q += " WHERE (`inventories`.`owner` IN (" + final_players_id
					+ "0) AND `inventories`.`date` != ?";
			/*
			 * if (Config.multiworld) q += " AND `inventories`.`world` = ?";
			 */

			q += ")";

			try {
				PreparedStatement sth = getConn().prepareStatement(q);
				sth.setLong(1, getCurrentCheckEpoch());
				// sth.setString(1, final_players_id);
				/*
				 * if (Config.multiworld) sth.setString(2,
				 * p.getWorld().getName());
				 */
				sth.executeUpdate();
				sth.close();
				InventorySQL.d(this.hashCode()
						+ " => Cleaning:OldRecordsOfinventories");
				InventorySQL.d(this.hashCode() + " => " + q + " - IDs: "
						+ final_players_id + " - time: "
						+ getCurrentCheckEpoch());
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private boolean doMirroring(int userID, Player p) throws SQLException {
		InventorySQL.d(this.hashCode() + " => Mirroring:Start");
		boolean r = false;
		String q = "SELECT `inventories`.`id` AS `p_id`, `inventories`.`item` AS `item`, `inventories`.`data` AS `data`, `inventories`.`damage` AS `damage`, `inventories`.`count` AS `count`, `enchantments`.`ench` AS `ench`, `enchantments`.`level` AS `level`, `meta`.`key` AS `meta_key`, `meta`.`value` AS `meta_value`, `inventories`.`slot` AS `slot` FROM `"
				+ Config.dbTable_Inventories
				+ "` as `inventories` LEFT JOIN `"
				+ Config.dbTable_Enchantments
				+ "` AS `enchantments` ON `inventories`.`id` = `enchantments`.`id` LEFT JOIN `"
				+ Config.dbTable_Meta
				+ "` AS `meta` ON `inventories`.`id` = `meta`.`id` WHERE `inventories`.`date` > ? AND (`inventories`.`owner` = ?";
		if (Config.multiworld)
			q += " AND `inventories`.`world` = ?";

		if (Config.backup_enabled)
			q += " AND `enchantments`.`is_backup` = 0 AND `meta`.`is_backup` = 0";

		q += ");";
		PreparedStatement sth = getConn().prepareStatement(q);
		sth.setLong(1, PlayerManager.getInstance().get(p.getName()).getEpoch());
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
						stackList.get(latest_id).readMeta(rs);
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
		PreparedStatement sth2 = getConn().prepareStatement(up);
		sth2.setLong(1, getCurrentCheckEpoch());
		sth2.setString(2, getInitiator());
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
					+ this.getInitiator() + "', " + getCurrentCheckEpoch()
					+ ", '" + Config.serverUID + "')";
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

			ItemMeta invMeta = invSlotItem.getItemMeta();
			if (invMeta.hasEnchants()) {

				String q_ench = "INSERT INTO `"
						+ Config.dbTable_Enchantments
						+ "`(`id`, `ench_index`, `ench`, `level`, `is_backup`) VALUES ";
				int i = 0;
				for (Entry<Enchantment, Integer> e : invMeta.getEnchants()
						.entrySet()) {
					q_ench += "('" + invSlotID + "', " + i + ", "
							+ e.getKey().getId() + ", " + e.getValue()
							+ ", 0),";
					if (Config.backup_enabled) {
						q_ench += "('" + invSlotID + "', " + i + ", "
								+ e.getKey().getId() + ", " + e.getValue()
								+ ", 1),";
					}
					i++;
				}

				q_ench = q_ench.substring(0, q_ench.length() - 1);
				conn.prepareStatement(q_ench).executeUpdate();
			}
			if (invMeta.getDisplayName() != null) {
				String q_meta = "INSERT INTO `" + Config.dbTable_Meta
						+ "`(`id`, `key`, `value`, `is_backup`) VALUES ";

				q_meta += "('" + invSlotID + "', 'DisplayName', ?, 0)";
				if (Config.backup_enabled) {
					q_meta += ", ('" + invSlotID + "', 'DisplayName', ?, 1)";
				}

				//InventorySQL.d(q_meta);

				PreparedStatement prep = conn.prepareStatement(q_meta);
				prep.setString(1, invMeta.getDisplayName());
				if (Config.backup_enabled) {
					prep.setString(2, invMeta.getDisplayName());
				}
				prep.executeUpdate();
			}
			if (invMeta.getLore() != null)
				if (!invMeta.getLore().isEmpty()) {
					String q_meta = "INSERT INTO `" + Config.dbTable_Meta
							+ "`(`id`, `key`, `value`, `is_backup`) VALUES ";
					for (int i = 0; i < invMeta.getLore().size(); i++) {
						q_meta += "('" + invSlotID + "', ?, ?, 0),";
						if (Config.backup_enabled) {
							q_meta += "('" + invSlotID + "', ?, ?, 1),";
						}
					}
					q_meta = q_meta.substring(0, q_meta.length() - 1);
					//InventorySQL.d(q_meta);

					PreparedStatement prep = conn.prepareStatement(q_meta);
					int i = 0;
					int n = 2;
					for (String l : invMeta.getLore()) {
						if (Config.backup_enabled) {
							n = 4;
							prep.setString(i*n + 3, "Lore_" + (i+1));
							prep.setString(i*n + 4, l);
						}
						prep.setString(i*n + 1, "Lore_" + (i+1));
						prep.setString(i*n + 2, l);
						i++;
					}
					InventorySQL.d(prep.toString());
					prep.executeUpdate();
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
		PreparedStatement statement = getConn().prepareStatement(
				"UPDATE `" + Config.dbTable_Pendings
						+ "` SET `count`=? WHERE `id`=?");
		statement.setInt(1, left);
		statement.setString(2, item_id);
		statement.executeUpdate();
	}

	private int giveItem(final ItemStack item, final Player p) {
		InventorySQL.d(this.hashCode() + " => giveItem:" + item.toString());
		try {
			HashMap<Integer, ItemStack> m = getCoreSQL().callSyncMethod(
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
			HashMap<Integer, ItemStack> m = getCoreSQL().callSyncMethod(
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
