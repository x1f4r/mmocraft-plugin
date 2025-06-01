package com.x1f4r.mmocraft.world.spawning.model;

import com.x1f4r.mmocraft.world.spawning.conditions.SpawnCondition;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomSpawnRuleTest {

    @Mock private MobSpawnDefinition mockDefinition;
    @Mock private SpawnCondition mockCondition1;
    @Mock private SpawnCondition mockCondition2;
    @Mock private Location mockLocation;
    @Mock private World mockWorld;
    @Mock private Player mockPlayer;

    private List<SpawnCondition> conditions;

    @BeforeEach
    void setUp() {
        conditions = new ArrayList<>();
        // Default mock behavior for conditions to pass
        lenient().when(mockCondition1.check(any(), any(), any())).thenReturn(true);
        lenient().when(mockCondition2.check(any(), any(), any())).thenReturn(true);

        lenient().when(mockLocation.getBlockY()).thenReturn(100); // Default Y for height checks
    }

    @Test
    void constructor_validArgs_createsInstance() {
        conditions.add(mockCondition1);
        CustomSpawnRule rule = new CustomSpawnRule("rule1", mockDefinition, conditions, 0.5, 60, 120, 5, 32.0, 100L);
        assertEquals("rule1", rule.getRuleId());
        assertSame(mockDefinition, rule.getMobSpawnDefinition());
        assertEquals(1, rule.getConditions().size());
        assertEquals(0.5, rule.getSpawnChance());
        assertEquals(60, rule.getMinSpawnHeight());
        assertEquals(120, rule.getMaxSpawnHeight());
        assertEquals(5, rule.getMaxNearbyEntities());
        assertEquals(32.0, rule.getSpawnRadiusCheck());
        assertEquals(100L, rule.getSpawnIntervalTicks());
        assertEquals(0, rule.getLastSpawnAttemptTickGlobal()); // Initialized to 0
    }

    // Add tests for constructor argument validation (e.g., negative spawnChance, invalid heights)

    @Test
    void conditionsMet_allConditionsTrueAndHeightValid_returnsTrue() {
        conditions.add(mockCondition1);
        conditions.add(mockCondition2);
        CustomSpawnRule rule = new CustomSpawnRule("rule_all_pass", mockDefinition, conditions, 0.1, 50, 150, 3, 16, 20);

        assertTrue(rule.conditionsMet(mockLocation, mockWorld, mockPlayer));
        verify(mockCondition1).check(mockLocation, mockWorld, mockPlayer);
        verify(mockCondition2).check(mockLocation, mockWorld, mockPlayer);
    }

    @Test
    void conditionsMet_oneConditionFalse_returnsFalse() {
        when(mockCondition2.check(any(), any(), any())).thenReturn(false); // Condition 2 fails
        conditions.add(mockCondition1);
        conditions.add(mockCondition2);
        CustomSpawnRule rule = new CustomSpawnRule("rule_one_fail", mockDefinition, conditions, 0.1, 50, 150, 3, 16, 20);

        assertFalse(rule.conditionsMet(mockLocation, mockWorld, mockPlayer));
        verify(mockCondition1).check(mockLocation, mockWorld, mockPlayer); // Condition 1 is checked
        verify(mockCondition2).check(mockLocation, mockWorld, mockPlayer); // Condition 2 is checked (and fails)
    }

    @Test
    void conditionsMet_heightBelowMin_returnsFalse() {
        when(mockLocation.getBlockY()).thenReturn(40);
        conditions.add(mockCondition1);
        CustomSpawnRule rule = new CustomSpawnRule("rule_height_low", mockDefinition, conditions, 0.1, 50, 150, 3, 16, 20);
        assertFalse(rule.conditionsMet(mockLocation, mockWorld, mockPlayer));
        verify(mockCondition1, never()).check(any(),any(),any()); // Condition check should be skipped
    }

    @Test
    void conditionsMet_heightAboveMax_returnsFalse() {
        when(mockLocation.getBlockY()).thenReturn(160);
        conditions.add(mockCondition1);
        CustomSpawnRule rule = new CustomSpawnRule("rule_height_high", mockDefinition, conditions, 0.1, 50, 150, 3, 16, 20);
        assertFalse(rule.conditionsMet(mockLocation, mockWorld, mockPlayer));
         verify(mockCondition1, never()).check(any(),any(),any());
    }


    @Test
    void isReadyToAttemptSpawn_initialState_returnsTrue() {
        CustomSpawnRule rule = new CustomSpawnRule("rule_interval", mockDefinition, conditions, 0.1, 0, 255, 5, 32, 100L);
        assertTrue(rule.isReadyToAttemptSpawn(0L)); // currentTick = 0, lastSpawn = 0
        assertTrue(rule.isReadyToAttemptSpawn(99L));
    }

    @Test
    void isReadyToAttemptSpawn_afterInterval_returnsTrue() {
        CustomSpawnRule rule = new CustomSpawnRule("rule_interval", mockDefinition, conditions, 0.1, 0, 255, 5, 32, 100L);
        rule.setLastSpawnAttemptTickGlobal(50L);
        assertTrue(rule.isReadyToAttemptSpawn(150L)); // 50 + 100 = 150
    }

    @Test
    void isReadyToAttemptSpawn_beforeInterval_returnsFalse() {
        CustomSpawnRule rule = new CustomSpawnRule("rule_interval", mockDefinition, conditions, 0.1, 0, 255, 5, 32, 100L);
        rule.setLastSpawnAttemptTickGlobal(50L);
        assertFalse(rule.isReadyToAttemptSpawn(149L)); // 50 + 100 > 149
    }

    @Test
    void rollForSpawn_chancePoint5_roughly50PercentSuccessOverManyRolls() {
        CustomSpawnRule rule = new CustomSpawnRule("roll_test", mockDefinition, conditions, 0.5, 0, 255, 1, 1, 0);
        int successes = 0;
        int attempts = 10000;
        for (int i = 0; i < attempts; i++) {
            if (rule.rollForSpawn()) {
                successes++;
            }
        }
        // Check if success rate is within a reasonable margin of 0.5 (e.g., 0.4 to 0.6)
        double successRate = (double) successes / attempts;
        assertTrue(successRate > 0.4 && successRate < 0.6,
                   "Success rate " + successRate + " not within expected range for 0.5 chance.");
    }

    @Test
    void rollForSpawn_chance1_alwaysSucceeds() {
        CustomSpawnRule rule = new CustomSpawnRule("roll_test_1", mockDefinition, conditions, 1.0, 0, 255, 1, 1, 0);
        assertTrue(rule.rollForSpawn());
    }

    @Test
    void rollForSpawn_chance0_neverSucceeds() {
        CustomSpawnRule rule = new CustomSpawnRule("roll_test_0", mockDefinition, conditions, 0.0, 0, 255, 1, 1, 0);
        assertFalse(rule.rollForSpawn());
    }
}
