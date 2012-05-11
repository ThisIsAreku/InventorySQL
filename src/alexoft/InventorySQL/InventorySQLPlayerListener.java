package alexoft.InventorySQL;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.EventExecutor;

import alexoft.InventorySQL.database.CoreSQLItem;

public class InventorySQLPlayerListener implements Listener {
	/*
	 * private final Long EVENT_QUEUE = (long) (3 * 1000); private Map<Player,
	 * Long> delayer = new HashMap<Player, Long>();
	 */
	private Main plugin;

	/*
	 * private boolean doDelay(Player p) { if (delayer.containsKey(p)) {
	 * //Main.log((System.currentTimeMillis() - delayer.get(p)) + ">" +
	 * EVENT_QUEUE); if ((System.currentTimeMillis() - delayer.get(p)) >
	 * EVENT_QUEUE) { Main.d("EVENT_QUEUE reached for " + p.getName());
	 * delayer.put(p, System.currentTimeMillis()); return true; } } else {
	 * delayer.put(p, System.currentTimeMillis()); } return false; }
	 */

	public InventorySQLPlayerListener(Main plugin) {
		try {
			this.plugin = plugin;
			/*
			 * if (Config.lightweight_mode) { this.plugin .getServer()
			 * .getPluginManager() .registerEvent(PlayerJoinEvent.class, this,
			 * EventPriority.NORMAL, new EventExecutor() {
			 * 
			 * @Override public void execute(Listener arg0, Event arg1) throws
			 * EventException { onPlayerJoin((PlayerJoinEvent) arg1); }
			 * 
			 * }, this.plugin, true); this.plugin .getServer()
			 * .getPluginManager() .registerEvent(PlayerQuitEvent.class, this,
			 * EventPriority.NORMAL, new EventExecutor() {
			 * 
			 * @Override public void execute(Listener arg0, Event arg1) throws
			 * EventException { onPlayerQuit((PlayerQuitEvent) arg1); }
			 * 
			 * }, this.plugin, true); } else {
			 */
			if (Config.update_events.contains("join")){
				Main.d("Registering PlayerJoinEvent");
				registerThis(PlayerJoinEvent.class, new EventExecutor() {
					@Override
					public void execute(Listener arg0, Event arg1)
							throws EventException {
						onPlayerJoin((PlayerJoinEvent) arg1);
					}
				});
			}

			if (Config.update_events.contains("quit")){
				Main.d("Registering PlayerQuitEvent");
				registerThis(PlayerQuitEvent.class, new EventExecutor() {
					@Override
					public void execute(Listener arg0, Event arg1)
							throws EventException {
						onPlayerQuit((PlayerQuitEvent) arg1);
					}
				});
			}

			if (Config.update_events.contains("changeworld")){
				Main.d("Registering PlayerChangedWorldEvent");
				registerThis(PlayerChangedWorldEvent.class, new EventExecutor() {
					@Override
					public void execute(Listener arg0, Event arg1)
							throws EventException {
						onPlayerChangedWorld((PlayerChangedWorldEvent) arg1);
					}
				});
			}

			if (Config.update_events.contains("respawn")){
				Main.d("Registering PlayerRespawnEvent");
				registerThis(PlayerRespawnEvent.class, new EventExecutor() {
					@Override
					public void execute(Listener arg0, Event arg1)
							throws EventException {
						onPlayerRespawn((PlayerRespawnEvent) arg1);
					}
				});
			}

			if (Config.update_events.contains("bedenter")){
				Main.d("Registering PlayerBedEnterEvent");
				registerThis(PlayerBedEnterEvent.class, new EventExecutor() {
					@Override
					public void execute(Listener arg0, Event arg1)
							throws EventException {
						onPlayerBedEnter((PlayerBedEnterEvent) arg1);
					}
				});
			}

			if (Config.update_events.contains("bedleave")){
				Main.d("Registering PlayerBedLeaveEvent");
				registerThis(PlayerBedLeaveEvent.class, new EventExecutor() {
					@Override
					public void execute(Listener arg0, Event arg1)
							throws EventException {
						onPlayerBedLeave((PlayerBedLeaveEvent) arg1);
					}
				});
			}
			// }
		} catch (Exception e) {
			Main.logException(e, "Listener init");
		}
	}

	private void registerThis(Class<? extends Event> eventClass,
			EventExecutor exec) {
		this.plugin
				.getServer()
				.getPluginManager()
				.registerEvent(eventClass, this, EventPriority.NORMAL, exec,
						this.plugin, true);
	}

	/*
	 * @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	 * public void onInventoryOpen(InventoryOpenEvent event) { if
	 * (!Config.checkChest) return; Chest[] c = null; Block b =
	 * event.getPlayer().getTargetBlock(null, 5); if (b.getType() ==
	 * Material.CHEST) { c = new Chest[] { (Chest) b.getState() }; }
	 * this.plugin.invokeCheck(new Player[] { (Player) event.getPlayer() }, c,
	 * null); }
	 * 
	 * @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	 * public void onInventoryClose(InventoryCloseEvent event) { if
	 * (!Config.checkChest) return; Chest[] c = null; Block b =
	 * event.getPlayer().getTargetBlock(null, 5); if (b.getType() ==
	 * Material.CHEST) { c = new Chest[] { (Chest) b.getState() }; }
	 * this.plugin.invokeCheck(new Player[] { (Player) event.getPlayer() }, c,
	 * null); }
	 * 
	 * @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	 * public void onPlayerDropItem(PlayerDropItemEvent event) { //
	 * Main.log("onPlayerDropItem"); if (doDelay(event.getPlayer()))
	 * this.plugin.invokeCheck(new Player[] { event.getPlayer() }, null); }
	 * 
	 * @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	 * public void onPlayerPickupItem(PlayerPickupItemEvent event) { //
	 * Main.log("onPlayerPickupItem"); if (doDelay(event.getPlayer()))
	 * this.plugin.invokeCheck(new Player[] { event.getPlayer() }, null); }
	 * 
	 * @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	 * public void onPlayerInteract(PlayerInteractEvent event) { if
	 * ((event.getMaterial() == Material.CHEST) && (event.getAction() ==
	 * Action.RIGHT_CLICK_BLOCK) &&
	 * (event.getClickedBlock().getState().getType() == Material.CHEST) &&
	 * Config.checkChest) { this.plugin.invokeCheck(new Chest[] { (Chest) event
	 * .getClickedBlock().getState() }, null); } }
	 * 
	 * @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	 * public void onBlockPlace(BlockPlaceEvent event) { if
	 * ((event.getBlockPlaced().getType() == Material.CHEST) &&
	 * Config.checkChest) { this.plugin.invokeCheck(new Player[] {
	 * event.getPlayer() }, new Chest[] { (Chest)
	 * event.getBlockPlaced().getState() }, null); } else { if
	 * (doDelay(event.getPlayer())) this.plugin.invokeCheck(new Player[] {
	 * event.getPlayer() }, null); } }
	 */

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event) {
		this.plugin.getCoreSQLProcess().runCheckThisTask(
				new CoreSQLItem(new Player[] { event.getPlayer() }), true,
				Config.afterLoginDelay);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent event) {
		this.plugin.getCoreSQLProcess().runCheckThisTask(
				new CoreSQLItem(new Player[] { event.getPlayer() }), false, 0);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		this.plugin.getCoreSQLProcess().runCheckThisTask(
				new CoreSQLItem(new Player[] { event.getPlayer() }), false, 0);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		this.plugin.getCoreSQLProcess().runCheckThisTask(
				new CoreSQLItem(new Player[] { event.getPlayer() }), true, 0);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerBedEnter(PlayerBedEnterEvent event) {
		this.plugin.getCoreSQLProcess().runCheckThisTask(
				new CoreSQLItem(new Player[] { event.getPlayer() }), true, 0);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
		this.plugin.getCoreSQLProcess().runCheckThisTask(
				new CoreSQLItem(new Player[] { event.getPlayer() }), true, 0);
	}

}
