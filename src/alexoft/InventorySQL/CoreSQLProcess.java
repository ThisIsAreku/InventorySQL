package alexoft.InventorySQL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

/**
 * 
 * @author Alexandre
 */
public class CoreSQLProcess {
	public static Pattern pInventory = Pattern
			.compile("\\[([0-9]{1,2})\\(([0-9]{1,8}):([0-9]{1,3})(\\|([0-9=,]*?))?\\)x(-?[0-9]{1,2})\\]");
	public static Pattern pPendings = Pattern
			.compile("\\[(-|\\+)?\\(([0-9]{1,8}):([0-9]{1,3})(\\|([0-9=,]*?))?\\)x(-?[0-9]{1,2})\\]");
	public Main plugin;
	private Queue<CoreSQLItem> q;
	private int loopTaskPeriod = 10;
	private int loopTask = -1;
	private int checkAllTask = -1;

	public CoreSQLProcess(Main plugin) {
		this.plugin = plugin;
		this.q = new ConcurrentLinkedQueue<CoreSQLItem>();
		if (this.plugin.ready)
			reload();
	}

	public void reload() {
		this.plugin.getServer().getScheduler().cancelTask(loopTask);
		this.plugin.getServer().getScheduler().cancelTask(checkAllTask);
		loopTask = this.plugin.getServer().getScheduler()
				.scheduleAsyncRepeatingTask(plugin, new Runnable() {
					public void run() {
						processQueue();
					}
				}, 20, loopTaskPeriod);
		checkAllTask = this.plugin.getServer().getScheduler()
				.scheduleSyncRepeatingTask(plugin, new Runnable() {
					public void run() {
						q.add(new CoreSQLItem(null, null, null));
					}
				}, 20, this.plugin.delayCheck);
	}

	public void addTask(CoreSQLItem i) {
		if(!this.q.contains(i)) this.q.add(i);
	}

	private void processQueue() {
		if (!this.plugin.ready)
			return;
		CoreSQLItem i = this.q.poll();
		if (i == null)
			return;
		if (i.hasPlayersData()) {
			checkPlayers(i);
		}
		if (i.hasChestData()) {
			checkChests(i);
		}
		if ((!i.hasChestData()) && (!i.hasPlayersData())) {
			try {
				List<Chest> cList = new ArrayList<Chest>();
				if (this.plugin.checkChest) {
					ResultSet r;
					r = this.plugin.MYSQLDB
							.query("SELECT `id`, `world`, `x`, `y`, `z`, `pendings` FROM `"
									+ this.plugin.dbTable
									+ "` WHERE `ischest`= 1;");
					List<Integer> rmList = new ArrayList<Integer>();
					while (r.next()) {
						World w = this.plugin.getServer().getWorld(
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
						this.plugin.MYSQLDB
								.queryUpdate("DELETE FROM`"
										+ this.plugin.dbTable
										+ "` WHERE id="
										+ StringUtils.join(rmList.toArray(),
												" OR id="));
					}
				}

				Future<Player[]> f = this.plugin.getServer().getScheduler()
						.callSyncMethod(this.plugin, new Callable<Player[]>() {
							@Override
							public Player[] call() throws Exception {
								return plugin.getServer().getOnlinePlayers();
							}
						});
				Player[] pList = f.get();
				if ((pList.length > 0) || (cList.size() > 0)) {
					addTask(new CoreSQLItem(pList, cList.toArray(new Chest[0]),
							i.getCommandSender()));
				}
			} catch (Exception ex) {
				Main.logException(ex, "exception in playerlogic - check all");
			}
		}
	}

	private void checkPlayers(CoreSQLItem i) {
		for (final Player p : i.getPlayers()) {
			try {
				ResultSet r;
				int added = 0;
				int removed = 0;
				int pendings = 0;
				if (!usualChecks(p))
					return;

				r = this.plugin.MYSQLDB.query("SELECT * FROM `"
						+ this.plugin.dbTable
						+ "` WHERE LOWER(`owner`) = LOWER('" + p.getName()
						+ "') AND `world` = '" + p.getWorld().getName() + "';");

				if (r.first()) {
					List<ActionStack> fullInv = new ArrayList<ActionStack>();
					String pendingData = r.getString("pendings");

					if (!"".equals(pendingData)) {
						Main.log(Level.WARNING,
								"pendings items for " + p.getName());

						final List<ItemStack> addStack = new ArrayList<ItemStack>();
						final List<ItemStack> removeStack = new ArrayList<ItemStack>();

						for (ActionStack pendingStack : buildPendList(pendingData)) {
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

							Future<HashMap<Integer, ItemStack>> f = this.plugin
									.getServer()
									.getScheduler()
									.callSyncMethod(
											this.plugin,
											new Callable<HashMap<Integer, ItemStack>>() {
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

							f = this.plugin
									.getServer()
									.getScheduler()
									.callSyncMethod(
											this.plugin,
											new Callable<HashMap<Integer, ItemStack>>() {
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

					String invData = buildInvString(p.getInventory());

					if (fullInv.size() != 0) {
						Main.log(Level.WARNING, "\t Unable to add/remove "
								+ fullInv.size() + " item(s)");
					}
					this.plugin.MYSQLDB.queryUpdate("UPDATE `"
							+ this.plugin.dbTable
							+ "` SET `inventory` = '"
							+ invData
							+ "', `pendings` = '"
							+ (fullInv.isEmpty() ? ""
									: buildPendString(fullInv)) + "', "
							+ "`x`= '" + p.getLocation().getBlockX() + "', "
							+ "`y`= '" + p.getLocation().getBlockY() + "', "
							+ "`z`= '" + p.getLocation().getBlockZ()
							+ "' WHERE `id`= '" + r.getInt("id") + "';");

				} else {
					String invData = buildInvString(p.getInventory());

					this.plugin.MYSQLDB
							.queryUpdate("INSERT INTO `"
									+ this.plugin.dbTable
									+ "`(`owner`, `world`, `inventory`, `pendings`, `x`, `y`, `z`) VALUES ('"
									+ p.getName() + "','"
									+ p.getWorld().getName() + "','" + invData
									+ "','','" + p.getLocation().getBlockX()
									+ "','" + p.getLocation().getBlockY()
									+ "','" + p.getLocation().getBlockZ()
									+ "')");
					this.plugin.MYSQLDB.queryUpdate("INSERT INTO `"
							+ this.plugin.dbTable + "_users"
							+ "`(`name`, `password`) VALUES ('" + p.getName()
							+ "', '')");
				}
			} catch (Exception ex) {
				Main.logException(ex,
						"exception in playerlogic - check players");
			}
		}
	}

	private void checkChests(CoreSQLItem i) {
		if (!this.plugin.checkChest)
			return;
		for (final Chest c : i.getChest()) {
			try {
				ResultSet r;
				int added = 0;
				int removed = 0;
				int pendings = 0;
				if (!usualChecks(c))
					return;

				r = this.plugin.MYSQLDB.query("SELECT * FROM `"
						+ this.plugin.dbTable + "` WHERE `x` ='" + c.getX()
						+ "'" + " AND `y` ='" + c.getY() + "'" + " AND `z` ='"
						+ c.getZ() + "'" + " AND `world` = '"
						+ c.getWorld().getName() + "';");

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

						for (ActionStack pendingStack : buildPendList(pendingData)) {
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

							Future<HashMap<Integer, ItemStack>> f = this.plugin
									.getServer()
									.getScheduler()
									.callSyncMethod(
											this.plugin,
											new Callable<HashMap<Integer, ItemStack>>() {
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

							f = this.plugin
									.getServer()
									.getScheduler()
									.callSyncMethod(
											this.plugin,
											new Callable<HashMap<Integer, ItemStack>>() {
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

					String invData = buildInvString(c.getInventory());

					if (fullInv.size() != 0) {
						Main.log(Level.WARNING, "\t Unable to add/remove "
								+ fullInv.size() + " item(s)");
					}
					this.plugin.MYSQLDB.queryUpdate("UPDATE `"
							+ this.plugin.dbTable
							+ "` SET `inventory` = '"
							+ invData
							+ "', `pendings` = '"
							+ (fullInv.isEmpty() ? ""
									: buildPendString(fullInv)) + "', "
							+ "`x`= '" + c.getX() + "', " + "`y`= '" + c.getY()
							+ "', " + "`z`= '" + c.getZ() + "' WHERE `id`= '"
							+ r.getInt("id") + "';");

				} else {
					String invData = buildInvString(c.getInventory());

					this.plugin.MYSQLDB
							.queryUpdate("INSERT INTO `"
									+ this.plugin.dbTable
									+ "`(`owner`, `ischest`, `world`, `inventory`, `pendings`, `x`, `y`, `z`) VALUES ('', '1','"
									+ c.getWorld().getName() + "','" + invData
									+ "','','" + c.getX() + "','" + c.getY()
									+ "','" + c.getZ() + "')");
				}
			} catch (Exception ex) {
				Main.logException(ex, "exception in playerlogic - check chest");
			}
		}
	}

	private boolean usualChecks(Player p) throws SQLException {
		if (this.plugin == null) {
			return false;
		}
		if ((this.plugin.noCreative) && (p.getGameMode() == GameMode.CREATIVE)) {
			return false;
		}
		if (this.plugin.MYSQLDB == null) {
			Main.logException(new NullPointerException("MYSQLDB is null.."),
					"this.plugin.MYSQLDB");
			this.plugin.Disable();
			return false;
		}
		if (!this.plugin.MYSQLDB.checkConnectionIsAlive(true)) {
			Main.log(Level.SEVERE, "MySQL Connection error..");
			return false;
		}
		if (!this.plugin.MYSQLDB.tableExist(this.plugin.dbTable)) {
			Main.log(Level.SEVERE,
					"Table has suddenly disappear, disabling plugin...");
			this.plugin.Disable();
			return false;
		}

		return true;
	}

	private boolean usualChecks(Chest c) throws SQLException {
		if (this.plugin == null) {
			return false;
		}
		if (c == null) {
			return false;
		}
		if (this.plugin.MYSQLDB == null) {
			Main.logException(new NullPointerException("MYSQLDB is null.."),
					"this.plugin.MYSQLDB");
			this.plugin.Disable();
			return false;
		}
		if (!this.plugin.MYSQLDB.checkConnectionIsAlive(true)) {
			Main.log(Level.SEVERE, "MySQL Connection error..");
			return false;
		}
		if (!this.plugin.MYSQLDB.tableExist(this.plugin.dbTable)) {
			Main.log(Level.SEVERE,
					"Table has suddenly disappear, disabling plugin...");
			this.plugin.Disable();
			return false;
		}
		return true;
	}

	private List<ActionStack> buildInvList(String data) {
		List<ActionStack> inv = new ArrayList<ActionStack>();
		Matcher m;

		for (String i : data.split(",")) {
			m = pInventory.matcher(i);
			if (m.matches()) {
				ItemStack itmstck = new ItemStack(Integer.decode(m.group(2)),
						Integer.decode(m.group(6)), (short) 0, Byte.decode(m
								.group(3)));

				inv.add(new ActionStack(itmstck, m.group(1)));
			}
		}
		return inv;
	}

	private List<ActionStack> buildPendList(String data) {
		List<ActionStack> inv = new ArrayList<ActionStack>();
		Matcher m;

		for (String i : data.split(",")) {
			m = pPendings.matcher(i);
			if (m.matches()) {
				ItemStack itmstck = new ItemStack(Integer.decode(m.group(2)),
						Integer.decode(m.group(6)), (short) 0, Byte.decode(m
								.group(3)));
				if (m.group(5) != null) {
					for (String e : m.group(5).split(",")) {
						String[] d = e.split("=");
						Enchantment k = Enchantment.getById(Integer
								.decode(d[0]));
						if (k != null) {
							Integer l = Integer.decode(d[1]);
							if (l > k.getMaxLevel())
								l = k.getMaxLevel();
							if (l < 1)
								l = 1;
							itmstck.addEnchantment(k, l);
						}
					}
				}
				inv.add(new ActionStack(itmstck, m.group(1)));
			}
		}
		return inv;
	}

	private String buildEnchString(ItemStack itemStack) {
		String s = "";
		for (Entry<Enchantment, Integer> e : itemStack.getEnchantments()
				.entrySet()) {
			s += e.getKey().getId() + "=" + e.getValue() + ",";
		}
		if (s.endsWith(","))
			s = s.substring(0, s.length() - 1);
		if (s != "")
			s = "|" + s;
		return s;
	}

	private String buildInvString(Inventory inventory) {
		ItemStack m;
		String l = "";
		MaterialData b;
		for (int i = 0; i < inventory.getSize(); i++) {
			m = inventory.getItem(i);
			if (m != null) {
				b = m.getData();
				l += "[" + i + "(" + m.getTypeId() + ":"
						+ (b != null ? b.getData() : "0") + buildEnchString(m)
						+ ")x" + m.getAmount() + "],";
			}
		}
		if (l.length() > 1)
			l = l.substring(0, l.length() - 1);
		return l;
	}

	private String buildPendString(List<ActionStack> items) {
		String l = "";
		MaterialData b;

		for (ActionStack m : items) {
			b = m.item().getData();
			l += "[" + m.params() + "(" + m.item().getTypeId() + ":"
					+ (b != null ? b.getData() : "0") + ")x"
					+ m.item().getAmount() + "],";
		}
		if (l.length() > 1)
			l = l.substring(0, l.length() - 1);
		return l;
	}
}
