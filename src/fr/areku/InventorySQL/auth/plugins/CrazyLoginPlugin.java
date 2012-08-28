package fr.areku.InventorySQL.auth.plugins;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import de.st_ddt.crazylogin.CrazyLogin;
import fr.areku.InventorySQL.auth.OfflineModePluginAuthenticator;

public class CrazyLoginPlugin implements OfflineModePluginAuthenticator {
	private CrazyLogin thePlugin;

	@Override
	public String getName() {
		return "CrazyLogin";
	}

	@Override
	public boolean isPlayerLoggedIn(Player player) {
		return thePlugin.isLoggedIn(player);
	}

	@Override
	public void initialize(Plugin p) {
		thePlugin = (CrazyLogin) p;
	}

}
