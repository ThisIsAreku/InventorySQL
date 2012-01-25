package com.alta189.MySQL;


// ~--- JDK imports ------------------------------------------------------------

import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;


public class mysqlCore {
    public String           database;
    public String           host;
    private Logger          log;
    private String          logPrefix;
    private DatabaseHandler manageDB;
    public String           password;
    public String           username;

    public mysqlCore(Logger log, String logPrefix, String host, String database, String username, String password) {
        this.log = log;
        this.logPrefix = logPrefix;
        this.database = database;
        this.host = host;
        this.username = username;
        this.password = password;
    }

    public void initialize() {
        this.manageDB = new DatabaseHandler(this, this.host, this.database,
                this.username, this.password);
    }

    public void writeInfo(String toWrite) {
        if (toWrite != null) {
            this.log.info(this.logPrefix + toWrite);
        }
    }

    public void writeError(String toWrite, Boolean severe) {
        if (severe) {
            if (toWrite != null) {
                this.log.severe(this.logPrefix + toWrite);
            }
        } else {
            if (toWrite != null) {
                this.log.warning(this.logPrefix + toWrite);
            }
        }
    }

    public ResultSet sqlQuery(String query)
        throws MalformedURLException, InstantiationException, IllegalAccessException {
        return this.manageDB.sqlQuery(query);
    }

    public Boolean createTable(String query) {
        return this.manageDB.createTable(query);
    }

    public void insertQuery(String query) throws MalformedURLException, InstantiationException, IllegalAccessException {
        this.manageDB.insertQuery(query);
    }

    public void updateQuery(String query) throws MalformedURLException, InstantiationException, IllegalAccessException {
        this.manageDB.updateQuery(query);
    }

    public void deleteQuery(String query) throws MalformedURLException, InstantiationException, IllegalAccessException {
        this.manageDB.deleteQuery(query);
    }

    public Boolean checkTable(String table)
        throws MalformedURLException, InstantiationException, IllegalAccessException {
        return this.manageDB.checkTable(table);
    }

    public Boolean wipeTable(String table)
        throws MalformedURLException, InstantiationException, IllegalAccessException {
        return this.manageDB.wipeTable(table);
    }

    public Connection getConnection() throws MalformedURLException, InstantiationException, IllegalAccessException, SQLException {
        return this.manageDB.getConnection();
    }

    public void close() {
        this.manageDB.closeConnection();
    }

    public Boolean checkConnection() throws MalformedURLException, InstantiationException, IllegalAccessException {
        return this.manageDB.checkConnection();
    }
}
