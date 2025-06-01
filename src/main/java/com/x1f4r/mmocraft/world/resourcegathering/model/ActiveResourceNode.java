package com.x1f4r.mmocraft.world.resourcegathering.model;

import org.bukkit.Location;
import java.util.Objects;

public class ActiveResourceNode {

    private final Location location; // Using Bukkit's Location, assumes it's properly cloned and world is loaded
    private final String nodeTypeId;
    private boolean isDepleted;
    private long respawnAtMillis;

    public ActiveResourceNode(Location location, String nodeTypeId) {
        this.location = Objects.requireNonNull(location, "location cannot be null").clone(); // Clone to ensure immutability if original Location changes
        this.nodeTypeId = Objects.requireNonNull(nodeTypeId, "nodeTypeId cannot be null");
        this.isDepleted = false;
        this.respawnAtMillis = 0; // Not respawning initially
    }

    public Location getLocation() {
        // Return a clone if Location is mutable and shared, though Bukkit Locations are generally safe post-creation.
        // For safety in a concurrent environment or if Location objects are modified elsewhere, cloning is good.
        return location.clone();
    }

    // Provides direct access for map keys if needed, but use .clone() for external passing
    public Location getInternalLocation() {
        return location;
    }


    public String getNodeTypeId() {
        return nodeTypeId;
    }

    public boolean isDepleted() {
        return isDepleted;
    }

    public void setDepleted(boolean depleted) {
        isDepleted = depleted;
    }

    public long getRespawnAtMillis() {
        return respawnAtMillis;
    }

    public void setRespawnAtMillis(long respawnAtMillis) {
        this.respawnAtMillis = respawnAtMillis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActiveResourceNode that = (ActiveResourceNode) o;
        // Location's equals method handles world and coordinates.
        return location.equals(that.location);
    }

    @Override
    public int hashCode() {
        // Location's hashCode method handles world and coordinates.
        return Objects.hash(location);
    }

    @Override
    public String toString() {
        return "ActiveResourceNode{" +
               "location=" + location.getWorld().getName() + "(" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + ")" +
               ", nodeTypeId='" + nodeTypeId + '\'' +
               ", isDepleted=" + isDepleted +
               ", respawnAtMillis=" + respawnAtMillis +
               '}';
    }
}
