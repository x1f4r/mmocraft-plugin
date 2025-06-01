package com.x1f4r.mmocraft.world.zone.listeners;

import com.x1f4r.mmocraft.eventbus.EventBusService;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.util.StringUtil;
import com.x1f4r.mmocraft.world.zone.event.PlayerEnterZoneEvent;
import com.x1f4r.mmocraft.world.zone.event.PlayerLeaveZoneEvent;
import com.x1f4r.mmocraft.world.zone.model.Zone;
import com.x1f4r.mmocraft.world.zone.service.ZoneManager;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PlayerZoneTrackerListener implements Listener {

    private final ZoneManager zoneManager;
    private final LoggingUtil logger;
    private final EventBusService eventBusService;

    public PlayerZoneTrackerListener(ZoneManager zoneManager, LoggingUtil logger, EventBusService eventBusService) {
        this.zoneManager = zoneManager;
        this.logger = logger;
        this.eventBusService = eventBusService;
        logger.debug("PlayerZoneTrackerListener initialized.");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updatePlayerZones(player, null, player.getLocation()); // Treat join as a move from null location
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Set<String> oldZoneIds = zoneManager.getPlayerCurrentZoneIds(player);

        for (String zoneId : oldZoneIds) {
            zoneManager.getZone(zoneId).ifPresent(zone -> {
                eventBusService.call(new PlayerLeaveZoneEvent(player, zone));
                // No message to player as they are quitting
                logger.fine("Player " + player.getName() + " left zone " + zone.getZoneName() + " on quit.");
            });
        }
        zoneManager.clearPlayerZoneCache(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Optimization: check if player actually moved to a new block
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ() &&
            event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            return;
        }
        updatePlayerZones(event.getPlayer(), event.getFrom(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Teleports also trigger PlayerMoveEvent, but handling it explicitly can be clearer
        // or allow for special logic if PlayerMoveEvent is too noisy.
        // For now, PlayerMoveEvent should cover teleports as well.
        // If specific teleport logic (e.g. different messages) is needed, it can be added here.
        logger.finer("PlayerTeleportEvent for " + event.getPlayer().getName() + ", will be handled by PlayerMoveEvent logic.");
         updatePlayerZones(event.getPlayer(), event.getFrom(), event.getTo());
    }


    private void updatePlayerZones(Player player, Location fromLocation, Location toLocation) {
        if (player == null || toLocation == null) return;

        List<Zone> zonesAtTo = zoneManager.getZones(toLocation);
        Set<String> currentZoneIds = zonesAtTo.stream().map(Zone::getZoneId).map(String::toLowerCase).collect(Collectors.toSet());
        Set<String> previousZoneIds = zoneManager.getPlayerCurrentZoneIds(player); // Already lowercase if stored correctly

        // Check for entered zones
        for (Zone newZone : zonesAtTo) {
            if (!previousZoneIds.contains(newZone.getZoneId().toLowerCase())) {
                // Player entered newZone
                player.sendActionBar(StringUtil.colorize("&eNow entering: " + newZone.getZoneName()));
                // player.sendMessage(StringUtil.colorize("&eNow entering: " + newZone.getZoneName()));
                eventBusService.call(new PlayerEnterZoneEvent(player, newZone));
                logger.fine("Player " + player.getName() + " entered zone " + newZone.getZoneName());
            }
        }

        // Check for exited zones
        for (String oldZoneId : previousZoneIds) {
            if (!currentZoneIds.contains(oldZoneId)) {
                zoneManager.getZone(oldZoneId).ifPresent(oldZone -> {
                    // Player exited oldZone
                    player.sendActionBar(StringUtil.colorize("&7Now leaving: " + oldZone.getZoneName()));
                    // player.sendMessage(StringUtil.colorize("&7Now leaving: " + oldZone.getZoneName()));
                    eventBusService.call(new PlayerLeaveZoneEvent(player, oldZone));
                    logger.fine("Player " + player.getName() + " left zone " + oldZone.getZoneName());
                });
            }
        }
        zoneManager.updatePlayerCurrentZones(player.getUniqueId(), currentZoneIds);
    }
}
