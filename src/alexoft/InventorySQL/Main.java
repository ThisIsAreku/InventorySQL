package alexoft.InventorySQL;

import PatPeter.SQLibrary.MySQL;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.logging.Level;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Alexandre
 */
public class Main extends JavaPlugin {

    public String dbDatabase = null;
    public String dbHost = null;
    public String dbPass = null;
    public String dbTable = null;
    public String dbUser = null;
    public int verbosity = 0;
    public long delayCheck = 0;
    private InventorySQLPlayerListener playerListener;
    private InventorySQLCommandListener commandListener;
    public Boolean MySQL = true;
    public MySQL manageMySQL;
    public static String[] MYSQL_FIELDS = new String[]{
        "id", "owner", "ischest", "x", "y", "z", "inventory", "pendings"
    };

    public void log(Level level, String l) {
        if ((level == Level.FINER) && (verbosity < 2)) {
            return;
        }
        if ((level == Level.FINE) && (verbosity < 1)) {
            return;
        }
        this.getServer().getLogger().log(level, "[InventorySQL] " + l);
    }

    public void log(String l) {
        log(Level.INFO, l);
    }

    public void logException(Throwable e) {
        log(Level.SEVERE, "---------------------------------------");
        log(Level.SEVERE, "--- an unexpected error has occured ---");
        log(Level.SEVERE, "-- please send line below to the dev --");
        log(Level.SEVERE, e.toString() + " : " + e.getLocalizedMessage());
        for (StackTraceElement t : e.getStackTrace()) {
            log(Level.SEVERE, "\t" + t.toString());
        }
        log(Level.SEVERE, "---------------------------------------");
    }

    @Override
    public void onDisable() {
        log("Disabling...");
        this.getServer().getScheduler().cancelTasks(this);
        this.invokeCheck(false);

        log("Disabled !");
    }

    @Override
    public void onEnable() {
        log("ThisIsAreku present INVENTORYSQL, v"
                + this.getDescription().getVersion());
        log("Enabling...");

        try {
            this.loadConfig();
        } catch (Exception e) {
            log("Unable to load config");
            this.Disable();
        }

        if (this.MySQL) {
            try {
                manageMySQL = new MySQL(this.getServer().getLogger(),
                        "[InventorySQL] ", this.dbHost, "3306", this.dbDatabase, this.dbUser,
                        this.dbPass);

                if (this.manageMySQL.checkConnection()) {
                    log("MySQL connection successful");
                    checkUpdateTable();

                    this.playerListener = new InventorySQLPlayerListener(this);
                    this.commandListener = new InventorySQLCommandListener(this);

                    this.getCommand("invSQL").setExecutor(commandListener);

                    this.getServer().getScheduler().scheduleAsyncRepeatingTask(this,
                            new UpdateDatabase(this), 10 * 20, this.delayCheck);
                    log("Enabled !");
                } else {
                    log(Level.SEVERE, "MySQL connection failed");
                    this.Disable();
                }
            } catch (Exception ex) {
                this.logException(ex);
                log(Level.SEVERE, "MySQL connection failed");
                this.Disable();
            }
        } else {
            log(Level.SEVERE, "Configuration error, plugin disabled");
            this.Disable();
        }
    }

    public void Disable() {
        this.getPluginLoader().disablePlugin(this);
    }

    public void checkUpdateTable() {
        try {
            String query = "CREATE TABLE `"
                    + this.dbTable
                    + "` (`id` INT NOT NULL AUTO_INCREMENT,"
                    + "`owner` VARCHAR(32) NOT NULL,"
                    + "`ischest` tinyint(1) NOT NULL DEFAULT '0',"
                    + "`x` int(11) NOT NULL DEFAULT '0',"
                    + "`y` tinyint(3) unsigned NOT NULL DEFAULT '0',"
                    + "`z` int(11) NOT NULL DEFAULT '0',"
                    + "`inventory` longtext,"
                    + "`pendings` longtext, PRIMARY KEY (`id`))"
                    + "ENGINE=InnoDB DEFAULT CHARSET=utf8;";
            if (!this.manageMySQL.checkTable(this.dbTable)) {
                log("Creating table...");
                if (!this.manageMySQL.createTable(query)) {
                    log(Level.SEVERE, "Cannot create table, check your config !");
                }
            } else {
                ResultSet rs = this.manageMySQL.query("SELECT * FROM `" + this.dbTable + "`");
                ResultSetMetaData metadata = rs.getMetaData();
                if (metadata.getColumnCount() != MYSQL_FIELDS.length) {
                    log("table is an old version, updating...");
                    this.manageMySQL.query("DROP TABLE `" + this.dbTable + "`");
                    if (!this.manageMySQL.createTable(query)) {
                        log(Level.SEVERE, "Cannot create table, check your config !");
                    }
                }
            }
        } catch (Exception ex) {
            this.logException(ex);
        }
    }

    public void loadConfig() throws FileNotFoundException, IOException, InvalidConfigurationException {
        File cfgDir = this.getDataFolder();
        File cfgFile = new File(cfgDir + "/config.yml");

        if (!cfgDir.exists()) {
            cfgDir.mkdirs();
        }
        if (!cfgFile.exists()) {
            try {
                cfgFile.createNewFile();
            } catch (IOException ex) {
                logException(ex);
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
        this.verbosity = this.getConfig().getInt("verbosity", -1);

        if (this.verbosity == -1) {
            log(Level.WARNING, "Creating 'verbosity' config...");
            this.verbosity = 0;
            this.getConfig().set("verbosity", this.verbosity);
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

    private void updateUser(Player player, boolean async, int delay) {
        if (async) {
            this.getServer().getScheduler().scheduleAsyncDelayedTask(this,
                    new UpdateDatabase(this, true, player), delay *20);
        } else {
            this.getServer().getScheduler().scheduleSyncDelayedTask(this,
                    new UpdateDatabase(this, true, player), delay *20);
        }
    }

    public void invokeCheck(boolean async) {
        for (Player p : this.getServer().getOnlinePlayers()) {
            updateUser(p, async, 1);
        }
    }

    public void invokeCheck(Player player, boolean async) {
        updateUser(player, async, 1);
    }

    public void invokeCheck(boolean async, int delay) {
        for (Player p : this.getServer().getOnlinePlayers()) {
            updateUser(p, async, delay);
        }
    }

    public void invokeCheck(Player player, boolean async, int delay) {
        updateUser(player, async, delay);
    }
}
