package fr.areku.InventorySQL.database;

import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import fr.areku.InventorySQL.Config;
import fr.areku.InventorySQL.InventorySQL;

/**
 * 
 * @author Alexandre
 */
public class CoreSQLProcess implements Runnable {

	private InventorySQL plugin;
	public ConnectionManager connectionManager = null;
	private boolean databaseReady = false;

	public CoreSQLProcess(InventorySQL plugin) {
		this.plugin = plugin;
	}

	public void reload() throws ClassNotFoundException {
		InventorySQL.log(Level.INFO, "Reloading SQL process");
		this.databaseReady = false;
		if (this.connectionManager != null)
			this.connectionManager.close();
		this.connectionManager = new ConnectionManager("jdbc:mysql://"
				+ Config.dbHost + "/" + Config.dbDatabase, Config.dbUser,
				Config.dbPass);
		Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, this);
	}

	/**
	 * Run at server stop, release connections
	 */
	public void onDisable() {
		disable();
	}

	private void disable() {
		this.databaseReady = false;
		Bukkit.getScheduler().cancelTasks(plugin);
		if (this.connectionManager != null)
			this.connectionManager.close();
		this.connectionManager = null;
	}

	/**
	 * Runnable task; check the connection and update the tables
	 */
	@Override
	public void run() {
		SQLUpdater updater = new SQLUpdater(plugin, this.connectionManager);

		if (updater.checkUpdateTable()) {
			Bukkit.getScheduler().cancelTasks(plugin);
			if (Config.check_interval > 0) {
				InventorySQL.d("Init Check at interval "
						+ Config.check_interval);
				Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin,
						new SQLCheck(this, "Scheduler"), Config.check_interval,
						Config.check_interval);
			}

			if (Config.backup_enabled)
				Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin,
						new SQLBackup(this).clean(), Config.check_interval,
						Config.check_interval * 2);

			this.databaseReady = true;
			InventorySQL.log("InventorySQL is ready ! :)");
		} else {
			InventorySQL.log(Level.WARNING,
					"Check your config and use /invsql reload");
			disable();
		}
	}

	private void runTask(Runnable task, int delay) {
		if (this.databaseReady)
			Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, task, delay);
	}

	public void runCheckThisTask(CoreSQLItem i, String initiator,
			boolean doGive, int delay) {
		runTask(new SQLCheck(this, initiator).manualCheck(i, doGive), delay);
	}

	public void runCheckAllTask(int delay) {
		runTask(new SQLCheck(this, "CheckAll").manualCheck(), delay);
	}

	public void runBackupClean() {
		InventorySQL.log("Backup is not implemented now, sorry !");
		/*
		 * this.plugin .getServer() .getScheduler()
		 * .scheduleAsyncDelayedTask(plugin, new SQLBackup(this).clean());
		 */
	}

	public boolean updatePlayerPassword(String player, String password) {
		try {
			JDCConnection conn = this.connectionManager.getConnection();
			conn.createStatement().executeUpdate(
					"INSERT `" + Config.dbTable_Users
							+ "`(`name`,`password`) VALUES ('" + player
							+ "', MD5('" + password
							+ "')) ON DUPLICATE KEY UPDATE `password`= MD5('"
							+ password + "');");
			conn.close();
			return true;
		} catch (SQLException e) {
			InventorySQL.logException(e, "updatePassword");
			return false;
		}
	}

	public synchronized boolean isDatabaseReady() {
		return this.databaseReady;
	}

	public Player[] getOnlinePlayers() throws InterruptedException,
			ExecutionException {
		return callSyncMethod(new Callable<Player[]>() {
			@Override
			public Player[] call() throws Exception {
				return Bukkit.getOnlinePlayers();
			}
		}).get();
	}

	public Player getPlayer(final String p) throws InterruptedException,
			ExecutionException {
		return callSyncMethod(new Callable<Player>() {
			@Override
			public Player call() throws Exception {
				return Bukkit.getPlayer(p);
			}
		}).get();
	}

	public <T> Future<T> callSyncMethod(Callable<T> methode) {
		return Bukkit.getScheduler().callSyncMethod(plugin, methode);
	}

	public synchronized JDCConnection getConnection() {
		try {
			return this.connectionManager.getConnection();
		} catch (SQLException e) {
			InventorySQL.logException(e, "Cannot get a new connection");
		}
		return null;
	}

}
