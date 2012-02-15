package alexoft.InventorySQL;


import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.MaterialData;


/**
 *
 * @author Alexandre
 */
public class UpdateDatabase extends Thread {
    public static Pattern pInventory = Pattern.compile(
            "\\[([0-9]{1,2})\\(([0-9]{1,3}):([0-9]{1,2})\\)x(-?[0-9]{1,2})\\]");
    public static Pattern pPendings = Pattern.compile(
            "\\[(-|\\+)?\\(([0-9]{1,3}):([0-9]{1,2})\\)x([0-9]{1,2})\\]");
    public Main plugin;
    public boolean playerUpdate;
    public Player[] players;
	
    public UpdateDatabase(Main plugin) {
        this.plugin = plugin;
        this.playerUpdate = false;
    }

    public UpdateDatabase(Main plugin, boolean playerUpdate, Player[] players) {
        this.plugin = plugin;
        this.playerUpdate = playerUpdate;
        this.players = players;
    }

    public List<ActionStack> buildInvList(String data) {
        List<ActionStack> inv = new ArrayList<ActionStack>();
        Matcher m;

        for (String i : data.split(",")) {
            m = pInventory.matcher(i);
            if (m.matches()) {
                inv.add(
                        new ActionStack(
                                new ItemStack(Integer.decode(m.group(2)),
                                Integer.decode(m.group(4)), (short) 0,
                                Byte.decode(m.group(3))),
                                m.group(1)));
            }
        }
        return inv;
    }

    public List<ActionStack> buildPendList(String data) {
        List<ActionStack> inv = new ArrayList<ActionStack>();
        Matcher m;

        for (String i : data.split(",")) {
            m = pPendings.matcher(i);
            if (m.matches()) {
                inv.add(
                        new ActionStack(
                                new ItemStack(Integer.decode(m.group(2)),
                                Integer.decode(m.group(4)), (short) 0,
                                Byte.decode(m.group(3))),
                                m.group(1)));
            }
        }
        return inv;
    }

    public String buildInvString(PlayerInventory inventory) {
        ItemStack m;
        String l = "";
        MaterialData b;

        for (int i = 0; i <= 39; i++) {
            m = inventory.getItem(i);
            b = m.getData();
            l += "[" + i + "(" + m.getTypeId() + ":"
                    + (b != null ? b.getData() : "0") + ")x" + m.getAmount()
                    + "],";
        }
        return l.substring(0, l.length() - 1);
    }

    public String buildPendString(List<ActionStack> items) {
        String l = "";
        MaterialData b;

        for (ActionStack m: items) {
            b = m.item().getData();
            l += "[" + m.params() + "(" + m.item().getTypeId() + ":"
                    + (b != null ? b.getData() : "0") + ")x"
                    + m.item().getAmount() + "],";
        }
        return l.substring(0, l.length() - 1);
    }
	
    public void playerLogic(Player player) {
        try {
            ResultSet r;
            if(!this.plugin.MYSQLDB.checkConnectionIsAlive(true)) {
            	Main.log(Level.SEVERE, "MySQL Connection error..");
            	return;
            }
            if (!this.plugin.MYSQLDB.tableExist(this.plugin.dbTable)) {
            	Main.log(Level.SEVERE, "Table has suddenly disappear, disabling plugin...");
            	this.plugin.Disable();
            	return;
            	
            }
            r = this.plugin.MYSQLDB.query(
                    "SELECT * FROM `" + this.plugin.dbTable
                    + "` WHERE LOWER(`owner`) =LOWER('" + player.getName()
                    + "');");
			
            if (r.first()) {
                List<ActionStack> fullInv = new ArrayList<ActionStack>();
                String pendingData = r.getString("pendings");

                if (!"".equals(pendingData)) {
                    Main.log(Level.FINE, "pendings items for " + player.getName());
                    int empty;

                    for (ActionStack i:buildPendList(pendingData)) {
                        if ("+".equals(i.params())) {
                            empty = player.getInventory().firstEmpty();
                            if (empty == -1) {
                                fullInv.add(i);
                            } else {
                                player.getInventory().setItem(empty, i.item());
                                                        
                                Main.log(Level.FINER, "\t" +
                                        player.getName() + " : " + i.params()
                                        + " => " + i.item().getType().toString());
                            }
								
                        } else if ("-".equals(i.params())) {
                            if (player.getInventory().contains(
                                    i.item().getType())) {
                                HashMap<Integer, ItemStack> m = player.getInventory().removeItem(i.item());
                                Set<Integer> cles = m.keySet();
                                Iterator<Integer> it = cles.iterator();

                                while (it.hasNext()) {
                                    int key = Integer.parseInt(
                                            it.next().toString()); 

                                    fullInv.add(new ActionStack(m.get(key), "-"));
                                }                                                                        
                                Main.log(Level.FINER, "\t" +
                                        player.getName() + " : " + i.params()
                                        + " => " + i.item().getType().toString());
                            } else {
                                fullInv.add(i);
                            }
                        } else {
                            Main.log(Level.INFO,
                                    "bad command '" + i.params()
                                    + "' for player '" + player.getName()
                                    + "' in pendings data, ignored");
                        }
						
                    }
                }
				
                String invData = buildInvString(player.getInventory());

                Main.log(Level.FINE, "\t Unable to add/remove " + fullInv.size() + " item(s)");
                this.plugin.MYSQLDB.queryUpdate(
                        "UPDATE `" + this.plugin.dbTable
                        + "` SET `inventory` = '" + invData
                        + "', `pendings` = '"
                        + (fullInv.isEmpty() ? "" : buildPendString(fullInv))
                        + "' WHERE `id`=" + r.getInt("id"));
            } else {
                String invData = buildInvString(player.getInventory());

                this.plugin.MYSQLDB.queryUpdate(
                        "INSERT INTO `" + this.plugin.dbTable
                        + "`(`id`, `owner`, `inventory`, `pendings`) VALUES (null,'"
                        + player.getName() + "','" + invData + "','')");
            }
        } catch (Exception ex) {
            Main.logException(ex, "exception in playerlogic");
        }
    }
	
    @Override
    public void run() {
        if (this.playerUpdate) {
            for (Player p: this.plugin.getServer().getOnlinePlayers()) {
            	playerLogic(p);
            }
            

        } else {
            for (Player p: this.plugin.getServer().getOnlinePlayers()) {
                playerLogic(p);
            }
        }
    }
}
