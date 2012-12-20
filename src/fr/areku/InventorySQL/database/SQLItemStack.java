package fr.areku.InventorySQL.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fr.areku.InventorySQL.Config;
import fr.areku.InventorySQL.InventorySQL;

public class SQLItemStack {
	public enum Action {
		ADD, REMOVE
	};

	private ItemStack theItemStack;
	private String theID;
	private Action theAction;
	private int theSlotID;
	private static Pattern LorePattern = Pattern.compile("Lore_(\\d)");
	private TreeMap<Integer, String> lores = new TreeMap<Integer, String>();

	public ItemStack getItemStack() {
		return theItemStack;
	}

	public String getID() {
		return theID;
	}

	public Action getAction() {
		return theAction;
	}

	public int getSlotID() {
		return theSlotID;
	}

	public SQLItemStack(ResultSet rs, String id) throws SQLException {
		readStack(rs, id, -1);
	}

	public SQLItemStack(ResultSet rs, String id, int slotId)
			throws SQLException {
		readStack(rs, id, slotId);
	}

	private void readStack(ResultSet rs, String id, int slotId) // read a stack
																// from
																// ResultSet
			throws SQLException {
		theID = id;
		theSlotID = slotId;
		int count = rs.getInt("count");
		theItemStack = new ItemStack(rs.getInt("item"), Math.abs(count),
				(short) rs.getInt("damage"));
		theItemStack.getData().setData(rs.getByte("data"));
		readEnch(rs);
		readMeta(rs);

		if (count > 0) {
			theAction = Action.ADD;
		} else if (count < 0) {
			theAction = Action.REMOVE;
		}
	}

	public void readMeta(ResultSet rs) throws SQLException {
		String meta_key = rs.getString("meta_key");
		InventorySQL.d("Readed a meta : " + meta_key);
		if (!rs.wasNull()) {
			ItemMeta meta = theItemStack.getItemMeta();
			String meta_value = rs.getString("meta_value");
			InventorySQL.d("Readed a meta val : " + meta_value);
			if ("DisplayName".equals(meta_key)) {
				meta.setDisplayName(meta_value);
			} else {
				Matcher match = LorePattern.matcher(meta_key);
				if (match.matches()) {
					int l = Integer.parseInt(match.group(1));
					lores.put(l, meta_value); //use the TreeMap to sort Lores lines
					// complexe way to avoid ClassCastException
					meta.setLore(Arrays.asList(lores.values().toArray(new String[]{})));
				}
			}
			theItemStack.setItemMeta(meta);
		}
	}

	public void readEnch(ResultSet rs) throws SQLException {
		int ench_id = rs.getInt("ench");
		if (!rs.wasNull()) {
			ItemMeta m = theItemStack.getItemMeta();
			Enchantment e = Enchantment.getById(ench_id);
			int ench_level = rs.getInt("level");
			if ((e != null) && ench_level > 0) {
				try {
					m.addEnchant(e, ench_level, Config.allow_unsafe_ench);
				} catch (Exception ex) {
					InventorySQL.log(Level.WARNING,
							"Error while adding " + e.getName() + "/"
									+ ench_level + " to " + this.toString());
					InventorySQL.log(Level.WARNING, ex.getLocalizedMessage());
				}
			}
			theItemStack.setItemMeta(m);
		}
	}

	@Override
	public String toString() {
		return "SQLItemStack [theItemStack=" + theItemStack + ", theID="
				+ theID + ", theAction=" + theAction + ", theSlotID="
				+ theSlotID + "]";
	}

}
