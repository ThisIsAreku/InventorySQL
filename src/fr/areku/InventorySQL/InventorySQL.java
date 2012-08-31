package fr.areku.InventorySQL;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import fr.areku.InventorySQL.database.CoreSQLProcess;
import fr.areku.commons.UpdateChecker;

public class InventorySQL extends JavaPlugin {
	private static InventorySQL instance;

	private CoreSQLProcess coreSQLProcess;
	private PlayerManager playerManager;

	private UpdateEventListener playerListener;
	private InventorySQLCommandListener commandListener;
	private boolean offlineModePlugin = false;

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
		log(Level.SEVERE, "Bukkit version " + Bukkit.getVersion());
		if(InventorySQL.isUsingAuthenticator()){
			log(Level.SEVERE, "Authenticator version " + Bukkit.getPluginManager().getPlugin("Authenticator").getDescription().getVersion());
		}
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
		try {
			getCoreSQLProcess().onDisable();
			getPlayerManager().saveDatas();
		} catch (Exception e) {
			logException(e, "Error while disabling..");
		}
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
			this.Disable();
			return;
		}
		this.playerManager = new PlayerManager(new File(getDataFolder(),
				"players.txt"));
		this.coreSQLProcess = new CoreSQLProcess(this);
		this.playerListener = new UpdateEventListener(this);
		this.commandListener = new InventorySQLCommandListener(this);

		this.getCommand("invSQL").setExecutor(commandListener);
		this.getCommand("ichk").setExecutor(commandListener);

		startMetrics();
		if (Config.check_plugin_updates)
			startUpdate();

		reload();

		linkOfflineMode();
	}

	public void linkOfflineMode() {
		Plugin p = Bukkit.getServer().getPluginManager()
				.getPlugin("Authenticator");
		if (p != null) {
			if (fr.areku.Authenticator.Authenticator.isUsingOfflineModePlugin()) {
				offlineModePlugin = true;
				fr.areku.Authenticator.Authenticator.setDebug(Config.debug,
						this);

				this.playerListener.registerOfflineModeSupport();
				InventorySQL
						.log("Using Authenticator for offline-mode support");
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
		Bukkit.getPluginManager().disablePlugin(this);
	}

	public void reload() {
		try {
			new Config(this);
		} catch (Exception e) {
			logException(e, "Unable to load config");
			this.Disable();
			return;
		}

		try {
			getCoreSQLProcess().reload();
		} catch (ClassNotFoundException e) {
			log(Level.SEVERE, "Cannot found MySQL Class !");
			this.Disable();
			return;
		}
	}

	public static PlayerManager getPlayerManager() {
		return instance.playerManager;
	}

	public static CoreSQLProcess getCoreSQLProcess() {
		return instance.coreSQLProcess;
	}

	public static boolean isUsingAuthenticator() {
		return instance.offlineModePlugin;
	}

}
