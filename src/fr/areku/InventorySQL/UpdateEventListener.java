package fr.areku.InventorySQL;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.EventExecutor;

import fr.areku.Authenticator.api.IOfflineModeListener;
import fr.areku.InventorySQL.database.CoreSQLItem;

public class UpdateEventListener implements Listener, IOfflineModeListener {
	private Main plugin;

	public UpdateEventListener(Main pl) {
		try {
			this.plugin = pl;

			if (Config.update_events.contains("join")) {
				Main.d("Registering PlayerJoinEvent");
				registerThis(PlayerJoinEvent.class, new EventExecutor() {
					public void execute(Listener listener, Event event)
							throws EventException {
						doPlayerJoin((PlayerJoinEvent) event);
					}
				});
			}

			if (Config.update_events.contains("quit")) {
				Main.d("Registering PlayerQuitEvent");
				registerThis(PlayerQuitEvent.class, new EventExecutor() {

					public void execute(Listener listener, Event event)
							throws EventException {
						doPlayerQuit((PlayerQuitEvent) event);
					}
				});
			}

			if (Config.update_events.contains("changeworld")) {
				Main.d("Registering PlayerChangedWorldEvent");
				registerThis(PlayerChangedWorldEvent.class,
						new EventExecutor() {
							public void execute(Listener listener, Event event)
									throws EventException {
								doPlayerChangedWorld((PlayerChangedWorldEvent) event);
							}
						});
			}

			if (Config.update_events.contains("respawn")) {
				Main.d("Registering PlayerRespawnEvent");
				registerThis(PlayerRespawnEvent.class, new EventExecutor() {
					public void execute(Listener listener, Event event)
							throws EventException {
						doPlayerRespawn((PlayerRespawnEvent) event);
					}
				});
			}

			if (Config.update_events.contains("bedenter")) {
				Main.d("Registering PlayerBedEnterEvent");
				registerThis(PlayerBedEnterEvent.class, new EventExecutor() {
					public void execute(Listener listener, Event event)
							throws EventException {
						doPlayerBedEnter((PlayerBedEnterEvent) event);
					}
				});
			}

			if (Config.update_events.contains("bedleave")) {
				Main.d("Registering PlayerBedLeaveEvent");
				registerThis(PlayerBedLeaveEvent.class, new EventExecutor() {
					public void execute(Listener listener, Event event)
							throws EventException {
						doPlayerBedLeave((PlayerBedLeaveEvent) event);
					}
				});
			}

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

	public void doPlayerJoin(PlayerJoinEvent event) {
		if (this.plugin.isOfflineModePlugin())
			return;
		Main.d("onPlayerJoin(" + event.toString() + ")");
		this.plugin.getCoreSQLProcess().runCheckThisTask(
				new CoreSQLItem(new Player[] { event.getPlayer() }), true,
				Config.afterLoginDelay);
	}

	public void doPlayerQuit(PlayerQuitEvent event) {
		if (this.plugin.isOfflineModePlugin()) {
			if (!fr.areku.Authenticator.Authenticator.isPlayerLoggedIn(event
					.getPlayer()))
				return;
		}
		Main.d("onPlayerQuit(" + event.toString() + ")");
		this.plugin.getCoreSQLProcess().runCheckThisTask(
				new CoreSQLItem(new Player[] { event.getPlayer() }), false, 0);
	}

	public void doPlayerChangedWorld(PlayerChangedWorldEvent event) {
		if (this.plugin.isOfflineModePlugin()) {
			if (!fr.areku.Authenticator.Authenticator.isPlayerLoggedIn(event
					.getPlayer()))
				return;
		}
		Main.d("onPlayerChangedWorld(" + event.toString() + ")");
		this.plugin.getCoreSQLProcess().runCheckThisTask(
				new CoreSQLItem(new Player[] { event.getPlayer() }), false, 0);
	}

	public void doPlayerRespawn(PlayerRespawnEvent event) {
		if (this.plugin.isOfflineModePlugin()) {
			if (!fr.areku.Authenticator.Authenticator.isPlayerLoggedIn(event
					.getPlayer()))
				return;
		}
		Main.d("onPlayerRespawn(" + event.toString() + ")");
		this.plugin.getCoreSQLProcess().runCheckThisTask(
				new CoreSQLItem(new Player[] { event.getPlayer() }), true, 0);
	}

	public void doPlayerBedEnter(PlayerBedEnterEvent event) {
		Main.d("onPlayerBedEnter(" + event.toString() + ")");
		this.plugin.getCoreSQLProcess().runCheckThisTask(
				new CoreSQLItem(new Player[] { event.getPlayer() }), true, 0);
	}

	public void doPlayerBedLeave(PlayerBedLeaveEvent event) {
		Main.d("onPlayerBedLeave(" + event.toString() + ")");
		this.plugin.getCoreSQLProcess().runCheckThisTask(
				new CoreSQLItem(new Player[] { event.getPlayer() }), true, 0);
	}

	@Override
	public void onPlayerPluginLogin(Player player) {
		Main.d("onPlayerOfflineModeLogin(" + player.toString() + ")");
		plugin.getCoreSQLProcess().runCheckThisTask(
				new CoreSQLItem(new Player[] { player }), true, 0);
	}

}
