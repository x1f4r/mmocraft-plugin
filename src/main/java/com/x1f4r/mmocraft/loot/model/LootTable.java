package com.x1f4r.mmocraft.loot.model;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections; // Added
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a collection of possible loot drops, typically associated with a mob type or a chest.
 */
public class LootTable {

    private final String lootTableId;
    private final List<LootTableEntry> entries;
    private final Random random = ThreadLocalRandom.current(); // Efficient random for concurrent use if needed

    /**
     * Constructs a new LootTable.
     *
     * @param lootTableId A unique identifier for this loot table (e.g., "zombie_common_drops").
     * @param entries A list of {@link LootTableEntry} defining the possible items and their drop chances.
     */
    public LootTable(String lootTableId, List<LootTableEntry> entries) {
        this.lootTableId = Objects.requireNonNull(lootTableId, "lootTableId cannot be null");
        this.entries = new ArrayList<>(Objects.requireNonNull(entries, "Loot table entries cannot be null."));
    }

    public String getLootTableId() {
        return lootTableId;
    }

    /**
     * Returns an unmodifiable view of the loot table entries.
     * @return A list of loot table entries.
     */
    public List<LootTableEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Adds an entry to this loot table.
     * @param entry The LootTableEntry to add.
     */
    public void addEntry(LootTableEntry entry) {
        this.entries.add(Objects.requireNonNull(entry));
    }


    /**
     * Generates a list of ItemStacks based on the drop chances and amounts defined in this loot table.
     *
     * @param itemRegistry The {@link CustomItemRegistry} used to create {@link ItemStack}s from custom item IDs.
     * @param plugin The {@link MMOCraftPlugin} instance, potentially needed by item creation or NBT utilities.
     * @return A list of {@link ItemStack}s that were determined to drop. The list may be empty.
     */
    public List<ItemStack> generateLoot(CustomItemRegistry itemRegistry, MMOCraftPlugin plugin) {
        List<ItemStack> generatedItems = new ArrayList<>();
        if (itemRegistry == null) {
            // Consider logging this error through a passed logger or plugin instance
            System.err.println("LootTable.generateLoot: CustomItemRegistry is null! Cannot generate loot.");
            return generatedItems;
        }

        for (LootTableEntry entry : entries) {
            if (random.nextDouble() < entry.dropChance()) { // Check if the item drops
                int amountToDrop = entry.minAmount();
                if (entry.maxAmount() > entry.minAmount()) {
                    amountToDrop = random.nextInt((entry.maxAmount() - entry.minAmount()) + 1) + entry.minAmount();
                }

                try {
                    ItemStack itemStack = itemRegistry.createItemStack(entry.customItemId(), amountToDrop);
                    if (itemStack != null && itemStack.getType() != org.bukkit.Material.AIR) {
                        generatedItems.add(itemStack);
                    } else {
                        // Log warning if item ID is valid but createItemStack returns AIR/null
                         if (plugin != null && plugin.getLoggingUtil() != null) {
                            plugin.getLoggingUtil().warning("LootTable '" + lootTableId + "' generated AIR/null for customItemId: " + entry.customItemId());
                        } else {
                             System.err.println("LootTable '" + lootTableId + "' generated AIR/null for customItemId: " + entry.customItemId());
                         }
                    }
                } catch (IllegalArgumentException e) {
                    // This typically means the item ID was not found in the registry
                    if (plugin != null && plugin.getLoggingUtil() != null) {
                        plugin.getLoggingUtil().warning("LootTable '" + lootTableId + "' failed to create item: " + e.getMessage());
                    } else {
                        System.err.println("LootTable '" + lootTableId + "' failed to create item: " + e.getMessage());
                    }
                }
            }
        }
        return generatedItems;
    }
}
