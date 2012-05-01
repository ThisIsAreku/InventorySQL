package alexoft.InventorySQL;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class InventorySQLPlayerListener implements Listener {
	//private HashMap<Player, Integer> updateTempo;
	private Main plugin;
	
	private boolean doTemp(Player p){
		/*if(updateTempo.containsKey(p)){
			Integer i = updateTempo.get(p);
			if(i >= 5){
				updateTempo.put(p, 0);
				return true;
			}else{
				updateTempo.put(p, i+1);
				return false;
			}
		}else{
			updateTempo.put(p, 1);
			return false;
		}*/
		return true;
	}

	public InventorySQLPlayerListener(Main plugin) {
		try {
			this.plugin = plugin;
			//this.updateTempo = new HashMap<Player, Integer>();
			plugin.getServer().getPluginManager().registerEvents(this, plugin);
		} catch (Exception e) {
			Main.logException(e, "Listener init");
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onInventoryOpen(InventoryOpenEvent event) {
		Chest[] c = null;
		Block b = event.getPlayer().getTargetBlock(null, 5);
		if(b.getType() == Material.CHEST){
			c = new Chest[] { (Chest) b.getState() };
		}
		this.plugin.invokeCheck(new Player[] { (Player) event.getPlayer() }, c, null);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onInventoryClose(InventoryCloseEvent event) {
		Chest[] c = null;
		Block b = event.getPlayer().getTargetBlock(null, 5);
		if(b.getType() == Material.CHEST){
			c = new Chest[] { (Chest) b.getState() };
		}
		this.plugin.invokeCheck(new Player[] { (Player) event.getPlayer() }, c, null);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		// Main.log("onPlayerDropItem");
		this.plugin.invokeCheck(new Player[] { event.getPlayer() }, null);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		// Main.log("onPlayerPickupItem");
		this.plugin.invokeCheck(new Player[] { event.getPlayer() }, null);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerQuit(PlayerQuitEvent event) {
		this.plugin.invokeCheck(new Player[] { event.getPlayer() }, null);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerInteract(PlayerInteractEvent event) {		
		if ((event.getMaterial() == Material.CHEST) && (event.getAction() == Action.RIGHT_CLICK_BLOCK) && (event.getClickedBlock().getState().getType() == Material.CHEST)) {
			this.plugin.invokeCheck(
					new Chest[] { (Chest) event.getClickedBlock().getState() }, null);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.getBlockPlaced().getType() == Material.CHEST) {
			this.plugin.invokeCheck(new Player[] { event.getPlayer() },
					new Chest[] { (Chest) event.getBlockPlaced().getState() }, null);
		}else{
			if(doTemp(event.getPlayer())) this.plugin.invokeCheck(new Player[] { event.getPlayer() }, null);
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerJoin(PlayerJoinEvent event) {
		this.plugin.invokeCheck(new Player[] { event.getPlayer() },
				this.plugin.afterLoginDelay, null);
	}

}
