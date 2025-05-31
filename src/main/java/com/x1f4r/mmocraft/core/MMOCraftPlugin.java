package com.x1f4r.mmocraft.core;

import com.x1f4r.mmocraft.combat.listeners.PlayerCombatListener; // Added
import com.x1f4r.mmocraft.combat.service.BasicDamageCalculationService; // Added
import com.x1f4r.mmocraft.combat.service.DamageCalculationService; // Added
import com.x1f4r.mmocraft.command.BasicCommandRegistryService;
import com.x1f4r.mmocraft.command.CommandRegistryService;
import com.x1f4r.mmocraft.command.commands.ExecuteSkillCommand; // Added
import com.x1f4r.mmocraft.command.commands.MMOCraftInfoCommand;
import com.x1f4r.mmocraft.command.commands.admin.PlayerDataAdminCommand;
import com.x1f4r.mmocraft.config.BasicConfigService;
import com.x1f4r.mmocraft.config.ConfigService;
import com.x1f4r.mmocraft.eventbus.BasicEventBusService;
import com.x1f4r.mmocraft.eventbus.EventBusService;
import com.x1f4r.mmocraft.eventbus.events.PluginReloadedEvent;
import com.x1f4r.mmocraft.persistence.PersistenceService;
import com.x1f4r.mmocraft.persistence.SqlitePersistenceService;
import com.x1f4r.mmocraft.playerdata.BasicPlayerDataService;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.listeners.PlayerJoinQuitListener;
import com.x1f4r.mmocraft.skill.impl.MinorHealSkill; // Added
import com.x1f4r.mmocraft.skill.impl.StrongStrikeSkill;
import com.x1f4r.mmocraft.skill.service.BasicSkillRegistryService;
import com.x1f4r.mmocraft.skill.service.SkillRegistryService;
import com.x1f4r.mmocraft.statuseffect.manager.BasicStatusEffectManager; // Added
import com.x1f4r.mmocraft.statuseffect.manager.StatusEffectManager; // Added
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask; // Added

import java.sql.SQLException;

/**
 * Main class for the MMOCraft plugin.
 * Handles the lifecycle of the plugin, including enabling and disabling,
 * and initializes and holds references to all core services.
 *
 * @author x1f4r
 * @version 0.1.0-SNAPSHOT
 */
public final class MMOCraftPlugin extends JavaPlugin {

    private ConfigService configService;
    private EventBusService eventBusService;
    private PersistenceService persistenceService;
    private CommandRegistryService commandRegistryService;
    private PlayerDataService playerDataService;
    private DamageCalculationService damageCalculationService;
    private SkillRegistryService skillRegistryService;
    private StatusEffectManager statusEffectManager; // Added
    private LoggingUtil loggingUtil;
    private BukkitTask statusEffectTickTask; // Added

    /**
     * Called when the plugin is first enabled.
     * This method initializes all core services in the correct order:
     * <ol>
     *     <li>{@link ConfigService} (with a preliminary logger)</li>
     *     <li>{@link LoggingUtil} (final version, using ConfigService)</li>
     *     <li>{@link EventBusService}</li>
     *     <li>{@link CommandRegistryService} (and registers commands)</li>
     *     <li>{@link PersistenceService} (and initializes database schema)</li>
     * </ol>
     * It also registers event handlers and logs basic plugin information.
     */
    @Override
    public void onEnable() {
        // Initialize ConfigService first
        // BasicConfigService needs JavaPlugin for file operations. LoggingUtil is now a dependency.
        // To provide LoggingUtil to ConfigService, LoggingUtil must be created first.
        // This creates a small dilemma: LoggingUtil wants ConfigService for debug flag, ConfigService wants LoggingUtil for logging.
        // Solution: 1. Create a preliminary logger. 2. Create ConfigService with it. 3. Create final logger with ConfigService.

        LoggingUtil preliminaryLogger = new LoggingUtil(this); // No config access yet
        configService = new BasicConfigService(this, preliminaryLogger); // Pass plugin and preliminary logger

        // Initialize LoggingUtil (final) now that ConfigService is available
        loggingUtil = new LoggingUtil(this, configService);
        preliminaryLogger.info("Preliminary logger transitioning to final logger."); // Optional: Log transition
        loggingUtil.info("Final LoggingUtil initialized.");


        // Initialize EventBusService
        eventBusService = new BasicEventBusService(loggingUtil); // Pass LoggingUtil

        // Initialize CommandRegistryService
        // BasicCommandRegistryService constructor takes JavaPlugin and internally gets LoggingUtil from it.
        commandRegistryService = new BasicCommandRegistryService(this);

        // Register commands
        commandRegistryService.registerCommand("mmoc", new MMOCraftInfoCommand(this, "mmoc", "mmocraft.command.info", "Base command for MMOCraft.", loggingUtil));
        commandRegistryService.registerCommand("playerdata", new PlayerDataAdminCommand(this));
        commandRegistryService.registerCommand("useskill", new ExecuteSkillCommand(this)); // Added /useskill command


        // Initialize PersistenceService
        try {
            // SqlitePersistenceService constructor takes JavaPlugin and internally gets LoggingUtil from it.
            persistenceService = new SqlitePersistenceService(this);
            persistenceService.initDatabase(); // This also includes logging its status
        } catch (SQLException e) {
            // Log with final logger if available, otherwise preliminary
            LoggingUtil loggerToUse = (loggingUtil != null) ? loggingUtil : preliminaryLogger;
            loggerToUse.severe("Failed to initialize PersistenceService or database. Plugin may not function correctly.", e);
            // Consider disabling the plugin if persistence is critical
            // getServer().getPluginManager().disablePlugin(this);
            // return;
        } catch (RuntimeException e) { // Catch RuntimeException from driver loading (e.g., ClassNotFound for JDBC)
            LoggingUtil loggerToUse = (loggingUtil != null) ? loggingUtil : preliminaryLogger;
            loggerToUse.severe("Failed to load database driver. Critical error, plugin may not function.", e);
            // Consider disabling the plugin
            // getServer().getPluginManager().disablePlugin(this);
            // return;
        }

        // Initialize PlayerDataService (depends on Persistence, Logging, EventBus)
        playerDataService = new BasicPlayerDataService(this, persistenceService, loggingUtil, eventBusService);
        playerDataService.initDatabaseSchema(); // Create tables if they don't exist

        // Register PlayerData listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinQuitListener(playerDataService, loggingUtil), this);
        loggingUtil.info("PlayerDataService and its listeners initialized.");

        // Initialize DamageCalculationService (depends on PlayerDataService, LoggingUtil)
        damageCalculationService = new BasicDamageCalculationService(playerDataService, loggingUtil);
        loggingUtil.info("DamageCalculationService initialized.");

        // Register Combat listeners
        getServer().getPluginManager().registerEvents(new PlayerCombatListener(damageCalculationService, playerDataService, loggingUtil), this);
        loggingUtil.info("Combat listeners registered.");

        // Initialize SkillRegistryService
        skillRegistryService = new BasicSkillRegistryService(loggingUtil);
        registerSkills(); // Register implemented skills
        loggingUtil.info("SkillRegistryService initialized and skills registered.");

        // Initialize StatusEffectManager
        statusEffectManager = new BasicStatusEffectManager(this, loggingUtil, playerDataService);
        loggingUtil.info("StatusEffectManager initialized.");

        // Start StatusEffect tick scheduler
        long tickInterval = 20L; // 20 ticks = 1 second (Bukkit ticks)
        statusEffectTickTask = getServer().getScheduler().runTaskTimer(this, () -> {
            if (statusEffectManager != null) {
                statusEffectManager.tickAllActiveEffects();
            }
        }, tickInterval, tickInterval);
        loggingUtil.info("StatusEffect tick scheduler started (every " + tickInterval + " server ticks).");


        // Register a handler for PluginReloadedEvent
        if (eventBusService != null) { // Defensive check, should be initialized
            eventBusService.register(PluginReloadedEvent.class, event -> {
                loggingUtil.info("PluginReloadedEvent handled: Configuration has been reloaded. Event name: " + event.getEventName());
            });
        } else {
            // This case should ideally not be reached if initialization order is correct.
            loggingUtil.warning("EventBusService was not initialized. Cannot register PluginReloadedEvent handler.");
        }

        loggingUtil.info("MMOCraft core loaded and all services initialized!");
        loggingUtil.info("Default Max Health from config: " + configService.getInt("stats.max-health"));
        loggingUtil.debug("onEnable sequence completed. Debug logging is " + (configService.getBoolean("core.debug-logging") ? "enabled" : "disabled") + ".");
    }

    /**
     * Called when the plugin is disabled.
     * Performs cleanup of services, such as closing database connections.
     * Logs the shutdown sequence.
     */
    @Override
    public void onDisable() {
        loggingUtil.info("MMOCraft shutting down...");

        // Close persistence service connection
        if (persistenceService != null) {
            try {
                persistenceService.close(); // Service itself should log success/failure
            } catch (SQLException e) {
                // This catch is a fallback; service should ideally handle its own logging for close errors.
                loggingUtil.severe("Error while closing database connection: " + e.getMessage(), e);
            }
        } else {
            loggingUtil.warning("PersistenceService was not initialized, nothing to close.");
        }

        // Add any other service cleanup here in the future (e.g., eventBusService.shutdown())
        if (playerDataService instanceof BasicPlayerDataService) {
            ((BasicPlayerDataService) playerDataService).shutdown();
        }
        if (statusEffectManager instanceof BasicStatusEffectManager) { // Added manager shutdown
            ((BasicStatusEffectManager) statusEffectManager).shutdown();
        }
        if (statusEffectTickTask != null && !statusEffectTickTask.isCancelled()) { // Added task cancellation
            statusEffectTickTask.cancel();
            loggingUtil.info("StatusEffect tick scheduler cancelled.");
        }

        loggingUtil.info("MMOCraft has been disabled.");
    }

    // --- Service Getters ---

    /**
     * Gets the active configuration service.
     * Used to access plugin configuration values.
     * @return The {@link ConfigService} instance.
     */
    public ConfigService getConfigService() {
        return configService;
    }

    /**
     * Gets the active event bus service.
     * Used to register custom event handlers and to publish custom events.
     * @return The {@link EventBusService} instance.
     */
    public EventBusService getEventBusService() {
        return eventBusService;
    }

    /**
     * Gets the active persistence service.
     * Used for database interactions.
     * @return The {@link PersistenceService} instance, or null if it failed to initialize.
     */
    public PersistenceService getPersistenceService() {
        return persistenceService;
    }

    /**
     * Gets the active command registry service.
     * Used to register plugin commands.
     * @return The {@link CommandRegistryService} instance.
     */
    public CommandRegistryService getCommandRegistryService() {
        return commandRegistryService;
    }

    /**
     * Gets the active player data service.
     * Used for managing player profiles and game-specific data.
     * @return The {@link PlayerDataService} instance.
     */
    public PlayerDataService getPlayerDataService() {
        return playerDataService;
    }

    /**
     * Gets the active damage calculation service.
     * Used for determining combat outcomes.
     * @return The {@link DamageCalculationService} instance.
     */
    public DamageCalculationService getDamageCalculationService() { // Added
        return damageCalculationService;
    }

    /**
     * Gets the active skill registry service.
     * Used to manage and access available skills.
     * @return The {@link SkillRegistryService} instance.
     */
    public SkillRegistryService getSkillRegistryService() { // Added
        return skillRegistryService;
    }

    /**
     * Gets the active status effect manager.
     * Used for applying, removing, and ticking status effects on entities.
     * @return The {@link StatusEffectManager} instance.
     */
    public StatusEffectManager getStatusEffectManager() { // Added
        return statusEffectManager;
    }

    /**
     * Gets the primary logging utility for the plugin.
     * Used for standardized console logging.
     * @return The {@link LoggingUtil} instance.
     */
    public LoggingUtil getLoggingUtil() {
        return loggingUtil;
    }

    // --- Plugin Actions ---

    private void registerSkills() {
        if (skillRegistryService == null) {
            loggingUtil.severe("SkillRegistryService not initialized. Cannot register skills.");
            return;
        }
        skillRegistryService.registerSkill(new StrongStrikeSkill(this));
        skillRegistryService.registerSkill(new MinorHealSkill(this));
        // Add more skills here as they are implemented
    }

    /**
     * Reloads the plugin's configuration.
     * This involves calling the reload method on the {@link ConfigService}
     * and then firing a {@link PluginReloadedEvent} on the event bus.
     */
    public void reloadPluginConfig() {
        if (configService != null) {
            configService.reloadConfig(); // ConfigService should log its own status
            loggingUtil.info("MMOCraft configuration reloaded via reloadPluginConfig().");
            // Example of re-checking a config value after reload
            loggingUtil.info("Default Max Health after reload: " + configService.getInt("stats.max-health"));
            loggingUtil.debug("Debug status after reload: " + (configService.getBoolean("core.debug-logging") ? "enabled" : "disabled") + ".");

            if (eventBusService != null) {
                eventBusService.call(new PluginReloadedEvent());
            } else {
                loggingUtil.warning("EventBusService not available, cannot fire PluginReloadedEvent.");
            }
        } else {
            loggingUtil.severe("ConfigService not available. Cannot reload configuration.");
        }
    }
}
