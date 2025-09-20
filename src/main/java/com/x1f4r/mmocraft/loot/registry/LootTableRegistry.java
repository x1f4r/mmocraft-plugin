package com.x1f4r.mmocraft.loot.registry;

import com.x1f4r.mmocraft.config.gameplay.LootTablesConfig;
import com.x1f4r.mmocraft.loot.model.LootTable;
import com.x1f4r.mmocraft.loot.model.LootTableEntry;
import com.x1f4r.mmocraft.loot.service.LootService;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Manages loot tables sourced from configuration files and applies them to the active {@link LootService}.
 */
public class LootTableRegistry {

    private final LootService lootService;
    private final LoggingUtil logger;
    private final Set<String> managedGenericTables = new HashSet<>();
    private final Map<EntityType, String> managedMobTables = new EnumMap<>(EntityType.class);

    public LootTableRegistry(LootService lootService, LoggingUtil logger) {
        this.lootService = Objects.requireNonNull(lootService, "lootService");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public synchronized void applyConfig(LootTablesConfig config) {
        Objects.requireNonNull(config, "config");
        for (String tableId : new HashSet<>(managedGenericTables)) {
            lootService.unregisterLootTableById(tableId);
        }
        managedGenericTables.clear();
        for (EntityType entityType : new HashSet<>(managedMobTables.keySet())) {
            lootService.unregisterLootTable(entityType);
        }
        managedMobTables.clear();

        for (LootTablesConfig.LootTableDefinition definition : config.getTablesById().values()) {
            LootTable table = buildLootTable(definition);
            lootService.registerLootTableById(table);
            managedGenericTables.add(definition.tableId());
            logger.info("Registered loot table: " + definition.tableId());
        }

        for (Map.Entry<EntityType, String> entry : config.getMobAssignments().entrySet()) {
            EntityType type = entry.getKey();
            String tableId = entry.getValue();
            LootTablesConfig.LootTableDefinition definition = config.getTable(tableId);
            if (definition == null) {
                logger.warning("Mob loot assignment references unknown table: " + tableId + " for entity " + type.name());
                continue;
            }
            LootTable table = buildLootTable(definition);
            lootService.registerLootTable(type, table);
            managedMobTables.put(type, tableId);
            logger.info("Assigned loot table '" + tableId + "' to " + type.name());
        }
    }

    private LootTable buildLootTable(LootTablesConfig.LootTableDefinition definition) {
        List<LootTableEntry> entries = new ArrayList<>();
        for (LootTablesConfig.LootEntryDefinition entry : definition.entries()) {
            entries.add(new LootTableEntry(entry.type(), entry.identifier(), entry.chance(), entry.minAmount(), entry.maxAmount()));
        }
        return new LootTable(definition.tableId(), entries);
    }
}
