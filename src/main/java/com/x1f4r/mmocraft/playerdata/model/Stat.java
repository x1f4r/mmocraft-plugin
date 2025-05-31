package com.x1f4r.mmocraft.playerdata.model;

/**
 * Represents core player statistics that can influence various aspects of gameplay.
 * These are typically used as keys in a map to store their values.
 */
public enum Stat {
    // Offensive Stats
    STRENGTH("Strength", "Increases physical damage and carrying capacity."),
    AGILITY("Agility", "Increases attack speed, critical hit chance, and evasion."),
    INTELLIGENCE("Intelligence", "Increases magical damage and effectiveness of spells."),

    // Defensive Stats
    DEFENSE("Defense", "Reduces incoming physical damage."),
    VITALITY("Vitality", "Increases maximum health points."),
    WISDOM("Wisdom", "Increases maximum mana points and mana regeneration."),

    // Other Potential Stats
    LUCK("Luck", "Influences chances of rare item drops and other random events."),
    PERCEPTION("Perception", "Affects detection range and accuracy.");
    // Add more stats as needed, e.g., RESISTANCE_FIRE, RESISTANCE_COLD, etc.

    private final String displayName;
    private final String description;

    Stat(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets the user-friendly display name of the stat.
     * @return The display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets a brief description of what the stat does.
     * @return The stat's description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * A common key prefix for storing these stats, e.g., in configuration or NBT.
     * @return A string like "stat.strength".
     */
    public String getKey() {
        return "stat." + this.name().toLowerCase();
    }
}
