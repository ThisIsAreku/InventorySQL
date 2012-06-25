package alexoft.InventorySQL.auth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import alexoft.InventorySQL.UpdateEventListener;
import alexoft.InventorySQL.Main;

import com.authdb.AuthDB;
import com.cypherx.xauth.xAuth;
import com.cypherx.xauth.xAuthPlayer;

public class OfflineMode extends TimerTask {
	private enum OfflineModePlugins {
		None, xAuth, AuthDB
	}

	// private AuthDB AuthDBPlugin;
	private xAuth xAuthPlugin;
	private OfflineModePlugins selectedAuthPlugin;
	private List<String> watchedPlayers;
	private UpdateEventListener loginListener;

	private Timer t;

	public OfflineMode(UpdateEventListener loginListener) {
		watchedPlayers = Collections.synchronizedList(new ArrayList<String>());
		this.loginListener = loginListener;
		hookAuthPlugins();
	}

	public void hookAuthPlugins() {
		selectedAuthPlugin = OfflineModePlugins.None;

		Plugin p = Bukkit.getServer().getPluginManager().getPlugin("xAuth");
		if (p != null) {
			xAuthPlugin = (xAuth) p;
			selectedAuthPlugin = OfflineModePlugins.xAuth;
		}
		p = Bukkit.getServer().getPluginManager().getPlugin("AuthDB");
		if (p != null) {
			// AuthDBPlugin = (AuthDB) p;
			selectedAuthPlugin = OfflineModePlugins.AuthDB;
		}

		if (selectedAuthPlugin != OfflineModePlugins.None) {
			Main.log("Selected " + selectedAuthPlugin.toString()
					+ " as offline mode plugin");
			this.t = new Timer();
			this.t.schedule(this, 0, 2000);
		} else {
			if (this.t != null)
				this.t.cancel();
		}
	}

	public boolean isUsingOfflineModePlugin() {
		return (selectedAuthPlugin != OfflineModePlugins.None);
	}

	public void watchPlayerLogin(String player) {
		synchronized (watchedPlayers) {
			watchedPlayers.add(player);
		}
	}

	public boolean isPlayerLoggedIn(String player) {
		switch (selectedAuthPlugin) {
		case xAuth:
			return (xAuthPlugin.getPlyrMngr().getPlayer(player).getStatus() == xAuthPlayer.Status.Authenticated);

		case AuthDB:
			return AuthDB.isAuthorized(Bukkit.getPlayerExact(player));

		default:
			return false;
		}
	}

	@Override
	public void run() {
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
					if (isPlayerLoggedIn(p)) {
						Main.d("Player " + p + " is now auth");
						toRemove.add(p);
						this.loginListener.onPlayerOfflineModeLogin(pl);
					}
				}
				pl = null;
			}
			watchedPlayers.removeAll(toRemove);
		}
	}
}
