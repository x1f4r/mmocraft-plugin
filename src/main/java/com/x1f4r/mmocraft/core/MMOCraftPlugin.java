package com.x1f4r.mmocraft.core;

import com.x1f4r.mmocraft.config.BasicConfigService;
import com.x1f4r.mmocraft.config.ConfigService;
import com.x1f4r.mmocraft.eventbus.BasicEventBusService;
import com.x1f4r.mmocraft.eventbus.EventBusService;
import com.x1f4r.mmocraft.command.BasicCommandRegistryService;
import com.x1f4r.mmocraft.command.CommandRegistryService;
import com.x1f4r.mmocraft.command.commands.MMOCraftInfoCommand;
import com.x1f4r.mmocraft.eventbus.events.PluginReloadedEvent;
import com.x1f4r.mmocraft.persistence.PersistenceService;
import com.x1f4r.mmocraft.persistence.SqlitePersistenceService;
import com.x1f4r.mmocraft.command.BasicCommandRegistryService;
import com.x1f4r.mmocraft.command.CommandRegistryService;
import com.x1f4r.mmocraft.command.commands.MMOCraftInfoCommand;
import com.x1f4r.mmocraft.config.BasicConfigService;
import com.x1f4r.mmocraft.config.ConfigService;
import com.x1f4r.mmocraft.eventbus.BasicEventBusService;
import com.x1f4r.mmocraft.eventbus.EventBusService;
import com.x1f4r.mmocraft.eventbus.events.PluginReloadedEvent;
import com.x1f4r.mmocraft.persistence.PersistenceService;
import com.x1f4r.mmocraft.persistence.SqlitePersistenceService;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
// import java.util.logging.Level; // No longer needed due to LoggingUtil

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
    private LoggingUtil loggingUtil;

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

        // Register commands - MMOCraftInfoCommand constructor was updated to take LoggingUtil
        commandRegistryService.registerCommand("mmoc", new MMOCraftInfoCommand(this, "mmoc", "mmocraft.command.info", "Base command for MMOCraft.", loggingUtil));

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
     * Gets the primary logging utility for the plugin.
     * Used for standardized console logging.
     * @return The {@link LoggingUtil} instance.
     */
    public LoggingUtil getLoggingUtil() {
        return loggingUtil;
    }

    // --- Plugin Actions ---

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
