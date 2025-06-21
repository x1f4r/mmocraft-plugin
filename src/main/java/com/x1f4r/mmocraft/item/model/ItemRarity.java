package com.x1f4r.mmocraft.item.model;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * Represents the rarity of a custom item.
 * Each rarity has a display name (with color codes) and a corresponding TextColor.
 */
public enum ItemRarity {
    COMMON("&fCommon", NamedTextColor.WHITE),
    UNCOMMON("&aUncommon", NamedTextColor.GREEN),
    RARE("&9Rare", NamedTextColor.BLUE), // Adventure's BLUE is generally a bright blue. For dark blue, use DARK_BLUE.
    EPIC("&5Epic", NamedTextColor.DARK_PURPLE),
    LEGENDARY("&6Legendary", NamedTextColor.GOLD),
    MYTHIC("&cMythic", NamedTextColor.RED),
    UNIQUE("&eUnique", NamedTextColor.YELLOW);

    private final String displayName; // This still contains legacy codes like "&fCommon"
    private final TextColor textColor;

    ItemRarity(String displayName, TextColor textColor) {
        this.displayName = displayName;
        this.textColor = textColor;
    }

    /**
     * Gets the pre-colorized display name of the rarity (e.g., "&fCommon").
     * This string contains legacy color codes.
     * @return The display name with Minecraft legacy color codes.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the {@link TextColor} associated with this rarity.
     * This can be used for Adventure API components.
     * @return The TextColor for this rarity.
     */
    public TextColor getTextColor() {
        return textColor;
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
