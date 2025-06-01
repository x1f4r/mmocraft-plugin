package com.x1f4r.mmocraft.world.zone.model;

import org.bukkit.Location;
import org.bukkit.World; // For world name comparison if Location.getWorld() is used

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a defined geographical area (cuboid) within a specific world,
 * potentially having special properties.
 */
public class Zone {

    private final String zoneId;
    private final String zoneName; // Optional, displayable name
    private final String worldName;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final Map<String, Object> properties;

    /**
     * Constructs a new Zone.
     * The coordinates are inclusive.
     *
     * @param zoneId Unique identifier for the zone.
     * @param zoneName Displayable name for the zone (can be null).
     * @param worldName The name of the world this zone belongs to.
     * @param x1 One corner's X coordinate.
     * @param y1 One corner's Y coordinate.
     * @param z1 One corner's Z coordinate.
     * @param x2 The opposite corner's X coordinate.
     * @param y2 The opposite corner's Y coordinate.
     * @param z2 The opposite corner's Z coordinate.
     * @param properties A map of custom properties for this zone (can be null or empty).
     */
    public Zone(String zoneId, String zoneName, String worldName,
                int x1, int y1, int z1, int x2, int y2, int z2,
                Map<String, Object> properties) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId cannot be null");
        this.zoneName = zoneName; // Nullable
        this.worldName = Objects.requireNonNull(worldName, "worldName cannot be null");

        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);

        this.properties = (properties != null) ? Collections.unmodifiableMap(new HashMap<>(properties)) : Collections.emptyMap();
    }

    // Minimal constructor without properties
    public Zone(String zoneId, String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        this(zoneId, null, worldName, x1, y1, z1, x2, y2, z2, null);
    }


    // Getters
    public String getZoneId() { return zoneId; }
    public String getZoneName() { return zoneName != null ? zoneName : zoneId; } // Fallback to ID if name is null
    public String getWorldName() { return worldName; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }
    public Map<String, Object> getProperties() { return properties; } // Already unmodifiable

    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, T defaultValue) {
        return (T) properties.getOrDefault(key, defaultValue);
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        Object value = properties.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) { // Allow "true"/"false" strings
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }


    /**
     * Checks if the given Bukkit Location is within this zone's boundaries.
     *
     * @param location The Bukkit Location to check.
     * @return True if the location is within this zone, false otherwise.
     */
    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        return contains(location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
    }

    /**
     * Checks if the given coordinates and world name fall within this zone's boundaries.
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param z The Z coordinate.
     * @param worldName The name of the world.
     * @return True if the coordinates are within this zone, false otherwise.
     */
    public boolean contains(int x, int y, int z, String worldName) {
        if (!this.worldName.equalsIgnoreCase(worldName)) {
            return false;
        }
        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Zone zone = (Zone) o;
        return zoneId.equals(zone.zoneId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zoneId);
    }

    @Override
    public String toString() {
        return "Zone{" +
               "zoneId='" + zoneId + '\'' +
               (zoneName != null ? ", zoneName='" + zoneName + '\'' : "") +
               ", worldName='" + worldName + '\'' +
               ", minX=" + minX + ", minY=" + minY + ", minZ=" + minZ +
               ", maxX=" + maxX + ", maxY=" + maxY + ", maxZ=" + maxZ +
               ", properties=" + properties.size() +
               '}';
    }
}
