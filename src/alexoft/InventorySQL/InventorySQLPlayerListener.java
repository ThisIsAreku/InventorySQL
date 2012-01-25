package alexoft.InventorySQL;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;

/**
 *
 * @author Alexandre
 */
public class InventorySQLPlayerListener implements Listener {

    private Main plugin;

    public InventorySQLPlayerListener(Main plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        this.plugin.invokeCheck(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        this.plugin.invokeCheck(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.plugin.invokeCheck(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // TODO Auto-generated method stub
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event){
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.plugin.invokeCheck(event.getPlayer(), true, 10);
        // InventorySQLTable inv = plugin.getDatabase().find(InventorySQLTable.class).where().ieq("player",
        /*
         * event.getPlayer().getName()).findUnique();
         *
         * // update inventory if (inv != null) { inv.setId(1);
         * inv.setplayer(event.getPlayer().getName());
         * this.plugin.getDatabase().save(inv);
         }
         */    }
    
}
