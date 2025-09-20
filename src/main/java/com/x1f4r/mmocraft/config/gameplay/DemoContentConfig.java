package com.x1f4r.mmocraft.config.gameplay;

import com.x1f4r.mmocraft.loot.model.LootType;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable configuration bundle powering the demo content module.
 */
public final class DemoContentConfig {

    private final DemoToggles toggles;
    private final List<LootTableDefinition> genericLootTables;
    private final List<MobLootTableDefinition> mobLootTables;
    private final List<ResourceNodeTypeConfig> resourceNodeTypes;
    private final List<ResourceNodePlacementConfig> resourceNodePlacements;
    private final List<CustomSpawnRuleConfig> customSpawnRules;

    public DemoContentConfig(DemoToggles toggles,
                             List<LootTableDefinition> genericLootTables,
                             List<MobLootTableDefinition> mobLootTables,
                             List<ResourceNodeTypeConfig> resourceNodeTypes,
                             List<ResourceNodePlacementConfig> resourceNodePlacements,
                             List<CustomSpawnRuleConfig> customSpawnRules) {
        this.toggles = Objects.requireNonNull(toggles, "toggles");
        this.genericLootTables = List.copyOf(genericLootTables);
        this.mobLootTables = List.copyOf(mobLootTables);
        this.resourceNodeTypes = List.copyOf(resourceNodeTypes);
        this.resourceNodePlacements = List.copyOf(resourceNodePlacements);
        this.customSpawnRules = List.copyOf(customSpawnRules);
    }

    public DemoToggles getToggles() {
        return toggles;
    }

    public List<LootTableDefinition> getGenericLootTables() {
        return genericLootTables;
    }

    public List<MobLootTableDefinition> getMobLootTables() {
        return mobLootTables;
    }

    public List<ResourceNodeTypeConfig> getResourceNodeTypes() {
        return resourceNodeTypes;
    }

    public List<ResourceNodePlacementConfig> getResourceNodePlacements() {
        return resourceNodePlacements;
    }

    public List<CustomSpawnRuleConfig> getCustomSpawnRules() {
        return customSpawnRules;
    }

    public static DemoContentConfig defaults() {
        return new DemoContentConfig(DemoToggles.defaults(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public record DemoToggles(boolean master,
                               boolean items,
                               boolean skills,
                               boolean lootTables,
                               boolean customSpawns,
                               boolean resourceNodes,
                               boolean zones) {
        public static DemoToggles defaults() {
            return new DemoToggles(false, true, true, true, true, true, false);
        }
    }

    public record LootEntryDefinition(LootType type, String identifier, double chance, int minAmount, int maxAmount) {
        public LootEntryDefinition {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(identifier, "identifier");
        }
    }

    public record LootTableDefinition(String tableId, List<LootEntryDefinition> entries) {
        public LootTableDefinition {
            Objects.requireNonNull(tableId, "tableId");
            entries = List.copyOf(entries);
        }
    }

    public record MobLootTableDefinition(EntityType entityType, LootTableDefinition table) {
        public MobLootTableDefinition {
            Objects.requireNonNull(entityType, "entityType");
            Objects.requireNonNull(table, "table");
        }
    }

    public record ResourceNodeTypeConfig(String typeId,
                                         Material blockMaterial,
                                         double harvestSeconds,
                                         Set<Material> requiredTools,
                                         String lootTableId,
                                         int respawnSeconds,
                                         String displayName) {
        public ResourceNodeTypeConfig {
            Objects.requireNonNull(typeId, "typeId");
            Objects.requireNonNull(blockMaterial, "blockMaterial");
            Objects.requireNonNull(requiredTools, "requiredTools");
            Objects.requireNonNull(lootTableId, "lootTableId");
            Objects.requireNonNull(displayName, "displayName");
            EnumSet<Material> copy = requiredTools.isEmpty()
                    ? EnumSet.noneOf(Material.class)
                    : EnumSet.copyOf(requiredTools);
            requiredTools = Collections.unmodifiableSet(copy);
        }
    }

    public record ResourceNodePlacementConfig(String nodeTypeId, String world, int x, int y, int z) {
        public ResourceNodePlacementConfig {
            Objects.requireNonNull(nodeTypeId, "nodeTypeId");
        }
    }

    public record CustomSpawnRuleConfig(String ruleId,
                                        String mobId,
                                        EntityType entityType,
                                        String displayName,
                                        String statProfileId,
                                        String lootTableId,
                                        Map<EquipmentSlot, Material> equipment,
                                        Set<Biome> biomes,
                                        TimeWindow timeWindow,
                                        double spawnChance,
                                        int minY,
                                        int maxY,
                                        int maxNearby,
                                        double radius,
                                        long intervalTicks) {
        public CustomSpawnRuleConfig {
            Objects.requireNonNull(ruleId, "ruleId");
            Objects.requireNonNull(mobId, "mobId");
            Objects.requireNonNull(entityType, "entityType");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(statProfileId, "statProfileId");
            Objects.requireNonNull(lootTableId, "lootTableId");
            Objects.requireNonNull(equipment, "equipment");
            Objects.requireNonNull(biomes, "biomes");
            Objects.requireNonNull(timeWindow, "timeWindow");
            EnumMap<EquipmentSlot, Material> equipmentCopy = new EnumMap<>(EquipmentSlot.class);
            equipmentCopy.putAll(equipment);
            equipment = Collections.unmodifiableMap(equipmentCopy);
            Set<Biome> biomeCopy = biomes.isEmpty()
                    ? Collections.emptySet()
                    : new HashSet<>(biomes);
            biomes = Collections.unmodifiableSet(biomeCopy);
        }
    }

    public record TimeWindow(int start, int end) {
        public TimeWindow {
            if (start < 0 || end < 0) {
                throw new IllegalArgumentException("Time window values must be non-negative");
            }
        }
    }
}
