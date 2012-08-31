package fr.areku.InventorySQL.database;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;

import fr.areku.InventorySQL.Config;
import fr.areku.InventorySQL.EmptyException;
import fr.areku.InventorySQL.InventorySQL;

public class SQLUpdater {
	private ConnectionManager connectionManager = null;
	private InventorySQL plugin = null;
	private static final HashMap<String, String> tableFirstRow = new HashMap<String, String>();
	static {
		tableFirstRow.put("_inventories",
				"varchar(36) NOT NULL, PRIMARY KEY (`id`)");
		tableFirstRow.put("_backups",
				"varchar(36) NOT NULL, PRIMARY KEY (`id`)");
		tableFirstRow.put("_pendings",
				"varchar(36) NOT NULL, PRIMARY KEY (`id`)");
		tableFirstRow.put("_enchantments", "varchar(36) NOT NULL");
		tableFirstRow.put("_users",
				"int(11) NOT NULL AUTO_INCREMENT, PRIMARY KEY (`id`)");
	}

	public SQLUpdater(InventorySQL plugin, ConnectionManager connectionManager) {
		this.plugin = plugin;
		this.connectionManager = connectionManager;
	}

	public boolean checkUpdateTable() {
		try {
			JDCConnection conn = this.connectionManager.getConnection();
			check_table_version("_inventories", conn);
			check_table_version("_backups", conn);
			check_table_version("_pendings", conn);
			check_table_version("_enchantments", conn);
			check_table_version("_users", conn);
			/*
			 * if (Config.backup_enabled) check_table_version("_backup", conn);
			 */
			conn.close();
			return true;
		} catch (SQLException ex) {
			InventorySQL.log(Level.SEVERE, "Cannot connect to mySQL database !");
			InventorySQL.log(Level.SEVERE, "Message: " + ex.getLocalizedMessage());
		} catch (Exception ex) {
			InventorySQL.logException(ex, "table need update?");
		}
		return false;
	}

	private void check_table_version(String selector, JDCConnection conn)
			throws SQLException, EmptyException {
		if (!JDBCUtil.tableExistsCaseSensitive(conn.getMetaData(),
				Config.dbTablePrefix + selector)) {
			InventorySQL.log("Creating '" + Config.dbTablePrefix + selector
					+ "' table...");
			String create = "CREATE TABLE IF NOT EXISTS `"
					+ Config.dbTablePrefix + selector + "` (`id` "
					+ tableFirstRow.get(selector)
					+ ") ENGINE=MyISAM  DEFAULT CHARSET=utf8;";
			if (conn.createStatement().executeUpdate(create) != 0) {
				InventorySQL.log(Level.SEVERE, "Cannot create table '"
						+ Config.dbTablePrefix + selector
						+ "', check your config !");
			} else {
				update_table_fields(selector, conn);
			}
		} else {
			ResultSet rs = conn.createStatement().executeQuery(
					"SHOW CREATE TABLE `" + Config.dbTablePrefix + selector
							+ "`");
			rs.first();
			String comment = rs.getString(2);
			int p = comment.indexOf("COMMENT='");
			if (p == -1) {
				update_table_fields(selector, conn);
			} else {
				comment = comment
						.substring(p + 9, comment.indexOf('\'', p + 9));

				if (!("table format : " + plugin.getDescription().getVersion())
						.equals(comment)) {
					update_table_fields(selector, conn);
				}
			}
			rs.close();
		}
	}

	private void update_table_fields(String selector, JDCConnection conn)
			throws SQLException {
		InventorySQL.log("Table '" + Config.dbTablePrefix + selector + "' need update");
		String query = read(plugin
				.getResource("fr/areku/InventorySQL/schemas/schema" + selector
						+ ".sql"));
		query = query.replace("%%TABLENAME%%", Config.dbTablePrefix + selector);
		for (String r : query.split(";")) {
			try {
				// Main.d("Update:" + r);
				conn.createStatement().executeUpdate(r);
			} catch (SQLException e) {
			}
		}
		query = "ALTER IGNORE TABLE `%%TABLENAME%%` COMMENT = 'table format : %%VERSION%%'"
				.replace("%%TABLENAME%%", Config.dbTablePrefix + selector)
				.replace("%%VERSION%%", plugin.getDescription().getVersion());
		try {
			conn.createStatement().executeUpdate(query);
		} catch (SQLException e) {
		}
		InventorySQL.log("'" + Config.dbTablePrefix + selector + "' table: update done");
	}

	private String read(InputStream src) {
		Scanner reader = new Scanner(src);
		String s = "";
		String l;
		while (reader.hasNextLine()) {
			l = reader.nextLine();
			if (!l.startsWith("#")) {
				s += System.getProperty("line.separator") + l;
			}
		}
		try {
			src.close();
			reader.close();
		} catch (Exception e) {
		}
		return s;
	}

}
