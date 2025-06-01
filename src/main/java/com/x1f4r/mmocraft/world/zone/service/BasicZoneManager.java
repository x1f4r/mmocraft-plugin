package com.x1f4r.mmocraft.world.zone.service;

import com.x1f4r.mmocraft.core.MMOCraftPlugin; // Not strictly needed if only logger/eventbus passed
import com.x1f4r.mmocraft.eventbus.EventBusService;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.world.zone.model.Zone;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BasicZoneManager implements ZoneManager {

    private final LoggingUtil logger;
    private final EventBusService eventBusService; // For firing PlayerEnterZoneEvent/PlayerLeaveZoneEvent

    private final Map<String, Zone> zonesById = new ConcurrentHashMap<>();
    // Cache for player's current zones to avoid re-calculating on every minor move.
    private final Map<UUID, Set<String>> playerCurrentZoneIds = new ConcurrentHashMap<>();


    public BasicZoneManager(MMOCraftPlugin plugin, LoggingUtil logger, EventBusService eventBusService) {
        // this.plugin = plugin; // Store if needed for other Bukkit services directly
        this.logger = logger;
        this.eventBusService = eventBusService;
        logger.debug("BasicZoneManager initialized.");
    }

    @Override
    public void registerZone(Zone zone) {
        if (zone == null || zone.getZoneId() == null || zone.getZoneId().trim().isEmpty()) {
            logger.warning("Attempted to register null zone or zone with invalid ID.");
            return;
        }
        Zone existing = zonesById.put(zone.getZoneId().toLowerCase(), zone);
        if (existing != null) {
            logger.warning("Zone ID '" + zone.getZoneId() + "' was already registered. Overwriting '" + existing.getZoneName() + "'.");
        } else {
            logger.info("Registered zone: " + zone.getZoneName() + " (ID: " + zone.getZoneId() + ")");
        }
    }

    @Override
    public void unregisterZone(String zoneId) {
        if (zoneId == null) return;
        Zone removed = zonesById.remove(zoneId.toLowerCase());
        if (removed != null) {
            logger.info("Unregistered zone: " + removed.getZoneName() + " (ID: " + zoneId + ")");
            // Also remove this zoneId from all players' current zones if they were in it
            playerCurrentZoneIds.values().forEach(set -> set.remove(zoneId.toLowerCase()));
        }
    }

    @Override
    public Optional<Zone> getZone(String zoneId) {
        if (zoneId == null) return Optional.empty();
        return Optional.ofNullable(zonesById.get(zoneId.toLowerCase()));
    }

    @Override
    public List<Zone> getZones(Location location) {
        if (location == null || location.getWorld() == null) return Collections.emptyList();
        return getZones(location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
    }

    @Override
    public List<Zone> getZones(int x, int y, int z, String worldName) {
         if (worldName == null) return Collections.emptyList();
        return zonesById.values().stream()
                .filter(zone -> zone.contains(x, y, z, worldName))
                .collect(Collectors.toList());
    }


    @Override
    public Set<String> getPlayerCurrentZoneIds(Player player) {
        if (player == null) return Collections.emptySet();
        return Collections.unmodifiableSet(playerCurrentZoneIds.getOrDefault(player.getUniqueId(), Collections.emptySet()));
    }

    @Override
    public List<Zone> getPlayerCurrentZones(Player player) {
        if (player == null) return Collections.emptyList();
        Set<String> zoneIds = playerCurrentZoneIds.get(player.getUniqueId());
        if (zoneIds == null || zoneIds.isEmpty()) {
            return Collections.emptyList();
        }
        return zoneIds.stream()
                      .map(this::getZone) // Uses the lowercase ID from the cache
                      .filter(Optional::isPresent)
                      .map(Optional::get)
                      .collect(Collectors.toList());
    }


    @Override
    public void updatePlayerCurrentZones(UUID playerUUID, Set<String> currentZoneIds) {
        if (currentZoneIds == null || currentZoneIds.isEmpty()) {
            playerCurrentZoneIds.remove(playerUUID);
        } else {
            // Store lowercase IDs for consistent lookup
            Set<String> lowerCaseZoneIds = currentZoneIds.stream().map(String::toLowerCase).collect(Collectors.toSet());
            playerCurrentZoneIds.put(playerUUID, new HashSet<>(lowerCaseZoneIds));
        }
        logger.finest("Updated current zones for player " + playerUUID + " to: " + currentZoneIds);
    }

    @Override
    public void clearPlayerZoneCache(UUID playerUUID) {
        Set<String> removed = playerCurrentZoneIds.remove(playerUUID);
        if (removed != null) {
            logger.fine("Cleared zone cache for player " + playerUUID);
        }
    }

    @Override
    public Collection<Zone> getAllZones() {
        return Collections.unmodifiableCollection(zonesById.values());
    }
}
