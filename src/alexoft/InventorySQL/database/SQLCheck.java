package alexoft.InventorySQL.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import alexoft.InventorySQL.ActionStack;
import alexoft.InventorySQL.Config;
import alexoft.InventorySQL.Constants;
import alexoft.InventorySQL.Main;

public class SQLCheck implements Runnable {
	private CoreSQLProcess parent;
	private CoreSQLItem runThis = null;
	private boolean manualCheck = false;
	private JDCConnection conn = null;

	public SQLCheck(CoreSQLProcess parent) {
		Main.d("New SQLCheck !");
		this.parent = parent;
	}

	public SQLCheck manualCheck(CoreSQLItem i) {
		this.runThis = i;
		return manualCheck();
	}

	public SQLCheck manualCheck() {
		Main.d("SQLCheck->ManualCheck");
		manualCheck = true;
		return this;
	}

	private void updateConn() {
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
			if(manualCheck){
				Main.d("Running manual check");
			}else{
				Main.d("Running scheduled check");
			}
			
			if (manualCheck && (runThis != null)) {
				if (runThis.hasChestData() && Config.checkChest) {
					checkChests(runThis);
				}
				if (runThis.hasPlayersData()) {
					checkPlayers(runThis);
				}
			} else {

				List<Chest> cList = new ArrayList<Chest>();
				if (Config.checkChest) {
					ResultSet r;
					r = conn.createStatement().executeQuery(
							"SELECT `id`, `world`, `x`, `y`, `z`, `pendings` FROM `"
									+ Config.dbTable + "` WHERE `ischest`= 1;");
					List<Integer> rmList = new ArrayList<Integer>();
					while (r.next()) {
						World w = this.parent.plugin.getServer().getWorld(
								r.getString(2));
						if (w == null)
							continue;
						Location l = new Location(w, r.getInt(3), r.getInt(4),
								r.getInt(5));
						if (w.getBlockTypeIdAt(l) == Material.CHEST.getId()) {
							cList.add((Chest) w.getBlockAt(l).getState());
						} else {
							rmList.add(r.getInt(1));
						}
					}
					if (rmList.size() > 0) {
						conn.createStatement().executeUpdate(
								"DELETE FROM`"
										+ Config.dbTable
										+ "` WHERE id="
										+ StringUtils.join(rmList.toArray(),
												" OR id="));
					}

					if (cList.size() > 0)
						checkChests(new CoreSQLItem(cList.toArray(new Chest[0])));
				}

				Player[] pList = this.parent.getOnlinePlayers();
				if (pList.length > 0)
					checkPlayers(new CoreSQLItem(pList));
			}
		} catch (Exception ex) {
			Main.logException(ex, "exception in playerlogic - check all");
		}

		if (manualCheck) {
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
		for (final Player p : i.getPlayers()) {
			try {
				if ((Config.noCreative)
						&& (p.getGameMode() == GameMode.CREATIVE)) {
					return;
				}

				ResultSet r;
				int added = 0;
				int removed = 0;
				int pendings = 0;
				String invData = "";

				String q = "SELECT * FROM `" + Config.dbTable
						+ "` WHERE LOWER(`owner`) = LOWER('" + p.getName()
						+ "')";
				if (Config.multiworld) {
					q += " AND `world` = '" + p.getWorld().getName() + "';";
				} else {
					q += ";";
				}
				r = conn.createStatement().executeQuery(q);

				if (r.first()) {
					List<ActionStack> fullInv = new ArrayList<ActionStack>();
					String pendingData = r.getString("pendings");

					if (!"".equals(pendingData)) {
						Main.log(Level.WARNING,
								"pendings items for " + p.getName());

						final List<ItemStack> addStack = new ArrayList<ItemStack>();
						final List<ItemStack> removeStack = new ArrayList<ItemStack>();

						for (ActionStack pendingStack : CoreSQLProcess
								.buildPendList(pendingData)) {
							if ("+".equals(pendingStack.params())) {
								addStack.add(pendingStack.item());
							} else if ("-".equals(pendingStack.params())) {
								removeStack.add(pendingStack.item());
							} else {
								Main.log(Level.INFO, "bad command '"
										+ pendingStack.params()
										+ "' for player '" + p.getName()
										+ "' in pendings data, ignored");
							}

							Future<HashMap<Integer, ItemStack>> f = this.parent
									.callSyncMethod(new Callable<HashMap<Integer, ItemStack>>() {
										@Override
										public HashMap<Integer, ItemStack> call()
												throws Exception {
											return p.getInventory()
													.removeItem(
															removeStack
																	.toArray(new ItemStack[0]));
										}
									});
							for (Entry<Integer, ItemStack> e : f.get()
									.entrySet()) {
								fullInv.add(new ActionStack(e.getValue(), "-"));
							}
							pendings += f.get().size();
							removed += removeStack.size() - f.get().size();

							f = this.parent
									.callSyncMethod(new Callable<HashMap<Integer, ItemStack>>() {
										@Override
										public HashMap<Integer, ItemStack> call()
												throws Exception {
											return p.getInventory()
													.addItem(
															addStack.toArray(new ItemStack[0]));
										}
									});
							for (Entry<Integer, ItemStack> e : f.get()
									.entrySet()) {
								fullInv.add(new ActionStack(e.getValue(), "+"));
							}
							pendings += f.get().size();
							added += addStack.size() - f.get().size();
						}

						if (i.getCommandSender() != null) {
							i.getCommandSender().sendMessage(
									"[InventorySQL] "
											+ ChatColor.GREEN
											+ "("
											+ p.getName()
											+ ") "
											+ Main.getMessage("modif", removed,
													added, pendings));
						}

					} else {

						if (i.getCommandSender() != null) {
							i.getCommandSender().sendMessage(
									"[InventorySQL] " + ChatColor.GREEN + "("
											+ p.getName() + ") "
											+ Main.getMessage("no-modif"));
						}

					}

					invData = CoreSQLProcess.buildInvString(p.getInventory());

					if (fullInv.size() != 0) {
						Main.log(Level.WARNING, "\t Unable to add/remove "
								+ fullInv.size() + " item(s)");
					}

					conn.createStatement().executeUpdate(
							String.format(
									Constants.REQ_UPDATE_INV,
									Config.dbTable,
									invData,
									(fullInv.isEmpty() ? "" : CoreSQLProcess
											.buildPendString(fullInv)), p
											.getLocation().getBlockX(), p
											.getLocation().getBlockY(), p
											.getLocation().getBlockZ(), r
											.getInt("id")));

				} else {
					invData = CoreSQLProcess.buildInvString(p.getInventory());

					conn.createStatement().executeUpdate(
							String.format(Constants.REQ_INSERT_INV,
									Config.dbTable, p.getName(), p.getWorld()
											.getName(), invData, p
											.getLocation().getBlockX(), p
											.getLocation().getBlockY(), p
											.getLocation().getBlockZ()));

					conn.createStatement().executeUpdate(
							String.format(Constants.REQ_INSERT_USER,
									Config.dbTable, p.getName()));
				}
			} catch (Exception ex) {
				Main.logException(ex,
						"exception in playerlogic - check players");
			}
		}
	}

	private void checkChests(CoreSQLItem i) throws SQLException {
		if (!i.hasChestData())
			return;
		if (!Config.checkChest)
			return;
		updateConn();
		if (conn == null)
			return;
		for (final Chest c : i.getChest()) {
			try {
				ResultSet r;
				int added = 0;
				int removed = 0;
				int pendings = 0;
				if (c == null) {
					return;
				}

				String q = "SELECT * FROM `" + Config.dbTable
						+ "` WHERE `x` ='" + c.getX() + "'" + " AND `y` ='"
						+ c.getY() + "'" + " AND `z` ='" + c.getZ() + "'";
				if (Config.multiworld) {
					q += " AND `world` = '" + c.getWorld().getName() + "';";
				} else {
					q += ";";
				}
				r = conn.createStatement().executeQuery(q);

				if (r.first()) {
					String chestLocationString = String.format(
							"Chest[x=%s;y=%s;z=%s]", c.getX(), c.getY(),
							c.getZ());

					List<ActionStack> fullInv = new ArrayList<ActionStack>();
					String pendingData = r.getString("pendings");

					if (!"".equals(pendingData)) {
						Main.log(Level.WARNING, "pendings items for "
								+ chestLocationString);

						final List<ItemStack> addStack = new ArrayList<ItemStack>();
						final List<ItemStack> removeStack = new ArrayList<ItemStack>();

						for (ActionStack pendingStack : CoreSQLProcess
								.buildPendList(pendingData)) {
							if ("+".equals(pendingStack.params())) {
								addStack.add(pendingStack.item());
							} else if ("-".equals(pendingStack.params())) {
								removeStack.add(pendingStack.item());
							} else {
								Main.log(Level.INFO, "bad command '"
										+ pendingStack.params()
										+ "' for player '"
										+ chestLocationString
										+ "' in pendings data, ignored");
							}

							Future<HashMap<Integer, ItemStack>> f = this.parent
									.callSyncMethod(new Callable<HashMap<Integer, ItemStack>>() {
										@Override
										public HashMap<Integer, ItemStack> call()
												throws Exception {
											return c.getInventory()
													.removeItem(
															removeStack
																	.toArray(new ItemStack[0]));
										}
									});
							for (Entry<Integer, ItemStack> e : f.get()
									.entrySet()) {
								fullInv.add(new ActionStack(e.getValue(), "-"));
							}
							pendings += f.get().size();
							removed += removeStack.size() - f.get().size();

							f = this.parent
									.callSyncMethod(new Callable<HashMap<Integer, ItemStack>>() {
										@Override
										public HashMap<Integer, ItemStack> call()
												throws Exception {
											return c.getInventory()
													.addItem(
															addStack.toArray(new ItemStack[0]));
										}
									});
							for (Entry<Integer, ItemStack> e : f.get()
									.entrySet()) {
								fullInv.add(new ActionStack(e.getValue(), "+"));
							}
							pendings += f.get().size();
							added += addStack.size() - f.get().size();
						}
						if (i.getCommandSender() != null) {
							i.getCommandSender().sendMessage(
									"[InventorySQL] "
											+ ChatColor.GREEN
											+ "("
											+ chestLocationString
											+ ") "
											+ Main.getMessage("modif", removed,
													added, pendings));
						}
					} else {

						if (i.getCommandSender() != null) {
							i.getCommandSender().sendMessage(
									"[InventorySQL] " + ChatColor.GREEN + "("
											+ chestLocationString + ") "
											+ Main.getMessage("no-modif"));
						}

					}

					String invData = CoreSQLProcess.buildInvString(c
							.getInventory());

					if (fullInv.size() != 0) {
						Main.log(Level.WARNING, "\t Unable to add/remove "
								+ fullInv.size() + " item(s)");
					}
					conn.createStatement()
							.executeUpdate(
									"UPDATE `"
											+ Config.dbTable
											+ "` SET `inventory` = '"
											+ invData
											+ "', `pendings` = '"
											+ (fullInv.isEmpty() ? ""
													: CoreSQLProcess
															.buildPendString(fullInv))
											+ "', " + "`x`= '" + c.getX()
											+ "', " + "`y`= '" + c.getY()
											+ "', " + "`z`= '" + c.getZ()
											+ "' WHERE `id`= '"
											+ r.getInt("id") + "';");

				} else {
					String invData = CoreSQLProcess.buildInvString(c
							.getInventory());

					conn.createStatement()
							.executeUpdate(
									"INSERT INTO `"
											+ Config.dbTable
											+ "`(`owner`, `ischest`, `world`, `inventory`, `pendings`, `x`, `y`, `z`) VALUES ('', '1','"
											+ c.getWorld().getName() + "','"
											+ invData + "','','" + c.getX()
											+ "','" + c.getY() + "','"
											+ c.getZ() + "')");
				}
			} catch (Exception ex) {
				Main.logException(ex, "exception in playerlogic - check chest");
			}
		}
	}

}
