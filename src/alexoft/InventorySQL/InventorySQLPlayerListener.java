package alexoft.InventorySQL;


import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;


public class InventorySQLPlayerListener implements Listener {

    private Main plugin;

    public InventorySQLPlayerListener(Main plugin) {
        try {
            this.plugin = plugin;
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        } catch (Exception e) {
            Main.logException(e, "Listener init");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        this.plugin.invokeCheck(new Player[] { event.getPlayer()}, true, null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        this.plugin.invokeCheck(new Player[] { event.getPlayer()}, true, null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.plugin.invokeCheck(new Player[] { event.getPlayer()}, true, null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {// TODO Auto-generated method stub
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {}

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.plugin.invokeCheck(new Player[] { event.getPlayer()}, true, 10, null);
        // InventorySQLTable inv =
        // plugin.getDatabase().find(InventorySQLTable.class).where().ieq("player",
        /*
         * event.getPlayer().getName()).findUnique();
         * 
         * // update inventory if (inv != null) { inv.setId(1);
         * inv.setplayer(event.getPlayer().getName());
         * this.plugin.getDatabase().save(inv); }
         */}

}
