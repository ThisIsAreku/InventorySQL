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
        if("ichk".equals(label)){
            if (isNotPlayer) {
                sendMessage(cs,
                        ChatColor.RED + "You cannot check yourself as a Console !");
            } else {
                sendMessage(cs, ChatColor.GREEN + Main.getMessage("check-yourself"));
                this.plugin.invokeCheck(new Player[] { (Player) cs }, true, cs);
            }
            return true;
        }
        if (!cs.isOp()) {
            sendMessage(cs, ChatColor.RED + "You cannot use this command");
            return true;
        }
        if( (args.length == 0)||(args.length == 1 && "help".equals(args[0]))) {
            sendMessage(cs, ChatColor.GREEN + "Usage :");
            sendMessage(cs, ChatColor.GREEN + " * /invSQL check : update yourself");
            sendMessage(cs, ChatColor.GREEN + " * /invSQL check all : update all players");
            sendMessage(cs,
                    ChatColor.GREEN
                    + " * /invSQL check <player>, <player>, <player>, .. : update specified players");
            return true;
        }
        if ("check".equals(args[0])) {
            if (args.length >= 2) {
                if ("all".equals(args[1])) {
                    sendMessage(cs, ChatColor.GREEN + Main.getMessage("check-all-players"));
                    this.plugin.invokeCheck(true, null);
                    return true;
                }
                Player pT;
                List<Player> p = new ArrayList<Player>();

                for (int i = 1; i < args.length; i++) {
                    pT = this.plugin.getServer().getPlayer(args[i]);
                    if (pT != null) {
                        if(!p.contains(pT)) p.add(pT);
                    }
                }
                if (p.size() > 0) {
                    sendMessage(cs,
                            ChatColor.GREEN + Main.getMessage("check-n-players",p.size()));
                    this.plugin.invokeCheck(p.toArray(new Player[] {}), true, cs);
                } else {
                    sendMessage(cs,
                            ChatColor.GREEN + Main.getMessage("no-online"));
                }
            } else {
                if (isNotPlayer) {
                    sendMessage(cs,
                            ChatColor.RED + "You cannot check yourself as a Console !");
                } else {
                    sendMessage(cs, ChatColor.GREEN + Main.getMessage("check-yourself"));
                    this.plugin.invokeCheck(new Player[] { (Player) cs }, true, cs);
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
