package fr.areku.InventorySQL;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class InventorySQLCommand implements CommandExecutor {
	private CommandSender sender;
	private Command command;
	private String label;
	private String[] args;
	private InventorySQLCommandListener parent;
	private boolean player = false;

	public InventorySQLCommand(CommandSender sender, Command command,
			String label, String[] args, InventorySQLCommandListener parent) {
		this.sender = sender;
		this.command = command;
		this.label = label;
		this.args = args;
		this.parent = parent;

		if (sender instanceof Player) {
			player = true;
		}
	}

	public boolean execute() {
		return onCommand(sender, command, label, args);
	}

	public InventorySQLCommandListener getParent() {
		return parent;
	}

	public boolean isPlayer() {
		return player;
	}

	public void sendMessage(CommandSender cs, String m) {
		cs.sendMessage("[InventorySQL] " + m);
	}

	public static String combine(String[] s, String glue) {
		int k = s.length;
		if (k == 0)
			return null;
		StringBuilder out = new StringBuilder();
		out.append(s[1]);
		for (int x = 2; x < k; ++x)
			out.append(glue).append(s[x]);
		return out.toString();
	}

	public boolean SenderCan(CommandSender cs, String permission, boolean mustopped) {
		if (Config.usePermissions) {
			return cs.hasPermission(permission);
		} else {
			if(mustopped){
				return cs.isOp();
			}else{
				return true;
			}
		}
	}

}
