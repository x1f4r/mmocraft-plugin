package com.x1f4r.mmocraft.world.spawning.model;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.EnumMap; // Added
import java.util.Map;
import java.util.Objects;
import java.util.Optional; // Added

/**
 * Defines the properties of a specific type of mob to be spawned by the custom spawning system.
 */
public class MobSpawnDefinition {

    private final String definitionId; // Unique ID for this definition, e.g., "goblin_warrior"
    private final EntityType entityType;   // Bukkit EntityType, e.g., EntityType.ZOMBIE
    private final String displayName;      // Optional custom name, e.g., "&c Goblin Warrior"
    private final String mobStatKey;       // Key for MobStatProvider, e.g., "GOBLIN_WARRIOR" or EntityType.name()
    private final String lootTableId;      // Optional: ID of a specific loot table from LootService
    private final Map<EquipmentSlot, ItemStack> equipment; // Optional: Equipment for the mob

    public MobSpawnDefinition(String definitionId, EntityType entityType, String displayName,
                              String mobStatKey, String lootTableId, Map<EquipmentSlot, ItemStack> equipment) {
        this.definitionId = Objects.requireNonNull(definitionId, "definitionId cannot be null");
        this.entityType = Objects.requireNonNull(entityType, "entityType cannot be null");
        this.displayName = displayName; // Nullable
        this.mobStatKey = Objects.requireNonNull(mobStatKey, "mobStatKey cannot be null (can be EntityType.name())");
        this.lootTableId = lootTableId; // Nullable
        this.equipment = (equipment != null) ? Collections.unmodifiableMap(new EnumMap<>(equipment)) : Collections.emptyMap();
    }

    // Minimal constructor
    public MobSpawnDefinition(String definitionId, EntityType entityType) {
        this(definitionId, entityType, null, entityType.name(), null, null);
    }


    // Getters
    public String getDefinitionId() { return definitionId; }
    public EntityType getEntityType() { return entityType; }
    public Optional<String> getDisplayName() { return Optional.ofNullable(displayName); }
    public String getMobStatKey() { return mobStatKey; }
    public Optional<String> getLootTableId() { return Optional.ofNullable(lootTableId); }
    public Map<EquipmentSlot, ItemStack> getEquipment() { return equipment; } // Already unmodifiable

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MobSpawnDefinition that = (MobSpawnDefinition) o;
        return definitionId.equals(that.definitionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(definitionId);
    }

    @Override
    public String toString() {
        return "MobSpawnDefinition{" +
               "id='" + definitionId + '\'' +
               ", type=" + entityType +
               (displayName != null ? ", name='" + displayName + '\'' : "") +
               ", statKey='" + mobStatKey + '\'' +
               (lootTableId != null ? ", lootTable='" + lootTableId + '\'' : "") +
               ", equipmentSlots=" + equipment.size() +
               '}';
    }
}
