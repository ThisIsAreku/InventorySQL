package fr.areku.InventorySQL.database;

import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.bukkit.entity.Player;

import fr.areku.InventorySQL.Config;
import fr.areku.InventorySQL.Main;

/**
 * 
 * @author Alexandre
 */
public class CoreSQLProcess implements Runnable {

	public Main plugin;
	private SQLUpdater sqlUpdater;
	public ConnectionManager connectionManager = null;
	private boolean databaseReady = false;
	private PlayersInventoryHash trackPlayerInventoryHash;

	/*
	 * private int backupProcess = 0; private int checkProcess = 0;
	 */

	public CoreSQLProcess(Main plugin) {
		this.plugin = plugin;
	}

	public void reload() throws ClassNotFoundException {
		Main.log(Level.INFO, "Reloading SQL process");
		this.trackPlayerInventoryHash = new PlayersInventoryHash(
				plugin.getDataFolder());
		this.databaseReady = false;
		if (this.connectionManager != null)
			this.connectionManager.close();
		this.connectionManager = new ConnectionManager("jdbc:mysql://"
				+ Config.dbHost + "/" + Config.dbDatabase, Config.dbUser,
				Config.dbPass);
		this.plugin.getServer().getScheduler()
				.scheduleAsyncDelayedTask(this.plugin, this);
	}

	public void run() {
		this.sqlUpdater = new SQLUpdater(plugin, this.connectionManager);
		if (this.sqlUpdater.checkUpdateTable()) {
			this.plugin.getServer().getScheduler().cancelTasks(plugin);
			if (Config.check_interval > 0) {
				Main.d("Init Check at interval " + Config.check_interval);
				this.plugin
						.getServer()
						.getScheduler()
						.scheduleAsyncRepeatingTask(plugin,
								new SQLCheck(this, "Scheduler"),
								Config.check_interval, Config.check_interval);
			}

			if (Config.backup_enabled)
				this.plugin
						.getServer()
						.getScheduler()
						.scheduleAsyncRepeatingTask(plugin,
								new SQLBackup(this).clean(),
								Config.check_interval,
								Config.check_interval * 2);

			this.databaseReady = true;
			Main.log("InventorySQL is ready ! :)");
		} else {
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

	public void runCheckThisTask(CoreSQLItem i, String initiator,
			boolean doGive, int delay) {
		if (this.databaseReady)
			this.plugin
					.getServer()
					.getScheduler()
					.scheduleAsyncDelayedTask(
							plugin,
							new SQLCheck(this, initiator)
									.manualCheck(i, doGive), delay);
	}

	public void runCheckAllTask(int delay) {
		if (this.databaseReady)
			this.plugin
					.getServer()
					.getScheduler()
					.scheduleAsyncDelayedTask(plugin,
							new SQLCheck(this, "CheckAll").manualCheck(), delay);
	}

	public void runBackupClean() {
		Main.log("Backup is not implemented now, sorry !");
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

	public boolean isPlayerInventoryModified(Player p) {
		return this.trackPlayerInventoryHash.isPlayerInventoryModified(p);
	}

	public long getPlayerLastCheck(Player p) {
		return this.trackPlayerInventoryHash.getPlayerLastCheck(p);
	}

	public void updatePlayerLastCheck(Player p, long currentEpoch) {
		this.trackPlayerInventoryHash.updatePlayerLastCheck(p, currentEpoch);
	}

}
