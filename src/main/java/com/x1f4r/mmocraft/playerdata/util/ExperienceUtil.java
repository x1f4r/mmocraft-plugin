package com.x1f4r.mmocraft.playerdata.util;

/**
 * Utility class for experience and leveling calculations.
 */
public class ExperienceUtil {

    private static final double BASE_XP_REQUIRED = 100.0;
    private static final double LEVEL_EXPONENT_FACTOR = 1.5;
    private static final int MAX_LEVEL = 100; // Example maximum level
    private static final int MIN_LEVEL = 1;

    /**
     * Calculates the total experience points needed to reach a specific level
     * from the very beginning (level 1, 0 XP).
     * For example, getXPForLevel(2) is the XP needed to go from level 1 to level 2.
     * getXPForLevel(1) is the XP needed to reach level 1 (which is 0).
     *
     * @param level The target level.
     * @return The total experience points required to achieve that level.
     */
    public static long getTotalXPForLevel(int level) {
        if (level <= MIN_LEVEL) {
            return 0; // No XP required to reach or be at level 1.
        }
        if (level > MAX_LEVEL) {
            level = MAX_LEVEL;
        }
        // This formula calculates XP needed to *reach* 'level' from level 1.
        // Example: Level 2 needs 100 * (1^1.5) = 100 (this is XP to go from 1 to 2)
        // Level 3 needs 100 * (2^1.5) approx 282 (this is XP to go from 1 to 3, so from 2 to 3 is 282-100)
        // The formula represents the XP threshold for the *previous* level to advance to the current one.
        // So, XP for level 'L' means XP accumulated upon *completing* level 'L-1'.
        return (long) (BASE_XP_REQUIRED * Math.pow(level - 1, LEVEL_EXPONENT_FACTOR));
    }

    /**
     * Calculates the amount of experience needed to advance from the given currentLevel
     * to currentLevel + 1.
     *
     * @param currentLevel The current level of the player.
     * @return The amount of experience points needed to reach the next level.
     *         Returns Long.MAX_VALUE if already at max level.
     */
    public static long getXPForNextLevel(int currentLevel) {
        if (currentLevel < MIN_LEVEL) {
            currentLevel = MIN_LEVEL; // Should not happen with proper level management
        }
        if (currentLevel >= MAX_LEVEL) {
            return Long.MAX_VALUE; // Cannot gain more XP or effectively infinite
        }
        // XP needed to go from currentLevel to currentLevel + 1
        // is the total XP for (currentLevel + 1) minus total XP for currentLevel.
        // However, a simpler common approach is that getXPForLevel(N) is the XP needed to complete level N-1 and reach N.
        // Let's use a common game dev approach:
        // XP to reach level L = Base * (L-1)^Exponent
        // This means experienceToNextLevel = XP to reach (currentLevel + 1)

        // If getXPForLevel(level) means "XP needed to complete this level and ding next":
        // e.g. getXPForLevel(1) is XP needed to go from 1 -> 2
        //      getXPForLevel(2) is XP needed to go from 2 -> 3
        if (currentLevel >= MAX_LEVEL) {
            return Long.MAX_VALUE;
        }
        if (currentLevel < MIN_LEVEL) { // Should not happen
             return (long) (BASE_XP_REQUIRED * Math.pow(MIN_LEVEL, LEVEL_EXPONENT_FACTOR));
        }
        // XP to complete 'currentLevel' and reach 'currentLevel + 1'
        return (long) (BASE_XP_REQUIRED * Math.pow(currentLevel, LEVEL_EXPONENT_FACTOR));
    }


    /**
     * Gets the defined maximum player level.
     * @return The maximum level.
     */
    public static int getMaxLevel() {
        return MAX_LEVEL;
    }

    /**
     * Gets the defined minimum player level.
     * @return The minimum level (typically 1).
     */
    public static int getMinLevel() {
        return MIN_LEVEL;
    }
}
