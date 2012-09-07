package fr.areku.InventorySQL;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import fr.areku.InventorySQL.command.Commandichk;
import fr.areku.InventorySQL.command.Commandinvsql;

public class InventorySQLCommandListener implements CommandExecutor {
	public InventorySQL plugin;

	public InventorySQLCommandListener(InventorySQL plugin) {
		this.plugin = plugin;
	}

	public boolean onCommand(CommandSender cs, Command cmnd, String label,
			String[] args) {
		if (!this.plugin.ready) {
			cs.sendMessage("[InventorySQL] " + ChatColor.RED
					+ "Error in config, please check and use /invsql reload");
			return true;
		}

		if ("ichk".equals(label)) {
			return new Commandichk(cs, cmnd, label, args, this).execute();
		}
		if ("invsql".equals(label)) {
			return new Commandinvsql(cs, cmnd, label, args, this).execute();
			
		}
		return true;
	}

}
