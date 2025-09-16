package com.x1f4r.mmocraft.loot.service;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry; // Not directly used by BasicLootService itself, but LootTable needs it.
import com.x1f4r.mmocraft.loot.model.LootTable;
import com.x1f4r.mmocraft.util.LoggingUtil;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.World; // For world.dropItemNaturally

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class BasicLootService implements LootService {

    private final MMOCraftPlugin plugin; // May be needed for plugin-specific actions or context
    private final LoggingUtil logger;
    // CustomItemRegistry is passed to LootTable.generateLoot(), not stored here directly.

    private final Map<EntityType, LootTable> mobLootTables = new EnumMap<>(EntityType.class);
    private final Map<String, LootTable> genericLootTables = new ConcurrentHashMap<>();


    public BasicLootService(MMOCraftPlugin plugin, LoggingUtil logger) {
        this.plugin = plugin;
        this.logger = logger;
        logger.debug("BasicLootService initialized.");
    }

    @Override
    public void registerLootTable(EntityType mobType, LootTable lootTable) {
        if (mobType == null || lootTable == null) {
            logger.warning("Attempted to register null mobType or lootTable.");
            return;
        }
        LootTable oldTable = mobLootTables.put(mobType, lootTable);
        if (oldTable != null) {
            logger.info("Replaced existing loot table for mob type: " + mobType);
        } else {
            logger.info("Registered loot table for mob type: " + mobType + " (ID: " + lootTable.getLootTableId() + ")");
        }
    }

    @Override
    public boolean unregisterLootTable(EntityType mobType) {
        if (mobType == null) {
            return false;
        }
        LootTable removed = mobLootTables.remove(mobType);
        if (removed != null) {
            logger.info("Removed loot table for mob type: " + mobType);
            return true;
        }
        return false;
    }

    @Override
    public Optional<LootTable> getLootTable(EntityType mobType) {
        if (mobType == null) return Optional.empty();
        return Optional.ofNullable(mobLootTables.get(mobType));
    }

    @Override
    public Optional<LootTable> getLootTableById(String lootTableId) {
        if (lootTableId == null || lootTableId.trim().isEmpty()) return Optional.empty();
        return Optional.ofNullable(genericLootTables.get(lootTableId));
    }

    @Override
    public void registerLootTableById(LootTable lootTable) {
        if (lootTable == null || lootTable.getLootTableId() == null || lootTable.getLootTableId().trim().isEmpty()) {
            logger.warning("Attempted to register null or invalid generic loot table.");
            return;
        }
        LootTable oldTable = genericLootTables.put(lootTable.getLootTableId(), lootTable);
         if (oldTable != null) {
            logger.info("Replaced existing generic loot table with ID: " + lootTable.getLootTableId());
        } else {
            logger.info("Registered generic loot table with ID: " + lootTable.getLootTableId());
        }
    }

    @Override
    public boolean unregisterLootTableById(String lootTableId) {
        if (lootTableId == null || lootTableId.trim().isEmpty()) {
            return false;
        }
        LootTable removed = genericLootTables.remove(lootTableId);
        if (removed != null) {
            logger.info("Removed generic loot table with ID: " + lootTableId);
            return true;
        }
        return false;
    }

    @Override
    public void spawnLoot(Location location, List<ItemStack> itemsToDrop) {
        if (location == null || itemsToDrop == null || itemsToDrop.isEmpty()) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            logger.warning("Cannot spawn loot, location has no world.");
            return;
        }

        for (ItemStack itemStack : itemsToDrop) {
            if (itemStack != null && !itemStack.getType().isAir()) {
                world.dropItemNaturally(location, itemStack);
                logger.finer("Dropped item: " + itemStack.getType() + " x" + itemStack.getAmount() + " at " + location);
            }
        }
    }
}
