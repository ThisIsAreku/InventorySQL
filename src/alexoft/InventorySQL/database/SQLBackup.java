package alexoft.InventorySQL.database;

import java.sql.SQLException;

import org.bukkit.entity.Player;

import alexoft.InventorySQL.Config;
import alexoft.InventorySQL.Constants;
import alexoft.InventorySQL.Main;

public class SQLBackup implements Runnable {
	private CoreSQLProcess parent;
	private long ITERATION = 0;
	private final long SEC_BETWEEN_CLEANUP = (Config.backup_cleanup_days * 86400) /2;

	public SQLBackup(CoreSQLProcess parent) {
		Main.d("New SQLBackup !");
		this.parent = parent;
	}

	@Override
	public void run() {
		Main.d("Running Scheduled backup..");
		try {
			JDCConnection conn = null;
			Player[] players = this.parent.getOnlinePlayers();

			if (players.length != 0) {
				conn = this.parent.getConnection();
				if (conn != null) {
					for (Player p : players) {
						try {
							conn.createStatement().executeUpdate(
									String.format(Constants.REQ_INSERT_BACKUP,
											Config.dbTable, p.getName(), p
													.getWorld().getName(), p
													.getLocation().getBlockX(),
											p.getLocation().getBlockY(), p
													.getLocation().getBlockZ(),
											CoreSQLProcess.buildInvString(p
													.getInventory())));
						} catch (SQLException e) {
						}
					}
				}
			}
			if ((ITERATION * Config.backup_interval) >= (SEC_BETWEEN_CLEANUP)) {
				Main.d("CLEANING BACKUP");
				if (conn == null)
					conn = this.parent.getConnection();
				if (conn != null) {
					conn.createStatement()
							.executeUpdate(
									String.format(Constants.REQ_CLEANUP_BACKUP,
											Config.dbTable,
											Config.backup_cleanup_days));
				}
				ITERATION = -1;
			}
			if (conn != null)
				conn.close();
		} catch (Exception e) {
			Main.logException(e, "Cannot do backup");
		}
		ITERATION++;
	}

}
