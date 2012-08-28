package fr.areku.InventorySQL.auth.plugins;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import uk.org.whoami.authme.cache.auth.PlayerCache;
import fr.areku.InventorySQL.auth.OfflineModePluginAuthenticator;

public class AuthMePlugin implements OfflineModePluginAuthenticator {
	//private AuthMe thePlugin;

	@Override
	public String getName() {
		return "AuthMe";
	}

	@Override
	public boolean isPlayerLoggedIn(Player player) {
		return PlayerCache.getInstance().isAuthenticated(player.getName());
	}

	@Override
	public void initialize(Plugin p) {
		//thePlugin = (AuthMe) p;
	}

}
