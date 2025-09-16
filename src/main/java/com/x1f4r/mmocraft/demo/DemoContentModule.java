package com.x1f4r.mmocraft.demo;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.demo.item.SimpleSword;
import com.x1f4r.mmocraft.demo.item.TrainingArmor;
import com.x1f4r.mmocraft.demo.skill.MinorHealSkill;
import com.x1f4r.mmocraft.demo.skill.StrongStrikeSkill;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.loot.model.LootTable;
import com.x1f4r.mmocraft.loot.model.LootTableEntry;
import com.x1f4r.mmocraft.loot.model.LootType;
import com.x1f4r.mmocraft.loot.service.LootService;
import com.x1f4r.mmocraft.skill.model.Skill;
import com.x1f4r.mmocraft.skill.service.SkillRegistryService;
import com.x1f4r.mmocraft.util.LoggingUtil;
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
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Coordinates registration of demo content so that it can be toggled independently of the engine core.
 */
public final class DemoContentModule {

    private static final String ZOMBIE_LOOT_TABLE_ID = "zombie_common_drops";
    private static final String WARRIOR_LOOT_TABLE_ID = "warrior_common_loot";
    private static final String STONE_NODE_TYPE_ID = "stone_node";
    private static final String IRON_NODE_TYPE_ID = "iron_ore_node";
    private static final String STONE_NODE_LOOT_ID = "stone_node_loot";
    private static final String IRON_NODE_LOOT_ID = "iron_ore_node_loot";
    private static final String SKELETAL_WARRIOR_RULE_ID = "plains_skeletal_warriors_night";

    private static final List<ResourceNodePlacement> DEFAULT_NODE_PLACEMENTS = List.of(
            new ResourceNodePlacement(STONE_NODE_TYPE_ID, 10, 60, 10),
            new ResourceNodePlacement(STONE_NODE_TYPE_ID, 12, 60, 10),
            new ResourceNodePlacement(IRON_NODE_TYPE_ID, 10, 60, 12)
    );

    private final MMOCraftPlugin plugin;
    private final LoggingUtil logger;
    private DemoContentSettings currentSettings = DemoContentSettings.disabled();
    private final Set<String> registeredItemIds = new HashSet<>();
    private final Set<String> registeredSkillIds = new HashSet<>();
    private final Set<String> registeredResourceNodeTypeIds = new HashSet<>();
    private final Set<String> registeredGenericLootTableIds = new HashSet<>();
    private final Map<EntityType, String> registeredMobLootTableIds = new EnumMap<>(EntityType.class);
    private final Set<String> registeredSpawnRuleIds = new HashSet<>();

    public DemoContentModule(MMOCraftPlugin plugin, LoggingUtil logger) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");
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

        if (lootService.getLootTable(EntityType.ZOMBIE).isEmpty()) {
            LootTable zombieLootTable = new LootTable(ZOMBIE_LOOT_TABLE_ID,
                    List.of(new LootTableEntry("simple_sword", 0.05, 1, 1)));
            lootService.registerLootTable(EntityType.ZOMBIE, zombieLootTable);
            registeredMobLootTableIds.put(EntityType.ZOMBIE, ZOMBIE_LOOT_TABLE_ID);
        } else {
            logger.debug("Zombie loot table already present. Demo loot will not override existing configuration.");
        }

        registerGenericLootTableIfAbsent(lootService, STONE_NODE_LOOT_ID,
                List.of(new LootTableEntry(LootType.VANILLA, "COBBLESTONE", 1.0, 1, 1)));
        registerGenericLootTableIfAbsent(lootService, IRON_NODE_LOOT_ID,
                List.of(new LootTableEntry(LootType.VANILLA, "RAW_IRON", 1.0, 1, 1)));
        registerGenericLootTableIfAbsent(lootService, WARRIOR_LOOT_TABLE_ID,
                List.of(new LootTableEntry("simple_sword", 0.25, 1, 1)));
    }

    private void registerGenericLootTableIfAbsent(LootService lootService, String lootTableId, List<LootTableEntry> entries) {
        if (lootService.getLootTableById(lootTableId).isPresent()) {
            logger.debug("Loot table '" + lootTableId + "' already exists. Skipping demo registration.");
            return;
        }
        lootService.registerLootTableById(new LootTable(lootTableId, entries));
        registeredGenericLootTableIds.add(lootTableId);
    }

    private void registerDemoCustomSpawns() {
        CustomSpawningService spawningService = plugin.getCustomSpawningService();
        if (spawningService == null) {
            logger.severe("CustomSpawningService not initialized. Demo custom spawns cannot be registered.");
            return;
        }
        boolean alreadyRegistered = spawningService.getAllRules().stream()
                .anyMatch(rule -> rule.getRuleId().equals(SKELETAL_WARRIOR_RULE_ID));
        if (alreadyRegistered) {
            logger.debug("Demo spawn rule '" + SKELETAL_WARRIOR_RULE_ID + "' already registered. Skipping.");
            return;
        }

        MobSpawnDefinition skeletalWarriorDef = new MobSpawnDefinition(
                "skeletal_warrior",
                EntityType.SKELETON,
                ChatColor.RED + "Skeletal Warrior",
                "skeletal_warrior_stats",
                WARRIOR_LOOT_TABLE_ID,
                Map.of(EquipmentSlot.HAND, new ItemStack(Material.IRON_SWORD))
        );
        List<SpawnCondition> conditions = List.of(
                new BiomeCondition(Set.of(Biome.PLAINS, Biome.MEADOW, Biome.SAVANNA)),
                new TimeCondition(13000, 23000)
        );
        CustomSpawnRule warriorRule = new CustomSpawnRule(
                SKELETAL_WARRIOR_RULE_ID,
                skeletalWarriorDef,
                conditions,
                0.1,
                60,
                200,
                5,
                16.0,
                200
        );
        spawningService.registerRule(warriorRule);
        registeredSpawnRuleIds.add(warriorRule.getRuleId());
    }

    private void registerDemoResourceNodeTypes() {
        ResourceNodeRegistryService registryService = plugin.getResourceNodeRegistryService();
        LootService lootService = plugin.getLootService();
        if (registryService == null || lootService == null) {
            logger.severe("Resource node services unavailable. Demo nodes cannot be registered.");
            return;
        }

        registerResourceNodeTypeIfAbsent(registryService, lootService,
                STONE_NODE_TYPE_ID,
                new ResourceNodeType(STONE_NODE_TYPE_ID, Material.STONE, 5.0,
                        Set.of(Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                                Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE),
                        STONE_NODE_LOOT_ID,
                        60,
                        "&7Stone Deposit"));

        registerResourceNodeTypeIfAbsent(registryService, lootService,
                IRON_NODE_TYPE_ID,
                new ResourceNodeType(IRON_NODE_TYPE_ID, Material.IRON_ORE, 10.0,
                        Set.of(Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE,
                                Material.NETHERITE_PICKAXE),
                        IRON_NODE_LOOT_ID,
                        180,
                        "&fIron Vein"));
    }

    private void registerResourceNodeTypeIfAbsent(ResourceNodeRegistryService registryService, LootService lootService,
                                                  String typeId, ResourceNodeType nodeType) {
        if (registryService.getNodeType(typeId).isPresent()) {
            logger.debug("Resource node type '" + typeId + "' already registered. Skipping demo registration.");
            return;
        }
        if (lootService.getLootTableById(nodeType.getLootTableId()).isEmpty()) {
            logger.warning("Required loot table '" + nodeType.getLootTableId() + "' missing for node type '" + typeId + "'.");
            return;
        }
        registryService.registerNodeType(nodeType);
        registeredResourceNodeTypeIds.add(typeId);
    }

    private void placeInitialResourceNodes() {
        ActiveNodeManager nodeManager = plugin.getActiveNodeManager();
        if (nodeManager == null) {
            logger.severe("ActiveNodeManager not initialized. Demo nodes cannot be placed.");
            return;
        }
        Optional<World> worldOpt = resolveDefaultWorld();
        if (worldOpt.isEmpty()) {
            logger.warning("No worlds available to place demo resource nodes.");
            return;
        }
        World world = worldOpt.get();
        int placed = 0;
        for (ResourceNodePlacement placement : DEFAULT_NODE_PLACEMENTS) {
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
        }
        if (placed > 0) {
            logger.info("Placed " + placed + " demo resource nodes in world '" + world.getName() + "'.");
        }
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

    private record ResourceNodePlacement(String nodeTypeId, int x, int y, int z) {
    }
}
