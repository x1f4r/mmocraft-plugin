package com.x1f4r.mmocraft.world.zone.service;

import com.x1f4r.mmocraft.world.zone.model.Zone;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Manages defined geographical zones within the game world.
 */
public interface ZoneManager {

    /**
     * Loads or reloads all zones from the configuration source.
     */
    void loadZones();

    /**
     * Updates whether the default zone configuration should be copied from the plugin JAR.
     * Implementations that do not support this behaviour may ignore the call.
     */
    default void setCopyDefaultZoneFile(boolean copyDefault) {
        // no-op by default
    }

    /**
     * Registers a new zone.
     * @param zone The Zone to register.
     */
    void registerZone(Zone zone);

    /**
     * Unregisters a zone by its ID.
     * @param zoneId The ID of the zone to remove.
     */
    void unregisterZone(String zoneId);

    /**
     * Retrieves a zone by its unique ID.
     * @param zoneId The ID of the zone.
     * @return An Optional containing the Zone if found, otherwise empty.
     */
    Optional<Zone> getZone(String zoneId);

    /**
     * Finds all registered zones that contain the given Bukkit Location.
     * @param location The location to check.
     * @return A List of Zones containing the location. May be empty.
     */
    List<Zone> getZones(Location location);

    /**
     * Finds all registered zones that contain the given coordinates in the specified world.
     * @param x X coordinate.
     * @param y Y coordinate.
     * @param z Z coordinate.
     * @param worldName Name of the world.
     * @return A List of Zones containing the coordinates. May be empty.
     */
    List<Zone> getZones(int x, int y, int z, String worldName);


    /**
     * Gets the set of Zone IDs the player is currently known to be inside.
     * This relies on an internal cache updated by player movement.
     *
     * @param player The player.
     * @return A Set of zone IDs the player is currently in. Might be empty.
     */
    Set<String> getPlayerCurrentZoneIds(Player player);

    /**
     * Gets the Zone objects the player is currently known to be inside.
     * @param player The player
     * @return A List of Zone objects.
     */
    List<Zone> getPlayerCurrentZones(Player player);


    /**
     * Updates the internal cache of which zones a player is currently in.
     * This is typically called by listeners when a player moves or teleports.
     *
     * @param playerUUID The UUID of the player.
     * @param currentZoneIds The set of zone IDs the player is now in.
     */
    void updatePlayerCurrentZones(UUID playerUUID, Set<String> currentZoneIds);

    /**
     * Clears the cached zone information for a player, typically on quit.
     * @param playerUUID The UUID of the player.
     */
    void clearPlayerZoneCache(UUID playerUUID);


    /**
     * Retrieves all registered zones.
     * @return An unmodifiable collection of all zones.
     */
    Collection<Zone> getAllZones();
}
