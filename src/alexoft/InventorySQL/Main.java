package alexoft.InventorySQL;

import alexoft.InventorySQL.auth.OfflineMode;
import alexoft.InventorySQL.database.CoreSQLProcess;
import alexoft.commons.UpdateChecker;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.authdb.AuthDB;
import com.cypherx.xauth.xAuth;

@SuppressWarnings("unused")
public class Main extends JavaPlugin {
	public static Main instance;
	public static final String TABLE_VERSION = "1.2";

	public static int reload_count = -1; // Initialized to -1 for the first
											// pseudo-reload

	private UpdateEventListener playerListener;
	private InventorySQLCommandListener commandListener;
	private OfflineMode offlineModeController;
	
	
	private CoreSQLProcess coreSQLProcess;
	public Boolean ready = true;

	public static void log(Level level, String m) {
		instance.getLogger().log(level, m);
	}

	public static void d(Level level, String m) {
		if (!Config.debug)
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
			log("Loading config...");
			new Config(this);
		} catch (Exception e) {
			logException(e, "Unable to load config");
		}

		this.coreSQLProcess = new CoreSQLProcess(this);
		this.playerListener = new UpdateEventListener(this);
		this.commandListener = new InventorySQLCommandListener(this);
		this.offlineModeController = new OfflineMode(this.playerListener);

		this.getCommand("invSQL").setExecutor(commandListener);
		this.getCommand("ichk").setExecutor(commandListener);

		startMetrics();
		if (Config.check_plugin_updates)
			startUpdate();

		reload();

		// debug code to pring pretty-formated ids
		// used to update the webui
		if (Config.debug) {
			for (Material m : Material.values()) {
				System.out.println("$items[" + m.getId() + "] = '"
						+ m.toString() + "';");
			}
			System.out.println("//----------------");
			for (Enchantment e : Enchantment.values()) {
				System.out.println("$ench[" + e.getId()
						+ "] = array('name' => '" + e.getName()
						+ "', 'startlevel' => " + e.getStartLevel()
						+ ", 'maxlevel' => " + e.getMaxLevel() + ");");
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
		reload_count++;
		try {
			new Config(this);
		} catch (Exception e) {
			logException(e, "Unable to load config");
			this.Disable();
			return;
		}

		try {
			this.coreSQLProcess.reload();
		} catch (ClassNotFoundException e) {
			log(Level.SEVERE, "Cannot found MySQL Class !");
			this.Disable();
			return;
		}
	}

	public CoreSQLProcess getCoreSQLProcess() {
		return this.coreSQLProcess;
	}
	public OfflineMode getOfflineModeController() {
		return this.offlineModeController;
	}

}
