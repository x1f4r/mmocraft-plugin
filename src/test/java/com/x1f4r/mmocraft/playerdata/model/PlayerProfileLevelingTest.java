package com.x1f4r.mmocraft.playerdata.model;

import com.x1f4r.mmocraft.playerdata.util.ExperienceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

class PlayerProfileLevelingTest {

    private PlayerProfile profile;

    @BeforeEach
    void setUp() {
        profile = new PlayerProfile(UUID.randomUUID(), "LevelTestPlayer");
        // Initial level is 1, experience 0.
    }

    @Test
    void getExperienceToNextLevel_forLevel1_shouldMatchUtil() {
        assertEquals(ExperienceUtil.getXPForNextLevel(1), profile.getExperienceToNextLevel());
        assertEquals(100L, profile.getExperienceToNextLevel()); // Assuming Base 100, Lvl 1, Exp 1.5
    }

    @Test
    void getExperienceToNextLevel_forHigherLevel_shouldMatchUtil() {
        profile.setLevel(10); // This also calls recalculateDerivedAttributes
        assertEquals(ExperienceUtil.getXPForNextLevel(10), profile.getExperienceToNextLevel());
    }

    @Test
    void getExperienceToNextLevel_atMaxLevel_shouldReturnLongMaxValue() {
        profile.setLevel(ExperienceUtil.getMaxLevel());
        assertEquals(Long.MAX_VALUE, profile.getExperienceToNextLevel());
    }

    @Test
    void getExperienceToNextLevel_aboveMaxLevel_shouldStillReturnLongMaxValue() {
        // PlayerProfile setLevel should cap at MaxLevel, but if somehow it went over:
        // For this test, we'll assume level can be set higher to test ExperienceUtil via profile.
        // However, PlayerProfile.setLevel already caps at ExperienceUtil.getMaxLevel().
        // So this test is effectively the same as the one above.
        profile.setLevel(ExperienceUtil.getMaxLevel() + 5);
        assertEquals(ExperienceUtil.getMaxLevel(), profile.getLevel(), "Level should be capped at MaxLevel.");
        assertEquals(Long.MAX_VALUE, profile.getExperienceToNextLevel());
    }

    @Test
    void constructor_newPlayer_initializesLevelAndExperienceCorrectly() {
        assertEquals(1, profile.getLevel());
        assertEquals(0, profile.getExperience());
    }
}
