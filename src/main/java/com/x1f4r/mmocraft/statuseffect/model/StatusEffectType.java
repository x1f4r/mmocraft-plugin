package com.x1f4r.mmocraft.statuseffect.model;

/**
 * Defines the types of status effects that can be applied to entities.
 * Each type can have distinct characteristics regarding its application and impact.
 */
public enum StatusEffectType {
    // Positive Effects (Buffs)
    HEALTH_REGEN(true, false, "Health Regeneration"), // Regeneration over time
    MANA_REGEN(true, false, "Mana Regeneration"),     // Mana regeneration over time
    STAT_BUFF_STRENGTH(true, false, "Strength Buff"), // Temporary increase to Strength
    STAT_BUFF_DEFENSE(true, false, "Defense Buff"),
    STAT_BUFF_HEALTH(true, false, "Health Buff"),
    STAT_BUFF_INTELLIGENCE(true, false, "Intelligence Buff"),
    STAT_BUFF_CRIT_CHANCE(true, false, "Critical Chance Buff"),
    STAT_BUFF_CRIT_DAMAGE(true, false, "Critical Damage Buff"),
    STAT_BUFF_ABILITY_POWER(true, false, "Ability Power Buff"),
    STAT_BUFF_SPEED(true, false, "Speed Buff"),
    STAT_BUFF_FEROCITY(true, false, "Ferocity Buff"),
    STAT_BUFF_EVASION(true, false, "Evasion Buff"),
    MOVEMENT_SPEED_BUFF(true, false, "Speed Buff"),   // Increased movement speed
    DAMAGE_ABSORPTION_SHIELD(true, false, "Absorption Shield"), // Shield that absorbs a certain amount of damage

    // Negative Effects (Debuffs/Ailments)
    POISON(false, true, "Poison"),             // Damage over time
    BLEED(false, true, "Bleed"),               // Damage over time, often physical
    STUN(false, true, "Stun"),                 // Prevents actions
    ROOT(false, true, "Root"),                 // Prevents movement but allows actions
    SLOW(false, true, "Slow"),                 // Decreased movement speed
    WEAKNESS(false, true, "Weakness"),         // Reduced physical damage output
    FRAILTY(false, true, "Frailty"),           // Increased physical damage taken (reduced defense)
    SILENCE(false, true, "Silence"),           // Prevents casting magical skills
    BLIND(false, true, "Blind"),               // Reduced vision range or accuracy
    STAT_DEBUFF_STRENGTH(false, true, "Strength Debuff"), // Temporary decrease to Strength
    // ... other stat debuffs

    // Neutral or Mixed Effects
    BERSERK(true, true, "Berserk"); // Increased damage output but also increased damage taken

    private final boolean isBuff;
    private final boolean isHarmful;
    private final String displayName;

    StatusEffectType(boolean isBuff, boolean isHarmful, String displayName) {
        this.isBuff = isBuff;
        this.isHarmful = isHarmful;
        this.displayName = displayName;
    }

    /**
     * @return True if the effect is generally considered beneficial (a buff).
     */
    public boolean isBuff() {
        return isBuff;
    }

    /**
     * @return True if the effect is generally considered detrimental (a debuff or harmful condition).
     */
    public boolean isHarmful() {
        return isHarmful;
    }

    /**
     * @return A user-friendly name for the status effect type.
     */
    public String getDisplayName() {
        return displayName;
    }
}
