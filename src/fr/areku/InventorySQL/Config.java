package fr.areku.InventorySQL;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

public class Config {
	public static final String TABLE_VERSION = "2.0";
	
	private static int reload_count = -1; // Initialized to -1 for the first
											// pseudo-reload
	public static String dbTable_Inventories = "";
	public static String dbTable_Pendings = "";
	public static String dbTable_Users = "";
	public static String dbTable_Enchantments = "";

	public static String dbDatabase = null;
	public static String dbHost = null;
	public static String dbPass = null;
	public static String dbTablePrefix = null;
	public static String dbUser = null;

	public static boolean check_plugin_updates = true;
	public static boolean checkChest = false;
	public static boolean noCreative = true;
	public static boolean multiworld = true;
	public static int afterLoginDelay = 20;

	public static boolean backup_enabled = true;
	public static int backup_cleanup_days = 0;

	public static List<String> update_events = new ArrayList<String>();

	public static long check_interval = 0;

	public static boolean allow_unsafe_ench = false;

	public static boolean debug = false;

	public Config(Main plugin) throws IOException,
			InvalidConfigurationException {
		reload_count++;
		File file = new File(plugin.getDataFolder(), "config.yml");
		if (!plugin.getDataFolder().exists())
			plugin.getDataFolder().mkdirs();
		if (!file.exists())
			copy(plugin.getResource("config.yml"), file);

		plugin.getConfig().load(file);

		YamlConfiguration defaults = new YamlConfiguration();
		defaults.load(plugin.getResource("config.yml"));
		plugin.getConfig().addDefaults(defaults);
		plugin.getConfig().options().copyDefaults(true);

		Config.dbHost = plugin.getConfig().getString("mysql.host");
		Config.dbUser = plugin.getConfig().getString("mysql.user");
		Config.dbPass = plugin.getConfig().getString("mysql.pass");
		Config.dbDatabase = plugin.getConfig().getString("mysql.db");
		Config.dbTablePrefix = plugin.getConfig().getString(
				"mysql.table-prefix");
		Config.check_interval = plugin.getConfig().getInt("check-interval");
		Config.check_plugin_updates = plugin.getConfig().getBoolean(
				"check-plugin-updates");
		Config.noCreative = plugin.getConfig().getBoolean("no-creative");
		Config.afterLoginDelay = plugin.getConfig().getInt("after-login-delay");
		Config.multiworld = plugin.getConfig().getBoolean("multiworld");

		Config.backup_enabled = plugin.getConfig().getBoolean("backup.enabled");
		Config.backup_cleanup_days = plugin.getConfig().getInt(
				"backup.cleanup-days");

		Config.allow_unsafe_ench = plugin.getConfig().getBoolean(
				"allow-unsafe-ench");

		Config.debug = plugin.getConfig().getBoolean("debug");

		Config.update_events = new ArrayList<String>();
		MemorySection events = (MemorySection) plugin.getConfig().get(
				"update-events");
		for(String k : events.getKeys(false)){
			if(events.getBoolean(k)) Config.update_events.add(k);
		}
		if (Config.update_events.isEmpty() && (reload_count > 0)) {
			Main.log(Level.WARNING,
					"No update event ! Data will only be updated when using the command");
		}

		Config.dbTable_Inventories = Config.dbTablePrefix + "_inventories";
		Config.dbTable_Pendings = Config.dbTablePrefix + "_pendings";
		Config.dbTable_Users = Config.dbTablePrefix + "_users";
		Config.dbTable_Enchantments = Config.dbTablePrefix + "_enchantments";

		Config.check_interval *= 20;
		Config.afterLoginDelay *= 20;
		
		plugin.getConfig().save(file);
	}

	private void copy(InputStream src, File dst) throws IOException {
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = src.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		src.close();
		out.close();
	}
}
