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

	public boolean check_plugin_updates = true;
	public boolean checkChest = false;
	public boolean noCreative = true;
	public boolean multiworld = true;
	public String dbDatabase = null;
	public String dbHost = null;
	public String dbPass = null;
	public String dbTable = null;
	public String dbUser = null;
	public int afterLoginDelay = 20;

	public long delayCheck = 0;
	private InventorySQLPlayerListener playerListener;
	private InventorySQLCommandListener commandListener;
	private CoreSQLProcess coreSQLProcess;
	public Database MYSQLDB;
	public Boolean ready = true;
	public int invsqlTask;


	public static HashMap<String, String> MYSQL_FIELDS_TYPE = new HashMap<String, String>();
	public static HashMap<String, String> MYSQL_USERS_FIELDS_TYPE = new HashMap<String, String>();

	public static void log(Level level, String m) {
		instance.getLogger().log(level, m);
	}
	
	public static void d(Level level, String m) {
		if(!debug) return;
		instance.getLogger().log(level, "[DEBUG] "+m);
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

	private static void populateHashMap() {
		MYSQL_FIELDS_TYPE.clear();
		MYSQL_USERS_FIELDS_TYPE.clear();

		MYSQL_FIELDS_TYPE.put("id", "INT");
		MYSQL_FIELDS_TYPE.put("owner", "VARCHAR");
		MYSQL_FIELDS_TYPE.put("world", "VARCHAR");
		MYSQL_FIELDS_TYPE.put("ischest", "BIT");
		MYSQL_FIELDS_TYPE.put("x", "INT");
		MYSQL_FIELDS_TYPE.put("y", "INT");
		MYSQL_FIELDS_TYPE.put("z", "INT");
		MYSQL_FIELDS_TYPE.put("inventory", "LONGTEXT");
		MYSQL_FIELDS_TYPE.put("pendings", "LONGTEXT");
		MYSQL_FIELDS_TYPE.put("last_update", "TIMESTAMP");

		MYSQL_USERS_FIELDS_TYPE.put("id", "INT");
		MYSQL_USERS_FIELDS_TYPE.put("name", "VARCHAR");
		MYSQL_USERS_FIELDS_TYPE.put("password", "VARCHAR");
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

		populateHashMap();

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
			Main.logException(ex, "mysql init");
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
		/*
		 * for (Material m : Material.values()) { System.out.println("$items[" +
		 * m.getId() + "] = '" + m.toString() + "';"); }
		 */
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
		this.getServer().getScheduler().cancelTask(this.invsqlTask);
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
			if (!this.MYSQLDB.tableExist(this.dbTable)) {
				log("Creating data table...");
				String create = "CREATE TABLE IF NOT EXISTS `"
						+ this.dbTable
						+ "` (`id` int(11) NOT NULL AUTO_INCREMENT, PRIMARY KEY (`id`)) ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
				if (this.MYSQLDB.queryUpdate(create) != 0) {
					log(Level.SEVERE,
							"Cannot create data table, check your config !");
				} else {
					update_table_fields();
				}
			} else {
				ResultSet rs = this.MYSQLDB.query("SELECT * FROM `"
						+ this.dbTable + "`");
				ResultSetMetaData metadata = rs.getMetaData();

				if (metadata.getColumnCount() != MYSQL_FIELDS_TYPE.size()) {
					log("Data table is an old version (fields count differs, "
							+ metadata.getColumnCount() + ":"
							+ MYSQL_FIELDS_TYPE.size() + "), updating...");
					// this.MYSQLDB.queryUpdate("DROP TABLE "+this.dbTable+";");
					update_table_fields();
				} else {
					DatabaseMetaData meta = this.MYSQLDB.getMetaData();
					ResultSet rsColumns = meta.getColumns(null, null,
							this.dbTable, null);
					while (rsColumns.next()) {
						String columnName = rsColumns.getString("COLUMN_NAME");
						String columnType = rsColumns.getString("TYPE_NAME");
						int size = rsColumns.getInt("COLUMN_SIZE");
						if (!columnType.equalsIgnoreCase(MYSQL_FIELDS_TYPE
								.get(columnName))) {
							log("Data table is an old version (fields not match, "
									+ columnName
									+ " => "
									+ columnType
									+ ":"
									+ MYSQL_FIELDS_TYPE.get(columnName)
									+ "), updating...");
							update_table_fields();
							break;
						}
					}
				}
				rs.close();
			}
			if (!this.MYSQLDB.tableExist(this.dbTable + "_users")) {
				log("Creating users table...");
				String create = "CREATE TABLE IF NOT EXISTS `"
						+ this.dbTable
						+ "_users"
						+ "` (`id` int(11) NOT NULL AUTO_INCREMENT, PRIMARY KEY (`id`)) ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
				if (this.MYSQLDB.queryUpdate(create) != 0) {
					log(Level.SEVERE,
							"Cannot create users table, check your config !");
				} else {
					update_users_table_fields();
				}
			} else {
				ResultSet rs = this.MYSQLDB.query("SELECT * FROM `"
						+ this.dbTable + "_users" + "`");
				ResultSetMetaData metadata = rs.getMetaData();

				if (metadata.getColumnCount() != MYSQL_USERS_FIELDS_TYPE.size()) {
					log("Users table is an old version (fields count differs, "
							+ metadata.getColumnCount() + ":"
							+ MYSQL_USERS_FIELDS_TYPE.size() + "), updating...");
					// this.MYSQLDB.queryUpdate("DROP TABLE "+this.dbTable+";");
					update_users_table_fields();
				} else {
					DatabaseMetaData meta = this.MYSQLDB.getMetaData();
					ResultSet rsColumns = meta.getColumns(null, null,
							this.dbTable + "_users", null);
					while (rsColumns.next()) {
						String columnName = rsColumns.getString("COLUMN_NAME");
						String columnType = rsColumns.getString("TYPE_NAME");
						int size = rsColumns.getInt("COLUMN_SIZE");
						if (!columnType
								.equalsIgnoreCase(MYSQL_USERS_FIELDS_TYPE
										.get(columnName))) {
							log("Users table is an old version (fields not match, "
									+ columnName
									+ " => "
									+ columnType
									+ ":"
									+ MYSQL_USERS_FIELDS_TYPE.get(columnName)
									+ "), updating...");
							update_users_table_fields();
							break;
						}
					}
				}
				rs.close();
			}
			return true;
		} catch (Exception ex) {
			Main.logException(ex, "table need update?");
		}
		return false;
	}

	private void update_table_fields() {
		InputStream is = this
				.getResource("alexoft/InventorySQL/schema_data.sql");
		Scanner reader = new Scanner(is);
		String query = "";
		while (reader.hasNextLine()) {
			query += System.getProperty("line.separator") + reader.nextLine();
		}
		query = query.replace("%%TABLENAME%%", this.dbTable);
		for (String r : query.split(";")) {
			this.MYSQLDB.queryUpdateQuiet(r);
		}

		is = this.getResource("alexoft/InventorySQL/schema_users.sql");
		reader = new Scanner(is);
		query = "";
		while (reader.hasNextLine()) {
			query += System.getProperty("line.separator") + reader.nextLine();
		}
		query = query.replace("%%TABLENAME%%", this.dbTable);
		for (String r : query.split(";")) {
			this.MYSQLDB.queryUpdateQuiet(r);
		}
		log("Data table update done");
	}

	private void update_users_table_fields() {
		InputStream is = this
				.getResource("alexoft/InventorySQL/schema_users.sql");
		Scanner reader = new Scanner(is);
		String query = "";
		while (reader.hasNextLine()) {
			query += System.getProperty("line.separator") + reader.nextLine();
		}
		query = query.replace("%%TABLENAME%%", this.dbTable);
		for (String r : query.split(";")) {
			this.MYSQLDB.queryUpdateQuiet(r);
		}
		log("Users table update done");
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
		this.delayCheck = this.getConfig().getInt("check-interval");
		this.check_plugin_updates = this.getConfig().getBoolean(
				"check-plugin-updates");
		this.noCreative = this.getConfig().getBoolean("no-creative");
		this.afterLoginDelay = this.getConfig().getInt("after-login-delay");
		this.multiworld = this.getConfig().getBoolean("multiworld");
		
		Main.debug = this.getConfig().getBoolean("debug");
		/*
		 * try{ CoreSQLProcess.pInventory =
		 * Pattern.compile(this.getConfig().getString("regex.inventory"));
		 * CoreSQLProcess.pPendings =
		 * Pattern.compile(this.getConfig().getString("regex.pendings"));
		 * }catch(PatternSyntaxException psE){ logException(psE,
		 * "Error in regex format, check the entry or delete to regenerate"); }
		 */

		this.delayCheck *= 20;
		this.afterLoginDelay *= 20;
		this.getConfig().save(file);
	}

	private void updateUser(Player[] players, Chest[] chests, CommandSender cs, int delay) {
		if (!this.ready)
			return;
		final CoreSQLItem i = new CoreSQLItem(players,chests, cs);
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

	public void invokeCheck(Player[] players, Chest[] chests, int delay, CommandSender cs) {
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
}
