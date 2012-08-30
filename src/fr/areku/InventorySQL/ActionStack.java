package fr.areku.InventorySQL;


import org.bukkit.inventory.ItemStack;


public class ActionStack {
    private ItemStack i;
    private String params;

    public ActionStack(ItemStack i, String params) {
        this.i = i;
        this.params = params;
    }

    public ItemStack item() {
        return i;
    }

    public String params() {
        return params;
    }
}
