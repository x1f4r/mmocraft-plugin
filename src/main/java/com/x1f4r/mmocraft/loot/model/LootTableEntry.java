package com.x1f4r.mmocraft.loot.model;

import java.util.Objects;

/**
 * Represents a single entry in a loot table.
 *
 * @param customItemId The unique ID of the {@link com.x1f4r.mmocraft.item.model.CustomItem} to drop.
 * @param dropChance The probability of this item dropping (0.0 to 1.0).
 * @param minAmount The minimum amount of this item to drop if the drop occurs.
 * @param maxAmount The maximum amount of this item to drop if the drop occurs.
 */
public record LootTableEntry(
    String customItemId,
    double dropChance,
    int minAmount,
    int maxAmount
) {
    public LootTableEntry {
        Objects.requireNonNull(customItemId, "customItemId cannot be null");
        if (dropChance < 0.0 || dropChance > 1.0) {
            throw new IllegalArgumentException("Drop chance must be between 0.0 and 1.0");
        }
        if (minAmount <= 0) {
            throw new IllegalArgumentException("Minimum amount must be positive.");
        }
        if (maxAmount < minAmount) {
            throw new IllegalArgumentException("Maximum amount cannot be less than minimum amount.");
        }
    }
}
