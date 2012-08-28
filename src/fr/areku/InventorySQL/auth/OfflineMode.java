package fr.areku.InventorySQL.auth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import fr.areku.InventorySQL.Main;
import fr.areku.InventorySQL.UpdateEventListener;

public class OfflineMode extends TimerTask {

	// private AuthDB AuthDBPlugin;
	private OfflineModePluginAuthenticator selectedAuthPlugin;
	private List<String> watchedPlayers;
	private UpdateEventListener loginListener;

	private Timer t;

	public OfflineMode(UpdateEventListener loginListener) {
		watchedPlayers = Collections.synchronizedList(new ArrayList<String>());
		this.loginListener = loginListener;
		hookAuthPlugins();
	}

	public void hookAuthPlugins() {
		try {
			JarInputStream jarFile = new JarInputStream(this.getClass()
					.getProtectionDomain().getCodeSource().getLocation()
					.openStream());
			JarEntry jarEntry = null;
			OfflineModePluginAuthenticator authenticator = null;
			while ((jarEntry = jarFile.getNextJarEntry()) != null) {
				try {
					if (jarEntry != null)
						if (jarEntry.getName().startsWith(
								"fr/areku/InventorySQL/auth/plugins/")
								&& jarEntry.getName().trim() != "fr/areku/InventorySQL/auth/plugins/"
								&& !jarEntry.isDirectory()) {
							String classname = jarEntry.getName()
									.replace('/', '.').trim();
							classname = classname.substring(0,
									classname.length() - 6);
							authenticator = (OfflineModePluginAuthenticator) this
									.getClass().getClassLoader()
									.loadClass(classname).newInstance();
							Main.d("Found new OfflineModePlugin:"
									+ authenticator.getName());
							Plugin p = Bukkit.getServer().getPluginManager()
									.getPlugin(authenticator.getName());
							if (p != null) {
								selectedAuthPlugin = authenticator;
								selectedAuthPlugin.initialize(p);
								break;
							}
						}
				} catch (Exception e) {Main.logException(e, "Error while initializing OfflineMode"); }
			}
			jarFile.close();

			if (selectedAuthPlugin != null) {
				Main.log("Selected " + selectedAuthPlugin.getName()
						+ " as offline mode plugin");
				this.t = new Timer();
				this.t.schedule(this, 0, 2000);
			} else {
				Main.log("No compatible offline mode plugin found");
				if (this.t != null)
					this.t.cancel();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean isUsingOfflineModePlugin() {
		return (selectedAuthPlugin != null);
	}

	public void watchPlayerLogin(String player) {
		synchronized (watchedPlayers) {
			watchedPlayers.add(player);
		}
	}

	public boolean isPlayerLoggedIn(Player player) {
		try {
			return selectedAuthPlugin.isPlayerLoggedIn(player);
		} catch (Exception e) {
			Main.log(Level.WARNING, "Cannot get player status with "
					+ selectedAuthPlugin + ": ");
			Main.log(Level.WARNING, e.getLocalizedMessage());
		}
		return false;
	}

	@Override
	public void run() {
		Main.d("OfflineMode: watching " + watchedPlayers.size() + " player(s)");
		if (watchedPlayers.isEmpty())
			return;
		List<String> toRemove = new ArrayList<String>();
		Player pl = null;
		synchronized (watchedPlayers) {
			Iterator<String> i = watchedPlayers.iterator();
			while (i.hasNext()) {
				String p = i.next();
				pl = Bukkit.getPlayerExact(p);
				if (pl != null) {
					if (isPlayerLoggedIn(pl)) {
						Main.d("Player " + p + " is now auth");
						toRemove.add(p);
						this.loginListener.doPlayerOfflineModeLogin(pl);
					}
				}
				pl = null;
			}
			watchedPlayers.removeAll(toRemove);
		}
	}
}
