package com.x1f4r.mmocraft.world.spawning.conditions;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeConditionTest {

    @Mock private Location mockLocation; // Not directly used by TimeCondition logic itself
    @Mock private World mockWorld;
    @Mock private Player mockPlayer; // Not directly used

    @Test
    void check_currentTimeWithinStandardRange_returnsTrue() {
        when(mockWorld.getTime()).thenReturn(6000L); // Noon
        TimeCondition condition = new TimeCondition(0L, 12000L); // Daytime (0 to 12000 sunset)
        assertTrue(condition.check(mockLocation, mockWorld, mockPlayer));
    }

    @Test
    void check_currentTimeOutsideStandardRange_returnsFalse() {
        when(mockWorld.getTime()).thenReturn(18000L); // Midnight
        TimeCondition condition = new TimeCondition(0L, 12000L); // Daytime
        assertFalse(condition.check(mockLocation, mockWorld, mockPlayer));
    }

    @Test
    void check_currentTimeWithinWrappedRange_returnsTrue() {
        TimeCondition nightCondition = new TimeCondition(13000L, 23000L); // Typical night
        // Test times within this wrapped range
        when(mockWorld.getTime()).thenReturn(18000L); // Midnight
        assertTrue(nightCondition.check(mockLocation, mockWorld, mockPlayer));

        // Test a wrapped range like 22000 (late night) to 2000 (early morning)
        TimeCondition lateNightToEarlyMorning = new TimeCondition(22000L, 2000L);
        when(mockWorld.getTime()).thenReturn(23000L); // Late night
        assertTrue(lateNightToEarlyMorning.check(mockLocation, mockWorld, mockPlayer), "Late night should match");
        when(mockWorld.getTime()).thenReturn(1000L); // Early morning
        assertTrue(lateNightToEarlyMorning.check(mockLocation, mockWorld, mockPlayer), "Early morning should match");
    }

    @Test
    void check_currentTimeOutsideWrappedRange_returnsFalse() {
        TimeCondition lateNightToEarlyMorning = new TimeCondition(22000L, 2000L);
        when(mockWorld.getTime()).thenReturn(12000L); // Mid-day
        assertFalse(lateNightToEarlyMorning.check(mockLocation, mockWorld, mockPlayer));
    }

    @Test
    void check_currentTimeAtBoundaries_isInclusive() {
        TimeCondition condition = new TimeCondition(1000L, 2000L);
        when(mockWorld.getTime()).thenReturn(1000L);
        assertTrue(condition.check(mockLocation, mockWorld, mockPlayer));
        when(mockWorld.getTime()).thenReturn(2000L);
        assertTrue(condition.check(mockLocation, mockWorld, mockPlayer));

        TimeCondition wrappedCondition = new TimeCondition(22000L, 2000L);
        when(mockWorld.getTime()).thenReturn(22000L);
        assertTrue(wrappedCondition.check(mockLocation, mockWorld, mockPlayer), "Min boundary of wrapped");
        when(mockWorld.getTime()).thenReturn(2000L);
        assertTrue(wrappedCondition.check(mockLocation, mockWorld, mockPlayer), "Max boundary of wrapped");
    }

    @Test
    void constructor_normalizesTimes() {
        TimeCondition condition = new TimeCondition(25000L, 26000L); //Equivalent to 1000L, 2000L
        assertEquals(1000L, condition.getMinTimeTicks());
        assertEquals(2000L, condition.getMaxTimeTicks());

        when(mockWorld.getTime()).thenReturn(1500L);
        assertTrue(condition.check(mockLocation, mockWorld, mockPlayer));
    }

    @Test
    void check_nullWorld_returnsFalse() {
        TimeCondition condition = new TimeCondition(0L, 1000L);
        assertFalse(condition.check(mockLocation, null, mockPlayer));
    }
}
