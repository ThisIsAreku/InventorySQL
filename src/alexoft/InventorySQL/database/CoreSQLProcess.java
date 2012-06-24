package alexoft.InventorySQL.database;

import alexoft.InventorySQL.ActionStack;
import alexoft.InventorySQL.Config;
import alexoft.InventorySQL.EmptyException;
import alexoft.InventorySQL.Main;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

/**
 * 
 * @author Alexandre
 */
public class CoreSQLProcess implements Runnable {
	public static final Pattern pInventory = Pattern
			.compile("\\[([0-9]{1,2})\\(([0-9]+?):([0-9]+?)(\\|([0-9=,]*?))?\\)x(-?[0-9]{1,2})\\]");
	public static final Pattern pPendings = Pattern
			.compile("\\[(-|\\+)\\(([0-9]+?):([0-9]+?)(\\|([0-9=,]*?))?\\)x(-?[0-9]{1,2})\\]");

	public Main plugin;
	// private static final LinkedBlockingQueue<CoreSQLItem> queue = new
	// LinkedBlockingQueue<CoreSQLItem>();
	public ConnectionManager connectionManager = null;
	private boolean databaseReady = false;

	/*
	 * private int backupProcess = 0; private int checkProcess = 0;
	 */

	public CoreSQLProcess(Main plugin) {
		this.plugin = plugin;
	}

	public void reload() throws ClassNotFoundException {
		Main.log(Level.INFO, "Reloading SQL process");
		this.databaseReady = false;
		if (this.connectionManager != null)
			this.connectionManager.close();
		this.connectionManager = new ConnectionManager("jdbc:mysql://"
				+ Config.dbHost + "/" + Config.dbDatabase, Config.dbUser,
				Config.dbPass);
		this.plugin.getServer().getScheduler()
				.scheduleAsyncDelayedTask(this.plugin, this);
	}

	@Override
	public void run() {
		if (checkUpdateTable()) {
			if (!Config.lightweight_mode) {
				this.plugin.getServer().getScheduler().cancelTasks(plugin);
				/* checkProcess = */this.plugin
						.getServer()
						.getScheduler()
						.scheduleAsyncRepeatingTask(plugin, new SQLCheck(this),
								Config.check_interval, Config.check_interval);

				if (Config.backup_enabled) {
					/* backupProcess = */this.plugin
							.getServer()
							.getScheduler()
							.scheduleAsyncRepeatingTask(plugin,
									new SQLBackup(this),
									Config.backup_interval,
									Config.backup_interval);
				}
			}
			this.databaseReady = true;
		} else {
			Main.log(Level.WARNING, "Cannot connect to database !");
			Main.log(Level.WARNING, "Check your config and use /invsql reload");
			disable();
		}
	}

	public void disable() {
		this.databaseReady = false;
		this.plugin.getServer().getScheduler().cancelTasks(plugin);
		if (this.connectionManager != null)
			this.connectionManager.close();
		this.connectionManager = null;
	}

	public void runCheckThisTask(CoreSQLItem i, boolean doGive, int delay) {
		if (this.databaseReady)
			this.plugin
					.getServer()
					.getScheduler()
					.scheduleAsyncDelayedTask(plugin,
							new SQLCheck(this).manualCheck(i, doGive), delay);
	}

	public void runCheckAllTask(int delay) {
		if (this.databaseReady)
			this.plugin
					.getServer()
					.getScheduler()
					.scheduleAsyncDelayedTask(plugin,
							new SQLCheck(this).manualCheck(), delay);
	}

	public boolean updatePlayerPassword(String player, String password) {
		try {
			JDCConnection conn = this.connectionManager.getConnection();
			conn.createStatement().executeUpdate(
					"INSERT `" + Config.dbTable
							+ "_users`(`name`,`password`) VALUES ('" + player
							+ "', MD5('" + password
							+ "')) ON DUPLICATE KEY UPDATE `password`= MD5('"
							+ password + "');");
			conn.close();
			return true;
		} catch (SQLException e) {
			Main.logException(e, "updatePassword");
			return false;
		}
	}

	public boolean isDatabaseReady() {
		return this.databaseReady;
	}

	public Player[] getOnlinePlayers() throws InterruptedException,
			ExecutionException {
		return this.plugin.getServer().getScheduler()
				.callSyncMethod(this.plugin, new Callable<Player[]>() {
					@Override
					public Player[] call() throws Exception {
						return plugin.getServer().getOnlinePlayers();
					}
				}).get();
	}

	public Player getPlayer(final String p) throws InterruptedException,
			ExecutionException {
		return this.plugin.getServer().getScheduler()
				.callSyncMethod(this.plugin, new Callable<Player>() {
					@Override
					public Player call() throws Exception {
						return plugin.getServer().getPlayer(p);
					}
				}).get();
	}

	public JDCConnection getConnection() {
		try {
			return this.connectionManager.getConnection();
		} catch (SQLException e) {
			Main.logException(e, "Cannot get a new connection");
		}
		return null;
	}

	public <T> Future<T> callSyncMethod(Callable<T> methode) {
		return this.plugin.getServer().getScheduler()
				.callSyncMethod(this.plugin, methode);
	}

	public static List<ActionStack> buildInvList(String data) {
		List<ActionStack> inv = new ArrayList<ActionStack>();
		Matcher m;

		for (String i : data.split(";")) {
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

	public static List<ActionStack> buildPendList(String data) {
		List<ActionStack> inv = new ArrayList<ActionStack>();
		Matcher m;

		for (String i : data.split(";")) {
			m = pPendings.matcher(i);
			Main.d("STR?'" + i + "'");
			if (m.matches()) {
				ItemStack itmstck;
				Integer id = Integer.decode(m.group(2));
				Integer num = Integer.decode(m.group(6));
				if (id == 373) {
					itmstck = new ItemStack(id, num);
					itmstck.setDurability(Short.decode(m.group(3)));
					Main.d("POTION/" + m.group(3));
				} else {
					itmstck = new ItemStack(id, num, (short) 0,
							Byte.parseByte(m.group(3)));
				}
				if (m.group(5) != null) {
					for (String e : m.group(5).split(",")) {
						String[] d = e.split("=");
						Enchantment k = Enchantment.getById(Integer
								.decode(d[0]));
						Main.d("ENCH/" + k.getName() + "=" + d[1]);
						if (k != null) {
							Integer l = Integer.decode(d[1]);
							if (l > k.getMaxLevel())
								l = k.getMaxLevel();
							if (l < 1)
								l = 1;
							if (Config.allow_unsafe_ench) {
								itmstck.addUnsafeEnchantment(k, l);
							} else {
								try {
									itmstck.addEnchantment(k, l);
								} catch (IllegalArgumentException iargE) {
									Main.log(
											Level.WARNING,
											"Cannot add enchantment '"
													+ k.getName() + "' ("
													+ k.getId() + ") to "
													+ itmstck.getType().name());
								}
							}
						}
					}
				}
				inv.add(new ActionStack(itmstck, m.group(1)));
			}
		}
		return inv;
	}

	public static String buildEnchString(ItemStack itemStack) {
		String s = "";
		for (Entry<Enchantment, Integer> e : itemStack.getEnchantments()
				.entrySet()) {
			s += e.getKey().getId() + "=" + e.getValue() + ",";
		}
		if (s.endsWith(","))
			s = s.substring(0, s.length() - 1);
		if (!"".equals(s))
			s = "|" + s;
		return s;
	}

	public static String buildInvString(Inventory inventory) {
		ItemStack m;
		String l = "";
		MaterialData b;
		for (int i = 0; i < inventory.getSize(); i++) {
			m = inventory.getItem(i);
			if (m != null) {
				b = m.getData();
				l += "["
						+ i
						+ "("
						+ m.getTypeId()
						+ ":"
						+ (m.getTypeId() == 373 ? (m.getDurability())
								: (b != null ? b.getData() : "0"))
						+ buildEnchString(m) + ")x" + m.getAmount() + "];";
			}
		}
		if (l.length() > 1)
			l = l.substring(0, l.length() - 1);
		return l;
	}

	public static String buildPendString(List<ActionStack> items) {
		String l = "";
		MaterialData b;

		for (ActionStack m : items) {
			b = m.item().getData();
			l += "[" + m.params() + "(" + m.item().getTypeId() + ":"
					+ (b != null ? b.getData() : "0") + ")x"
					+ m.item().getAmount() + "];";
		}
		if (l.length() > 1)
			l = l.substring(0, l.length() - 1);
		return l;
	}

	public boolean checkUpdateTable() {
		try {
			JDCConnection conn = this.connectionManager.getConnection();
			check_table_version("", conn);
			check_table_version("_users", conn);
			if (Config.backup_enabled)
				check_table_version("_backup", conn);
			conn.close();
			return true;
		} catch (Exception ex) {
			// Main.logException(ex, "table need update?");
		}
		return false;
	}

	private void check_table_version(String selector, JDCConnection conn)
			throws SQLException, EmptyException {
		if (!JDBCUtil.tableExistsCaseSensitive(conn.getMetaData(),
				Config.dbTable + selector)) {
			Main.log("Creating '" + Config.dbTable + selector + "' table...");
			String create = "CREATE TABLE IF NOT EXISTS `"
					+ Config.dbTable
					+ selector
					+ "` (`id` int(11) NOT NULL AUTO_INCREMENT, PRIMARY KEY (`id`)) ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
			if (conn.createStatement().executeUpdate(create) != 0) {
				Main.log(Level.SEVERE,
						"Cannot create users table, check your config !");
			} else {
				update_table_fields(selector, conn);
			}
		} else {
			ResultSet rs = conn.createStatement().executeQuery(
					"SHOW CREATE TABLE `" + Config.dbTable + selector + "`");
			rs.first();
			String comment = rs.getString(2);
			int p = comment.indexOf("COMMENT='");
			if (p == -1) {
				update_table_fields(selector, conn);
			} else {
				comment = comment
						.substring(p + 9, comment.indexOf('\'', p + 9));

				if (!("table format : " + Main.TABLE_VERSION).equals(comment)) {
					update_table_fields(selector, conn);
				}
			}
			rs.close();
		}
	}

	private void update_table_fields(String selector, JDCConnection conn)
			throws SQLException {
		Main.log("Table '" + Config.dbTable + selector + "' need update");
		String query = read(plugin.getResource("alexoft/InventorySQL/schema"
				+ selector + ".sql"));
		query = query.replace("%%TABLENAME%%", Config.dbTable);
		for (String r : query.split(";")) {
			try {
				conn.createStatement().executeUpdate(r);
			} catch (SQLException e) {
			}
		}
		query = "ALTER IGNORE TABLE `%%TABLENAME%%` COMMENT = 'table format : %%VERSION%%'"
				.replace("%%TABLENAME%%", Config.dbTable + selector).replace(
						"%%VERSION%%", Main.TABLE_VERSION);
		try {
			conn.createStatement().executeUpdate(query);
		} catch (SQLException e) {
		}
		Main.log("'" + Config.dbTable + selector + "' table: update done");
	}

	private String read(InputStream src) {
		Scanner reader = new Scanner(src);
		String s = "";
		String l;
		while (reader.hasNextLine()) {
			l = reader.nextLine();
			if (!l.startsWith("#")) {
				s += System.getProperty("line.separator") + l;
			}
		}
		try {
			src.close();
			reader.close();
		} catch (Exception e) {
		}
		return s;
	}

}
