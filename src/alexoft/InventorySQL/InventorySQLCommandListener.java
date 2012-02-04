package alexoft.InventorySQL;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InventorySQLCommandListener implements CommandExecutor {
	public Main plugin;

	public InventorySQLCommandListener(Main plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender cs, Command cmnd, String label,
			String[] args) {
		if (!(cs instanceof Player)) {
			return false;
		}
		Player player = (Player) cs;

		if (!player.isOp()) {
			player.sendMessage(ChatColor.RED + "You cannot use this command");
			return true;
		}
		if (args.length == 0) {
			player.sendMessage(ChatColor.GREEN + "Usage :");
			player.sendMessage(ChatColor.GREEN
					+ " * /invSQL check : update yourself");
			player.sendMessage(ChatColor.GREEN
					+ " * /invSQL check all : update all players");
			player.sendMessage(ChatColor.GREEN
					+ " * /invSQL check <player>, <player>, <player>, .. : update specified players");
			return true;
		}
		if ("check".equals(args[0])) {
			if (args.length >= 2) {
				if ("all".equals(args[1])) {
					this.plugin.invokeCheck(true);
				}
				Player p;

				for (int i = 1; i < args.length; i++) {
					p = this.plugin.getServer().getPlayer(args[i]);
					if (p != null) {
						this.plugin.invokeCheck(p, true);
					}
				}
			} else {
				this.plugin.invokeCheck(player, true);
			}
			return true;
		}
		return true;
	}

}
