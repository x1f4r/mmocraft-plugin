package com.x1f4r.mmocraft.playerdata.model;

/**
 * Represents player statistics inspired by Hypixel-style RPG attributes.
 * These act as keys for persistent storage and for runtime calculations.
 */
public enum Stat {
    // Core combat stats
    HEALTH("Health", "Increases maximum health."),
    DEFENSE("Defense", "Reduces incoming non-true damage."),
    TRUE_DEFENSE("True Defense", "Reduces incoming true damage."),
    STRENGTH("Strength", "Increases melee and physical damage."),
    CRITICAL_CHANCE("Critical Chance", "Increases the chance for attacks to crit."),
    CRITICAL_DAMAGE("Critical Damage", "Increases the damage dealt by critical hits."),
    INTELLIGENCE("Intelligence", "Increases mana capacity and spell scaling."),
    MANA_REGEN("Mana Regeneration", "Improves passive mana regeneration."),
    ABILITY_POWER("Ability Power", "Boosts ability and spell damage."),
    ATTACK_SPEED("Attack Speed", "Improves weapon swing rate."),
    FEROCITY("Ferocity", "Chance to strike additional times."),
    EVASION("Evasion", "Chance to dodge incoming attacks."),

    // Utility and progression stats
    SPEED("Speed", "Increases movement speed up to the configured cap."),
    MAGIC_FIND("Magic Find", "Improves chances for rare loot drops."),
    PET_LUCK("Pet Luck", "Improves chances for higher-quality pets."),

    // Gathering stats
    MINING_SPEED("Mining Speed", "Determines how fast blocks are broken."),
    MINING_FORTUNE("Mining Fortune", "Increases drops from mining blocks."),
    FARMING_FORTUNE("Farming Fortune", "Increases crops harvested per break."),
    FORAGING_FORTUNE("Foraging Fortune", "Increases drops from foraging."),
    FISHING_FORTUNE("Fishing Fortune", "Improves treasure from fishing.");

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
