package alexoft.InventorySQL;

import java.util.ArrayList;
import java.util.List;

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

	public boolean onCommand(CommandSender cs, Command cmnd, String label,
			String[] args) {
		boolean isNotPlayer = true;

		if (cs instanceof Player) {
			isNotPlayer = false;
		}
		
		if (!"ichk".equals(label) && ((args.length == 0) || (args.length == 1 && "help".equals(args[0])))) {
			sendMessage(cs, ChatColor.GREEN + "Usage :");
			sendMessage(cs, ChatColor.GREEN	+ " * /invSQL check : update yourself");
			if (cs.isOp()) {
				sendMessage(cs, ChatColor.GREEN + " * /invSQL check all : update all players");
				sendMessage(cs,	ChatColor.GREEN	+ " * /invSQL check <player>, <player>, <player>, .. : update specified players");
				sendMessage(cs, ChatColor.GREEN	+ " * /invSQL reload : reload config");
			}
			return true;
		}

		if (cs.isOp() && isNotPlayer && (args.length == 1 && "reload".equals(args[0]))) {
			sendMessage(cs, ChatColor.YELLOW + "Reloading InventorySQL");
			this.plugin.reload();
			return true;
		}
		
		if(!this.plugin.ready){
			sendMessage(cs, ChatColor.RED + "Error in config, please check and use /invsql reload");
			return true;
		}
		
		if ("ichk".equals(label)) {
			if (isNotPlayer) {
				sendMessage(cs, ChatColor.RED + "You cannot check yourself as a Console !");
			} else {
				sendMessage(cs,	ChatColor.GREEN + Main.getMessage("check-yourself"));
				this.plugin.invokeCheck(new Player[] { (Player) cs }, cs);
			}
			return true;
		}
		if ("pw".equals(args[0]) && (!isNotPlayer)) {
			if (args.length != 2) {
				sendMessage(cs, ChatColor.GREEN + "Usage :");
				sendMessage(cs, ChatColor.GREEN
						+ " * /invSQL pw <password> : change your password");
				return true;
			}
			try {
				this.plugin.MYSQLDB.queryUpdate("UPDATE `"
						+ this.plugin.dbTable + "_users` SET `password`=MD5('"
						+ args[1] + "') WHERE `name` = '" + cs.getName() + "'");
				sendMessage(cs, ChatColor.BLUE + "Password changed");
			} catch (EmptyException e) {
				sendMessage(cs, ChatColor.RED + "Unable to change password");
			}
		}
		if (!cs.isOp()) {
			sendMessage(cs, ChatColor.RED + "You cannot use this command");
			return true;
		}
		if ("check".equals(args[0])) {
			if (args.length >= 2) {
				if ("all".equals(args[1])) {
					sendMessage(
							cs,
							ChatColor.GREEN
									+ Main.getMessage("check-all-players"));
					this.plugin.invokeAllCheck(null);
					return true;
				}
				Player pT;
				List<Player> p = new ArrayList<Player>();

				for (int i = 1; i < args.length; i++) {
					pT = this.plugin.getServer().getPlayer(args[i]);
					if (pT != null) {
						if (!p.contains(pT))
							p.add(pT);
					}
				}
				if (p.size() > 0) {
					sendMessage(
							cs,
							ChatColor.GREEN
									+ Main.getMessage("check-n-players",
											p.size()));
					this.plugin.invokeCheck(p.toArray(new Player[] {}), cs);
				} else {
					sendMessage(cs,
							ChatColor.GREEN + Main.getMessage("no-online"));
				}
			} else {
				if (isNotPlayer) {
					sendMessage(cs, ChatColor.RED
							+ "You cannot check yourself as a Console !");
				} else {
					sendMessage(cs,
							ChatColor.GREEN + Main.getMessage("check-yourself"));
					this.plugin.invokeCheck(new Player[] { (Player) cs }, cs);
				}
			}
			return true;
		}
		return true;
	}

	public void sendMessage(CommandSender cs, String m) {
		cs.sendMessage("[InventorySQL] " + m);
	}

}
