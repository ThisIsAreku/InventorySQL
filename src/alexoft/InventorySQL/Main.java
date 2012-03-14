package alexoft.InventorySQL;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;



@SuppressWarnings("unused")
public class Main extends JavaPlugin {
    public static Logger l;
    public static String p_version;
    public static String b_version;
    public String dbDatabase = null;
    public String dbHost = null;
    public String dbPass = null;
    public String dbTable = null;
    public String dbUser = null;
    public static int verbosity = 2;
    public long delayCheck = 0;
    private InventorySQLPlayerListener playerListener;
    private InventorySQLCommandListener commandListener;
    public Database MYSQLDB;
    public Boolean MySQL = true;

    public static HashMap<String, String> MYSQL_FIELDS = new HashMap<String, String>();

    public static void log(Level level, String m) {
        if (level == Level.WARNING && verbosity < 2) {
            return;
        }
        if (level == Level.INFO && verbosity < 1) {
            return;
        }
        l.log(level, m);
    }

    public static void log(String m) {
        log(Level.INFO, m);
    }

    public static void logException(Exception e, String m) {
        if (e instanceof EmptyException) return;
        log(Level.SEVERE, "---------------------------------------");
        log(Level.SEVERE, "--- an unexpected error has occured ---");
        log(Level.SEVERE, "-- please send line below to the dev --");
        log(Level.SEVERE, "InventorySQL version " + p_version);
        log(Level.SEVERE, "Bukkit version " + b_version);
        log(Level.SEVERE, "Message: " + m);
        if (e instanceof SQLException) {
            log(Level.SEVERE, "SQLState: " + ((SQLException) e).getSQLState());
            log(Level.SEVERE, "Error Code: " + ((SQLException) e).getErrorCode());
        }
        log(Level.SEVERE, e.toString() + " : " + e.getLocalizedMessage());
        for (StackTraceElement t : e.getStackTrace()) {
            log(Level.SEVERE, "\t" + t.toString());
        }
        log(Level.SEVERE, "---------------------------------------");
    }
    
    private static void populateHashMap(){
    	MYSQL_FIELDS.put("id", "INT");
    	MYSQL_FIELDS.put("owner", "VARCHAR");
    	MYSQL_FIELDS.put("ischest", "BIT");
    	MYSQL_FIELDS.put("x", "INT");
    	MYSQL_FIELDS.put("y", "INT UNSIGNED");
    	MYSQL_FIELDS.put("z", "INT");
    	MYSQL_FIELDS.put("inventory", "LONGTEXT");
    	MYSQL_FIELDS.put("pendings", "LONGTEXT");
    	MYSQL_FIELDS.put("last_update", "TIMESTAMP");
    }

    @Override
    public void onDisable() {
        log("Disabling...");
        this.getServer().getScheduler().cancelTasks(this);
        //this.invokeCheck(false, null);

        log("Disabled !");
    }

    @Override
    public void onEnable() {
        l = this.getLogger();
        b_version = this.getServer().getVersion();
        p_version = this.getDescription().getVersion();
        log("ThisIsAreku present " + this.getDescription().getName().toUpperCase() + ", v" + p_version);
        log("= " + this.getDescription().getWebsite() + " =");
        log("Enabling...");

        populateHashMap();
        
        try {
            this.loadConfig();
        } catch (Exception e) {
            log("Unable to load config");
            this.Disable();
            return;
        }

        if (!this.MySQL) {
            log(Level.SEVERE, "Configuration error, plugin disabled");
            this.Disable();
            return;
        }
        try {
            MYSQLDB = new Database(
                    "jdbc:mysql://" + this.dbHost + "/" + this.dbDatabase,
                    this.dbUser, this.dbPass);

            log("MySQL connection successful");
            checkUpdateTable();
        } catch (SQLException ex) {
            Main.logException(ex, "mysql init");
            log(Level.SEVERE, "MySQL connection failed");
            this.Disable();
            return;
        } catch (ClassNotFoundException e) {
            Main.logException(e, "mysql init");
            log(Level.SEVERE, "MySQL connection failed");
            this.Disable();
            return;
        }

        this.playerListener = new InventorySQLPlayerListener(this);
        this.commandListener = new InventorySQLCommandListener(this);

        this.getCommand("invSQL").setExecutor(commandListener);

        this.getServer().getScheduler().scheduleAsyncRepeatingTask(this,
                new UpdateDatabase(this), 10 * 20, this.delayCheck);
        
        startMetrics();
        
        log("Enabled !");

        // debug code to pring pretty-formated ids
        // used to update the webui
        /*
         * for (Material m : Material.values()) { System.out.println("$items[" +
         * m.getId() + "] = '" + m.toString() + "';"); }
         */
    }
    
    public void startMetrics(){

        try {
            log("Starting Metrics");
            Metrics metrics = new Metrics();
            metrics.beginMeasuringPlugin(this);
        } catch (IOException e) {
            log("Cannot start Metrics...");
        }
    }

    public void Disable() {
        this.getPluginLoader().disablePlugin(this);
    }

    public void checkUpdateTable() {
        try {
        	InputStream is = this.getResource("alexoft/InventorySQL/schema.sql");
        	Scanner reader = new Scanner(is);
        	String query = "";
			while(reader.hasNextLine()) {
				query += " "+reader.nextLine();
			}
			query = query.replace("%%TABLENAME%%", this.dbTable);

            if (!this.MYSQLDB.tableExist(this.dbTable)) {
                log("Creating table...");
                if (this.MYSQLDB.queryUpdate(query) != 0) {
                    log(Level.SEVERE, "Cannot create table, check your config !");
                }
            } else {
                ResultSet rs = this.MYSQLDB.query("SELECT * FROM `" + this.dbTable + "`");
                ResultSetMetaData metadata = rs.getMetaData();

                if (metadata.getColumnCount() != MYSQL_FIELDS.size()) {
                    log("table is an old version, updating...");
                    this.MYSQLDB.queryUpdate("DROP TABLE "+this.dbTable+";");
                    if (this.MYSQLDB.queryUpdate(query) != 0) {
                        log(Level.SEVERE, "Cannot update table, check your config !");
                    }
                }else{
                	DatabaseMetaData meta = this.MYSQLDB.getMetaData();
                    ResultSet rsColumns = meta.getColumns(null, null, this.dbTable, null);
                    while (rsColumns.next()) {
                      String columnName = rsColumns.getString("COLUMN_NAME");
                      String columnType = rsColumns.getString("TYPE_NAME");
                      int size = rsColumns.getInt("COLUMN_SIZE");
                      if(MYSQL_FIELDS.get(columnName) != columnType){
                          log("table is an old version, updating...");
                          this.MYSQLDB.queryUpdate("DROP TABLE "+this.dbTable+";");
                          if (this.MYSQLDB.queryUpdate(query) != 0) {
                              log(Level.SEVERE, "Cannot update table, check your config !");
                          }
                    	  break;
                      }
                    }
                }
                rs.close();
            }
        } catch (Exception ex) {
            Main.logException(ex, "table need update?");
        }
    }

    public void loadConfig() throws FileNotFoundException, IOException,
                InvalidConfigurationException {
        File cfgDir = this.getDataFolder();
        File cfgFile = new File(cfgDir + "/config.yml");

        if (!cfgDir.exists()) {
            cfgDir.mkdirs();
        }
        if (!cfgFile.exists()) {
            try {
                cfgFile.createNewFile();
            } catch (IOException ex) {
                Main.logException(ex, "creating config");
            }
        } else {
            this.getConfig().load(cfgFile);
        }

        String tmp = String.valueOf(Math.random());

        this.MySQL = true;

        this.dbHost = this.getConfig().getString("mysql.host", "");
        this.dbUser = this.getConfig().getString("mysql.user", "");
        this.dbPass = this.getConfig().getString("mysql.pass", tmp);
        this.dbDatabase = this.getConfig().getString("mysql.db", "");
        this.dbTable = this.getConfig().getString("mysql.table", "");
        this.delayCheck = this.getConfig().getInt("check-interval", -1);
        Main.verbosity = this.getConfig().getInt("verbosity", -1);

        if (Main.verbosity == -1 || Main.verbosity > 2) {
            log(Level.WARNING, "Creating 'verbosity' config...");
            Main.verbosity = 2;
            this.getConfig().set("verbosity", Main.verbosity);
        }
        if (this.delayCheck == -1) {
            log(Level.WARNING, "Creating 'check-interval' config...");
            this.delayCheck = 600;
            this.getConfig().set("check-interval", this.delayCheck);
        }
        if (this.dbHost.equals("")) {
            log(Level.WARNING, "Creating 'host' config...");
            this.getConfig().set("mysql.host", "localhost");
            this.MySQL = false;
        }
        if (this.dbUser.equals("")) {
            log(Level.WARNING, "Creating 'user' config...");
            this.getConfig().set("mysql.user", "root");
            this.MySQL = false;
        }
        if (this.dbPass.equals(tmp)) {
            log(Level.WARNING, "Creating 'pass' config...");
            this.getConfig().set("mysql.pass", "pass");
            this.MySQL = false;
        }
        if (this.dbDatabase.equals("")) {
            log(Level.WARNING, "Creating 'db' config...");
            this.getConfig().set("mysql.db", "minecraft");
            this.MySQL = false;
        }
        if (this.dbTable.equals("")) {
            log(Level.WARNING, "Creating 'table' config...");
            this.getConfig().set("mysql.table", "InventorySQL");
            this.MySQL = false;
        }
        this.delayCheck *= 20;
        this.getConfig().save(cfgFile);
    }

    private void updateUser(Player[] players, boolean async, int delay, CommandSender cs) {
        if (async) {
            this.getServer().getScheduler().scheduleAsyncDelayedTask(this,
                    new UpdateDatabase(this, true, players, cs), delay * 20);
        } else {
            this.getServer().getScheduler().scheduleSyncDelayedTask(this,
                    new UpdateDatabase(this, true, players, cs), delay * 20);
        }
    }

    public void invokeCheck(boolean async, CommandSender cs) {
        updateUser(this.getServer().getOnlinePlayers(), async, 5, cs);
    }

    public void invokeCheck(Player[] players, boolean async, CommandSender cs) {
        updateUser(players, async, 5, cs);
    }

    public void invokeCheck(boolean async, int delay, CommandSender cs) {
        updateUser(this.getServer().getOnlinePlayers(), async, delay, cs);
    }

    public void invokeCheck(Player[] players, boolean async, int delay, CommandSender cs) {
        updateUser(players, async, delay, cs);
    }
}
