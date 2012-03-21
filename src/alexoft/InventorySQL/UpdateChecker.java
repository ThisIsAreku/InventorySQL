package alexoft.InventorySQL;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;

public class UpdateChecker implements Runnable {
	private Main plugin;
	private static final String UPDATE_YML_URL = "https://raw.github.com/ThisIsAreku/InventorySQL/master/src/plugin.yml";
	private static final String USER_UPDATE_URL = "http://dev.bukkit.org/server-mods/inventorysql/";

	public UpdateChecker(Main plugin) {
		this.plugin = plugin;
	}
	
	public void start(){
		this.plugin.getServer().getScheduler().scheduleAsyncDelayedTask(this.plugin, this);
	}

	@Override
	public void run() {
		try {
			URL u = new URL(UPDATE_YML_URL);
			PluginDescriptionFile web_pdf = new PluginDescriptionFile(u.openStream());
			if(!this.plugin.getDescription().getVersion().equals(web_pdf.getVersion())){
				this.plugin.getLogger().log(Level.WARNING, "New version found : " + web_pdf.getVersion());
				this.plugin.getLogger().log(Level.WARNING, "Current version: " + this.plugin.getDescription().getVersion());
				this.plugin.getLogger().log(Level.WARNING, "Go grab it from " + USER_UPDATE_URL + " !");
			}
		} catch (MalformedURLException e) {
			Main.logException(e, "UpdateChecker run()");
		} catch (IOException e) {
			Main.logException(e, "UpdateChecker run()");
		} catch (InvalidDescriptionException e) {
			Main.logException(e, "UpdateChecker run()");
		}
	}
}
