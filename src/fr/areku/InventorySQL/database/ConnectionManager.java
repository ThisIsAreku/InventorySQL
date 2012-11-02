package fr.areku.InventorySQL.database;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import fr.areku.InventorySQL.InventorySQL;
import fr.areku.InventorySQL.Config;

public class ConnectionManager implements Closeable{
	private static ConnectionManager instance;

	private boolean ready = false;
	//private static int poolsize = 3;
	private static long timeToLive = 300000;
	private static List<JDCConnection> connections;
	private final ConnectionReaper reaper;
	private final String url;
	private final String user;
	private final String password;

	public ConnectionManager(String url, String user, String password)
			throws ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
		InventorySQL.d("Attempting to connecting to database at: " + url);
		this.url = url;
		this.user = user;
		this.password = password;
		// poolsize = Config.PoolSize;
		ready = true;
		connections = new ArrayList<JDCConnection>(Config.dbPoolSize);
		reaper = new ConnectionReaper();
		reaper.start();
		instance = this;
	}
	public boolean isReady(){
		return ready;
	}

	public static ConnectionManager getInstance() {
		return instance;
	}

	/**
	 * Returns a connection from the pool
	 * 
	 * @return returns a {JDCConnection}
	 * @throws SQLException
	 */
	public synchronized JDCConnection getConnection() throws SQLException {
		if(!ready) return null;
		JDCConnection conn;
		for (int i = 0; i < connections.size(); i++) {
			conn = connections.get(i);
			if (conn.lease()) {
				if (conn.isValid())
					return conn;
				InventorySQL.d("Removing dead MySQL connection");
				connections.remove(conn);
				conn.terminate();
			}
		}
		InventorySQL.d("No available MySQL connections, attempting to create new one");
		conn = new JDCConnection(DriverManager.getConnection(url, user,
				password));
		conn.lease();
		if (!conn.isValid()) {
			conn.terminate();
			throw new SQLException("Could not create new connection");
		}
		connections.add(conn);
		return conn;
	}

	/**
	 * Removes a connection from the pool
	 * 
	 * @param {JDCConnection} to remove
	 */
	public static synchronized void removeConn(Connection conn) {
		connections.remove((JDCConnection) conn);
	}

	/**
	 * Loops through connections, reaping old ones
	 */
	private synchronized void reapConnections() {
		if(!ready) return;
		
		InventorySQL.d("Attempting to reap dead connections");
		final long stale = System.currentTimeMillis() - timeToLive;
		int count = 0;
		int i = 1;
		for (final JDCConnection conn : connections) {
			if (conn.inUse() && stale > conn.getLastUse() && !conn.isValid()) {
				connections.remove(conn);
				count++;
			}

			if (i > Config.dbPoolSize) {
				connections.remove(conn);
				count++;
				conn.terminate();
			}
			i++;
		}
		InventorySQL.d(count + " connections reaped");
	}

	/**
	 * Reaps connections
	 * 
	 * @author oliverw92
	 */
	private class ConnectionReaper extends Thread {
		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(300000);
				} catch (final InterruptedException e) {
				}
				reapConnections();
			}
		}
	}

	public void close() {
			ready = false;
			InventorySQL.d("Closing all MySQL connections");
			for (JDCConnection conn : connections) {
				conn.terminate();
			}
			connections.clear();
	}
}
