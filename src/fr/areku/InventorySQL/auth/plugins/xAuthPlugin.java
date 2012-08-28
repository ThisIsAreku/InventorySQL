package fr.areku.InventorySQL.auth.plugins;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.cypherx.xauth.xAuth;
import com.cypherx.xauth.xAuthPlayer;

import fr.areku.InventorySQL.auth.OfflineModePluginAuthenticator;

public class xAuthPlugin implements OfflineModePluginAuthenticator {
	private xAuth thePlugin;

	@Override
	public String getName() {
		return "xAuth";
	}

	@Override
	public boolean isPlayerLoggedIn(Player player) {
		return (thePlugin.getPlayerManager().getPlayer(player)
				.getStatus() == xAuthPlayer.Status.Authenticated);
	}

	@Override
	public void initialize(Plugin p) {
		thePlugin = (xAuth) p;
	}

}
