package fr.areku.InventorySQL.database;

import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.areku.InventorySQL.Config;
import fr.areku.InventorySQL.InventorySQL;
import fr.areku.InventorySQL.database.methods.PlayerCheck;

/**
 * 
 * @author Alexandre
 */
public class CoreSQL implements SQLUpdaterResult {
	private static CoreSQL instance = null;

	public static CoreSQL getInstance() {
		if (instance == null)
			instance = new CoreSQL();
		return instance;
	}

	public ConnectionManager connectionManager = null;
	private boolean databaseReady = false;

	public CoreSQL() {
	}

	public void reload() throws ClassNotFoundException {
		InventorySQL.log(Level.INFO, "Reloading SQL process");
		initialize();
	}

	public void initialize() throws ClassNotFoundException {
		this.databaseReady = false;
		if (this.connectionManager != null)
			this.connectionManager.close();
		this.connectionManager = new ConnectionManager("jdbc:mysql://"
				+ Config.dbHost + "/" + Config.dbDatabase, Config.dbUser,
				Config.dbPass);
		if (this.connectionManager.isReady()) {
			SQLUpdater updater = new SQLUpdater(this);
			updater.checkUpdateTable();
		}
	}

	/**
	 * Run at server stop, release connections
	 */
	public void onDisable() {
		disable();
	}

	private void disable() {
		this.databaseReady = false;
		Bukkit.getScheduler().cancelTasks(InventorySQL.getInstance());
		if (this.connectionManager != null)
			this.connectionManager.close();
		this.connectionManager = null;
	}

	private void runTask(Runnable task, int delay) {
		if (this.databaseReady)
			Bukkit.getScheduler().runTaskLaterAsynchronously(
					InventorySQL.getInstance(), task, delay);
	}

	public void runPlayerCheck(String initiator, CommandSender cs,
			boolean doGive, int delay) {
		runTask(new PlayerCheck(initiator, cs).setDoGive(doGive), delay);
	}

	public void runPlayerCheck(Player p, String initiator, CommandSender cs,
			boolean doGive, int delay) {
		runTask(new PlayerCheck(initiator, cs).setDoGive(doGive).setTarget(p),
				delay);
	}

	public void runPlayerCheck(Player p, String initiator, CommandSender cs) {
		runTask(new PlayerCheck(initiator, cs).setTarget(p), 0);
	}

	public void runPlayerCheck(Player[] p, String initiator, CommandSender cs) {
		runTask(new PlayerCheck(initiator, cs).setTarget(p), 0);
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
		return this.databaseReady && this.connectionManager.isReady();
	}

	public synchronized JDCConnection getConnection() {
		try {
			return this.connectionManager.getConnection();
		} catch (SQLException e) {
			InventorySQL.logException(e, "Cannot get a new connection");
		}
		return null;
	}

	@Override
	public void updateResult(boolean success) {
		if (success) {
			Bukkit.getScheduler().cancelTasks(InventorySQL.getInstance());
			if (Config.check_interval > 0) {
				InventorySQL.d("Init Check at interval "
						+ Config.check_interval);
				Bukkit.getScheduler().runTaskTimerAsynchronously(
						InventorySQL.getInstance(),
						new PlayerCheck("Scheduler", null),
						Config.check_interval, Config.check_interval);
			}

			if (Config.backup_enabled)
				Bukkit.getScheduler().runTaskTimerAsynchronously(
						InventorySQL.getInstance(),
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

}
