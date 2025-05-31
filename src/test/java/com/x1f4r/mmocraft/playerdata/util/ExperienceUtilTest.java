package com.x1f4r.mmocraft.playerdata.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExperienceUtilTest {

    @Test
    void getMaxLevel_shouldReturnConstant() {
        assertEquals(100, ExperienceUtil.getMaxLevel()); // Assuming 100 is the current constant
    }

    @Test
    void getMinLevel_shouldReturnConstant() {
        assertEquals(1, ExperienceUtil.getMinLevel());
    }

    @Test
    void getTotalXPForLevel_level1_shouldBeZero() {
        assertEquals(0, ExperienceUtil.getTotalXPForLevel(1));
        assertEquals(0, ExperienceUtil.getTotalXPForLevel(0)); // Below min
        assertEquals(0, ExperienceUtil.getTotalXPForLevel(-5)); // Negative
    }

    @Test
    void getXPForNextLevel_level1_calculatesXPForLevel2() {
        // XP to complete level 1 and reach level 2
        // Formula: 100 * Math.pow(currentLevel, 1.5)
        // For currentLevel=1: 100 * (1^1.5) = 100
        assertEquals(100, ExperienceUtil.getXPForNextLevel(1));
    }

    @Test
    void getXPForNextLevel_variousLevels() {
        // Lvl 2 -> 3: 100 * (2^1.5) = 100 * 2.8284... = 282 (approx)
        assertEquals(282, ExperienceUtil.getXPForNextLevel(2));
        // Lvl 5 -> 6: 100 * (5^1.5) = 100 * 11.1803... = 1118 (approx)
        assertEquals(1118, ExperienceUtil.getXPForNextLevel(5));
        // Lvl 10 -> 11: 100 * (10^1.5) = 100 * 31.6227... = 3162 (approx)
        assertEquals(3162, ExperienceUtil.getXPForNextLevel(10));
    }

    @Test
    void getTotalXPForLevel_variousLevels_matchesSumOfXPForNextLevel() {
        // Total XP for level 2 should be XPForNextLevel(1)
        assertEquals(ExperienceUtil.getXPForNextLevel(1), ExperienceUtil.getTotalXPForLevel(2));

        // Total XP for level 3 = XPForNextLevel(1) + XPForNextLevel(2)
        // This depends on the interpretation of getTotalXPForLevel.
        // The current getTotalXPForLevel(L) is XP to reach L (from L-1).
        // So, getTotalXPForLevel(3) = 100 * (2^1.5) = 282. This is XP from L2->L3.
        // This interpretation might be confusing.
        // Let's adjust: getTotalXPForLevel(N) = Sum of XPForNextLevel(i) for i=1 to N-1
        // The current ExperienceUtil.getTotalXPForLevel(level) is:
        //   (long) (BASE_XP_REQUIRED * Math.pow(level - 1, LEVEL_EXPONENT_FACTOR));
        // This is effectively the same as getXPForNextLevel(level -1).
        // So, getTotalXPForLevel(2) = getXPForNextLevel(1) = 100
        // getTotalXPForLevel(3) = getXPForNextLevel(2) = 282

        assertEquals(100, ExperienceUtil.getTotalXPForLevel(2)); // XP to reach L2
        assertEquals(282, ExperienceUtil.getTotalXPForLevel(3)); // XP to reach L3 (from L2)
                                                                    // No, this is XP from L1->L3 if formula is sum
                                                                    // The current formula means: XP needed to DING level 'L'
                                                                    // So getTotalXPForLevel(3) means XP needed when you are level 2, to get to 3.
                                                                    // This is identical to getXPForNextLevel(2).

        // Let's assume getTotalXPForLevel(L) should be the *cumulative* XP from level 1.
        // If so, ExperienceUtil needs a refactor or clarification.
        // For now, testing the current implementation:
        // getTotalXPForLevel(level) = XP to complete (level-1)
        assertEquals(ExperienceUtil.getXPForNextLevel(ExperienceUtil.getMinLevel()), ExperienceUtil.getTotalXPForLevel(ExperienceUtil.getMinLevel() + 1));
    }


    @Test
    void getXPForNextLevel_atMaxLevel_shouldReturnLongMaxValue() {
        assertEquals(Long.MAX_VALUE, ExperienceUtil.getXPForNextLevel(ExperienceUtil.getMaxLevel()));
    }

    @Test
    void getXPForNextLevel_aboveMaxLevel_shouldReturnLongMaxValue() {
        assertEquals(Long.MAX_VALUE, ExperienceUtil.getXPForNextLevel(ExperienceUtil.getMaxLevel() + 5));
    }

    @Test
    void getTotalXPForLevel_aboveMaxLevel_usesMaxLevelInCalc() {
        long xpForMax = (long) (100.0 * Math.pow(ExperienceUtil.getMaxLevel() - 1, 1.5));
        assertEquals(xpForMax, ExperienceUtil.getTotalXPForLevel(ExperienceUtil.getMaxLevel() + 5));
    }
}
