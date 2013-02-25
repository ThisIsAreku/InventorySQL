package fr.areku.InventorySQL.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.CoalType;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Builder;
import org.bukkit.Material;
import org.bukkit.SandstoneType;
import org.bukkit.TreeSpecies;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.Coal;
import org.bukkit.material.Dye;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Sandstone;
import org.bukkit.material.SpawnEgg;
import org.bukkit.material.Tree;
import org.bukkit.material.WoodenStep;
import org.bukkit.material.Wool;

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
	private Builder fwBuilder = FireworkEffect.builder();

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
		theItemStack = new ItemStack(rs.getInt("item"), Math.abs(count));
		applyMaterialData(rs.getByte("data"));
		InventorySQL.d("Sooo : " + theItemStack.getData().getData());
		theItemStack.setDurability((short) rs.getInt("damage"));
		if (((short) rs.getInt("damage") == 0) && (rs.getByte("data") != 0)) {
			theItemStack.setDurability(rs.getByte("data"));
		}
		readEnch(rs);
		readMeta(rs);

		if (count > 0) {
			theAction = Action.ADD;
		} else if (count < 0) {
			theAction = Action.REMOVE;
		}
	}

	private void applyMaterialData(byte data) {
		MaterialData d = theItemStack.getData();
		d.setData(data);
		switch (theItemStack.getType()) {
		case MONSTER_EGG:
			InventorySQL.d("SETTING TYPE !");
			InventorySQL.d(EntityType.fromId(data).toString());
			((SpawnEgg) d).setSpawnedType(EntityType.fromId(data));
			theItemStack.setDurability(data); // hack
			break;
		case INK_SACK:
			((Dye) d).setColor(DyeColor.getByDyeData(data));
			break;
		case WOOD:
		case SAPLING:
		case LOG:
		case LEAVES:
			((Tree) d).setSpecies(TreeSpecies.getByData(data));
			break;
		case WOOD_DOUBLE_STEP:
		case WOOD_STEP:
			((WoodenStep) d).setSpecies(TreeSpecies.getByData(data));
			break;
		case SANDSTONE:
			((Sandstone) d).setType(SandstoneType.getByData(data));
			break;
		case COAL:
			((Coal) d).setType(CoalType.getByData(data));
			break;
		case WOOL:
			((Wool) d).setColor(DyeColor.getByDyeData(data));
			break;
		default:
			d.setData(data);
		}
		theItemStack.setData(d);
	}

	public void readMeta(ResultSet rs) throws SQLException {
		String meta_key = rs.getString("meta_key");
		InventorySQL.d("Readed a meta : " + meta_key);
		if (rs.wasNull()){
			InventorySQL.d("empty meta key");
			return;
		}
		ItemMeta meta = theItemStack.getItemMeta();
		String meta_value = rs.getString("meta_value");
		InventorySQL.d("Readed a meta val : " + meta_value);
		if (rs.wasNull()){
			InventorySQL.d("empty meta val");
			return;
		}
		if ("DisplayName".equals(meta_key)) {
			meta.setDisplayName(meta_value);
		} else {
			Matcher match = LorePattern.matcher(meta_key);
			if (match.matches()) {
				int l = Integer.parseInt(match.group(1));
				lores.put(l, meta_value); // use the TreeMap to sort Lores
											// lines
				// complexe way to avoid ClassCastException
				meta.setLore(Arrays.asList(lores.values().toArray(
						new String[] {})));
			}
		}

		/* handling meta for special items */
		switch (theItemStack.getType()) {
		case SKULL_ITEM:
			if ("Owner".equals(meta_key)) {
				((SkullMeta) meta).setOwner(meta_value);
			}
			break;
		case MAP:
			if ("Scaling".equals(meta_key)) {
				((MapMeta) meta)
						.setScaling("true".equalsIgnoreCase(meta_value));
			}
			break;
		case FIREWORK_CHARGE:
			if ("Flicker".equals(meta_key)) {
				fwBuilder.flicker("true".equalsIgnoreCase(meta_value));
			} else if ("Trail".equals(meta_key)) {
				fwBuilder.trail("true".equalsIgnoreCase(meta_value));
			} else if ("Trail".equals(meta_key)) {
				// fwBuilder.
			} else
				break;
		default:
			break;

		}
		theItemStack.setItemMeta(meta);

	}

	public void readEnch(ResultSet rs) throws SQLException {
		int ench_id = rs.getInt("ench");
		if (!rs.wasNull()) {
			ItemMeta m = theItemStack.getItemMeta();
			Enchantment e = Enchantment.getById(ench_id);
			int ench_level = rs.getInt("level");
			if ((e != null) && ench_level > 0) {
				try {
					if (theItemStack.getType() == Material.ENCHANTED_BOOK) {
						InventorySQL.d("Adding ench to ENCHANTED_BOOK");
						((EnchantmentStorageMeta) m).addStoredEnchant(e,
								ench_level, Config.allow_unsafe_ench);
					} else {
						m.addEnchant(e, ench_level, Config.allow_unsafe_ench);
					}
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
		return "SQLItemStack [theItemStack=" + theItemStack.toString()
				+ ", theID=" + theID + ", theAction=" + theAction
				+ ", theSlotID=" + theSlotID + "]";
	}

}
