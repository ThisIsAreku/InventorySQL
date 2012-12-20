package fr.areku.InventorySQL;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.EventExecutor;

import fr.areku.Authenticator.events.PlayerOfflineModeLogin;

public class UpdateEventListener implements Listener {
	private InventorySQL plugin;

	public UpdateEventListener(InventorySQL pl) {
		try {
			this.plugin = pl;

			if (Config.update_events.contains("join")) {
				InventorySQL.d("Registering PlayerJoinEvent");
				registerThis(PlayerJoinEvent.class, new EventExecutor() {
					public void execute(Listener listener, Event event)
							throws EventException {
						doPlayerJoin((PlayerJoinEvent) event);
					}
				});
			}

			if (Config.update_events.contains("quit")) {
				InventorySQL.d("Registering PlayerQuitEvent");
				registerThis(PlayerQuitEvent.class, new EventExecutor() {

					public void execute(Listener listener, Event event)
							throws EventException {
						doPlayerQuit((PlayerQuitEvent) event);
					}
				});
			}

			if (Config.update_events.contains("changeworld")) {
				InventorySQL.d("Registering PlayerChangedWorldEvent");
				registerThis(PlayerChangedWorldEvent.class,
						new EventExecutor() {
							public void execute(Listener listener, Event event)
									throws EventException {
								doPlayerChangedWorld((PlayerChangedWorldEvent) event);
							}
						});
			}

			if (Config.update_events.contains("respawn")) {
				InventorySQL.d("Registering PlayerRespawnEvent");
				registerThis(PlayerRespawnEvent.class, new EventExecutor() {
					public void execute(Listener listener, Event event)
							throws EventException {
						doPlayerRespawn((PlayerRespawnEvent) event);
					}
				});
			}

			if (Config.update_events.contains("death")) {
				InventorySQL.d("Registering PlayerDeathEvent");
				registerThis(PlayerDeathEvent.class, new EventExecutor() {
					public void execute(Listener listener, Event event)
							throws EventException {
						if (event instanceof PlayerDeathEvent)
							doPlayerDeath((PlayerDeathEvent) event);
					}
				});
			}

			if (Config.update_events.contains("bedenter")) {
				InventorySQL.d("Registering PlayerBedEnterEvent");
				registerThis(PlayerBedEnterEvent.class, new EventExecutor() {
					public void execute(Listener listener, Event event)
							throws EventException {
						doPlayerBedEnter((PlayerBedEnterEvent) event);
					}
				});
			}

			if (Config.update_events.contains("bedleave")) {
				InventorySQL.d("Registering PlayerBedLeaveEvent");
				registerThis(PlayerBedLeaveEvent.class, new EventExecutor() {
					public void execute(Listener listener, Event event)
							throws EventException {
						doPlayerBedLeave((PlayerBedLeaveEvent) event);
					}
				});
			}

		} catch (Exception e) {
			InventorySQL.logException(e, "Listener init");
		}
	}

	private void registerThis(Class<? extends Event> eventClass,
			EventExecutor exec) {
		Bukkit.getPluginManager().registerEvent(eventClass, this,
				EventPriority.NORMAL, exec, this.plugin, true);
	}

	public void registerOfflineModeSupport() {
		if (!InventorySQL.isUsingAuthenticator())
			return;

		registerThis(PlayerOfflineModeLogin.class, new EventExecutor() {
			public void execute(Listener listener, Event event)
					throws EventException {
				doPlayerOfflineModeLogin((PlayerOfflineModeLogin) event);
			}
		});
	}

	public void doPlayerJoin(PlayerJoinEvent event) {
		if (Config.mirrorMode) {
			event.getPlayer().sendMessage(
					ChatColor.RED
							+ "[InventorySQL] "
							+ InventorySQL.getMessage("mirror-wait", event
									.getPlayer().getDisplayName()));
		}
		if (InventorySQL.isUsingAuthenticator())
			return;
		InventorySQL.d("onPlayerJoin(" + event.toString() + ")");
		InventorySQL.getCoreSQLProcess().runPlayerCheck(event.getPlayer(),
				"PlayerJoin", event.getPlayer());
	}

	public void doPlayerQuit(PlayerQuitEvent event) {
		if (InventorySQL.isUsingAuthenticator()) {
			if (!fr.areku.Authenticator.Authenticator.isPlayerLoggedIn(event
					.getPlayer()))
				return;
		}
		InventorySQL.d("onPlayerQuit(" + event.toString() + ")");
		InventorySQL.getCoreSQLProcess().runPlayerCheck(event.getPlayer(),
				"PlayerJoin", event.getPlayer(), false, 0);
	}

	public void doPlayerChangedWorld(PlayerChangedWorldEvent event) {
		if (InventorySQL.isUsingAuthenticator()) {
			if (!fr.areku.Authenticator.Authenticator.isPlayerLoggedIn(event
					.getPlayer()))
				return;
		}
		InventorySQL.d("onPlayerChangedWorld(" + event.toString() + ")");
		InventorySQL.getCoreSQLProcess().runPlayerCheck(event.getPlayer(),
				"PlayerChangedWorld", event.getPlayer(), false, 0);
	}

	public void doPlayerRespawn(PlayerRespawnEvent event) {
		if (InventorySQL.isUsingAuthenticator()) {
			if (!fr.areku.Authenticator.Authenticator.isPlayerLoggedIn(event
					.getPlayer()))
				return;
		}
		InventorySQL.d("onPlayerRespawn(" + event.toString() + ")");
		InventorySQL.getCoreSQLProcess().runPlayerCheck(event.getPlayer(),
				"PlayerRespawn", event.getPlayer());
	}

	public void doPlayerDeath(PlayerDeathEvent event) {
		if (InventorySQL.isUsingAuthenticator()) {
			if (!fr.areku.Authenticator.Authenticator.isPlayerLoggedIn(event
					.getEntity()))
				return;
		}
		InventorySQL.d("onPlayerDeath(" + event.toString() + ")");
		InventorySQL.getCoreSQLProcess().runPlayerCheck(event.getEntity(),
				"PlayerRespawn", event.getEntity(), false, 0);
	}

	public void doPlayerBedEnter(PlayerBedEnterEvent event) {
		InventorySQL.d("onPlayerBedEnter(" + event.toString() + ")");
		InventorySQL.getCoreSQLProcess().runPlayerCheck(event.getPlayer(),
				"PlayerBedEnter", event.getPlayer());
	}

	public void doPlayerBedLeave(PlayerBedLeaveEvent event) {
		InventorySQL.d("onPlayerBedLeave(" + event.toString() + ")");
		InventorySQL.getCoreSQLProcess().runPlayerCheck(event.getPlayer(),
				"PlayerBedLeave", event.getPlayer());
	}

	public void doPlayerOfflineModeLogin(PlayerOfflineModeLogin event) {
		InventorySQL.d("onPlayerOfflineModeLogin("
				+ event.getPlayer().toString() + ")");
		InventorySQL.getCoreSQLProcess().runPlayerCheck(event.getPlayer(),
				"PlayerOfflineModeLogin", event.getPlayer());
	}

}
