package fr.areku.InventorySQL.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import fr.areku.InventorySQL.Config;
import fr.areku.InventorySQL.InventorySQL;
import fr.areku.InventorySQL.InventorySQLCommand;
import fr.areku.InventorySQL.InventorySQLCommandListener;
import fr.areku.InventorySQL.PlayerManager;
import fr.areku.InventorySQL.database.ConnectionManager;

public class Commandinvsql extends InventorySQLCommand {

	public Commandinvsql(CommandSender sender, Command command, String label,
			String[] args, InventorySQLCommandListener parent) {
		super(sender, command, label, args, parent);
	}

	@Override
	public boolean onCommand(CommandSender cs, Command command, String label,
			String[] args) {
		if (args.length == 0) {
			sendHelp(cs);
			return true;
		}
		if ("reload".equals(args[0])) {
			if (!SenderCan(cs, "invsql.reload", true)) {
				sendMessage(cs, ChatColor.RED + "You cannot use this command");
				return true;
			}
			sendMessage(cs, ChatColor.YELLOW + "Reloading InventorySQL");
			getParent().plugin.reload();
			return true;
		}

		if ("debug".equals(args[0])) {
			if (!SenderCan(cs, "invsql.debug", true)) {
				sendMessage(cs, ChatColor.RED + "You cannot use this command");
				return true;
			}
			if (!Config.debug) {
				sendMessage(cs, ChatColor.RED + "Debug isn't enabled");
				return true;
			}
			sendMessage(cs, ChatColor.YELLOW
					+ "InventorySQL - DEBUG INFORMATIONS");
			sendMessage(cs, ChatColor.YELLOW + "Connection pool size : "
					+ ConnectionManager.getNumConn() + "/" + Config.dbPoolSize);
			sendMessage(cs, ChatColor.YELLOW + "Player cache : "
					+ PlayerManager.getNumPlayers());
			sendMessage(cs, ChatColor.YELLOW + "------------------");
			return true;
		}

		if ("pw".equals(args[0]) && (isPlayer())) {
			if (!SenderCan(cs, "invsql.pw", false)) {
				sendMessage(cs, ChatColor.RED + "You cannot use this command");
				return true;
			}
			if (!isPlayer()) {
				sendMessage(cs, ChatColor.RED
						+ "You cannot check yourself as a Console !");
				return true;
			}
			if (args.length < 2) {
				sendHelp(cs);
				return true;
			}
			if (InventorySQL.getCoreSQLProcess().updatePlayerPassword(
					cs.getName(), combine(args, " "))) {
				sendMessage(cs, ChatColor.BLUE + "Password changed");
			} else {
				sendMessage(cs, ChatColor.RED + "Unable to change password");
			}
		}

		if ("check".equals(args[0])) {
			if (args.length >= 2) {
				if (!SenderCan(cs, "invsql.check.others", true)) {
					sendMessage(cs, ChatColor.RED
							+ "You cannot use this command");
					return true;
				}
				if ("all".equals(args[1])) {
					sendMessage(
							cs,
							ChatColor.GREEN
									+ InventorySQL
											.getMessage("check-all-players"));
					InventorySQL.getCoreSQLProcess().runPlayerCheck("Command",
							null, true, 0);
					return true;
				}
				Player pT;
				List<Player> nP = new ArrayList<Player>();

				for (int i = 1; i < args.length; i++) {
					pT = Bukkit.getPlayerExact(args[i]);
					if (pT != null) {
						if (!nP.contains(pT))
							nP.add(pT);
					}
				}
				if (nP.size() > 0) {
					InventorySQL.getCoreSQLProcess().runPlayerCheck(
							nP.toArray(new Player[] {}), "Command", null);
					sendMessage(
							cs,
							ChatColor.GREEN
									+ InventorySQL.getMessage(
											"check-n-players", nP));
				} else {
					sendMessage(
							cs,
							ChatColor.GREEN
									+ InventorySQL.getMessage("no-online"));
				}
			} else {
				if (!isPlayer()) {
					sendMessage(cs, ChatColor.RED
							+ "You cannot check yourself as a Console !");
					return true;
				}
				if (!SenderCan(cs, "invsql.check.me", false)) {
					sendMessage(cs, ChatColor.RED
							+ "You cannot use this command");
					return true;
				}
				sendMessage(
						cs,
						ChatColor.GREEN
								+ InventorySQL.getMessage("check-yourself"));
				InventorySQL.getCoreSQLProcess().runPlayerCheck((Player) cs,
						"Command", cs);
			}
		}

		if ("backup".equals(args[0]) & (args.length == 2)) {
			if ("clean".equals(args[1])) {
				if (!SenderCan(cs, "invsql.backup.clean", true)) {
					sendMessage(cs, ChatColor.RED
							+ "You cannot use this command");
					return true;
				}
				sendMessage(cs, ChatColor.GREEN + "Cleaning backup..");
				InventorySQL.getCoreSQLProcess().runBackupClean();
			}

		}

		if ("showidlist".equals(args[0]) && (Config.debug)) {
			// debug code to pring pretty-formated ids
			// used to update the webui
			for (Material m : Material.values()) {
				System.out.println("$items[" + m.getId() + "] = '"
						+ m.toString() + "';");
			}
			System.out.println("//----------------");
			for (Enchantment e : Enchantment.values()) {
				System.out.println("$ench[" + e.getId()
						+ "] = array('name' => '" + e.getName()
						+ "', 'startlevel' => " + e.getStartLevel()
						+ ", 'maxlevel' => " + e.getMaxLevel() + ");");
			}
			return true;
		}

		if ("help".equals(args[0])) {
			sendHelp(cs);
			return true;
		}

		return true;
	}

	public void sendHelp(CommandSender cs) {
		int use = 0;
		cs.sendMessage("[InventorySQL] " + ChatColor.GREEN + "Usage :");

		if (SenderCan(cs, "invsql.check.me", false)) {
			cs.sendMessage("[InventorySQL] " + ChatColor.GREEN
					+ " * /invSQL check : update yourself (alias: /ichk)");
			use++;
		}

		if (SenderCan(cs, "invsql.pw", false)) {
			cs.sendMessage("[InventorySQL] " + ChatColor.GREEN
					+ " * /invSQL pw : change your web password");
			use++;
		}

		if (SenderCan(cs, "invsql.check.others", true)) {
			cs.sendMessage("[InventorySQL] " + ChatColor.GREEN
					+ " * /invSQL check all : update all players");
			cs.sendMessage("[InventorySQL] "
					+ ChatColor.GREEN
					+ " * /invSQL check <player>, <player>, <player>, .. : update specified players");
			use++;
		}

		if (SenderCan(cs, "invsql.reload", true)) {
			cs.sendMessage("[InventorySQL] " + ChatColor.GREEN
					+ " * /invSQL reload : reload config");
			use++;
		}

		if (SenderCan(cs, "invsql.backup.clean", true)) {
			cs.sendMessage("[InventorySQL] " + ChatColor.GREEN
					+ " * /invSQL backup clean : clean old backups from tables");
			use++;
		}

		if (use == 0) {
			cs.sendMessage("[InventorySQL] " + ChatColor.GREEN
					+ "Err.. you can't use any commands :/");
		}
	}

}
