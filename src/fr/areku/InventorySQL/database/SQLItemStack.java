package fr.areku.InventorySQL.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import fr.areku.InventorySQL.Config;
import fr.areku.InventorySQL.Main;

public class SQLItemStack{
	public enum Action{ADD, REMOVE};
	private ItemStack theItemStack;
	private String theID;
	private Action theAction;
	
	public ItemStack getItemStack(){
		return theItemStack;
	}
	
	public String getID(){
		return theID;
	}
	
	public Action getAction(){
		return theAction;
	}

	public SQLItemStack(ResultSet rs, String id) throws SQLException {
		theID = id;
		int count = rs.getInt("count");
		theItemStack = new ItemStack(rs.getInt("item"),
				Math.abs(count),
				(short) rs.getInt("damage"),
				rs.getByte("data"));
		readEnch(rs);
		
		if(count > 0){
			theAction = Action.ADD;
		}else if(count < 0){
			theAction = Action.REMOVE;
		}
	}

	public void readEnch(ResultSet rs) throws SQLException {
		int ench_id = rs.getInt("ench");
		if (!rs.wasNull()) {
			Enchantment e = Enchantment.getById(ench_id);
			int ench_level = rs.getInt("level");
			if ((e != null) && ench_level > 0) {
				try {
					if (Config.allow_unsafe_ench) {
						theItemStack.addUnsafeEnchantment(e, ench_level);
					} else {
						theItemStack.addEnchantment(e, ench_level);
					}
				} catch (Exception ex) {
					Main.log(Level.WARNING, "Error while adding " + e.getName()
							+ "/" + ench_level + " to " + this.toString());
					Main.log(Level.WARNING, ex.getLocalizedMessage());
				}
			}
		}
	}

	@Override
	public String toString() {
		return "SQLItemStack [theItemStack=" + theItemStack.toString() + " with "+theItemStack.getEnchantments().entrySet().toString()+", theID="
				+ theID + ", theAction=" + theAction + "]";
	}
	
	

}
