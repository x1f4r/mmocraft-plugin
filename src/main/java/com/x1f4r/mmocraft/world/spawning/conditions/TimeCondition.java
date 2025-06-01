package com.x1f4r.mmocraft.world.spawning.conditions;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * A spawn condition based on the current world time.
 * Time is measured in Bukkit ticks (0-23999).
 * 0 = sunrise, 6000 = noon, 12000 = sunset, 18000 = midnight.
 */
public class TimeCondition implements SpawnCondition {

    private final long minTimeTicks; // Inclusive
    private final long maxTimeTicks; // Inclusive

    /**
     * Creates a time condition.
     * If minTimeTicks > maxTimeTicks, it implies a time range that wraps around midnight
     * (e.g., spawn only at night: minTime=13000, maxTime=23000, or minTime=13000, maxTime=1000 for wrap).
     *
     * @param minTimeTicks Minimum world time in ticks for spawning (inclusive).
     * @param maxTimeTicks Maximum world time in ticks for spawning (inclusive).
     */
    public TimeCondition(long minTimeTicks, long maxTimeTicks) {
        this.minTimeTicks = Math.max(0, minTimeTicks % 24000); // Normalize to 0-23999 range
        this.maxTimeTicks = Math.max(0, maxTimeTicks % 24000);
    }

    @Override
    public boolean check(Location location, World world, Player nearestPlayer) {
        if (world == null) {
            return false;
        }
        long currentTime = world.getTime();

        if (minTimeTicks <= maxTimeTicks) {
            // Standard range (e.g., 0-6000 for daytime)
            return currentTime >= minTimeTicks && currentTime <= maxTimeTicks;
        } else {
            // Wrapped range (e.g., 18000-6000 for night time, crossing midnight)
            return currentTime >= minTimeTicks || currentTime <= maxTimeTicks;
        }
    }

    public long getMinTimeTicks() {
        return minTimeTicks;
    }

    public long getMaxTimeTicks() {
        return maxTimeTicks;
    }
}
