package com.x1f4r.mmocraft.world.zone.service;

import com.x1f4r.mmocraft.config.ConfigManager;
import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.eventbus.EventBusService;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.world.zone.model.Zone;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BasicZoneManager implements ZoneManager {

    private final MMOCraftPlugin plugin;
    private final LoggingUtil logger;
    private final EventBusService eventBusService;

    private final Map<String, Zone> zonesById = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> playerCurrentZoneIds = new ConcurrentHashMap<>();

    public BasicZoneManager(MMOCraftPlugin plugin, LoggingUtil logger, EventBusService eventBusService) {
        this.plugin = plugin;
        this.logger = logger;
        this.eventBusService = eventBusService;
        logger.debug("BasicZoneManager initialized.");
    }

    @Override
    public void loadZones() {
        ConfigManager zoneConfigManager = new ConfigManager(plugin, "zones.yml", logger);
        ConfigurationSection root = zoneConfigManager.getConfig();
        zonesById.clear();
        logger.info("Loading zones from zones.yml...");

        if (root == null) {
            logger.warning("zones.yml is empty or could not be read. No zones will be loaded.");
            return;
        }

        int loadedCount = 0;
        for (String zoneId : root.getKeys(false)) {
            String path = zoneId;
            if (!root.isConfigurationSection(path)) {
                logger.warning("Skipping non-section entry '" + zoneId + "' in zones.yml.");
                continue;
            }

            try {
                String name = root.getString(path + ".name", "Unnamed Zone");
                String world = root.getString(path + ".world");
                if (world == null || world.isBlank()) {
                    logger.severe("Zone '" + zoneId + "' is missing required 'world' property. Skipping.");
                    continue;
                }

                int minX = root.getInt(path + ".min-x");
                int minY = root.getInt(path + ".min-y");
                int minZ = root.getInt(path + ".min-z");
                int maxX = root.getInt(path + ".max-x");
                int maxZ = root.getInt(path + ".max-z");
                int maxY = root.getInt(path + ".max-y", 256);

                Map<String, Object> properties = new HashMap<>();
                ConfigurationSection propertiesSection = root.getConfigurationSection(path + ".properties");
                if (propertiesSection != null) {
                    for (String key : propertiesSection.getKeys(false)) {
                        properties.put(key, propertiesSection.get(key));
                    }
                }

                Zone zone = new Zone(zoneId, name, world, minX, minY, minZ, maxX, maxY, maxZ, properties);
                registerZone(zone);
                loadedCount++;
            } catch (Exception e) {
                logger.severe("Failed to load zone with ID '" + zoneId + "' from zones.yml. Please check configuration.", e);
            }
        }
        logger.info("Successfully loaded " + loadedCount + " zones.");
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
                      .map(this::getZone)
                      .filter(Optional::isPresent)
                      .map(Optional::get)
                      .collect(Collectors.toList());
    }


    @Override
    public void updatePlayerCurrentZones(UUID playerUUID, Set<String> currentZoneIds) {
        if (currentZoneIds == null || currentZoneIds.isEmpty()) {
            playerCurrentZoneIds.remove(playerUUID);
        } else {
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
