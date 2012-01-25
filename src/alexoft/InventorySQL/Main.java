
package alexoft.InventorySQL;


import com.alta189.MySQL.mysqlCore;
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
    public String              dbDatabase = null;
    public String              dbHost = null;
    public String              dbPass = null;
    public String              dbTable = null;
    public String              dbUser = null;
    public long                delayCheck = 0;
    
    private InventorySQLPlayerListener playerListener;
    private InventorySQLCommandListener commandListener;
    
    public Boolean             MySQL = true;
    public mysqlCore           manageMySQL;

    public static String[] MYSQL_FIELDS = new String[]{
    	"id", "owner", "ischest", "x", "y", "z", "inventory", "pendings"
    };

    public void log(Level level, String l) {
        this.getServer().getLogger().log(level, "[InventorySQL] {0}", l);
    }

    public void log(String l) {
        log(Level.INFO, l);
    }
    
    public void logException(Throwable e) {
        log(Level.SEVERE, "---------------------------------------");
        log(Level.SEVERE, "--- an unexpected error has occured ---");
        log(Level.SEVERE, "-- please send line below to the dev --");
        log(Level.SEVERE, e.toString() + " : " + e.getLocalizedMessage());
        for (StackTraceElement t:e.getStackTrace()) {  
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
            manageMySQL = new mysqlCore(this.getServer().getLogger(),
                    "[InventorySQL] ", this.dbHost, this.dbDatabase, this.dbUser,
                    this.dbPass);
            try {
                manageMySQL.initialize();

                if (this.manageMySQL.checkConnection()) {
                    log("MySQL connection successful");
                    checkUpdateTable();
                } else {
                    log(Level.SEVERE, "MySQL connection failed");
                    this.MySQL = false;
                }
            } catch (Exception ex) {
                this.logException(ex);
            }

            this.playerListener = new InventorySQLPlayerListener(this);
            this.commandListener = new InventorySQLCommandListener(this);
            
            this.getCommand("invSQL").setExecutor(commandListener);
            
            this.getServer().getScheduler().scheduleAsyncRepeatingTask(this,
                    new UpdateDatabase(this), 10 * 20, this.delayCheck);
            log("Enabled !");
        } else {
            log(Level.SEVERE, "Configuration error, plugin disabled");
            this.getPluginLoader().disablePlugin(this);
        }
    }
    
    public void Disable(){
    	this.getPluginLoader().disablePlugin(this);
    }
    
    public void checkUpdateTable(){
    	try{
            String query = "CREATE TABLE `"
            		+ this.dbTable
                    + "` (`id` INT NOT NULL AUTO_INCREMENT," +
                    "`owner` VARCHAR(32) NOT NULL," +
                    "`ischest` tinyint(1) NOT NULL DEFAULT '0'," +
                    "`x` int(11) NOT NULL DEFAULT '0'," +
                    "`y` tinyint(3) unsigned NOT NULL DEFAULT '0'," +
                    "`z` int(11) NOT NULL DEFAULT '0'," +
                    "`inventory` longtext," +
                    "`pendings` longtext, PRIMARY KEY (`id`))" +
                    "ENGINE=InnoDB DEFAULT CHARSET=utf8;";
	        if (!this.manageMySQL.checkTable(this.dbTable)) {
	            log("Creating table...");	            
	            if(!this.manageMySQL.createTable(query))
	            {
                    log(Level.SEVERE, "Cannot create table, check your config !");
	            }
	        }else{            
	            ResultSet rs = this.manageMySQL.sqlQuery("SELECT * FROM `" + this.dbTable+ "`");
	            ResultSetMetaData metadata = rs.getMetaData();
	            if(metadata.getColumnCount() != MYSQL_FIELDS.length){
		            log("table is an old version, updating...");	
	            	this.manageMySQL.deleteQuery("DROP TABLE `"+ this.dbTable + "`");	            
		            if(!this.manageMySQL.createTable(query))
		            {
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
        }else{
            this.getConfig().load(cfgFile);
        }
        
        String tmp = String.valueOf(Math.random());
        
        this.MySQL = true;
        
        this.dbHost = this.getConfig().getString("host", "");
        this.dbUser = this.getConfig().getString("user", "");
        this.dbPass = this.getConfig().getString("pass", tmp);
        this.dbDatabase = this.getConfig().getString("db", "");
        this.dbTable = this.getConfig().getString("table", "");
        this.delayCheck = this.getConfig().getInt("check-interval", -1);
        
        if (this.delayCheck == -1) {
            log(Level.WARNING, "Creating 'check-interval' config...");
            this.delayCheck = 600;
            this.getConfig().set("check-interval", this.delayCheck);
        }
        if (this.dbHost.equals("")) {
            log(Level.WARNING, "Creating 'host' config...");
            this.getConfig().set("host", "localhost");
            this.MySQL = false;
        }
        if (this.dbUser.equals("")) {
            log(Level.WARNING, "Creating 'user' config...");
            this.getConfig().set("user", "root");
            this.MySQL = false;
        }
        if (this.dbPass.equals(tmp)) {
            log(Level.WARNING, "Creating 'pass' config...");
            this.getConfig().set("pass", "pass");
            this.MySQL = false;
        }
        if (this.dbDatabase.equals("")) {
            log(Level.WARNING, "Creating 'db' config...");
            this.getConfig().set("db", "minecraft");
            this.MySQL = false;
        }
        if (this.dbTable.equals("")) {
            log(Level.WARNING, "Creating 'table' config...");
            this.getConfig().set("table", "InventorySQL");
            this.MySQL = false;
        }
        this.delayCheck *= 20;
        this.getConfig().save(cfgFile);
    }
   
    private void updateUser(Player player, boolean async) {
        if (async) {
            this.getServer().getScheduler().scheduleAsyncDelayedTask(this,
                    new UpdateDatabase(this, true, player));
        } else {
            this.getServer().getScheduler().scheduleSyncDelayedTask(this,
                    new UpdateDatabase(this, true, player));
        }
    }

    public void invokeCheck(boolean async) {
        for (Player p:this.getServer().getOnlinePlayers()) {
            updateUser(p, async);
        }        
    }

    public void invokeCheck(Player player, boolean async) {
        updateUser(player, async);          
    }
}
