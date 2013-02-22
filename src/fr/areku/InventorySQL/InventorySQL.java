package fr.areku.InventorySQL;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import fr.areku.InventorySQL.database.CoreSQL;
import fr.areku.commons.UpdateChecker;

public class InventorySQL extends JavaPlugin {
	private static InventorySQL instance;

	private InventorySQLCommandListener commandListener;

	private Permission perm = null;

	private boolean offlineModePlugin = false;
	private boolean vaultPlugin = false;

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
		log(Level.SEVERE, "InventorySQL version " + getVersion());
		log(Level.SEVERE, "Bukkit version " + Bukkit.getVersion());
		if (InventorySQL.isUsingAuthenticator()) {
			log(Level.SEVERE, "Authenticator version "
					+ Bukkit.getPluginManager().getPlugin("Authenticator")
							.getDescription().getVersion());
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

	public static String getVersion() {
		return instance.getDescription().getVersion();
	}

	@Override
	public void onDisable() {
		ready = false;
		try {
			CoreSQL.getInstance().onDisable();
			PlayerManager.getInstance().saveDatas();
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
			Config.reloadConfig();
		} catch (Exception e) {
			logException(e, "Unable to load config");
			this.Disable();
			return;
		}
		new PlayerManager(new File(getDataFolder(), "players.txt"));

		new UpdateEventListener();

		this.commandListener = new InventorySQLCommandListener();
		this.getCommand("invSQL").setExecutor(commandListener);
		this.getCommand("ichk").setExecutor(commandListener);

		startMetrics();
		if (Config.check_plugin_updates)
			startUpdate();

		reload();

		linkOfflineMode();
		linkVault();
	}

	private void linkOfflineMode() {
		Plugin p = Bukkit.getServer().getPluginManager()
				.getPlugin("Authenticator");
		if (p != null) {
			if (fr.areku.Authenticator.Authenticator.isUsingOfflineModePlugin()) {
				offlineModePlugin = true;
				fr.areku.Authenticator.Authenticator.setDebug(Config.debug,
						this);

				UpdateEventListener.registerOfflineModeSupport();
				InventorySQL
						.log("Using Authenticator for offline-mode support");
			}
		}
	}

	private void linkVault() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return;
		}
		RegisteredServiceProvider<Permission> rsp = getServer()
				.getServicesManager().getRegistration(Permission.class);
		if (rsp == null) {
			return;
		}
		perm = rsp.getProvider();
		vaultPlugin = (perm != null);
		if (vaultPlugin)
			InventorySQL.log("You have Vault ? oh great, so I'll use it");
	}

	private void startMetrics() {

		try {
			log("Starting Metrics");
			MetricsLite metrics = new MetricsLite(this);
			metrics.start();
		} catch (IOException e) {
			log("Cannot start Metrics...");
		}
	}

	private void startUpdate() {
		try {
			UpdateChecker update = new UpdateChecker(this);
			update.start();
		} catch (MalformedURLException e) {
			d("Cannot start Plugin Updater...");
			d(e.getMessage());
			// log("Cannot start Plugin Updater...");
		}
	}

	public void Disable() {
		Bukkit.getPluginManager().disablePlugin(this);
	}

	public void reload() {
		try {
			Config.reloadConfig();
		} catch (Exception e) {
			logException(e, "Unable to load config");
			this.Disable();
			return;
		}

		try {
			CoreSQL.getInstance().reload();
		} catch (ClassNotFoundException e) {
			log(Level.SEVERE, "Cannot found MySQL Class !");
			this.Disable();
			return;
		}
	}

	public static Permission getPerm() {
		return instance.perm;
	}

	public static boolean isUsingVault() {
		return instance.vaultPlugin;
	}

	public Player[] getOnlinePlayersSync() throws InterruptedException,
			ExecutionException {
		return callSyncMethod(new Callable<Player[]>() {
			@Override
			public Player[] call() throws Exception {
				return Bukkit.getOnlinePlayers();
			}
		}).get();
	}

	public Player getPlayerSync(final String p) throws InterruptedException,
			ExecutionException {
		return callSyncMethod(new Callable<Player>() {
			@Override
			public Player call() throws Exception {
				return Bukkit.getPlayer(p);
			}
		}).get();
	}

	public <T> Future<T> callSyncMethod(Callable<T> methode) {
		return Bukkit.getScheduler().callSyncMethod(this, methode);
	}

	public static InventorySQL getInstance() {
		return instance;
	}

	public static boolean isUsingAuthenticator() {
		return instance.offlineModePlugin;
	}

}
