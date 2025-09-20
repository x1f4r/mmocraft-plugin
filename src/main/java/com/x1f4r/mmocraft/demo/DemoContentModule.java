package com.x1f4r.mmocraft.demo;

import com.x1f4r.mmocraft.config.gameplay.DemoContentConfig;
import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.demo.item.SimpleSword;
import com.x1f4r.mmocraft.demo.item.TrainingArmor;
import com.x1f4r.mmocraft.demo.skill.MinorHealSkill;
import com.x1f4r.mmocraft.demo.skill.StrongStrikeSkill;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.loot.model.LootTable;
import com.x1f4r.mmocraft.loot.model.LootTableEntry;
import com.x1f4r.mmocraft.loot.service.LootService;
import com.x1f4r.mmocraft.skill.model.Skill;
import com.x1f4r.mmocraft.skill.service.SkillRegistryService;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.util.StringUtil;
import com.x1f4r.mmocraft.world.resourcegathering.model.ResourceNodeType;
import com.x1f4r.mmocraft.world.resourcegathering.service.ActiveNodeManager;
import com.x1f4r.mmocraft.world.resourcegathering.service.ResourceNodeRegistryService;
import com.x1f4r.mmocraft.world.spawning.conditions.BiomeCondition;
import com.x1f4r.mmocraft.world.spawning.conditions.SpawnCondition;
import com.x1f4r.mmocraft.world.spawning.conditions.TimeCondition;
import com.x1f4r.mmocraft.world.spawning.model.CustomSpawnRule;
import com.x1f4r.mmocraft.world.spawning.model.MobSpawnDefinition;
import com.x1f4r.mmocraft.world.spawning.service.CustomSpawningService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Coordinates registration of demo content so that it can be toggled independently of the engine core.
 */
public final class DemoContentModule {

    private final MMOCraftPlugin plugin;
    private final LoggingUtil logger;
    private DemoContentConfig demoConfig;
    private DemoContentSettings currentSettings = DemoContentSettings.disabled();
    private final Set<String> registeredItemIds = new HashSet<>();
    private final Set<String> registeredSkillIds = new HashSet<>();
    private final Set<String> registeredResourceNodeTypeIds = new HashSet<>();
    private final Set<String> registeredGenericLootTableIds = new HashSet<>();
    private final Map<EntityType, String> registeredMobLootTableIds = new EnumMap<>(EntityType.class);
    private final Set<String> registeredSpawnRuleIds = new HashSet<>();

    public DemoContentModule(MMOCraftPlugin plugin, LoggingUtil logger, DemoContentConfig demoConfig) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.demoConfig = demoConfig != null ? demoConfig : DemoContentConfig.defaults();
    }

    public synchronized void updateConfig(DemoContentConfig newConfig) {
        this.demoConfig = newConfig != null ? newConfig : DemoContentConfig.defaults();
        if (currentSettings.masterEnabled()) {
            DemoContentSettings snapshot = currentSettings;
            unloadDemoContent();
            currentSettings = DemoContentSettings.disabled();
            applySettings(snapshot);
        }
    }

    public synchronized void applySettings(DemoContentSettings newSettings) {
        Objects.requireNonNull(newSettings, "newSettings");
        if (currentSettings.masterEnabled()) {
            unloadDemoContent();
        }
        currentSettings = newSettings;
        if (!newSettings.masterEnabled()) {
            logger.debug("DemoContentModule disabled via settings.");
            return;
        }
        logger.info("Applying demo content: " + newSettings.describeEnabledFeatures());
        if (newSettings.itemsEnabled()) {
            executeSafely("register demo items", this::registerDemoItems);
        }
        if (newSettings.skillsEnabled()) {
            executeSafely("register demo skills", this::registerDemoSkills);
        }
        if (newSettings.lootTablesEnabled()) {
            executeSafely("register demo loot tables", this::registerDemoLootTables);
        }
        if (newSettings.customSpawnsEnabled()) {
            executeSafely("register demo custom spawns", this::registerDemoCustomSpawns);
        }
        if (newSettings.resourceNodesEnabled()) {
            executeSafely("register demo resource nodes", () -> {
                registerDemoResourceNodeTypes();
                placeInitialResourceNodes();
            });
        }
    }

    public synchronized void unload() {
        if (currentSettings.masterEnabled()) {
            unloadDemoContent();
        }
        currentSettings = DemoContentSettings.disabled();
    }

    private void executeSafely(String description, Runnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            logger.severe("Failed to " + description + ": " + ex.getMessage(), ex);
        }
    }

    private void registerDemoItems() {
        CustomItemRegistry registry = plugin.getCustomItemRegistry();
        if (registry == null) {
            logger.severe("CustomItemRegistry not initialized. Demo items cannot be registered.");
            return;
        }
        registerItemIfAbsent(registry, new SimpleSword(plugin));
        registerItemIfAbsent(registry, new TrainingArmor(plugin));
    }

    private void registerItemIfAbsent(CustomItemRegistry registry, CustomItem item) {
        if (registry.getCustomItem(item.getItemId()).isPresent()) {
            logger.debug("Custom item '" + item.getItemId() + "' already exists. Skipping demo registration.");
            return;
        }
        registry.registerItem(item);
        registeredItemIds.add(item.getItemId());
    }

    private void registerDemoSkills() {
        SkillRegistryService registry = plugin.getSkillRegistryService();
        if (registry == null) {
            logger.severe("SkillRegistryService not initialized. Demo skills cannot be registered.");
            return;
        }
        registerSkillIfAbsent(registry, new StrongStrikeSkill(plugin));
        registerSkillIfAbsent(registry, new MinorHealSkill(plugin));
    }

    private void registerSkillIfAbsent(SkillRegistryService registry, Skill skill) {
        if (registry.getSkill(skill.getSkillId()).isPresent()) {
            logger.debug("Skill '" + skill.getSkillId() + "' already exists. Skipping demo registration.");
            return;
        }
        registry.registerSkill(skill);
        registeredSkillIds.add(skill.getSkillId());
    }

    private void registerDemoLootTables() {
        LootService lootService = plugin.getLootService();
        if (lootService == null) {
            logger.severe("LootService not initialized. Demo loot tables cannot be registered.");
            return;
        }

        for (DemoContentConfig.MobLootTableDefinition mobTable : demoConfig.getMobLootTables()) {
            EntityType entityType = mobTable.entityType();
            if (lootService.getLootTable(entityType).isPresent()) {
                logger.debug("Loot table for '" + entityType.name() + "' already exists. Skipping demo registration.");
                continue;
            }
            lootService.registerLootTable(entityType, toLootTable(mobTable.table()));
            registeredMobLootTableIds.put(entityType, mobTable.table().tableId());
        }

        for (DemoContentConfig.LootTableDefinition tableDefinition : demoConfig.getGenericLootTables()) {
            registerGenericLootTableIfAbsent(lootService, tableDefinition);
        }
    }

    private void registerGenericLootTableIfAbsent(LootService lootService, DemoContentConfig.LootTableDefinition definition) {
        String lootTableId = definition.tableId();
        if (lootService.getLootTableById(lootTableId).isPresent()) {
            logger.debug("Loot table '" + lootTableId + "' already exists. Skipping demo registration.");
            return;
        }
        lootService.registerLootTableById(toLootTable(definition));
        registeredGenericLootTableIds.add(lootTableId);
    }

    private LootTable toLootTable(DemoContentConfig.LootTableDefinition definition) {
        List<LootTableEntry> entries = new ArrayList<>();
        for (DemoContentConfig.LootEntryDefinition entry : definition.entries()) {
            entries.add(new LootTableEntry(entry.type(), entry.identifier(), entry.chance(), entry.minAmount(), entry.maxAmount()));
        }
        return new LootTable(definition.tableId(), entries);
    }

    private void registerDemoCustomSpawns() {
        CustomSpawningService spawningService = plugin.getCustomSpawningService();
        if (spawningService == null) {
            logger.severe("CustomSpawningService not initialized. Demo custom spawns cannot be registered.");
            return;
        }
        for (DemoContentConfig.CustomSpawnRuleConfig spawnConfig : demoConfig.getCustomSpawnRules()) {
            boolean alreadyRegistered = spawningService.getAllRules().stream()
                    .anyMatch(rule -> rule.getRuleId().equals(spawnConfig.ruleId()));
            if (alreadyRegistered) {
                logger.debug("Demo spawn rule '" + spawnConfig.ruleId() + "' already registered. Skipping.");
                continue;
            }
            MobSpawnDefinition definition = buildMobSpawnDefinition(spawnConfig);
            List<SpawnCondition> conditions = buildSpawnConditions(spawnConfig);
            CustomSpawnRule rule = new CustomSpawnRule(
                    spawnConfig.ruleId(),
                    definition,
                    conditions,
                    spawnConfig.spawnChance(),
                    spawnConfig.minY(),
                    spawnConfig.maxY(),
                    spawnConfig.maxNearby(),
                    spawnConfig.radius(),
                    spawnConfig.intervalTicks()
            );
            spawningService.registerRule(rule);
            registeredSpawnRuleIds.add(rule.getRuleId());
        }
    }

    private MobSpawnDefinition buildMobSpawnDefinition(DemoContentConfig.CustomSpawnRuleConfig spawnConfig) {
        Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
        spawnConfig.equipment().forEach((slot, material) -> equipment.put(slot, new ItemStack(material)));
        String displayName = spawnConfig.displayName() != null ? StringUtil.colorize(spawnConfig.displayName()) : null;
        return new MobSpawnDefinition(spawnConfig.mobId(), spawnConfig.entityType(), displayName,
                spawnConfig.statProfileId(), spawnConfig.lootTableId(), equipment);
    }

    private List<SpawnCondition> buildSpawnConditions(DemoContentConfig.CustomSpawnRuleConfig spawnConfig) {
        List<SpawnCondition> conditions = new ArrayList<>();
        if (!spawnConfig.biomes().isEmpty()) {
            conditions.add(new BiomeCondition(spawnConfig.biomes()));
        }
        DemoContentConfig.TimeWindow window = spawnConfig.timeWindow();
        if (window != null) {
            conditions.add(new TimeCondition(window.start(), window.end()));
        }
        return conditions;
    }

    private void registerDemoResourceNodeTypes() {
        ResourceNodeRegistryService registryService = plugin.getResourceNodeRegistryService();
        LootService lootService = plugin.getLootService();
        if (registryService == null || lootService == null) {
            logger.severe("Resource node services unavailable. Demo nodes cannot be registered.");
            return;
        }

        for (DemoContentConfig.ResourceNodeTypeConfig typeConfig : demoConfig.getResourceNodeTypes()) {
            registerResourceNodeTypeIfAbsent(registryService, lootService, typeConfig);
        }
    }

    private void registerResourceNodeTypeIfAbsent(ResourceNodeRegistryService registryService, LootService lootService,
                                                  DemoContentConfig.ResourceNodeTypeConfig typeConfig) {
        String typeId = typeConfig.typeId();
        if (registryService.getNodeType(typeId).isPresent()) {
            logger.debug("Resource node type '" + typeId + "' already registered. Skipping demo registration.");
            return;
        }
        if (lootService.getLootTableById(typeConfig.lootTableId()).isEmpty()) {
            logger.warning("Required loot table '" + typeConfig.lootTableId() + "' missing for node type '" + typeId + "'.");
            return;
        }
        ResourceNodeType nodeType = new ResourceNodeType(typeConfig.typeId(), typeConfig.blockMaterial(),
                typeConfig.harvestSeconds(), typeConfig.requiredTools(), typeConfig.lootTableId(),
                typeConfig.respawnSeconds(), typeConfig.displayName());
        registryService.registerNodeType(nodeType);
        registeredResourceNodeTypeIds.add(typeId);
    }

    private void placeInitialResourceNodes() {
        ActiveNodeManager nodeManager = plugin.getActiveNodeManager();
        if (nodeManager == null) {
            logger.severe("ActiveNodeManager not initialized. Demo nodes cannot be placed.");
            return;
        }
        Optional<World> defaultWorld = resolveDefaultWorld();
        int placed = 0;
        Set<String> populatedWorlds = new LinkedHashSet<>();
        for (DemoContentConfig.ResourceNodePlacementConfig placement : demoConfig.getResourceNodePlacements()) {
            World world = resolvePlacementWorld(placement, defaultWorld);
            if (world == null) {
                continue;
            }
            Location location = new Location(world, placement.x(), placement.y(), placement.z());
            if (nodeManager.getActiveNode(location).isPresent()) {
                logger.debug("Resource node already present at " + location + ". Skipping placement.");
                continue;
            }
            if (!registeredResourceNodeTypeIds.contains(placement.nodeTypeId()) &&
                    nodeManager.countNodesOfType(placement.nodeTypeId()) > 0) {
                logger.debug("Resource node type '" + placement.nodeTypeId() + "' exists but not registered via demo. Skipping placement to avoid interfering with custom content.");
                continue;
            }
            nodeManager.placeNewNode(location, placement.nodeTypeId());
            placed++;
            if (world.getName() != null) {
                populatedWorlds.add(world.getName());
            }
        }
        if (placed > 0) {
            if (populatedWorlds.isEmpty()) {
                logger.info("Placed " + placed + " demo resource nodes.");
            } else if (populatedWorlds.size() == 1) {
                logger.info("Placed " + placed + " demo resource nodes in world '" + populatedWorlds.iterator().next() + "'.");
            } else {
                logger.info("Placed " + placed + " demo resource nodes across worlds: " + String.join(", ", populatedWorlds) + ".");
            }
        }
    }

    private World resolvePlacementWorld(DemoContentConfig.ResourceNodePlacementConfig placement, Optional<World> defaultWorld) {
        if (placement.world() != null && !placement.world().isBlank()) {
            World world = Bukkit.getWorld(placement.world());
            if (world == null) {
                logger.warning("World '" + placement.world() + "' not found for demo resource node placement.");
            }
            return world;
        }
        if (defaultWorld.isEmpty()) {
            logger.warning("No default world available to place demo resource node '" + placement.nodeTypeId() + "'.");
            return null;
        }
        return defaultWorld.get();
    }

    private Optional<World> resolveDefaultWorld() {
        World defaultWorld = Bukkit.getWorld("world");
        if (defaultWorld != null) {
            return Optional.of(defaultWorld);
        }
        List<World> worlds = Bukkit.getWorlds();
        return worlds.isEmpty() ? Optional.empty() : Optional.of(worlds.get(0));
    }

    private void unloadDemoContent() {
        unregisterResourceNodes();
        unregisterCustomSpawns();
        unregisterLootTables();
        unregisterSkills();
        unregisterItems();
    }

    private void unregisterItems() {
        CustomItemRegistry registry = plugin.getCustomItemRegistry();
        if (registry == null) {
            registeredItemIds.clear();
            return;
        }
        for (String itemId : new HashSet<>(registeredItemIds)) {
            if (registry.unregisterItem(itemId)) {
                logger.info("Unregistered demo item: " + itemId);
            }
        }
        registeredItemIds.clear();
    }

    private void unregisterSkills() {
        SkillRegistryService registry = plugin.getSkillRegistryService();
        if (registry == null) {
            registeredSkillIds.clear();
            return;
        }
        for (String skillId : new HashSet<>(registeredSkillIds)) {
            if (registry.unregisterSkill(skillId)) {
                logger.info("Unregistered demo skill: " + skillId);
            }
        }
        registeredSkillIds.clear();
    }

    private void unregisterLootTables() {
        LootService lootService = plugin.getLootService();
        if (lootService == null) {
            registeredGenericLootTableIds.clear();
            registeredMobLootTableIds.clear();
            return;
        }
        for (String lootTableId : new HashSet<>(registeredGenericLootTableIds)) {
            if (lootService.unregisterLootTableById(lootTableId)) {
                logger.info("Unregistered demo loot table: " + lootTableId);
            }
        }
        registeredGenericLootTableIds.clear();
        for (Map.Entry<EntityType, String> entry : new EnumMap<>(registeredMobLootTableIds).entrySet()) {
            if (lootService.unregisterLootTable(entry.getKey())) {
                logger.info("Unregistered demo loot table for mob: " + entry.getKey());
            }
        }
        registeredMobLootTableIds.clear();
    }

    private void unregisterCustomSpawns() {
        CustomSpawningService spawningService = plugin.getCustomSpawningService();
        if (spawningService == null) {
            registeredSpawnRuleIds.clear();
            return;
        }
        for (String ruleId : new HashSet<>(registeredSpawnRuleIds)) {
            if (spawningService.unregisterRule(ruleId)) {
                logger.info("Unregistered demo spawn rule: " + ruleId);
            }
        }
        registeredSpawnRuleIds.clear();
    }

    private void unregisterResourceNodes() {
        ResourceNodeRegistryService registryService = plugin.getResourceNodeRegistryService();
        ActiveNodeManager nodeManager = plugin.getActiveNodeManager();
        if (registryService == null || nodeManager == null) {
            registeredResourceNodeTypeIds.clear();
            return;
        }
        for (String typeId : new HashSet<>(registeredResourceNodeTypeIds)) {
            int removed = nodeManager.removeNodesByType(typeId);
            if (removed > 0) {
                logger.info("Removed " + removed + " active nodes of type '" + typeId + "'.");
            }
            if (registryService.unregisterNodeType(typeId)) {
                logger.info("Unregistered demo resource node type: " + typeId);
            }
        }
        registeredResourceNodeTypeIds.clear();
    }
}
