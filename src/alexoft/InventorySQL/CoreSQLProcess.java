package alexoft.InventorySQL;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.MaterialData;

/**
 * 
 * @author Alexandre
 */
public class CoreSQLProcess extends Thread {
	public static Pattern pInventory = Pattern
			.compile("\\[([0-9]{1,2})\\(([0-9]{1,4}):([0-9]{1,3})(\\|([0-9=,]*?))?\\)x(-?[0-9]{1,2})\\]");
	public static Pattern pPendings = Pattern
			.compile("\\[(-|\\+)?\\(([0-9]{1,4}):([0-9]{1,3})(\\|([0-9=,]*?))?\\)x(-?[0-9]{1,2})\\]");
	public Main plugin;
	public boolean playerUpdate;
	public Player[] players;
	private CommandSender cs;

	public CoreSQLProcess(Main plugin) {
		this.plugin = plugin;
		this.playerUpdate = false;
	}

	public CoreSQLProcess(Main plugin, boolean playerUpdate, Player[] players,
			CommandSender cs) {
		this.plugin = plugin;
		this.playerUpdate = playerUpdate;
		this.players = players;
		this.cs = cs;
	}

	public List<ActionStack> buildInvList(String data) {
		List<ActionStack> inv = new ArrayList<ActionStack>();
		Matcher m;

		for (String i : data.split(",")) {
			m = pInventory.matcher(i);
			if (m.matches()) {
				ItemStack itmstck = new ItemStack(Integer.decode(m.group(2)),
						Integer.decode(m.group(6)), (short) 0, Byte.decode(m
								.group(3)));

				inv.add(new ActionStack(itmstck, m.group(1)));
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
				ItemStack itmstck = new ItemStack(Integer.decode(m.group(2)),
						Integer.decode(m.group(6)), (short) 0, Byte.decode(m
								.group(3)));
				if (m.group(5) != null) {
					for (String e : m.group(5).split(",")) {
						String[] d = e.split("=");
						Enchantment k = Enchantment.getById(Integer
								.decode(d[0]));
						if (k != null) {
							Integer l = Integer.decode(d[1]);
							if (l > k.getMaxLevel())
								l = k.getMaxLevel();
							if (l < 1)
								l = 1;
							itmstck.addEnchantment(k, l);
						}
					}
				}
				inv.add(new ActionStack(itmstck, m.group(1)));
			}
		}
		return inv;
	}

	public String buildEnchString(ItemStack itemStack) {
		String s = "";
		for (Entry<Enchantment, Integer> e : itemStack.getEnchantments()
				.entrySet()) {
			s += e.getKey().getId() + "=" + e.getValue() + ",";
		}
		if (s.endsWith(","))
			s = s.substring(0, s.length() - 1);
		if (s != "")
			s = "|" + s;
		return s;
	}

	public String buildInvString(PlayerInventory inventory) {
		ItemStack m;
		String l = "";
		MaterialData b;
		for (int i = 0; i <= 39; i++) {
			m = inventory.getItem(i);
			if (m != null) {
				b = m.getData();
				l += "[" + i + "(" + m.getTypeId() + ":"
						+ (b != null ? b.getData() : "0") + buildEnchString(m)
						+ ")x" + m.getAmount() + "],";
			}
		}
		if (l.length() > 1)
			l = l.substring(0, l.length() - 1);
		return l;
	}

	public String buildPendString(List<ActionStack> items) {
		String l = "";
		MaterialData b;

		for (ActionStack m : items) {
			b = m.item().getData();
			l += "[" + m.params() + "(" + m.item().getTypeId() + ":"
					+ (b != null ? b.getData() : "0") + ")x"
					+ m.item().getAmount() + "],";
		}
		if (l.length() > 1)
			l = l.substring(0, l.length() - 1);
		return l;
	}

	public void playerLogic(Player player) {
		try {
			ResultSet r;
			int added = 0;
			int removed = 0;
			int pendings = 0;
			
			if ((this.plugin.no_creative) && (player.getGameMode() == GameMode.CREATIVE)) {
				return;
			}
			
			if (this.plugin == null) {
				Main.logException(new NullPointerException("Parent is null.."), "this.plugin");
				this.plugin.Disable();
				return;
			}			
			if (this.plugin.MYSQLDB == null) {
				Main.logException(new NullPointerException("MYSQLDB is null.."), "this.plugin.MYSQLDB");
				this.plugin.Disable();
				return;
			}
			if (!this.plugin.MYSQLDB.checkConnectionIsAlive(true)) {
				Main.log(Level.SEVERE, "MySQL Connection error..");
				return;
			}
			if (!this.plugin.MYSQLDB.tableExist(this.plugin.dbTable)) {
				Main.log(Level.SEVERE,
						"Table has suddenly disappear, disabling plugin...");
				this.plugin.Disable();
				return;
			}
			r = this.plugin.MYSQLDB.query("SELECT * FROM `"
					+ this.plugin.dbTable + "` WHERE LOWER(`owner`) = LOWER('"
					+ player.getName() + "') AND `world` = '" + player.getWorld().getName() + "';");

			if (r.first()) {
				List<ActionStack> fullInv = new ArrayList<ActionStack>();
				String pendingData = r.getString("pendings");

				if (!"".equals(pendingData)) {
					Main.log(Level.WARNING,
							"pendings items for " + player.getName());
					int empty;

					for (ActionStack i : buildPendList(pendingData)) {
						if ("+".equals(i.params())) {
							empty = player.getInventory().firstEmpty();
							if (empty == -1) {
								fullInv.add(i);
							} else {
								player.getInventory().setItem(empty, i.item());
								added++;
							}

						} else if ("-".equals(i.params())) {
							if (player.getInventory().contains(
									i.item().getType())) {
								HashMap<Integer, ItemStack> m = player
										.getInventory().removeItem(i.item());
								Set<Integer> cles = m.keySet();
								Iterator<Integer> it = cles.iterator();

								while (it.hasNext()) {
									int key = Integer.parseInt(it.next()
											.toString());

									fullInv.add(new ActionStack(m.get(key), "-"));
								}
								removed++;
							} else {
								fullInv.add(i);
								pendings++;
							}
						} else {
							Main.log(Level.INFO, "bad command '" + i.params()
									+ "' for player '" + player.getName()
									+ "' in pendings data, ignored");
						}
					}
					if (cs != null) {
						cs.sendMessage("[InventorySQL] " + ChatColor.GREEN + "(" + player.getName()
								+ ") " + Main.getMessage("modif", removed, added, pendings));
					}
				} else {
					if (cs != null) {
						cs.sendMessage("[InventorySQL] " + ChatColor.GREEN + "(" + player.getName()
								+ ") " + Main.getMessage("no-modif"));
					}
				}

				String invData = buildInvString(player.getInventory());

				if (fullInv.size() != 0) {
					Main.log(Level.WARNING, "\t Unable to add/remove "
							+ fullInv.size() + " item(s)");
				}
				this.plugin.MYSQLDB.queryUpdate("UPDATE `"
						+ this.plugin.dbTable + "` SET `inventory` = '"
						+ invData + "', `pendings` = '"
						+ (fullInv.isEmpty() ? "" : buildPendString(fullInv))
						+ "', " + "`x`= '" + player.getLocation().getBlockX()
						+ "', " + "`y`= '" + player.getLocation().getBlockY()
						+ "', " + "`z`= '" + player.getLocation().getBlockZ()
						+ "' WHERE `id`= '" + r.getInt("id") + "';");

			} else {
				String invData = buildInvString(player.getInventory());

				this.plugin.MYSQLDB
						.queryUpdate("INSERT INTO `"
								+ this.plugin.dbTable
								+ "`(`owner`, `world`, `inventory`, `pendings`, `x`, `y`, `z`) VALUES ('"
								+ player.getName() + "','" + player.getWorld().getName() + "','" + invData + "','','"
								+ player.getLocation().getBlockX() + "','"
								+ player.getLocation().getBlockY() + "','"
								+ player.getLocation().getBlockZ() + "')");
				this.plugin.MYSQLDB
				.queryUpdate("INSERT INTO `"
						+ this.plugin.dbTable + "_users" + "`(`name`, `password`) VALUES ('" + player.getName() + "', '')");
			}
		} catch (Exception ex) {
			Main.logException(ex, "exception in playerlogic");
		}
	}

	@Override
	public void run() {
		if (this.playerUpdate) {
			for (Player p : this.players) {
				playerLogic(p);
			}

		} else {
			for (Player p : this.plugin.getServer().getOnlinePlayers()) {
				playerLogic(p);
			}
		}
	}
}
