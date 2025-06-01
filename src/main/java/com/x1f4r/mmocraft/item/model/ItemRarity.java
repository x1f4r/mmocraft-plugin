package com.x1f4r.mmocraft.item.model;

import org.bukkit.ChatColor;

/**
 * Represents the rarity of a custom item.
 * Each rarity has a display name (with color codes) and a corresponding ChatColor.
 */
public enum ItemRarity {
    COMMON("&fCommon", ChatColor.WHITE),
    UNCOMMON("&aUncommon", ChatColor.GREEN),
    RARE("&9Rare", ChatColor.BLUE), // Bukkit's BLUE is dark blue, AQUA is light blue
    EPIC("&5Epic", ChatColor.DARK_PURPLE),
    LEGENDARY("&6Legendary", ChatColor.GOLD),
    MYTHIC("&cMythic", ChatColor.RED),
    UNIQUE("&eUnique", ChatColor.YELLOW); // Added another common one

    private final String displayName;
    private final ChatColor chatColor;

    ItemRarity(String displayName, ChatColor chatColor) {
        this.displayName = displayName;
        this.chatColor = chatColor;
    }

    /**
     * Gets the pre-colorized display name of the rarity (e.g., "&fCommon").
     * Use with StringUtil.colorize() if needed again, but it's stored colorized.
     * @return The display name with Minecraft color codes.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the {@link ChatColor} associated with this rarity.
     * This can be used to prefix item names or for other color-related operations.
     * @return The ChatColor for this rarity.
     */
    public ChatColor getChatColor() {
        return chatColor;
    }

    /**
     * Gets the display name without the internal color codes, using the ChatColor field.
     * Example: For COMMON ("&fCommon", ChatColor.WHITE), this would return "Common".
     * @return The plain display name.
     */
    public String getPlainDisplayName() { // Corrected typo
        // Strip existing codes from displayName and then apply the chatColor,
        // or more simply, derive from enum name or a separate plain field.
        // For now, let's assume displayName is the source of truth and strip it.
        // This is a bit redundant if ChatColor.stripColor is available/used.
        // A cleaner way: store plain name separately.
        // For this example, ChatColor.stripColor(displayName) is not ideal as it removes the color intended.
        // Let's return the enum name title-cased.
        String name = this.name(); // e.g. COMMON
        return name.charAt(0) + name.substring(1).toLowerCase(); // Common
    }
}
