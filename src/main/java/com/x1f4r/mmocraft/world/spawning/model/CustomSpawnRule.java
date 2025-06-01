package com.x1f4r.mmocraft.world.spawning.model;

import com.x1f4r.mmocraft.world.spawning.conditions.SpawnCondition;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Defines a rule for custom mob spawning, including the mob type, conditions,
 * frequency, and density checks.
 */
public class CustomSpawnRule {

    private final String ruleId;
    private final MobSpawnDefinition mobSpawnDefinition;
    private final List<SpawnCondition> conditions;
    private final double spawnChance; // 0.0 to 1.0, per attempt
    private final int minSpawnHeight;
    private final int maxSpawnHeight;
    private final int maxNearbyEntities; // Max of this mob type in the spawnRadiusCheck
    private final double spawnRadiusCheck; // Radius for maxNearbyEntities check
    private final long spawnIntervalTicks; // Minimum ticks between spawn attempts for this rule in a given area/chunk

    private transient long lastSpawnAttemptTickGlobal; // For global cooldown on rule if needed, not per-location
    private final Random random = ThreadLocalRandom.current();


    public CustomSpawnRule(String ruleId, MobSpawnDefinition mobSpawnDefinition, List<SpawnCondition> conditions,
                           double spawnChance, int minSpawnHeight, int maxSpawnHeight,
                           int maxNearbyEntities, double spawnRadiusCheck, long spawnIntervalTicks) {
        this.ruleId = Objects.requireNonNull(ruleId, "ruleId cannot be null");
        this.mobSpawnDefinition = Objects.requireNonNull(mobSpawnDefinition, "mobSpawnDefinition cannot be null");
        this.conditions = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(conditions, "conditions list cannot be null")));

        if (spawnChance < 0.0 || spawnChance > 1.0) throw new IllegalArgumentException("Spawn chance must be between 0.0 and 1.0");
        this.spawnChance = spawnChance;

        this.minSpawnHeight = minSpawnHeight;
        this.maxSpawnHeight = maxSpawnHeight;
        if (maxSpawnHeight < minSpawnHeight) throw new IllegalArgumentException("Max spawn height cannot be less than min spawn height.");

        if (maxNearbyEntities < 0) throw new IllegalArgumentException("Max nearby entities cannot be negative.");
        this.maxNearbyEntities = maxNearbyEntities;

        if (spawnRadiusCheck < 0) throw new IllegalArgumentException("Spawn radius check cannot be negative.");
        this.spawnRadiusCheck = spawnRadiusCheck;

        if (spawnIntervalTicks < 0) throw new IllegalArgumentException("Spawn interval ticks cannot be negative.");
        this.spawnIntervalTicks = spawnIntervalTicks;
        this.lastSpawnAttemptTickGlobal = 0; // Initialize to allow immediate first attempt
    }

    // Getters
    public String getRuleId() { return ruleId; }
    public MobSpawnDefinition getMobSpawnDefinition() { return mobSpawnDefinition; }
    public List<SpawnCondition> getConditions() { return conditions; }
    public double getSpawnChance() { return spawnChance; }
    public int getMinSpawnHeight() { return minSpawnHeight; }
    public int getMaxSpawnHeight() { return maxSpawnHeight; }
    public int getMaxNearbyEntities() { return maxNearbyEntities; }
    public double getSpawnRadiusCheck() { return spawnRadiusCheck; }
    public long getSpawnIntervalTicks() { return spawnIntervalTicks; }
    public long getLastSpawnAttemptTickGlobal() { return lastSpawnAttemptTickGlobal; }

    public void setLastSpawnAttemptTickGlobal(long tick) {
        this.lastSpawnAttemptTickGlobal = tick;
    }

    /**
     * Checks if all spawn conditions associated with this rule are met for the given location.
     * Also checks height constraints.
     * @param location The potential spawn location.
     * @param world The world of the spawn.
     * @param nearestPlayer Optional nearest player for context.
     * @return True if all conditions are met, false otherwise.
     */
    public boolean conditionsMet(Location location, World world, Player nearestPlayer) {
        int y = location.getBlockY();
        if (y < minSpawnHeight || y > maxSpawnHeight) {
            return false;
        }
        for (SpawnCondition condition : conditions) {
            if (!condition.check(location, world, nearestPlayer)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if this rule is ready to attempt a spawn based on its global interval.
     * This is a simple global cooldown; per-chunk/area cooldowns would be more complex.
     * @param currentTick The current server tick.
     * @return True if enough time has passed since the last global attempt for this rule.
     */
    public boolean isReadyToAttemptSpawn(long currentTick) {
        return currentTick >= lastSpawnAttemptTickGlobal + spawnIntervalTicks;
    }

    /**
     * Determines if a spawn should occur based on the rule's spawnChance.
     * @return True if the random roll succeeds, false otherwise.
     */
    public boolean rollForSpawn() {
        return random.nextDouble() < this.spawnChance;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomSpawnRule that = (CustomSpawnRule) o;
        return ruleId.equals(that.ruleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleId);
    }
}
