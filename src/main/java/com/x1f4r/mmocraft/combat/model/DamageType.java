package com.x1f4r.mmocraft.combat.model;

/**
 * Represents the type of damage being dealt.
 * This can influence how damage is calculated, reduced, and displayed.
 */
public enum DamageType {
    /**
     * Physical damage, typically reduced by armor/defense.
     * Often influenced by Strength or weapon properties.
     */
    PHYSICAL,

    /**
     * Magical damage, often reduced by magic resistance or specific enchantments.
     * Typically influenced by Intelligence or spell power.
     */
    MAGICAL,

    /**
     * True damage bypasses most forms of damage reduction (armor, resistances).
     * Useful for specific abilities or effects.
     */
    TRUE,

    /**
     * Damage originating from environmental sources (e.g., lava, fall damage, poison).
     * May have its own reduction mechanics or be unblockable.
     * (For future expansion, current system focuses on entity-to-entity combat).
     */
    ENVIRONMENTAL,

    /**
     * Healing, which can be thought of as negative damage.
     * (For future expansion if healing abilities use the same calculation pipeline).
     */
    HEALING // Optional, for future use if healing is processed similarly
}
