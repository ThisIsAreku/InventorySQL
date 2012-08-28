package fr.areku.InventorySQL.auth.plugins;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.authdb.AuthDB;

import fr.areku.InventorySQL.auth.OfflineModePluginAuthenticator;

public class AuthDBPlugin implements OfflineModePluginAuthenticator {
	//private AuthDB thePlugin;

	@Override
	public String getName() {
		return "AuthDB";
	}

	@Override
	public boolean isPlayerLoggedIn(Player player) {
		return AuthDB.isAuthorized(player);
	}

	@Override
	public void initialize(Plugin p) {
		//thePlugin = (AuthDB) p;
	}

}
