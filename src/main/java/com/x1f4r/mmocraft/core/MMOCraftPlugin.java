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
import com.x1f4r.mmocraft.item.equipment.listeners.PlayerEquipmentListener;
import com.x1f4r.mmocraft.item.equipment.service.PlayerEquipmentManager;
import com.x1f4r.mmocraft.item.impl.SimpleSword;
import com.x1f4r.mmocraft.item.impl.TrainingArmor;
import com.x1f4r.mmocraft.item.service.BasicCustomItemRegistry;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.loot.listeners.MobDeathLootListener;
import com.x1f4r.mmocraft.loot.model.LootTable;
import com.x1f4r.mmocraft.loot.model.LootTableEntry;
import com.x1f4r.mmocraft.loot.model.LootType;
import com.x1f4r.mmocraft.loot.service.BasicLootService;
import com.x1f4r.mmocraft.loot.service.LootService;
import com.x1f4r.mmocraft.persistence.PersistenceService;
import com.x1f4r.mmocraft.persistence.SqlitePersistenceService;
import com.x1f4r.mmocraft.playerdata.BasicPlayerDataService;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.listeners.PlayerJoinQuitListener;
import com.x1f4r.mmocraft.skill.impl.MinorHealSkill;
import com.x1f4r.mmocraft.skill.impl.StrongStrikeSkill;
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
import com.x1f4r.mmocraft.world.resourcegathering.model.ResourceNodeType;
import com.x1f4r.mmocraft.world.resourcegathering.service.ActiveNodeManager;
import com.x1f4r.mmocraft.world.resourcegathering.service.BasicResourceNodeRegistryService;
import com.x1f4r.mmocraft.world.resourcegathering.service.ResourceNodeRegistryService;
import com.x1f4r.mmocraft.world.spawning.conditions.BiomeCondition;
import com.x1f4r.mmocraft.world.spawning.conditions.SpawnCondition;
import com.x1f4r.mmocraft.world.spawning.conditions.TimeCondition;
import com.x1f4r.mmocraft.world.spawning.model.CustomSpawnRule;
import com.x1f4r.mmocraft.world.spawning.model.MobSpawnDefinition;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private BukkitTask statusEffectTickTask;
    private BukkitTask customSpawningTask;
    private BukkitTask resourceNodeTickTask;

    @Override
    public void onEnable() {
        if (!initLoggingAndConfig()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!initCoreServices()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        initGameplayServices();
        registerCustomContent();
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
        zoneManager = new BasicZoneManager(this, loggingUtil, eventBusService);

        resourceNodeRegistryService = new BasicResourceNodeRegistryService(loggingUtil);
        resourceNodeRepository = new ResourceNodeRepository(persistenceService, loggingUtil);
        resourceNodeRepository.initDatabaseSchema();
        activeNodeManager = new ActiveNodeManager(this, loggingUtil, resourceNodeRegistryService, resourceNodeRepository, lootService, customItemRegistry);

        loggingUtil.info("All gameplay services initialized.");
    }

    private void registerCustomContent() {
        registerCustomItems();
        loggingUtil.info("Custom items registered.");

        registerSkills();
        loggingUtil.info("Skills registered.");

        registerDefaultLootTables();

        registerCustomSpawns();

        zoneManager.loadZones();

        registerResourceNodeTypes();
        placeInitialResourceNodes();
        loggingUtil.info("Resource nodes registered and placed.");
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

    private void registerResourceNodeTypes() {
        if (resourceNodeRegistryService == null || lootService == null) {
            loggingUtil.severe("ResourceNodeRegistryService or LootService not initialized. Cannot register node types.");
            return;
        }

        if (lootService.getLootTableById("stone_node_loot").isEmpty()) {
            LootTable stoneLoot = new LootTable("stone_node_loot", List.of(new LootTableEntry(LootType.VANILLA, "COBBLESTONE", 1.0, 1, 1)));
            lootService.registerLootTableById(stoneLoot);
            loggingUtil.info("Registered 'stone_node_loot' table.");
        }

        if (lootService.getLootTableById("iron_ore_node_loot").isEmpty()) {
            LootTable ironLoot = new LootTable("iron_ore_node_loot", List.of(new LootTableEntry(LootType.VANILLA, "RAW_IRON", 1.0, 1, 1)));
            lootService.registerLootTableById(ironLoot);
            loggingUtil.info("Registered 'iron_ore_node_loot' table.");
        }

        ResourceNodeType stoneNode = new ResourceNodeType("stone_node", Material.STONE, 5.0, Set.of(Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE), "stone_node_loot", 60, "&7Stone Deposit");
        resourceNodeRegistryService.registerNodeType(stoneNode);

        ResourceNodeType ironOreNode = new ResourceNodeType("iron_ore_node", Material.IRON_ORE, 10.0, Set.of(Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE), "iron_ore_node_loot", 180, "&fIron Vein");
        resourceNodeRegistryService.registerNodeType(ironOreNode);

        loggingUtil.info("Registered default resource node types.");
    }

    private void placeInitialResourceNodes() {
        if (activeNodeManager == null) {
            loggingUtil.severe("ActiveNodeManager not initialized. Cannot place initial resource nodes.");
            return;
        }
        if (!activeNodeManager.getAllActiveNodesView().isEmpty()) {
            loggingUtil.info("Skipping initial resource node placement as nodes were loaded from the database.");
            return;
        }

        World defaultWorld = Bukkit.getWorld("world");
        if (defaultWorld == null && !Bukkit.getWorlds().isEmpty()) {
            defaultWorld = Bukkit.getWorlds().get(0);
        }
        if (defaultWorld == null) {
            loggingUtil.severe("No worlds loaded. Cannot place initial resource nodes.");
            return;
        }

        activeNodeManager.placeNewNode(new Location(defaultWorld, 10, 60, 10), "stone_node");
        activeNodeManager.placeNewNode(new Location(defaultWorld, 12, 60, 10), "stone_node");
        activeNodeManager.placeNewNode(new Location(defaultWorld, 10, 60, 12), "iron_ore_node");
        loggingUtil.info("Placed initial resource nodes in world '" + defaultWorld.getName() + "'.");
    }

    private void registerSkills() {
        if (skillRegistryService == null) {
            loggingUtil.severe("SkillRegistryService not initialized. Cannot register skills.");
            return;
        }
        skillRegistryService.registerSkill(new StrongStrikeSkill(this));
        skillRegistryService.registerSkill(new MinorHealSkill(this));
    }

    private void registerCustomItems() {
        if (customItemRegistry == null) {
            loggingUtil.severe("CustomItemRegistry not initialized. Cannot register custom items.");
            return;
        }
        customItemRegistry.registerItem(new SimpleSword(this));
        customItemRegistry.registerItem(new TrainingArmor(this));
    }

    private void registerDefaultLootTables() {
        if (lootService == null || customItemRegistry == null) {
            loggingUtil.severe("LootService or CustomItemRegistry not initialized. Cannot register default loot tables.");
            return;
        }
        LootTable zombieLootTable = new LootTable("zombie_common_drops", List.of(new LootTableEntry("simple_sword", 0.05, 1, 1)));
        lootService.registerLootTable(EntityType.ZOMBIE, zombieLootTable);
        loggingUtil.info("Default loot tables registered.");
    }

    private void registerCustomSpawns() {
        if (customSpawningService == null) {
            loggingUtil.severe("CustomSpawningService not initialized. Cannot register custom spawns.");
            return;
        }

        MobSpawnDefinition skeletalWarriorDef = new MobSpawnDefinition("skeletal_warrior", EntityType.SKELETON, ChatColor.RED + "Skeletal Warrior", "skeletal_warrior_stats", "warrior_common_loot", Map.of(EquipmentSlot.HAND, new ItemStack(Material.IRON_SWORD)));
        List<SpawnCondition> conditions = List.of(new BiomeCondition(Set.of(Biome.PLAINS, Biome.MEADOW, Biome.SAVANNA)), new TimeCondition(13000, 23000));
        CustomSpawnRule warriorRule = new CustomSpawnRule("plains_skeletal_warriors_night", skeletalWarriorDef, conditions, 0.1, 60, 200, 5, 16.0, 200);
        customSpawningService.registerRule(warriorRule);
    }

    public void reloadPluginConfig() {
        if (configService != null) {
            configService.reloadConfig();
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
