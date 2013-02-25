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

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.SkullMeta;

import fr.areku.InventorySQL.Config;
import fr.areku.InventorySQL.InventorySQL;
import fr.areku.InventorySQL.PlayerManager;
import fr.areku.InventorySQL.database.CoreSQL;
import fr.areku.InventorySQL.database.JDCConnection;
import fr.areku.InventorySQL.database.SQLItemStack;
import fr.areku.InventorySQL.database.SQLItemStack.Action;
import fr.areku.InventorySQL.database.SQLMethod;

public class PlayerCheck extends SQLMethod {
	private boolean doGive = true;

	private String updatesql_inventory = "";
	private String updatesql_ench = "";
	private String updatesql_meta = "";
	private boolean commitRequired = false;

	public PlayerCheck(String initiator, CommandSender cs) {
		super(initiator, cs);
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
				targetPlayers = InventorySQL.getInstance()
						.getOnlinePlayersSync();
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}

		if (targetPlayers.length == 0) {
			InventorySQL.d(this.hashCode() + " => noPlayers");
			return;
		}

		InventorySQL.d(this.hashCode() + " => targetPlayers:"
				+ targetPlayers.length);
		Map<String, String> players_to_remove = new HashMap<String, String>();
		// StringBuilder players_to_remove_sb = new StringBuilder();

		prepareSQLCommit();

		for (Player p : targetPlayers) {
			InventorySQL.d(this.hashCode() + " => checkPlayers:" + p.getName());
			String userName = p.getName();

			try {
				if (InventorySQL.isUsingAuthenticator()) {
					if (!fr.areku.Authenticator.Authenticator
							.isPlayerLoggedIn(p)) {
						InventorySQL.d(this.hashCode() + " => checkPlayers:"
								+ userName + " !UNAUTHORIZED");
						continue;
					}
				}
				if (!Config.gamemode.contains(p.getGameMode())) {
					InventorySQL.d(this.hashCode() + " => checkPlayers:"
							+ userName + " !GAMEMODE");
					return;
				}

				int userID = PlayerManager.getInstance().get(userName)
						.getSqlId();
				String userWorld = p.getWorld().getName();
				if (userID == -1) {
					InventorySQL
							.d(this.hashCode() + " => UserID is not cached");
					PreparedStatement sth = getConn().prepareStatement(
							"SELECT `id` FROM `" + Config.dbTable_Users
									+ "` WHERE UPPER(`name`) = UPPER(?);");
					sth.setString(1, userName);
					ResultSet rs = sth.executeQuery();
					if (rs.first()) {
						userID = rs.getInt(1);
					} else {
						InventorySQL.d(this.hashCode()
								+ " => Creating entry for " + userName);
						userID = executeInsert(getConn().prepareStatement(
								"INSERT INTO `" + Config.dbTable_Users
										+ "`(`name`, `password`) VALUES ('"
										+ userName + "','')",
								Statement.RETURN_GENERATED_KEYS));
						PlayerManager.getInstance().get(userName)
								.updateHash("");
					}
					rs.close();
					sth.close();
					PlayerManager.getInstance().get(userName).setSqlId(userID);
				}

				InventorySQL.d(this.hashCode() + " => userID: " + userID);

				int added = 0;
				int removed = 0;

				/*******************************/
				/** MIRROR-MODE ************/
				/*******************************/
				if (Config.mirrorMode) {
					boolean fs = PlayerManager.getInstance().get(userName)
							.isFirstSessionCheck();
					InventorySQL.d(this.hashCode()
							+ " => isFirstSessionCheck: " + fs);
					if (doMirroring(userID, p)) {
						InventorySQL.d(this.hashCode()
								+ " => MirrorEndAlreadyLatest");
						if (fs)
							p.sendMessage(ChatColor.RED + "[InventorySQL] "
									+ InventorySQL.getMessage("mirror-latest"));
					} else {
						InventorySQL
								.d(this.hashCode() + " => MirrorEndUpdated");
						if (fs)
							p.sendMessage(ChatColor.RED + "[InventorySQL] "
									+ InventorySQL.getMessage("mirror-done"));
					}
				}

				if (CoreSQL.getInstance().isDatabaseReady() && doGive) {
					String q = "SELECT `pendings`.`id` AS `p_id`, `pendings`.`item` AS `item`, `pendings`.`data` AS `data`, `pendings`.`damage` AS `damage`, `pendings`.`count` AS `count`, `enchantments`.`ench` AS `ench`, `enchantments`.`level` AS `level`, `meta`.`key` AS `meta_key`, `meta`.`value` AS `meta_value`"
							+ " FROM `"
							+ Config.dbTable_Pendings
							+ "` as `pendings`"
							+ " LEFT JOIN `"
							+ Config.dbTable_Enchantments
							+ "` AS `enchantments` ON `pendings`.`id` = `enchantments`.`id`";
					if (Config.backup_enabled)
						q += " AND (`enchantments`.`is_backup` != 1 OR `enchantments`.`is_backup` IS NULL)";

					q += " LEFT JOIN `" + Config.dbTable_Meta
							+ "` AS `meta` ON `pendings`.`id` = `meta`.`id`";
					if (Config.backup_enabled)
						q += " AND (`meta`.`is_backup` != 1 OR `meta`.`is_backup` IS NULL)";

					q += " WHERE (`pendings`.`owner` = ?";
					if (Config.multiworld)
						q += " AND `pendings`.`world` = ?";

					q += ");";
					
					InventorySQL.d(q);

					PreparedStatement sth = getConn().prepareStatement(q);
					sth.setInt(1, userID);
					if (Config.multiworld)
						sth.setString(2, p.getWorld().getName());

					ResultSet rs = sth.executeQuery();

					if (rs.first()) {
						InventorySQL.d(this.hashCode()
								+ " => pendings items for " + userName);

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
							InventorySQL.d(this.hashCode() + " => stack: "
									+ stack.toString());
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
						InventorySQL.d(this.hashCode()
								+ " => donePendings:"+donePendings);

						if (donePendings != "") {
							InventorySQL
									.d(this.hashCode()
											+ " => checkPlayers:PendingsDone:RemovingEntries:"
											+ donePendings);
							getConn().createStatement().executeUpdate(
									"DELETE FROM `"
											+ Config.dbTable_Enchantments
											+ "` WHERE `id` IN ("
											+ donePendings + "0);");
							getConn().createStatement().executeUpdate(
									"DELETE FROM `" + Config.dbTable_Pendings
											+ "` WHERE `id` IN ("
											+ donePendings + "0);");
							getConn().createStatement().executeUpdate(
									"DELETE FROM `" + Config.dbTable_Meta
											+ "` WHERE `id` IN ("
											+ donePendings + "0);");
						}
						sendMessage("[InventorySQL] "
								+ ChatColor.GREEN
								+ "("
								+ userName
								+ ") "
								+ InventorySQL.getMessage("modif", added,
										removed, stackList.size()));

					} else {
						sendMessage("[InventorySQL] " + ChatColor.GREEN + "("
								+ userName + ") "
								+ InventorySQL.getMessage("no-modif"));

					}
					rs.close();
					sth.close();
				}

				PlayerManager.getInstance().get(userName)
						.updateEpoch(getCurrentCheckEpoch());
				String theInvHash = PlayerManager.computePlayerInventoryHash(p);
				if (PlayerManager.getInstance().get(userName)
						.updateHash(theInvHash)) {
					InventorySQL.d(this.hashCode()
							+ " => checkPlayers:InventoryModified");

					if (!players_to_remove.containsKey(userWorld))
						players_to_remove.put(userWorld, "");
					players_to_remove.put(userWorld,
							players_to_remove.get(userWorld) + userID + ",");
					InventorySQL.d(this.hashCode()
							+ " => checkPlayers:AddedToGC:" + userName);

					// update standart slots
					for (Integer invSlotID = 0; invSlotID < 36; invSlotID++) {
						updateSQL(p, userID, null, invSlotID);
					}
					// moar slots !
					updateSQL(p, userID, p.getInventory().getBoots(), 100);
					updateSQL(p, userID, p.getInventory().getLeggings(), 101);
					updateSQL(p, userID, p.getInventory().getChestplate(), 102);
					updateSQL(p, userID, p.getInventory().getHelmet(), 103);
				} else {
					InventorySQL.d(this.hashCode()
							+ " => checkPlayers:InventoryNotModified");
				}
				p.saveData();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} // end for

		try {
			doSQLCommit(getConn());
		} catch (SQLException e1) {
			e1.printStackTrace();
		}

		if (players_to_remove.size() > 0) {
			String q = "DELETE `inventories`, `enchantments`, `meta` FROM `"
					+ Config.dbTable_Inventories
					+ "` AS `inventories` LEFT JOIN `"
					+ Config.dbTable_Enchantments
					+ "` AS `enchantments` ON `inventories`.`id` = `enchantments`.`id`";
			if (Config.backup_enabled)
				q += " AND (`enchantments`.`is_backup` != 1)";

			q += " LEFT JOIN `" + Config.dbTable_Meta
					+ "` AS `meta` ON `inventories`.`id` = `meta`.`id`";
			if (Config.backup_enabled)
				q += " AND (`meta`.`is_backup` != 1)";
			
			q += " WHERE";

			if (Config.multiworld) {
				for (Entry<String, String> ptr : players_to_remove.entrySet()) {
					InventorySQL.d("MW:" + ptr.getKey() + ":" + ptr.getValue());
					String w = q
							+ " (`inventories`.`owner` IN ("
							+ ptr.getValue()
							+ "0) AND `inventories`.`date` != ? AND `inventories`.`world` = ?)";

					try {
						PreparedStatement sth = getConn().prepareStatement(w);
						sth.setLong(1, getCurrentCheckEpoch());
						// sth.setString(1, final_players_id);

						sth.setString(2, ptr.getKey());

						sth.executeUpdate();
						sth.close();
						InventorySQL
								.d(this.hashCode()
										+ " => Cleaning:OldRecordsOfinventories:Multiworld");
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			} else {
				String all = "";
				for (String ptr : players_to_remove.values())
					all += ptr;
				InventorySQL.d("No MW:" + all);

				String w = q
						+ " (`inventories`.`owner` IN ("
						+ all
						+ "0) AND `inventories`.`date` != ? AND `inventories`.`world` = ?)";

				try {
					PreparedStatement sth = getConn().prepareStatement(w);
					sth.setLong(1, getCurrentCheckEpoch());

					sth.executeUpdate();
					sth.close();
					InventorySQL
							.d(this.hashCode()
									+ " => Cleaning:OldRecordsOfinventories:NoMultiworld");
				} catch (SQLException e) {
					e.printStackTrace();
				}
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
			q += " AND ((`enchantments`.`is_backup` != 1 OR `enchantments`.`is_backup` IS NULL) AND (`meta`.`is_backup` != 1 OR `meta`.`is_backup` IS NULL))";

		q += ");";

		InventorySQL.d(q + "//"
				+ PlayerManager.getInstance().get(p.getName()).getEpoch()
				+ "//" + userID);

		String worldName = p.getWorld().getName();

		PreparedStatement sth = getConn().prepareStatement(q);
		sth.setLong(1, PlayerManager.getInstance().get(p.getName()).getEpoch());
		sth.setInt(2, userID);
		if (Config.multiworld)
			sth.setString(3, worldName);

		ResultSet rs = sth.executeQuery();

		if (rs.first()) {
			InventorySQL.d(this.hashCode() + " => Mirroring:SQLMoreRecent");
			/*
			 * InventorySQL.d(this.hashCode() +
			 * " => Mirroring:TODO:Fetch inv from SQL");
			 */
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
			updateSQLTime(userID, worldName);
			r = false;
		} else {
			InventorySQL.d(this.hashCode()
					+ " => Mirroring:LocalMoreRecentOrNoSQLDATA");
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

	private void updateSQLTime(int userID, String worldName)
			throws SQLException {
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
			sth2.setString(4, worldName);
		sth2.executeUpdate();
		InventorySQL.d(this.hashCode() + " => Mirroring:SQLTimeUpdated");
	}

	private void prepareSQLCommit() {
		InventorySQL.d(this.hashCode() + " => prepareCommit");
		this.updatesql_inventory = "";
		this.updatesql_ench = "";
		this.updatesql_meta = "";
	}

	private void doSQLCommit(JDCConnection conn) throws SQLException {
		if (!this.commitRequired) {
			InventorySQL.d(this.hashCode() + " => doSQLCommit NOT REQUIRED");
			return;
		}
		InventorySQL.d(this.hashCode() + " => doSQLCommit");
		/* Inventory */
		if (this.updatesql_inventory.length() > 0) {
			this.updatesql_inventory = "INSERT INTO `%TABLE%`(`id`, `owner`, `world`, `item`, `data`,`damage`, `count`, `slot`, `event`, `date`, `suid`) VALUES "
					+ this.updatesql_inventory.substring(0,
							this.updatesql_inventory.length() - 1);

			conn.prepareStatement(
					this.updatesql_inventory.replace("%TABLE%",
							Config.dbTable_Inventories)).executeUpdate();
			if (Config.backup_enabled) {
				conn.prepareStatement(
						this.updatesql_inventory.replace("%TABLE%",
								Config.dbTable_Backups)).executeUpdate();
			}
		}

		/* ench */
		if (this.updatesql_ench.length() > 0) {
			this.updatesql_ench = "INSERT INTO `"
					+ Config.dbTable_Enchantments
					+ "`(`id`, `ench_index`, `ench`, `level`, `is_backup`) VALUES "
					+ this.updatesql_ench.substring(0,
							this.updatesql_ench.length() - 1);

			conn.prepareStatement(this.updatesql_ench).executeUpdate();
		}

		/* meta */
		if (this.updatesql_meta.length() > 0) {
			this.updatesql_meta = "INSERT INTO `"
					+ Config.dbTable_Meta
					+ "`(`id`, `key`, `value`, `is_backup`) VALUES "
					+ this.updatesql_meta.substring(0,
							this.updatesql_meta.length() - 1);

			conn.prepareStatement(this.updatesql_meta).executeUpdate();
		}
	}

	private void updateSQL(Player p, int userID, ItemStack stack, int slotID) {
		this.commitRequired = true;
		InventorySQL.d(this.hashCode() + " => updateSQL:" + p.getName()
				+ ":slotid=" + slotID);
		ItemStack invSlotItem;
		try {
			invSlotItem = p.getInventory().getItem(slotID);
		} catch (Exception ex) {
			invSlotItem = stack;
		}

		if (invSlotItem != null) {
			String invSlotID = UUID.randomUUID().toString();
			this.updatesql_inventory += "('" + invSlotID + "', " + userID
					+ ", '" + p.getWorld().getName() + "', "
					+ invSlotItem.getTypeId() + ", "
					+ invSlotItem.getData().getData() + ", "
					+ invSlotItem.getDurability() + ", "
					+ invSlotItem.getAmount() + ", " + slotID + ", '"
					+ this.getInitiator() + "', " + getCurrentCheckEpoch()
					+ ", '" + Config.serverUID + "'),";
			/*
			 * InventorySQL.d(this.hashCode() + " => InsertItemFromInvCommand:"
			 * + q_item); InventorySQL.d(this.hashCode() +
			 * " => InsertItemFromInv:" + invSlotID);
			 */

			ItemMeta invMeta = invSlotItem.getItemMeta();
			Map<Enchantment, Integer> enchMap = null;
			if (invMeta instanceof EnchantmentStorageMeta) {
				enchMap = ((EnchantmentStorageMeta) invMeta)
						.getStoredEnchants();
			} else if (invMeta.hasEnchants()) {
				enchMap = invMeta.getEnchants();
			}

			if (enchMap != null) {
				int i = 0;
				for (Entry<Enchantment, Integer> e : enchMap.entrySet()) {
					this.updatesql_ench += "('" + invSlotID + "', " + i + ", "
							+ e.getKey().getId() + ", " + e.getValue()
							+ ", 0),";
					if (Config.backup_enabled) {
						this.updatesql_ench += "('" + invSlotID + "', " + i
								+ ", " + e.getKey().getId() + ", "
								+ e.getValue() + ", 1),";
					}
					i++;
				}
			}
			if (invMeta.getDisplayName() != null) {
				this.updatesql_meta += "('" + invSlotID + "', 'DisplayName', '"
						+ invMeta.getDisplayName() + "', 0),";
				if (Config.backup_enabled) {
					this.updatesql_meta += "('" + invSlotID
							+ "', 'DisplayName', '" + invMeta.getDisplayName()
							+ "', 1),";
				}
			}
			if (invMeta.getLore() != null)
				if (!invMeta.getLore().isEmpty()) {
					int i = 0;
					for (String l : invMeta.getLore()) {
						this.updatesql_meta += "('" + invSlotID + "', 'Lore_"
								+ (i + 1) + "', '" + l + "', 0),";
						if (Config.backup_enabled) {
							this.updatesql_meta += "('" + invSlotID
									+ "', 'Lore_" + (i + 1) + "', '" + l
									+ "', 1),";
						}
					}
				}

			/* handle for special items */
			if (invMeta instanceof SkullMeta) {
				this.updatesql_meta += "('" + invSlotID + "', 'Owner', '"
						+ ((SkullMeta) invMeta).getOwner() + "', 0),";
				if (Config.backup_enabled) {
					this.updatesql_meta += "('" + invSlotID + "', 'Owner', '"
							+ ((SkullMeta) invMeta).getOwner() + "', 1),";
				}
			} else if (invMeta instanceof MapMeta) {
				this.updatesql_meta += "('" + invSlotID + "', 'Scaling', '"
						+ ((MapMeta) invMeta).isScaling() + "', 0),";
				if (Config.backup_enabled) {
					this.updatesql_meta += "('" + invSlotID + "', 'Scaling', '"
							+ ((MapMeta) invMeta).isScaling() + "', 1),";
				}
			}

			/*
			 * switch (invSlotItem.getType()) { case MONSTER_EGG:
			 * this.updatesql_meta += "('" + invSlotID + "', 'SpawnedType', '" +
			 * ((SpawnEgg) invMeta).getSpawnedType().getTypeId() + "', 0),";
			 * break; case INK_SACK: this.updatesql_meta += "('" + invSlotID +
			 * "', 'Color', '" + ((Dye) invMeta).getColor().getDyeData() +
			 * "', 0),"; break; case WOOD: case SAPLING: case LOG: case LEAVES:
			 * this.updatesql_meta += "('" + invSlotID + "', 'Species', '" +
			 * ((Tree) invMeta).getSpecies().getData() + "', 0),"; break; case
			 * WOOD_DOUBLE_STEP: case WOOD_STEP: this.updatesql_meta += "('" +
			 * invSlotID + "', 'Species', '" + ((WoodenStep)
			 * invMeta).getSpecies().getData() + "', 0),"; break; case
			 * SANDSTONE: this.updatesql_meta += "('" + invSlotID +
			 * "', 'Type', '" + ((Sandstone) invMeta).getType().getData() +
			 * "', 0),"; break; case COAL: this.updatesql_meta += "('" +
			 * invSlotID + "', 'Type', '" + ((Coal) invMeta).getType().getData()
			 * + "', 0),"; break; case WOOL: this.updatesql_meta += "('" +
			 * invSlotID + "', 'Color', '" + ((Wool)
			 * invMeta).getColor().getWoolData() + "', 0),"; break; default:
			 * break; }
			 */
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
			HashMap<Integer, ItemStack> m = InventorySQL
					.getInstance()
					.callSyncMethod(
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
			HashMap<Integer, ItemStack> m = InventorySQL
					.getInstance()
					.callSyncMethod(
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
