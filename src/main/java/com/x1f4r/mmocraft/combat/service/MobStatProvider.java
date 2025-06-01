package com.x1f4r.mmocraft.combat.service;

import org.bukkit.entity.EntityType;

/**
 * Provides base statistics for different mob types.
 * This allows for a centralized way to define and retrieve default mob combat attributes.
 */
public interface MobStatProvider {

    /**
     * Gets the base maximum health for a given mob type.
     *
     * @param type The EntityType of the mob.
     * @return The base health value.
     */
    double getBaseHealth(EntityType type);

    /**
     * Gets the base attack damage for a given mob type.
     * This might be for melee attacks; ranged attacks could be handled separately or via weapon stats.
     *
     * @param type The EntityType of the mob.
     * @return The base attack damage value.
     */
    double getBaseAttackDamage(EntityType type);

    /**
     * Gets the base defense value for a given mob type.
     * This can be used to calculate damage reduction.
     *
     * @param type The EntityType of the mob.
     * @return The base defense value (e.g., points that convert to reduction percentage).
     */
    double getBaseDefense(EntityType type);
}
