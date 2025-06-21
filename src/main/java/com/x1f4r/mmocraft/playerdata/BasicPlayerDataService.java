package com.x1f4r.mmocraft.playerdata;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.eventbus.EventBusService;
import com.x1f4r.mmocraft.persistence.PersistenceService;
import com.x1f4r.mmocraft.playerdata.events.PlayerLevelUpEvent;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.playerdata.util.ExperienceUtil;
import com.x1f4r.mmocraft.util.JsonUtil;
import com.x1f4r.mmocraft.util.LoggingUtil;

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

    private final MMOCraftPlugin plugin;
    private final PersistenceService persistenceService;
    private final LoggingUtil logger;
    private final EventBusService eventBusService;

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
                    profile.setPlayerName(playerName);
                    profile.setLastLogin(LocalDateTime.now());
                    logger.info("Loaded profile for player: " + playerName + " (UUID: " + playerUUID + ")");
                } else {
                    logger.info("No existing profile found for " + playerName + ". Creating new profile.");
                    profile = new PlayerProfile(playerUUID, playerName);
                    saveProfileData(profile, true);
                }
                cachePlayerProfile(profile);
                return profile;
            } catch (SQLException e) {
                logger.severe("Failed to load player profile for UUID: " + playerUUID, e);
                PlayerProfile tempProfile = new PlayerProfile(playerUUID, playerName);
                tempProfile.setCoreStats(getDefaultStats());
                cachePlayerProfile(tempProfile);
                logger.warning("Created temporary profile for " + playerName + " due to DB error. Data will not persist correctly until DB is fixed.");
                return tempProfile;
            }
        }, databaseExecutor);
    }

    private void saveProfileData(PlayerProfile profile, boolean isNewProfile) throws SQLException {
        String coreStatsJson = JsonUtil.statsMapToJson(profile.getCoreStats());
        String sql;
        if (isNewProfile) {
            // PlayerProfile constructor initializes firstLogin and lastLogin,
            // so profile.getFirstLogin() should not be null here.
            // If it were, it should be profile.setFirstLogin(LocalDateTime.now());
            // For now, assuming constructor handles it.
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
            // If isNewProfile is true, this means the INSERT failed, which is a problem.
            // If isNewProfile is false, this means the UPDATE failed (no row found for UUID), also a problem.
            String operationType = isNewProfile ? "insert new" : "update existing";
            logger.severe("Failed to " + operationType + " profile for " + profile.getPlayerName() +
                          " (no rows affected, UUID: " + profile.getPlayerUUID() + "). " +
                          "This may indicate a data consistency issue or a problem with the database operation.");
            // Consider if any fallback or specific error handling is needed beyond logging.
            // For now, throwing the original SQLException or a new specific one might be appropriate if the caller should handle it.
            // However, this method is called from async tasks, so throwing might just get logged by CompletableFuture.
            // For critical save operations, a more robust alerting or recovery mechanism might be needed in a production system.
        }
    }

    @Override
    public CompletableFuture<Void> savePlayerProfile(UUID playerUUID) {
        return CompletableFuture.runAsync(() -> {
            PlayerProfile profile = getPlayerProfile(playerUUID);
            if (profile == null) {
                logger.warning("Attempted to save profile for UUID " + playerUUID + ", but it was not found in cache.");
                return;
            }
            profile.setLastLogin(LocalDateTime.now());
            try {
                saveProfileData(profile, false);
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
        for (Stat stat : Stat.values()) {
            defaultStats.put(stat, 10.0);
        }
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
            profile.setExperience(0);
            return;
        }

        profile.setExperience(profile.getExperience() + amount);
        logger.fine("Added " + amount + " XP to " + profile.getPlayerName() + ". Current XP: " + profile.getExperience());

        boolean leveledUp = false;
        while (profile.getExperience() >= profile.getExperienceToNextLevel() && profile.getLevel() < ExperienceUtil.getMaxLevel()) {
            long xpForOldLevel = profile.getExperienceToNextLevel();
            profile.setExperience(profile.getExperience() - xpForOldLevel);

            int oldLevel = profile.getLevel();
            profile.setLevel(oldLevel + 1);
            leveledUp = true;

            logger.info(profile.getPlayerName() + " leveled up to level " + profile.getLevel() + "!");

            PlayerProfile snapshot = new PlayerProfile(
                profile.getPlayerUUID(), profile.getPlayerName(), profile.getCurrentHealth(), profile.getMaxHealth(),
                profile.getCurrentMana(), profile.getMaxMana(), profile.getLevel(),
                profile.getExperience(), profile.getCurrency(), profile.getCoreStats(),
                profile.getFirstLogin(), profile.getLastLogin()
            );

            eventBusService.call(new PlayerLevelUpEvent(profile.getPlayerUUID(), oldLevel, profile.getLevel(), snapshot));

            if (profile.getLevel() >= ExperienceUtil.getMaxLevel()) {
                logger.info(profile.getPlayerName() + " reached MAX LEVEL (" + ExperienceUtil.getMaxLevel() + ")!");
                profile.setExperience(0);
                break;
            }
        }
        if (profile.getExperience() < 0) {
            profile.setExperience(0);
        }

        if (leveledUp) {
            logger.fine(profile.getPlayerName() + " final state after leveling: Level " + profile.getLevel() + ", XP " + profile.getExperience());
        }
    }

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
