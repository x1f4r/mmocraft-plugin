package com.x1f4r.mmocraft.playerdata.events;

import com.x1f4r.mmocraft.eventbus.CustomEvent;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile; // Optional, for snapshot

import java.util.UUID;

/**
 * Event dispatched when a player levels up.
 */
public class PlayerLevelUpEvent extends CustomEvent {

    private final UUID playerUUID;
    private final int oldLevel;
    private final int newLevel;
    private final PlayerProfile profileSnapshot; // Optional: A snapshot of the profile at the time of level up

    /**
     * Constructs a new PlayerLevelUpEvent.
     *
     * @param playerUUID The UUID of the player who leveled up.
     * @param oldLevel The player's level before leveling up.
     * @param newLevel The player's new level.
     * @param profileSnapshot A snapshot of the player's profile at the moment of level up (can be null).
     */
    public PlayerLevelUpEvent(UUID playerUUID, int oldLevel, int newLevel, PlayerProfile profileSnapshot) {
        super();
        this.playerUUID = playerUUID;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.profileSnapshot = profileSnapshot; // Be mindful of mutability if passing direct reference
    }

    /**
     * Gets the UUID of the player who leveled up.
     * @return The player's UUID.
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    /**
     * Gets the player's level before this level-up event.
     * @return The old level.
     */
    public int getOldLevel() {
        return oldLevel;
    }

    /**
     * Gets the player's new level achieved.
     * @return The new level.
     */
    public int getNewLevel() {
        return newLevel;
    }

    /**
     * Gets a snapshot of the player's profile at the time of leveling up.
     * This can be useful for listeners that need to react based on the state
     * of the player when they leveled up. This may be null.
     * If not null, it's recommended this be a defensive copy or an immutable view
     * if the original PlayerProfile object is mutable and might change further.
     * For simplicity here, we assume it's used carefully.
     *
     * @return A {@link PlayerProfile} snapshot, or null.
     */
    public PlayerProfile getProfileSnapshot() {
        return profileSnapshot;
    }

    @Override
    public String toString() {
        return "PlayerLevelUpEvent{" +
               "eventName='" + getEventName() + '\'' +
               ", playerUUID=" + playerUUID +
               ", oldLevel=" + oldLevel +
               ", newLevel=" + newLevel +
               // Avoid printing full profile snapshot here for brevity unless needed for debugging
               (profileSnapshot != null ? ", profileSnapshotExists=true" : ", profileSnapshotExists=false") +
               '}';
    }
}
