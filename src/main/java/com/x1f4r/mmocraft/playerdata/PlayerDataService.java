package com.x1f4r.mmocraft.playerdata;

import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing player profile data, including loading,
 * saving, caching, and providing default configurations.
 */
public interface PlayerDataService {

    /**
     * Retrieves a player's profile.
     * Primarily checks the cache for online players. If not found, it might
     * attempt to load from DB if the design allows for fetching offline players' data,
     * or return null/empty Optional if the player is not loaded.
     *
     * @param playerUUID The UUID of the player.
     * @return The {@link PlayerProfile} if found and loaded, otherwise null or an empty Optional.
     */
    PlayerProfile getPlayerProfile(UUID playerUUID);

    /**
     * Loads a player's profile from the database. If the player does not exist,
     * a new profile is created with default values. This method is typically
     * called when a player joins the server.
     * <p>
     * This operation can be I/O intensive and might be performed asynchronously.
     *
     * @param playerUUID The UUID of the player.
     * @param playerName The current name of the player (used if creating a new profile).
     * @return A CompletableFuture that will complete with the loaded or newly created {@link PlayerProfile}.
     */
    CompletableFuture<PlayerProfile> loadPlayerProfile(UUID playerUUID, String playerName);

    /**
     * Saves a player's profile to the database.
     * This operation is typically called when a player quits or periodically.
     * <p>
     * This operation can be I/O intensive and might be performed asynchronously.
     *
     * @param playerUUID The UUID of the player whose profile needs to be saved.
     * @return A CompletableFuture that completes when the save operation is finished.
     */
    CompletableFuture<Void> savePlayerProfile(UUID playerUUID);

    /**
     * Adds a player's profile to the in-memory cache.
     *
     * @param profile The {@link PlayerProfile} to cache.
     */
    void cachePlayerProfile(PlayerProfile profile);

    /**
     * Removes a player's profile from the in-memory cache.
     *
     * @param playerUUID The UUID of the player whose profile to remove from cache.
     * @return The removed {@link PlayerProfile}, or null if it was not cached.
     */
    PlayerProfile uncachePlayerProfile(UUID playerUUID);

    /**
     * Provides a map of default core statistics for a new player.
     *
     * @return A map where keys are {@link Stat} enums and values are their default double values.
     */
    Map<Stat, Double> getDefaultStats();

    /**
     * Initializes the database schema required by the PlayerDataService.
     * This should be called once during plugin startup.
     */
    void initDatabaseSchema();

    /**
     * Adds experience to a player's profile and handles leveling up.
     *
     * @param playerUUID The UUID of the player to grant experience to.
     * @param amount The amount of experience to add. Must be positive.
     */
    void addExperience(UUID playerUUID, long amount);
}
