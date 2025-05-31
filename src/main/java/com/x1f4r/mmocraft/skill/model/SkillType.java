package com.x1f4r.mmocraft.skill.model;

/**
 * Defines the type and targeting mechanism of a skill.
 * This helps determine how a skill is activated and what kind of targets it might require.
 */
public enum SkillType {
    /**
     * A skill that is always active or triggers automatically under certain conditions,
     * without direct player activation for each use.
     * Example: Increased health regeneration, a chance to reflect damage.
     */
    PASSIVE,

    /**
     * An active skill that requires the player to target a specific entity (e.g., a monster or another player).
     * Example: A single-target damage spell, a healing spell cast on an ally.
     */
    ACTIVE_TARGETED_ENTITY,

    /**
     * An active skill that is cast by the player upon themselves.
     * Example: A personal buff, a self-heal.
     */
    ACTIVE_SELF,

    /**
     * An active skill that targets a specific point/location on the ground (Area of Effect).
     * Example: A ground-targeted AoE damage spell like a meteor or blizzard.
     */
    ACTIVE_AOE_POINT,

    /**
     * An active skill that does not require a specific target but is actively used.
     * Could be an AoE around the caster, a wave projectile, or a utility skill.
     * Example: A shout buffing nearby allies, a conal attack in front of the caster.
     */
    ACTIVE_NO_TARGET
}
