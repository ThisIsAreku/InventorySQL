package alexoft.InventorySQL;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Database {
	private String user;
	private String password;
	private String url;
	private Connection conn;

	public Database(String url, String user, String password)
			throws SQLException, ClassNotFoundException {

		this.user = user;
		this.password = password;
		this.url = url;
		openConnection();
		if (!checkConnectionIsAlive(false)) {
			conn.close();
			throw new SQLException("Could not create new connection");
		}
	}

	private void openConnection() throws SQLException, ClassNotFoundException {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(this.url, this.user,
					this.password);
		} catch (SQLException e) {
			throw new SQLException("Could not create new connection", e);
		} catch (ClassNotFoundException e) {
			throw new ClassNotFoundException("Could not create new connection",
					e);
		}
	}

	public boolean checkConnectionIsAlive(boolean reopen) {
		try {
			if (conn.isClosed()) {
				return false;
			}
			if (conn.isValid(10)) {
				return true;
			} else {
				if (reopen) {
					openConnection();
				}
				return checkConnectionIsAlive(false);
			}
		} catch (SQLException e) {
			Main.logException(e, "checkConnectionIsAlive");
			return false;
		} catch (ClassNotFoundException e) {
			Main.logException(e, "checkConnectionIsAlive");
			return false;
		}
	}

	public ResultSet query(String sql) throws SQLException {
		return conn.createStatement().executeQuery(sql);
	}

	public int queryUpdate(String sql) throws EmptyException {
		try {
			//Main.log(sql);
			return conn.createStatement().executeUpdate(sql);
		} catch (SQLException ex) {
			Main.logException(ex, "R: " + sql);
			throw new EmptyException();
		}
	}

	public boolean queryBool(String sql) throws EmptyException {
		try {
			return conn.createStatement().execute(sql);
		} catch (SQLException ex) {
			Main.logException(ex, "R: " + sql);
			throw new EmptyException();
		}
	}

	public boolean tableExist(String name) throws SQLException {
		DatabaseMetaData dbm = conn.getMetaData();
		// check if "employee" table is there
		ResultSet tables = dbm.getTables(null, null, name, null);

		return tables.next();
	}

}
