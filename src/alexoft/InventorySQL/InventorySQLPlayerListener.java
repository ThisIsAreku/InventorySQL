
package alexoft.InventorySQL;


import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInventoryEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;


/**
 *
 * @author Alexandre
 */
public class InventorySQLPlayerListener extends PlayerListener {
    private Main plugin;

    public InventorySQLPlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onInventoryOpen(PlayerInventoryEvent event) {
        this.plugin.invokeCheck(event.getPlayer(), true);
    }

    @Override
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        this.plugin.invokeCheck(event.getPlayer(), true);
    }
    
    @Override
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        this.plugin.invokeCheck(event.getPlayer(), true);        
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.plugin.invokeCheck(event.getPlayer(), true);        
    }
    
    

    @Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		// TODO Auto-generated method stub
    	this.plugin.log(event.getMaterial().toString());
		super.onPlayerInteract(event);
	}

	@Override
    public void onPlayerJoin(PlayerJoinEvent event) {// InventorySQLTable inv = plugin.getDatabase().find(InventorySQLTable.class).where().ieq("player",
        /* event.getPlayer().getName()).findUnique();

         // update inventory
         if (inv != null) {
         inv.setId(1);
         inv.setplayer(event.getPlayer().getName());
         this.plugin.getDatabase().save(inv);
         }*/}
}

// ~ Formatted by Jindent --- http://www.jindent.com
