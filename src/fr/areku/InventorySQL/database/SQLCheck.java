package fr.areku.InventorySQL.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import fr.areku.InventorySQL.Config;
import fr.areku.InventorySQL.Main;

public class SQLCheck implements Runnable {
	private CoreSQLProcess parent;
	private CoreSQLItem runThis = null;
	private boolean doGive = true;
	private boolean manualCheck = false;
	private JDCConnection conn = null;

	public SQLCheck(CoreSQLProcess parent) {
		Main.d("New SQLCheck !");
		this.parent = parent;
	}

	public SQLCheck manualCheck(CoreSQLItem i, boolean doGive) {
		this.runThis = i;
		this.doGive = doGive;
		return manualCheck();
	}

	public SQLCheck manualCheck() {
		Main.d("SQLCheck->ManualCheck");
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
		try {
			if (manualCheck) {
				Main.d("Running manual check");
			} else {
				Main.d("Running scheduled check");
			}
			Main.d("id: " + this.hashCode());

			if (manualCheck && (runThis != null)) {
				/*if (runThis.hasChestData() && Config.checkChest) {
					checkChests(runThis);
				}*/
				if (runThis.hasPlayersData()) {
					checkPlayers(runThis);
				}
			} else {
				/*
				 * List<Chest> cList = new ArrayList<Chest>(); if
				 * (Config.checkChest) { ResultSet r; r =
				 * conn.createStatement().executeQuery(
				 * "SELECT `id`, `world`, `x`, `y`, `z`, `pendings` FROM `" +
				 * Config.dbTable + "` WHERE `ischest`= 1;"); List<Integer>
				 * rmList = new ArrayList<Integer>(); while (r.next()) { World w
				 * = this.parent.plugin.getServer().getWorld( r.getString(2));
				 * if (w == null) continue; Location l = new Location(w,
				 * r.getInt(3), r.getInt(4), r.getInt(5)); if
				 * (w.getBlockTypeIdAt(l) == Material.CHEST.getId()) {
				 * cList.add((Chest) w.getBlockAt(l).getState()); } else {
				 * rmList.add(r.getInt(1)); } } if (rmList.size() > 0) {
				 * conn.createStatement().executeUpdate( "DELETE FROM`" +
				 * Config.dbTable + "` WHERE id=" +
				 * StringUtils.join(rmList.toArray(), " OR id=")); }
				 * 
				 * if (cList.size() > 0) { checkChests(new
				 * CoreSQLItem(cList.toArray(new Chest[0]))); } else {
				 * Main.d("No chests to check"); } }
				 */

				Player[] pList = this.parent.getOnlinePlayers();
				if (pList.length > 0) {
					checkPlayers(new CoreSQLItem(pList));
				} else {
					Main.d("No players to check");
				}
			}
		} catch (Exception ex) {
			Main.logException(ex, "exception in playerlogic - check all");
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
		for (Player p : i.getPlayers()) {
			if (this.parent.plugin.getOfflineModeController()
					.isUsingOfflineModePlugin()
					&& !this.parent.plugin.getOfflineModeController()
							.isPlayerLoggedIn(p.getName())) {
				Main.d(this.hashCode() + " => checkPlayers:" + p.getName()
						+ " !UNAUTHORIZED");
				continue;
			}
			Main.d(this.hashCode() + " => checkPlayers:" + p.getName());
			if ((Config.noCreative) && (p.getGameMode() == GameMode.CREATIVE)) {
				return;
			}

			PreparedStatement sth = conn.prepareStatement("SELECT `id` FROM `"
					+ Config.dbTable_Users + "` WHERE `name` = ?");
			sth.setString(1, p.getName());
			ResultSet rs = sth.executeQuery();
			Integer userID = -1;
			if (rs.first()) {
				userID = rs.getInt(1);
			} else {
				Main.d(this.hashCode() + " => Creating entry for "
						+ p.getName());
				userID = executeInsert(conn.prepareStatement("INSERT INTO `"
						+ Config.dbTable_Users
						+ "`(`name`, `password`) VALUES ('" + p.getName()
						+ "','')", Statement.RETURN_GENERATED_KEYS));
			}
			rs.close();
			sth.close();

			Main.d(this.hashCode() + " => userID: " + userID);

			int added = 0;
			int removed = 0;
			int pendings = 0;

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
					Main.d(this.hashCode() + " => pendings items for "
							+ p.getName());

					String donePendings = "";

					int latest_id = -1;
					int latest_count = 0;
					ItemStack latest_stack = null;
					int ench_id;
					do {
						if (latest_id != rs.getInt("p_id")) {
							if (latest_id != -1) {
								pendings++;
								Main.d("c:" + latest_count);
								int left = 0;
								if (latest_count > 0) {
									left = giveItem(latest_stack, p);
									if (left == 0) {
										donePendings += latest_id + ",";
										added++;
									}
								} else if (latest_count < 0) {
									left = (-1) * removeItem(latest_stack, p);
									if (left == 0) {
										donePendings += latest_id + ",";
										removed++;
									}
								}
								if (left != 0)
									executeItemsLeft(latest_id, left);
							}
							latest_id = rs.getInt("p_id");
							latest_count = rs.getInt("count");
							latest_stack = new ItemStack(rs.getInt("item"),
									Math.abs(latest_count),
									(short) rs.getInt("damage"),
									rs.getByte("data"));
							Main.d(this.hashCode()
									+ " => checkPlayers:NewItemStack:"
									+ latest_stack.toString());
							ench_id = rs.getInt("ench");
							if (!rs.wasNull()) {
								Enchantment e = Enchantment.getById(ench_id);
								int ench_level = rs.getInt(7);
								if ((e != null) && !rs.wasNull()) {
									Main.d(this.hashCode()
											+ " => checkPlayers:" + latest_id
											+ ":NewEnch:" + e.toString() + "/"
											+ ench_level);
									try {
										if (Config.allow_unsafe_ench) {
											latest_stack.addUnsafeEnchantment(
													e, ench_level);
										} else {
											latest_stack.addEnchantment(e,
													ench_level);
										}
									} catch (Exception ex) {
										Main.log(
												Level.WARNING,
												"Error while adding "
														+ e.getName()
														+ "/"
														+ ench_level
														+ " to "
														+ latest_stack
																.toString());
										Main.log(Level.WARNING,
												ex.getLocalizedMessage());
									}
								}
							}
						} else {
							ench_id = rs.getInt(6);
							if (!rs.wasNull()) {
								Enchantment e = Enchantment.getById(ench_id);
								int ench_level = rs.getInt(7);
								if ((e != null) && !rs.wasNull()) {
									Main.d(this.hashCode()
											+ " => checkPlayers:" + latest_id
											+ ":NewEnch:" + e.toString() + "/"
											+ ench_level);
									try {
										if (Config.allow_unsafe_ench) {
											latest_stack.addUnsafeEnchantment(
													e, ench_level);
										} else {
											latest_stack.addEnchantment(e,
													ench_level);
										}
									} catch (Exception ex) {
										Main.log(
												Level.WARNING,
												"Error while adding "
														+ e.getName()
														+ "/"
														+ ench_level
														+ " to "
														+ latest_stack
																.toString());
										Main.log(Level.WARNING,
												ex.getLocalizedMessage());
									}
								}
							}
						}
					} while (rs.next());
					pendings++;
					Main.d("c:" + latest_count);
					int left = 0;
					if (latest_count > 0) {
						left = giveItem(latest_stack, p);
						if (left == 0) {
							donePendings += latest_id + ",";
							added++;
						}
					} else if (latest_count < 0) {
						left = (-1) * removeItem(latest_stack, p);
						if (left == 0) {
							donePendings += latest_id + ",";
							removed++;
						}
					}
					if (left != 0)
						executeItemsLeft(latest_id, left);

					if (donePendings.endsWith(","))
						donePendings = donePendings.substring(0,
								donePendings.length() - 1);

					Main.d(this.hashCode() + " => checkPlayers:PendingsDone:+"
							+ added + "/-" + removed + " of " + pendings);
					if (donePendings != "") {
						Main.d(this.hashCode()
								+ " => checkPlayers:PendingsDone:RemovingEntries:"
								+ donePendings);
						conn.createStatement().executeUpdate(
								"DELETE FROM `" + Config.dbTable_Enchantments
										+ "` WHERE `owner` IN (" + donePendings
										+ ");");
						conn.createStatement().executeUpdate(
								"DELETE FROM `" + Config.dbTable_Pendings
										+ "` WHERE `id` IN (" + donePendings
										+ ");");
					}
					if (i.getCommandSender() != null) {
						i.getCommandSender().sendMessage(
								"[InventorySQL] "
										+ ChatColor.GREEN
										+ "("
										+ p.getName()
										+ ") "
										+ Main.getMessage("modif", added,
												removed, pendings));
					}

				} else {

					if (i.getCommandSender() != null) {
						i.getCommandSender().sendMessage(
								"[InventorySQL] " + ChatColor.GREEN + "("
										+ p.getName() + ") "
										+ Main.getMessage("no-modif"));
					}

				}
				rs.close();
				sth.close();
			}
			if (this.parent.isPlayerInventoryModified(p)) {
				if (!Config.backup_enabled) {
					/* Clear old inv */

					String q = "DELETE `inventories`, `enchantments` FROM `"
							+ Config.dbTable_Inventories
							+ "` AS `inventories` LEFT JOIN `"
							+ Config.dbTable_Enchantments
							+ "` AS `enchantments` ON `inventories`.`id` = `enchantments`.`id`"
							+ " WHERE (`inventories`.`owner` = ?";
					if (Config.multiworld)
						q += " AND `inventories`.`world` = ?";

					q += ");";
					sth = conn.prepareStatement(q);
					sth.setInt(1, userID);
					if (Config.multiworld)
						sth.setString(2, p.getWorld().getName());
					sth.executeUpdate();
					sth.close();

				}

				for (Integer invSlotID = 0; invSlotID < 36; invSlotID++) {
					updateSQL(p, userID, null, invSlotID, conn);
				}
				updateSQL(p, userID, p.getInventory().getBoots(), 100, conn);
				updateSQL(p, userID, p.getInventory().getLeggings(), 101, conn);
				updateSQL(p, userID, p.getInventory().getChestplate(), 102,
						conn);
				updateSQL(p, userID, p.getInventory().getHelmet(), 103, conn);
				// Main.d(this.hashCode() + " => checkPlayers:Inventory:" + q);
			}else{
				Main.d(this.hashCode() + " => checkPlayers:InventoryNotModified");
			}
		}
	}

	private void updateSQL(Player p, int userID, ItemStack stack, int slotID,
			JDCConnection conn) throws SQLException {
		String base_query = "INSERT INTO `"
				+ Config.dbTable_Inventories
				+ "`(`id`, `owner`, `world`, `item`, `data`,`damage`, `count`, `slot`) VALUES ";
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
					+ invSlotItem.getAmount() + ", " + slotID + ")";

			conn.prepareStatement(q_item).executeUpdate();

			Main.d(this.hashCode() + " => InsertItemFromInvCommand:" + q_item);
			Main.d(this.hashCode() + " => InsertItemFromInv:" + invSlotID);

			if (!invSlotItem.getEnchantments().isEmpty()) {

				String q_ench = "INSERT INTO `" + Config.dbTable_Enchantments
						+ "`(`id`, `ench_index`, `ench`, `level`) VALUES ";
				int i = 0;
				for (Entry<Enchantment, Integer> e : invSlotItem
						.getEnchantments().entrySet()) {
					q_ench += "('" + invSlotID + "', " + i + ", "
							+ e.getKey().getId() + ", " + e.getValue() + "),";
					i++;
				}
				q_ench = q_ench.substring(0, q_ench.length() - 1);
				conn.prepareStatement(q_ench).executeUpdate();
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

	private void executeItemsLeft(int item_id, int left) throws SQLException {
		PreparedStatement statement = conn.prepareStatement("UPDATE `"
				+ Config.dbTable_Pendings + "` SET `count`=? WHERE `id`=?");
		statement.setInt(1, left);
		statement.setInt(2, item_id);
		statement.executeUpdate();
	}

	private int giveItem(final ItemStack item, final Player p) {
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
