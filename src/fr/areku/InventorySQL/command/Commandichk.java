package fr.areku.InventorySQL.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.areku.InventorySQL.InventorySQL;
import fr.areku.InventorySQL.InventorySQLCommand;
import fr.areku.InventorySQL.InventorySQLCommandListener;
import fr.areku.InventorySQL.database.CoreSQLItem;

public class Commandichk extends InventorySQLCommand {

	public Commandichk(CommandSender sender, Command command, String label,
			String[] args, InventorySQLCommandListener parent) {
		super(sender, command, label, args, parent);
	}

	@Override
	public boolean onCommand(CommandSender cs, Command command, String label,
			String[] args) {
		if (!isPlayer()) {
			sendMessage(cs, ChatColor.RED
					+ "You cannot check yourself as a Console !");
			return true;
		}

		if (!SenderCan(cs, "invsql.check.me", false)) {
			sendMessage(cs, ChatColor.RED + "You cannot use this command");
			return true;
		}
		sendMessage(cs,
				ChatColor.GREEN + InventorySQL.getMessage("check-yourself"));
		InventorySQL
				.getCoreSQLProcess()
				.runCheckThisTask(
						new CoreSQLItem(new Player[] { (Player) cs })
								.setCommandSender(cs),
						"Command", true, 0);
		return true;
	}

}
