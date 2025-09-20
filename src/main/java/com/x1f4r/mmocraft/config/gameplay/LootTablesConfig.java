package com.x1f4r.mmocraft.config.gameplay;

import com.x1f4r.mmocraft.loot.model.LootType;
import org.bukkit.entity.EntityType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable configuration describing loot table definitions and assignments.
 */
public final class LootTablesConfig {

    private final Map<String, LootTableDefinition> tablesById;
    private final Map<EntityType, String> mobAssignments;

    public LootTablesConfig(Map<String, LootTableDefinition> tablesById, Map<EntityType, String> mobAssignments) {
        this.tablesById = Collections.unmodifiableMap(new java.util.HashMap<>(tablesById));
        EnumMap<EntityType, String> assignmentsCopy = mobAssignments.isEmpty()
                ? new EnumMap<>(EntityType.class)
                : new EnumMap<>(mobAssignments);
        this.mobAssignments = Collections.unmodifiableMap(assignmentsCopy);
    }

    public Map<String, LootTableDefinition> getTablesById() {
        return tablesById;
    }

    public Map<EntityType, String> getMobAssignments() {
        return mobAssignments;
    }

    public LootTableDefinition getTable(String id) {
        return tablesById.get(id);
    }

    public static LootTablesConfig defaults() {
        return new LootTablesConfig(Collections.emptyMap(), Collections.emptyMap());
    }

    public record LootTableDefinition(String tableId, List<LootEntryDefinition> entries) {
        public LootTableDefinition {
            Objects.requireNonNull(tableId, "tableId");
            entries = List.copyOf(entries);
        }
    }

    public record LootEntryDefinition(LootType type, String identifier, double chance, int minAmount, int maxAmount) {
        public LootEntryDefinition {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(identifier, "identifier");
        }
    }
}
