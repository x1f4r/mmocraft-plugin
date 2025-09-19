package com.x1f4r.mmocraft.core;

import com.x1f4r.mmocraft.combat.listeners.PlayerCombatListener;
import com.x1f4r.mmocraft.combat.service.BasicDamageCalculationService;
import com.x1f4r.mmocraft.combat.service.DamageCalculationService;
import com.x1f4r.mmocraft.combat.service.DefaultMobStatProvider;
import com.x1f4r.mmocraft.combat.service.MobStatProvider;
import com.x1f4r.mmocraft.command.BasicCommandRegistryService;
import com.x1f4r.mmocraft.command.CommandRegistryService;
import com.x1f4r.mmocraft.command.commands.CustomCraftCommand;
import com.x1f4r.mmocraft.command.commands.ExecuteSkillCommand;
import com.x1f4r.mmocraft.command.commands.MMOCraftInfoCommand;
import com.x1f4r.mmocraft.command.commands.admin.MMOCAdminRootCommand;
import com.x1f4r.mmocraft.config.BasicConfigService;
import com.x1f4r.mmocraft.config.ConfigService;
import com.x1f4r.mmocraft.crafting.service.BasicRecipeRegistryService;
import com.x1f4r.mmocraft.crafting.service.RecipeRegistryService;
import com.x1f4r.mmocraft.crafting.ui.CraftingUIManager;
import com.x1f4r.mmocraft.eventbus.BasicEventBusService;
import com.x1f4r.mmocraft.eventbus.EventBusService;
import com.x1f4r.mmocraft.eventbus.events.PluginReloadedEvent;
import com.x1f4r.mmocraft.diagnostics.PluginDiagnosticsService;
import com.x1f4r.mmocraft.item.equipment.listeners.PlayerEquipmentListener;
import com.x1f4r.mmocraft.item.equipment.service.PlayerEquipmentManager;
import com.x1f4r.mmocraft.item.service.BasicCustomItemRegistry;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.loot.listeners.MobDeathLootListener;
import com.x1f4r.mmocraft.loot.service.BasicLootService;
import com.x1f4r.mmocraft.loot.service.LootService;
import com.x1f4r.mmocraft.demo.DemoContentModule;
import com.x1f4r.mmocraft.demo.DemoContentSettings;
import com.x1f4r.mmocraft.persistence.PersistenceService;
import com.x1f4r.mmocraft.persistence.SqlitePersistenceService;
import com.x1f4r.mmocraft.playerdata.BasicPlayerDataService;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.listeners.PlayerJoinQuitListener;
import com.x1f4r.mmocraft.skill.service.BasicSkillRegistryService;
import com.x1f4r.mmocraft.skill.service.SkillRegistryService;
import com.x1f4r.mmocraft.statuseffect.manager.BasicStatusEffectManager;
import com.x1f4r.mmocraft.statuseffect.manager.StatusEffectManager;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.world.resourcegathering.persistence.ResourceNodeRepository;
import com.x1f4r.mmocraft.world.spawning.service.BasicCustomSpawningService;
import com.x1f4r.mmocraft.world.spawning.service.CustomSpawningService;
import com.x1f4r.mmocraft.world.zone.listeners.PlayerZoneTrackerListener;
import com.x1f4r.mmocraft.world.zone.service.BasicZoneManager;
import com.x1f4r.mmocraft.world.zone.service.ZoneManager;
import com.x1f4r.mmocraft.world.resourcegathering.listeners.ResourceNodeInteractionListener;
import com.x1f4r.mmocraft.world.resourcegathering.service.ActiveNodeManager;
import com.x1f4r.mmocraft.world.resourcegathering.service.BasicResourceNodeRegistryService;
import com.x1f4r.mmocraft.world.resourcegathering.service.ResourceNodeRegistryService;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Properties;

public final class MMOCraftPlugin extends JavaPlugin {

    private ConfigService configService;
    private EventBusService eventBusService;
    private PersistenceService persistenceService;
    private CommandRegistryService commandRegistryService;
    private PlayerDataService playerDataService;
    private DamageCalculationService damageCalculationService;
    private SkillRegistryService skillRegistryService;
    private StatusEffectManager statusEffectManager;
    private MobStatProvider mobStatProvider;
    private CustomItemRegistry customItemRegistry;
    private PlayerEquipmentManager playerEquipmentManager;
    private LootService lootService;
    private RecipeRegistryService recipeRegistryService;
    private CraftingUIManager craftingUIManager;
    private CustomSpawningService customSpawningService;
    private ZoneManager zoneManager;
    private ResourceNodeRegistryService resourceNodeRegistryService;
    private ResourceNodeRepository resourceNodeRepository;
    private ActiveNodeManager activeNodeManager;
    private LoggingUtil loggingUtil;
    private DemoContentSettings demoSettings = DemoContentSettings.disabled();
    private DemoContentModule demoContentModule;
    private BukkitTask statusEffectTickTask;
    private BukkitTask customSpawningTask;
    private BukkitTask resourceNodeTickTask;
    private PluginDiagnosticsService diagnosticsService;

    @Override
    public void onEnable() {
        if (!initLoggingAndConfig()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        DemoContentSettings configuredSettings = DemoContentSettings.fromConfig(configService, loggingUtil);
        demoSettings = applySetupPreferenceOverrides(configuredSettings);

        if (!initCoreServices()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        initGameplayServices();
        demoContentModule = new DemoContentModule(this, loggingUtil);
        applyDemoSettings(demoSettings);
        registerListeners();
        registerCommands();
        startSchedulers();

        loggingUtil.info("MMOCraft core loaded and all services initialized!");
        loggingUtil.info("Default Max Health from config: " + configService.getInt("stats.max-health"));
        loggingUtil.debug("onEnable sequence completed. Debug logging is " + (configService.getBoolean("core.debug-logging") ? "enabled" : "disabled") + ".");
    }

    @Override
    public void onDisable() {
        loggingUtil.info("MMOCraft shutting down...");

        if (demoContentModule != null) {
            demoContentModule.unload();
        }

        if (resourceNodeTickTask != null && !resourceNodeTickTask.isCancelled()) {
            resourceNodeTickTask.cancel();
            loggingUtil.info("ActiveNodeManager task scheduler cancelled.");
        }
        if (customSpawningTask != null && !customSpawningTask.isCancelled()) {
            customSpawningTask.cancel();
            loggingUtil.info("CustomSpawningService task scheduler cancelled.");
        }
        if (statusEffectTickTask != null && !statusEffectTickTask.isCancelled()) {
            statusEffectTickTask.cancel();
            loggingUtil.info("StatusEffect tick scheduler cancelled.");
        }

        if (playerDataService instanceof BasicPlayerDataService) {
            ((BasicPlayerDataService) playerDataService).shutdown();
        }
        if (statusEffectManager instanceof BasicStatusEffectManager) {
            ((BasicStatusEffectManager) statusEffectManager).shutdown();
        }
        if (customSpawningService instanceof BasicCustomSpawningService) {
            ((BasicCustomSpawningService) customSpawningService).shutdown();
        }
        if (activeNodeManager != null) {
            activeNodeManager.shutdown();
        }
        if (persistenceService != null) {
            try {
                persistenceService.close();
            } catch (SQLException e) {
                loggingUtil.severe("Error while closing database connection: " + e.getMessage(), e);
            }
        } else {
            loggingUtil.warning("PersistenceService was not initialized, nothing to close.");
        }
        loggingUtil.info("MMOCraft has been disabled.");
    }

    private boolean initLoggingAndConfig() {
        try {
            LoggingUtil preliminaryLogger = new LoggingUtil(this);
            configService = new BasicConfigService(this, preliminaryLogger);
            loggingUtil = new LoggingUtil(this, configService);
            preliminaryLogger.info("Preliminary logger transitioning to final logger.");
            loggingUtil.info("Final LoggingUtil initialized.");
            return true;
        } catch (Exception e) {
            getLogger().severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            getLogger().severe("FATAL: Could not initialize logging and configuration services.");
            getLogger().severe("The plugin cannot continue to load.");
            e.printStackTrace();
            getLogger().severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            return false;
        }
    }

    private boolean initCoreServices() {
        try {
            eventBusService = new BasicEventBusService(loggingUtil);
            commandRegistryService = new BasicCommandRegistryService(this);

            persistenceService = new SqlitePersistenceService(this);
            persistenceService.initDatabase();

            playerDataService = new BasicPlayerDataService(this, persistenceService, loggingUtil, eventBusService);
            playerDataService.initDatabaseSchema();

            loggingUtil.info("Core services (EventBus, Persistence, PlayerData) initialized.");
            return true;
        } catch (Exception e) {
            loggingUtil.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!", e);
            loggingUtil.severe("FATAL: Failed to initialize core services.", e);
            loggingUtil.severe("This is a critical error, and the plugin will be disabled.", e);
            loggingUtil.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!", e);
            return false;
        }
    }

    private void initGameplayServices() {
        customItemRegistry = new BasicCustomItemRegistry(this, loggingUtil);
        mobStatProvider = new DefaultMobStatProvider();
        damageCalculationService = new BasicDamageCalculationService(playerDataService, loggingUtil, mobStatProvider);
        playerEquipmentManager = new PlayerEquipmentManager(this, playerDataService, customItemRegistry, loggingUtil);
        skillRegistryService = new BasicSkillRegistryService(loggingUtil);
        statusEffectManager = new BasicStatusEffectManager(this, loggingUtil, playerDataService);
        lootService = new BasicLootService(this, loggingUtil);
        recipeRegistryService = new BasicRecipeRegistryService(this, loggingUtil, customItemRegistry);
        craftingUIManager = new CraftingUIManager(this, recipeRegistryService, playerDataService, customItemRegistry, loggingUtil);
        customSpawningService = new BasicCustomSpawningService(this, loggingUtil, mobStatProvider, lootService, customItemRegistry);
        zoneManager = new BasicZoneManager(this, loggingUtil, eventBusService, demoSettings.zonesEnabled());

        resourceNodeRegistryService = new BasicResourceNodeRegistryService(loggingUtil);
        resourceNodeRepository = new ResourceNodeRepository(persistenceService, loggingUtil);
        resourceNodeRepository.initDatabaseSchema();
        activeNodeManager = new ActiveNodeManager(this, loggingUtil, resourceNodeRegistryService, resourceNodeRepository, lootService, customItemRegistry);

        loggingUtil.info("All gameplay services initialized.");

        diagnosticsService = new PluginDiagnosticsService(
                loggingUtil,
                customItemRegistry,
                skillRegistryService,
                activeNodeManager,
                resourceNodeRegistryService,
                configService,
                persistenceService
        );
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinQuitListener(playerDataService, loggingUtil), this);
        getServer().getPluginManager().registerEvents(new PlayerZoneTrackerListener(zoneManager, loggingUtil, eventBusService), this);
        getServer().getPluginManager().registerEvents(new PlayerCombatListener(damageCalculationService, playerDataService, loggingUtil, mobStatProvider), this);
        getServer().getPluginManager().registerEvents(new PlayerEquipmentListener(this, playerEquipmentManager, loggingUtil), this);
        getServer().getPluginManager().registerEvents(new ResourceNodeInteractionListener(this, activeNodeManager, resourceNodeRegistryService, lootService, customItemRegistry, playerDataService, loggingUtil), this);
        getServer().getPluginManager().registerEvents(new MobDeathLootListener(lootService, customItemRegistry, this, loggingUtil), this);
        loggingUtil.info("Event listeners registered.");
    }

    private void registerCommands() {
        commandRegistryService.registerCommand("mmoc", new MMOCraftInfoCommand(this, "mmoc", "mmocraft.command.info", "Base command for MMOCraft.", loggingUtil));
        commandRegistryService.registerCommand("useskill", new ExecuteSkillCommand(this));
        commandRegistryService.registerCommand("mmocadm", new MMOCAdminRootCommand(this));
        commandRegistryService.registerCommand("customcraft", new CustomCraftCommand(this));
        loggingUtil.info("Commands registered.");
    }

    private void startSchedulers() {
        long statusEffectTickInterval = 20L;
        statusEffectTickTask = getServer().getScheduler().runTaskTimer(this, () -> {
            if (statusEffectManager != null) statusEffectManager.tickAllActiveEffects();
        }, statusEffectTickInterval, statusEffectTickInterval);
        loggingUtil.info("StatusEffect tick scheduler started.");

        long spawningInterval = 200L;
        customSpawningTask = getServer().getScheduler().runTaskTimer(this, () -> {
            if (customSpawningService != null) customSpawningService.attemptSpawns();
        }, spawningInterval, spawningInterval);
        loggingUtil.info("CustomSpawningService task scheduler started.");

        long resourceNodeTickInterval = 100L;
        resourceNodeTickTask = getServer().getScheduler().runTaskTimer(this, () -> {
            if (activeNodeManager != null) activeNodeManager.tickNodes();
        }, resourceNodeTickInterval, resourceNodeTickInterval);
        loggingUtil.info("ActiveNodeManager task scheduler started.");

        if (eventBusService != null) {
            eventBusService.register(PluginReloadedEvent.class, event -> {
                loggingUtil.info("PluginReloadedEvent handled: Configuration has been reloaded.");
            });
        } else {
            loggingUtil.warning("EventBusService was not initialized. Cannot register PluginReloadedEvent handler.");
        }
    }

    public ConfigService getConfigService() { return configService; }
    public EventBusService getEventBusService() { return eventBusService; }
    public PersistenceService getPersistenceService() { return persistenceService; }
    public CommandRegistryService getCommandRegistryService() { return commandRegistryService; }
    public PlayerDataService getPlayerDataService() { return playerDataService; }
    public DamageCalculationService getDamageCalculationService() { return damageCalculationService; }
    public SkillRegistryService getSkillRegistryService() { return skillRegistryService; }
    public StatusEffectManager getStatusEffectManager() { return statusEffectManager; }
    public MobStatProvider getMobStatProvider() { return mobStatProvider; }
    public CustomItemRegistry getCustomItemRegistry() { return customItemRegistry; }
    public PlayerEquipmentManager getPlayerEquipmentManager() { return playerEquipmentManager; }
    public LootService getLootService() { return lootService; }
    public RecipeRegistryService getRecipeRegistryService() { return recipeRegistryService; }
    public CraftingUIManager getCraftingUIManager() { return craftingUIManager; }
    public CustomSpawningService getCustomSpawningService() { return customSpawningService; }
    public ZoneManager getZoneManager() { return zoneManager; }
    public ResourceNodeRegistryService getResourceNodeRegistryService() { return resourceNodeRegistryService; }
    public ActiveNodeManager getActiveNodeManager() { return activeNodeManager; }
    public LoggingUtil getLoggingUtil() { return loggingUtil; }
    public DemoContentSettings getDemoSettings() { return demoSettings; }
    public PluginDiagnosticsService getDiagnosticsService() { return diagnosticsService; }

    private DemoContentSettings applySetupPreferenceOverrides(DemoContentSettings baseSettings) {
        if (baseSettings == null) {
            return DemoContentSettings.disabled();
        }

        File setupDir = new File(getDataFolder(), "setup");
        File overrideFile = new File(setupDir, "demo-preferences.properties");
        if (!overrideFile.exists()) {
            return baseSettings;
        }

        Properties properties = new Properties();
        try (FileReader reader = new FileReader(overrideFile)) {
            properties.load(reader);
        } catch (IOException ex) {
            loggingUtil.warning("Failed to read demo setup preferences: " + ex.getMessage());
            return baseSettings;
        }

        String rawValue = properties.getProperty("enable-demo");
        if (rawValue == null) {
            loggingUtil.warning("Demo setup preferences are missing 'enable-demo'. Ignoring override.");
            return baseSettings;
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("true") && !normalized.equals("false")) {
            loggingUtil.warning("Invalid value for 'enable-demo' in demo setup preferences: '" + rawValue + "'. Expected true or false.");
            return baseSettings;
        }

        boolean enableDemo = Boolean.parseBoolean(normalized);
        DemoContentSettings overridden = baseSettings.withAllFeatures(enableDemo);
        loggingUtil.info("Demo content " + (enableDemo ? "force-enabled" : "force-disabled") + " via setup preference file.");
        return overridden;
    }

    public synchronized void applyDemoSettings(DemoContentSettings newSettings) {
        if (newSettings == null) {
            loggingUtil.warning("Attempted to apply null demo settings. Ignoring request.");
            return;
        }
        demoSettings = newSettings;
        if (zoneManager instanceof BasicZoneManager basicZoneManager) {
            basicZoneManager.setCopyDefaultZoneFile(newSettings.zonesEnabled());
        }
        if (demoContentModule != null) {
            demoContentModule.applySettings(newSettings);
        }
        if (zoneManager != null) {
            zoneManager.loadZones();
        }
    }

    public void reloadPluginConfig() {
        if (configService != null) {
            configService.reloadConfig();
            DemoContentSettings reloadedSettings = DemoContentSettings.fromConfig(configService, loggingUtil);
            reloadedSettings = applySetupPreferenceOverrides(reloadedSettings);
            applyDemoSettings(reloadedSettings);
            loggingUtil.info("MMOCraft configuration reloaded via reloadPluginConfig().");
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
