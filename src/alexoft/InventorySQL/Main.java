package alexoft.InventorySQL;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.command.ColouredConsoleSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import alexoft.commons.UpdateChecker;

@SuppressWarnings("unused")
public class Main extends JavaPlugin {
	public static Main instance;
	public static boolean debug = false;
	public static final String TABLE_VERSION = "1.2";

	public String dbDatabase = null;
	public String dbHost = null;
	public String dbPass = null;
	public String dbTable = null;
	public String dbUser = null;

	public boolean check_plugin_updates = true;
	public boolean checkChest = false;
	public boolean noCreative = true;
	public boolean multiworld = true;
	public int afterLoginDelay = 20;

	public boolean backup_enabled = true;
	public long backup_interval = 0;
	public int backup_cleanup_days = 0;

	public long check_interval = 0;

	private InventorySQLPlayerListener playerListener;
	private InventorySQLCommandListener commandListener;
	private CoreSQLProcess coreSQLProcess;
	public Database MYSQLDB;
	public Boolean ready = true;

	public static void log(Level level, String m) {
		instance.getLogger().log(level, m);
	}

	public static void d(Level level, String m) {
		if (!debug)
			return;
		instance.getLogger().log(level, "[DEBUG] " + m);
	}

	public static void log(String m) {
		log(Level.INFO, m);
	}

	public static void d(String m) {
		d(Level.INFO, m);
	}

	public static void logException(Exception e, String m) {
		if (e instanceof EmptyException)
			return;
		log(Level.SEVERE, "---------------------------------------");
		log(Level.SEVERE, "--- an unexpected error has occured ---");
		log(Level.SEVERE, "-- please send line below to the dev --");
		log(Level.SEVERE, "InventorySQL version "
				+ instance.getDescription().getVersion());
		log(Level.SEVERE, "Bukkit version " + instance.getServer().getVersion());
		log(Level.SEVERE, "Message: " + m);
		if (e instanceof SQLException) {
			log(Level.SEVERE, "SQLState: " + ((SQLException) e).getSQLState());
			log(Level.SEVERE,
					"Error Code: " + ((SQLException) e).getErrorCode());
		}
		log(Level.SEVERE, e.toString() + " : " + e.getLocalizedMessage());
		for (StackTraceElement t : e.getStackTrace()) {
			log(Level.SEVERE, "\t" + t.toString());
		}
		log(Level.SEVERE, "---------------------------------------");
	}

	public static String getMessage(String k, Object... format) {
		return String.format(instance.getConfig().getString("messages." + k),
				format);
	}

	@Override
	public void onDisable() {
		// log("Disabling...");
		this.getServer().getScheduler().cancelTasks(this);
		// this.invokeCheck(false, null);

		log("Disabled !");
	}

	@Override
	public void onEnable() {
		instance = this;
		log("ThisIsAreku present "
				+ this.getDescription().getName().toUpperCase() + ", v"
				+ this.getDescription().getVersion());
		log("= " + this.getDescription().getWebsite() + " =");

		try {
			this.loadConfig();
		} catch (Exception e) {
			logException(e, "Unable to load config");
			this.ready = false;
		}
		try {
			if (this.ready) {
				MYSQLDB = new Database("jdbc:mysql://" + this.dbHost + "/"
						+ this.dbDatabase, this.dbUser, this.dbPass);

				log("MySQL connection successful");
				checkUpdateTable();
			} else {
				log(Level.SEVERE, "MySQL configuration error");
			}
		} catch (SQLException ex) {
			// Main.logException(ex, "mysql init");
			log(Level.SEVERE, "MySQL connection failed");
			this.ready = false;
		} catch (ClassNotFoundException e) {
			Main.logException(e, "mysql init");
			log(Level.SEVERE, "MySQL connection failed");
			this.ready = false;
		}

		if (!this.ready)
			log(Level.SEVERE, "check the config and use /invsql reload");

		this.coreSQLProcess = new CoreSQLProcess(this);
		this.playerListener = new InventorySQLPlayerListener(this);
		this.commandListener = new InventorySQLCommandListener(this);

		this.getCommand("invSQL").setExecutor(commandListener);
		this.getCommand("ichk").setExecutor(commandListener);

		startMetrics();
		if (this.check_plugin_updates)
			startUpdate();

		// debug code to pring pretty-formated ids
		// used to update the webui
		if (Main.debug) {
			for (Material m : Material.values()) {
				System.out.println("$items[" + m.getId() + "] = '"
						+ m.toString() + "';");
			}
		}

	}

	public void startMetrics() {

		try {
			log("Starting Metrics");
			MetricsLite metrics = new MetricsLite(this);
			metrics.start();
		} catch (IOException e) {
			log("Cannot start Metrics...");
		}
	}

	public void startUpdate() {
		try {
			UpdateChecker update = new UpdateChecker(this);
			update.start();
		} catch (MalformedURLException e) {
			log("Cannot start Plugin Updater...");
		}
	}

	public void Disable() {
		this.getPluginLoader().disablePlugin(this);
	}

	public void reload() {
		try {
			this.loadConfig();
		} catch (Exception e) {
			logException(e, "Unable to load config");
			this.ready = false;
		}
		try {
			if (this.ready) {
				MYSQLDB = new Database("jdbc:mysql://" + this.dbHost + "/"
						+ this.dbDatabase, this.dbUser, this.dbPass);

				log("MySQL connection successful");
			} else {
				log(Level.SEVERE, "MySQL configuration error");
			}
		} catch (SQLException ex) {
			Main.logException(ex, "mysql init");
			log(Level.SEVERE, "MySQL connection failed");
			this.ready = false;
		} catch (ClassNotFoundException e) {
			Main.logException(e, "mysql init");
			log(Level.SEVERE, "MySQL connection failed");
			this.ready = false;
		}

		if (!this.ready) {
			log(Level.SEVERE, "check the config and use /invsql reload");
		} else {
			this.getServer().getScheduler()
					.scheduleAsyncDelayedTask(this, new Runnable() {

						@Override
						public void run() {
							log("Checking table..");
							if (checkUpdateTable()) {
								ready = true;
								coreSQLProcess.reload();
							} else {
								log(Level.SEVERE,
										"Cannot update/create table !");
								log(Level.SEVERE,
										"check the config and use /invsql reload");
								ready = false;
							}
						}

					});
		}
	}

	public boolean checkUpdateTable() {
		try {
			check_table_version("");
			check_table_version("_users");
			if (this.backup_enabled)
				check_table_version("_backup");
			return true;
		} catch (Exception ex) {
			Main.logException(ex, "table need update?");
		}
		return false;
	}

	private void check_table_version(String selector) throws SQLException,
			EmptyException {
		if (!this.MYSQLDB.tableExist(this.dbTable + selector)) {
			log("Creating '" + this.dbTable + selector + "' table...");
			String create = "CREATE TABLE IF NOT EXISTS `"
					+ this.dbTable
					+ selector
					+ "` (`id` int(11) NOT NULL AUTO_INCREMENT, PRIMARY KEY (`id`)) ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
			if (this.MYSQLDB.queryUpdate(create) != 0) {
				log(Level.SEVERE,
						"Cannot create users table, check your config !");
			} else {
				update_table_fields(selector);
			}
		} else {
			ResultSet rs = this.MYSQLDB.query("SHOW CREATE TABLE `"
					+ this.dbTable + selector + "`");
			rs.first();
			String comment = rs.getString(2);
			int p = comment.indexOf("COMMENT='");
			if (p == -1) {
				update_table_fields(selector);
			} else {
				comment = comment
						.substring(p + 9, comment.indexOf('\'', p + 9));

				if (!("table format : " + Main.TABLE_VERSION).equals(comment)) {
					update_table_fields(selector);
				}
			}
			rs.close();
		}
	}

	private void update_table_fields(String selector) {
		log("Table '" + this.dbTable + selector + "' need update");
		String query = read(this.getResource("alexoft/InventorySQL/schema"
				+ selector + ".sql"));
		query = query.replace("%%TABLENAME%%", this.dbTable);
		for (String r : query.split(";")) {
			this.MYSQLDB.queryUpdateQuiet(r);
		}
		query = "ALTER IGNORE TABLE `%%TABLENAME%%` COMMENT = 'table format : %%VERSION%%'"
				.replace("%%TABLENAME%%", this.dbTable + selector).replace(
						"%%VERSION%%", Main.TABLE_VERSION);
		this.MYSQLDB.queryUpdateQuiet(query);
		log("'" + this.dbTable + selector + "' table: update done");
	}

	public void loadConfig() throws FileNotFoundException, IOException,
			InvalidConfigurationException {

		File file = new File(this.getDataFolder(), "config.yml");
		if (!this.getDataFolder().exists())
			this.getDataFolder().mkdirs();
		if (!file.exists())
			copy(this.getResource("config.yml"), file);

		this.getConfig().load(file);

		YamlConfiguration defaults = new YamlConfiguration();
		defaults.load(this.getResource("config.yml"));
		this.getConfig().addDefaults(defaults);
		this.getConfig().options().copyDefaults(true);

		this.ready = true;

		this.dbHost = this.getConfig().getString("mysql.host");
		this.dbUser = this.getConfig().getString("mysql.user");
		this.dbPass = this.getConfig().getString("mysql.pass");
		this.dbDatabase = this.getConfig().getString("mysql.db");
		this.dbTable = this.getConfig().getString("mysql.table");
		this.check_interval = this.getConfig().getInt("check-interval");
		this.check_plugin_updates = this.getConfig().getBoolean(
				"check-plugin-updates");
		this.noCreative = this.getConfig().getBoolean("no-creative");
		this.afterLoginDelay = this.getConfig().getInt("after-login-delay");
		this.multiworld = this.getConfig().getBoolean("multiworld");

		this.backup_enabled = this.getConfig().getBoolean("backup.enabled");
		this.backup_interval = this.getConfig().getInt("backup.interval");
		this.backup_cleanup_days = this.getConfig().getInt(
				"backup.cleanup-days");

		Main.debug = this.getConfig().getBoolean("debug");

		this.check_interval *= 20;
		this.backup_interval *= 20;
		this.afterLoginDelay *= 20;
		this.getConfig().save(file);
	}

	private void updateUser(Player[] players, Chest[] chests, CommandSender cs,
			int delay) {
		if (!this.ready)
			return;
		final CoreSQLItem i = new CoreSQLItem(players, chests, cs);
		this.getServer().getScheduler()
				.scheduleSyncDelayedTask(this, new Runnable() {
					@Override
					public void run() {
						coreSQLProcess.addTask(i);
					}
				}, delay);
	}

	public void invokeAllCheck(CommandSender cs) {
		updateUser(null, null, cs, 5);
	}

	public void invokeAllCheck(int delay, CommandSender cs) {
		updateUser(null, null, cs, delay);
	}

	public void invokeCheck(Player[] players, CommandSender cs) {
		updateUser(players, null, cs, 5);
	}

	public void invokeCheck(Player[] players, int delay, CommandSender cs) {
		updateUser(players, null, cs, delay);
	}

	public void invokeCheck(Chest[] chests, CommandSender cs) {
		updateUser(null, chests, cs, 5);
	}

	public void invokeCheck(Chest[] chests, int delay, CommandSender cs) {
		updateUser(null, chests, cs, delay);
	}

	public void invokeCheck(Player[] players, Chest[] chests, CommandSender cs) {
		updateUser(players, chests, cs, 5);
	}

	public void invokeCheck(Player[] players, Chest[] chests, int delay,
			CommandSender cs) {
		updateUser(players, chests, cs, delay);
	}

	private void copy(InputStream src, File dst) throws IOException {
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = src.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		src.close();
		out.close();
	}

	private String read(InputStream src) {
		Scanner reader = new Scanner(src);
		String s = "";
		String l = "";
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
