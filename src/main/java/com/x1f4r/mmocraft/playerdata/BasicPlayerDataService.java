package com.x1f4r.mmocraft.playerdata;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.eventbus.EventBusService;
import com.x1f4r.mmocraft.persistence.PersistenceService;
import com.x1f4r.mmocraft.eventbus.EventBusService;
import com.x1f4r.mmocraft.persistence.PersistenceService;
import com.x1f4r.mmocraft.playerdata.events.PlayerLevelUpEvent; // Added
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.playerdata.util.ExperienceUtil; // Added
import com.x1f4r.mmocraft.util.JsonUtil;
import com.x1f4r.mmocraft.util.LoggingUtil;

// import java.sql.ResultSet; // No longer directly used in this class after refactor
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BasicPlayerDataService implements PlayerDataService {

    private final MMOCraftPlugin plugin; // May not be strictly needed if all dependencies are passed
    private final PersistenceService persistenceService;
    private final LoggingUtil logger;
    private final EventBusService eventBusService; // For future events, e.g. PlayerProfileLoadedEvent

    private final Map<UUID, PlayerProfile> onlinePlayerProfiles = new ConcurrentHashMap<>();
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MMOCraft-PlayerData-DB");
        t.setDaemon(true);
        return t;
    });

    private static final String TABLE_NAME = "player_profiles";

    public BasicPlayerDataService(MMOCraftPlugin plugin, PersistenceService persistenceService,
                                  LoggingUtil logger, EventBusService eventBusService) {
        this.plugin = plugin;
        this.persistenceService = persistenceService;
        this.logger = logger;
        this.eventBusService = eventBusService;
        logger.debug("BasicPlayerDataService initialized.");
    }

    @Override
    public void initDatabaseSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                     "player_uuid TEXT PRIMARY KEY NOT NULL," +
                     "player_name TEXT NOT NULL," +
                     "current_health BIGINT DEFAULT 100 NOT NULL," +
                     "max_health BIGINT DEFAULT 100 NOT NULL," +
                     "current_mana BIGINT DEFAULT 50 NOT NULL," +
                     "max_mana BIGINT DEFAULT 50 NOT NULL," +
                     "level INTEGER DEFAULT 1 NOT NULL," +
                     "experience BIGINT DEFAULT 0 NOT NULL," +
                     "currency BIGINT DEFAULT 0 NOT NULL," +
                     "core_stats TEXT," + // JSON
                     "first_login TEXT NOT NULL," +
                     "last_login TEXT NOT NULL" +
                     ");";
        try {
            persistenceService.executeUpdate(sql);
            logger.info("'" + TABLE_NAME + "' table schema initialized successfully.");
        } catch (SQLException e) {
            logger.severe("Failed to initialize '" + TABLE_NAME + "' table schema.", e);
            // Consider disabling plugin or part of its functionality if DB schema fails
        }
    }


    @Override
    public PlayerProfile getPlayerProfile(UUID playerUUID) {
        return onlinePlayerProfiles.get(playerUUID);
    }

    @Override
    public CompletableFuture<PlayerProfile> loadPlayerProfile(UUID playerUUID, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Attempting to load profile for UUID: " + playerUUID + ", Name: " + playerName);
            if (onlinePlayerProfiles.containsKey(playerUUID)) {
                logger.debug("Profile for " + playerUUID + " already in cache.");
                return onlinePlayerProfiles.get(playerUUID);
            }

            String sql = "SELECT * FROM " + TABLE_NAME + " WHERE player_uuid = ?;";
            try {
                PlayerProfile profile = persistenceService.executeQuerySingle(sql, rs -> {
                    String name = rs.getString("player_name");
                    long currentHealth = rs.getLong("current_health");
                    long maxHealth = rs.getLong("max_health");
                    long currentMana = rs.getLong("current_mana");
                    long maxMana = rs.getLong("max_mana");
                    int level = rs.getInt("level");
                    long experience = rs.getLong("experience");
                    long currency = rs.getLong("currency");
                    String coreStatsJson = rs.getString("core_stats");
                    Map<Stat, Double> coreStats = JsonUtil.jsonToStatsMap(coreStatsJson);
                    LocalDateTime firstLogin = LocalDateTime.parse(rs.getString("first_login"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    LocalDateTime lastLogin = LocalDateTime.parse(rs.getString("last_login"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                    logger.fine("Deserialized profile for " + name + " (UUID: " + playerUUID + ")");
                    return new PlayerProfile(playerUUID, name, currentHealth, maxHealth, currentMana, maxMana,
                                             level, experience, currency, coreStats, firstLogin, lastLogin);
                }, playerUUID.toString()).orElse(null);

                if (profile != null) {
                    profile.setPlayerName(playerName); // Update name in case it changed with current Bukkit name
                    profile.setLastLogin(LocalDateTime.now());
                    // Recalculate attributes based on loaded stats, in case formulas changed or it wasn't done by constructor
                    // The PlayerProfile full constructor already calls recalculateDerivedAttributes.
                    // If loading from DB, it uses the full constructor.
                    // profile.recalculateDerivedAttributes(); // Ensure this is called if not by constructor
                    logger.info("Loaded profile for player: " + playerName + " (UUID: " + playerUUID + ")");
                } else {
                    logger.info("No existing profile found for " + playerName + ". Creating new profile.");
                    profile = new PlayerProfile(playerUUID, playerName); // This constructor calls recalculateDerivedAttributes
                    // Default stats are set by PlayerProfile constructor. If specific overrides are needed:
                    // profile.setCoreStats(getDefaultStats()); // This would also trigger recalculate
                    saveProfileData(profile, true); // Save the newly created profile
                }
                // Ensure derived attributes are up-to-date after any potential modifications or if just loaded
                // The constructors of PlayerProfile are now responsible for the initial call.
                // If there's a scenario where stats might be altered after construction but before caching,
                // then an explicit call here would be a safeguard.
                // For now, PlayerProfile constructors handle it.
                // profile.recalculateDerivedAttributes(); // This call might be redundant if constructors do it.

                cachePlayerProfile(profile);
                // TODO: Fire PlayerProfileLoadedEvent via eventBusService: eventBusService.call(new PlayerProfileLoadedEvent(profile));
                return profile;
            } catch (SQLException e) {
                logger.severe("Failed to load player profile for UUID: " + playerUUID, e);
                // Fallback: create a temporary default profile to allow player to join, but don't save it unless explicitly handled
                PlayerProfile tempProfile = new PlayerProfile(playerUUID, playerName);
                tempProfile.setCoreStats(getDefaultStats());
                cachePlayerProfile(tempProfile); // Cache temporary profile
                logger.warning("Created temporary profile for " + playerName + " due to DB error. Data will not persist correctly until DB is fixed.");
                return tempProfile;
            }
        }, databaseExecutor);
    }

    private void saveProfileData(PlayerProfile profile, boolean isNewProfile) throws SQLException {
        String coreStatsJson = JsonUtil.statsMapToJson(profile.getCoreStats());
        String sql;
        if (isNewProfile) {
            // Ensure firstLogin is set before this save if it's a truly new profile
            if (profile.getFirstLogin() == null) { // Should be set by PlayerProfile constructor
                profile.setLastLogin(LocalDateTime.now()); // Sets both first and last if first is null via constructor logic
            }
             sql = "INSERT INTO " + TABLE_NAME + " (player_uuid, player_name, current_health, max_health, " +
                      "current_mana, max_mana, level, experience, currency, core_stats, first_login, last_login) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        } else {
            sql = "UPDATE " + TABLE_NAME + " SET player_name = ?, current_health = ?, max_health = ?, " +
                  "current_mana = ?, max_mana = ?, level = ?, experience = ?, currency = ?, core_stats = ?, last_login = ? " +
                  "WHERE player_uuid = ?;";
        }

        int affectedRows;
        if (isNewProfile) {
            affectedRows = persistenceService.executeUpdate(sql,
                profile.getPlayerUUID().toString(), profile.getPlayerName(), profile.getCurrentHealth(), profile.getMaxHealth(),
                profile.getCurrentMana(), profile.getMaxMana(), profile.getLevel(), profile.getExperience(),
                profile.getCurrency(), coreStatsJson,
                profile.getFirstLogin().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                profile.getLastLogin().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
        } else {
             affectedRows = persistenceService.executeUpdate(sql,
                profile.getPlayerName(), profile.getCurrentHealth(), profile.getMaxHealth(),
                profile.getCurrentMana(), profile.getMaxMana(), profile.getLevel(), profile.getExperience(),
                profile.getCurrency(), coreStatsJson,
                profile.getLastLogin().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                profile.getPlayerUUID().toString()
            );
        }

        if (affectedRows > 0) {
            logger.fine("Successfully saved profile for " + profile.getPlayerName() + (isNewProfile ? " (new)" : " (update)"));
        } else {
            logger.warning("Failed to save profile for " + profile.getPlayerName() + " (no rows affected, UUID: " + profile.getPlayerUUID() + ")");
            // This might happen if an UPDATE is issued for a UUID not in DB, which should be an INSERT.
            // Consider using INSERT OR REPLACE or a more robust UPSERT for some DBs.
            // For SQLite, INSERT OR REPLACE is:
            String upsertSql = "INSERT OR REPLACE INTO " + TABLE_NAME + " (player_uuid, player_name, current_health, max_health, " +
                  "current_mana, max_mana, level, experience, currency, core_stats, first_login, last_login) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
             persistenceService.executeUpdate(upsertSql,
                profile.getPlayerUUID().toString(), profile.getPlayerName(), profile.getCurrentHealth(), profile.getMaxHealth(),
                profile.getCurrentMana(), profile.getMaxMana(), profile.getLevel(), profile.getExperience(),
                profile.getCurrency(), coreStatsJson,
                profile.getFirstLogin().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                profile.getLastLogin().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            logger.info("Attempted UPSERT for " + profile.getPlayerName() + " after failed initial save.");

        }
    }


    @Override
    public CompletableFuture<Void> savePlayerProfile(UUID playerUUID) {
        return CompletableFuture.runAsync(() -> {
            PlayerProfile profile = getPlayerProfile(playerUUID); // Get from cache
            if (profile == null) {
                logger.warning("Attempted to save profile for UUID " + playerUUID + ", but it was not found in cache.");
                return;
            }
            profile.setLastLogin(LocalDateTime.now()); // Update last login time on save
            try {
                saveProfileData(profile, false); // false for existing profile (update)
            } catch (SQLException e) {
                logger.severe("Failed to save player profile for UUID: " + playerUUID, e);
            }
        }, databaseExecutor);
    }

    @Override
    public void cachePlayerProfile(PlayerProfile profile) {
        if (profile != null) {
            onlinePlayerProfiles.put(profile.getPlayerUUID(), profile);
            logger.fine("Cached profile for player: " + profile.getPlayerName());
        }
    }

    @Override
    public PlayerProfile uncachePlayerProfile(UUID playerUUID) {
        PlayerProfile removedProfile = onlinePlayerProfiles.remove(playerUUID);
        if (removedProfile != null) {
            logger.fine("Uncached profile for player UUID: " + playerUUID + ", Name: " + removedProfile.getPlayerName());
        } else {
            logger.warning("Attempted to uncache profile for UUID " + playerUUID + ", but it was not found in cache.");
        }
        return removedProfile;
    }

    @Override
    public Map<Stat, Double> getDefaultStats() {
        Map<Stat, Double> defaultStats = new EnumMap<>(Stat.class);
        // These should match the defaults in PlayerProfile constructor or be configurable via ConfigService
        for (Stat stat : Stat.values()) {
            defaultStats.put(stat, 10.0); // Default base value for core stats
        }
        // Example overrides for specific stats, matching PlayerProfile's minimal constructor
        defaultStats.put(Stat.VITALITY, 12.0);
        defaultStats.put(Stat.WISDOM, 11.0);
        return defaultStats;
    }

    @Override
    public void addExperience(UUID playerUUID, long amount) {
        if (amount <= 0) {
            logger.fine("Attempted to add non-positive XP ("+ amount +") to " + playerUUID + ". Ignoring.");
            return;
        }

        PlayerProfile profile = getPlayerProfile(playerUUID);
        if (profile == null) {
            logger.warning("Cannot add experience: PlayerProfile not found in cache for UUID " + playerUUID);
            return;
        }

        if (profile.getLevel() >= ExperienceUtil.getMaxLevel()) {
            logger.fine("Player " + profile.getPlayerName() + " is at max level. No XP gained.");
            profile.setExperience(0); // Clear any overflow from before reaching max level
            return;
        }

        profile.setExperience(profile.getExperience() + amount);
        logger.fine("Added " + amount + " XP to " + profile.getPlayerName() + ". Current XP: " + profile.getExperience());

        boolean leveledUp = false;
        while (profile.getExperience() >= profile.getExperienceToNextLevel() && profile.getLevel() < ExperienceUtil.getMaxLevel()) {
            long xpForOldLevel = profile.getExperienceToNextLevel(); // XP needed for the level just completed
            profile.setExperience(profile.getExperience() - xpForOldLevel);

            int oldLevel = profile.getLevel();
            profile.setLevel(oldLevel + 1); // setLevel calls recalculateDerivedAttributes
            leveledUp = true;

            logger.info(profile.getPlayerName() + " leveled up to level " + profile.getLevel() + "!");

            // Create a snapshot for the event. For simplicity, this is the current profile state.
            // A true snapshot might involve deep copying if PlayerProfile is highly mutable
            // or if listeners might modify the profile passed in an event.
            PlayerProfile snapshot = new PlayerProfile(
                profile.getPlayerUUID(), profile.getPlayerName(), profile.getCurrentHealth(), profile.getMaxHealth(),
                profile.getCurrentMana(), profile.getMaxMana(), profile.getLevel(), // Use new level in snapshot
                profile.getExperience(), profile.getCurrency(), profile.getCoreStats(), // Pass copy of stats
                profile.getFirstLogin(), profile.getLastLogin()
            );

            eventBusService.call(new PlayerLevelUpEvent(profile.getPlayerUUID(), oldLevel, profile.getLevel(), snapshot));

            if (profile.getLevel() >= ExperienceUtil.getMaxLevel()) {
                logger.info(profile.getPlayerName() + " reached MAX LEVEL (" + ExperienceUtil.getMaxLevel() + ")!");
                profile.setExperience(0); // Set XP to 0 at max level
                break;
            }
        }
        // Ensure experience doesn't become negative if somehow 'getExperienceToNextLevel' was greater than current XP
        // though the loop condition should prevent this.
        if (profile.getExperience() < 0) {
            profile.setExperience(0);
        }

        if (leveledUp) {
            logger.fine(profile.getPlayerName() + " final state after leveling: Level " + profile.getLevel() + ", XP " + profile.getExperience());
            // Consider saving player profile after level up if desired immediately
            // savePlayerProfile(playerUUID); // This would make it async
        }
    }

    // Call this method on plugin disable to ensure all tasks are completed.
    public void shutdown() {
        logger.info("Shutting down PlayerDataService database executor...");
        databaseExecutor.shutdown();
        try {
            if (!databaseExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                logger.warning("Database executor did not terminate in time, forcing shutdown.");
                databaseExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.severe("Interrupted while waiting for database executor to terminate.", e);
            databaseExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("PlayerDataService shutdown complete.");
    }
}
