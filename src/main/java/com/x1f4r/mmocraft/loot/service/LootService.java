package com.x1f4r.mmocraft.loot.service;

import com.x1f4r.mmocraft.loot.model.LootTable;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing loot tables and spawning loot in the world.
 */
public interface LootService {

    /**
     * Registers a loot table for a specific mob type.
     * If a loot table is already registered for this mob type, it may be overwritten.
     *
     * @param mobType The {@link EntityType} of the mob.
     * @param lootTable The {@link LootTable} to associate with this mob type.
     */
    void registerLootTable(EntityType mobType, LootTable lootTable);

    /**
     * Retrieves the loot table associated with a specific mob type.
     *
     * @param mobType The {@link EntityType} of the mob.
     * @return An {@link Optional} containing the {@link LootTable} if found, or empty otherwise.
     */
    Optional<LootTable> getLootTable(EntityType mobType);

    /**
     * Retrieves the loot table associated with a specific loot table ID.
     * This allows for more generic loot tables not tied directly to mob types (e.g., for chests).
     *
     * @param lootTableId The unique ID of the loot table.
     * @return An {@link Optional} containing the {@link LootTable} if found, or empty otherwise.
     */
    Optional<LootTable> getLootTableById(String lootTableId);

    /**
     * Registers a generic loot table by its ID.
     * @param lootTable The loot table to register.
     */
    void registerLootTableById(LootTable lootTable);


    /**
     * Spawns a list of items as drops at a specified location in the world.
     *
     * @param location The {@link Location} where the items should be dropped.
     * @param itemsToDrop A list of {@link ItemStack}s to drop.
     */
    void spawnLoot(Location location, List<ItemStack> itemsToDrop);
}
