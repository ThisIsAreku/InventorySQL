/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package alexoft.InventorySQL;


import org.bukkit.inventory.ItemStack;


/**
 *
 * @author Alexandre
 */
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
