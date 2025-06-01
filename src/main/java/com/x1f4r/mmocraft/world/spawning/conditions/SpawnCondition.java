package com.x1f4r.mmocraft.world.spawning.conditions;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player; // Optional context, might not be needed for all conditions

/**
 * Interface for defining conditions that must be met for a custom mob to spawn.
 */
@FunctionalInterface
public interface SpawnCondition {
    /**
     * Checks if the condition is met at the given location.
     *
     * @param location The potential spawn location.
     * @param world The world where the spawn is being attempted.
     * @param nearestPlayer Optional: The nearest player to the spawn location, for context-sensitive conditions. Can be null.
     * @return True if the condition is met, false otherwise.
     */
    boolean check(Location location, World world, Player nearestPlayer);
}
