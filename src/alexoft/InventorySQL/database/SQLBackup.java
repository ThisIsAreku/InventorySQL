package alexoft.InventorySQL.database;

import java.sql.SQLException;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import alexoft.InventorySQL.Config;
import alexoft.InventorySQL.Constants;
import alexoft.InventorySQL.Main;

public class SQLBackup implements Runnable {
	public enum BackupAction {
		NONE, BACKUP, SHOW_BACKUP, RESTORE
	}

	private CoreSQLProcess parent;
	private JDCConnection conn = null;
	private long ITERATION = 0;
	private final long SEC_BETWEEN_CLEANUP = (Config.backup_cleanup_days * 86400) / 2;
	private BackupAction manualAction = BackupAction.NONE;
	private String player = "";
	private CommandSender cs = null;

	public SQLBackup(CoreSQLProcess parent) {
		Main.d("New SQLBackup !");
		this.parent = parent;
	}

	public SQLBackup restore(String player) {
		Main.d("SQLBackup->restore");
		this.player = player;
		manualAction = BackupAction.RESTORE;
		return this;
	}

	public SQLBackup showBackups(String player) {
		Main.d("SQLBackup->restore");
		this.player = player;
		manualAction = BackupAction.RESTORE;
		return this;
	}

	private void updateConn() {
		if (conn == null) {
			conn = this.parent.getConnection();
		} else {
			if (!conn.isValid())
				conn = this.parent.getConnection();
		}
	}

	@Override
	public void run() {
		Main.d("Running Scheduled backup..");
		try {
			updateConn();
			if (manualAction == BackupAction.RESTORE) {

			} else if (manualAction == BackupAction.SHOW_BACKUP) {

				if (cs != null) {
					cs.sendMessage(
							"[InventorySQL] " + ChatColor.GREEN
									+ "(" + player + ") "
									+ Main.getMessage("no-modif"));
				}

			} else if (manualAction == BackupAction.BACKUP) {
				if (this.parent.plugin.getOfflineModeController().isUsingOfflineModePlugin() && !this.parent.plugin.getOfflineModeController().isPlayerLoggedIn(player)) {
					return;
				}
				Player p = this.parent.getPlayer(player);
				if (p != null) {
					try {
						conn.createStatement().executeUpdate(
								String.format(Constants.REQ_INSERT_BACKUP,
										Config.dbTable, p.getName(), p
												.getWorld().getName(), p
												.getLocation().getBlockX(), p
												.getLocation().getBlockY(), p
												.getLocation().getBlockZ(),
										CoreSQLProcess.buildInvString(p
												.getInventory())));
					} catch (SQLException e) {
					}
				}
			} else if (manualAction == BackupAction.NONE) {
				Player[] players = this.parent.getOnlinePlayers();

				if (players.length != 0) {
					for (Player p : players) {
						if (this.parent.plugin.getOfflineModeController().isUsingOfflineModePlugin() && !this.parent.plugin.getOfflineModeController().isPlayerLoggedIn(p.getName())) {
							continue;
						}
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
