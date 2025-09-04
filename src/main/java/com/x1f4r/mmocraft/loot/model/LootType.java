package com.x1f4r.mmocraft.loot.model;

/**
 * Specifies the type of item in a loot entry.
 */
public enum LootType {
    /**
     * A custom item defined in the {@link com.x1f4r.mmocraft.item.service.CustomItemRegistry}.
     * The identifier for this type should be a custom item ID string.
     */
    CUSTOM,

    /**
     * A standard vanilla Minecraft item.
     * The identifier for this type should be the Bukkit {@link org.bukkit.Material} name.
     */
    VANILLA
}
