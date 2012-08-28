package fr.areku.InventorySQL.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.mysql.jdbc.Statement;

import fr.areku.InventorySQL.Config;
import fr.areku.InventorySQL.Main;


public class SQLBackup implements Runnable {
	public enum BackupAction {
		SHOW_BACKUP, RESTORE, CLEAN, NONE
	}

	private CoreSQLProcess parent;
	private JDCConnection conn = null;
	private long ITERATION = 0;
	private final long SEC_BETWEEN_CLEANUP = (Config.backup_cleanup_days * 86400) / 2;
	private BackupAction manualAction = BackupAction.NONE;
	@SuppressWarnings("unused")
	private String player = "";
	private CommandSender cs = null;

	public SQLBackup(CoreSQLProcess parent) {
		Main.d("New SQLBackup !");
		this.parent = parent;
	}

	public SQLBackup restore(String player, int id) {
		Main.d("SQLBackup->restore");
		this.player = player;
		manualAction = BackupAction.RESTORE;
		return this;
	}

	public SQLBackup showBackups(String player) {
		Main.d("SQLBackup->showBackups");
		this.player = player;
		manualAction = BackupAction.SHOW_BACKUP;
		return this;
	}

	public SQLBackup clean() {
		Main.d("SQLBackup->clean");
		manualAction = BackupAction.CLEAN;
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
		Main.d("Running Scheduled backup, action: "+manualAction);
		try {
			updateConn();
			if (manualAction == BackupAction.RESTORE) {

			} else if (manualAction == BackupAction.SHOW_BACKUP) {

				if (cs != null) {
					cs.sendMessage("[InventorySQL] " + ChatColor.GREEN + "(Not implemented yet)");
				}

			}
			if ((manualAction == BackupAction.CLEAN)||(ITERATION * Config.check_interval) >= (SEC_BETWEEN_CLEANUP)) {
				Main.d("CLEANING BACKUP");
				if (conn == null)
					conn = this.parent.getConnection();
				if (conn != null) {

					/* Clear old inv */
					String q = "DELETE `backups`, `enchantments` FROM `"
							+ Config.dbTable_Backups
							+ "` AS `backups` LEFT JOIN `"
							+ Config.dbTable_Enchantments
							+ "` AS `enchantments` ON (`inventories`.`id` = `enchantments`.`id` AND `enchantments`.`is_backup` = 1)"
							+ " WHERE `inventories`.`date` < (CURRENT_TIMESTAMP - INTERVAL '"+Config.backup_cleanup_days+"' DAY);";
					PreparedStatement sth = conn.prepareStatement(q, Statement.RETURN_GENERATED_KEYS);
					sth.executeUpdate();
					ResultSet rs = sth.getGeneratedKeys();
					int rows = 0;
					if (rs.first()) {
						rows =  rs.getInt(1);
					} else {
						rows =  -1;
					}
					sth.close();
					Main.d(rows + " rows deleted (inv+ench)");
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
