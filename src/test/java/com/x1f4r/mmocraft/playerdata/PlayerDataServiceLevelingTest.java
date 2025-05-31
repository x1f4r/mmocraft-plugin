package com.x1f4r.mmocraft.playerdata;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.eventbus.EventBusService;
import com.x1f4r.mmocraft.persistence.PersistenceService;
import com.x1f4r.mmocraft.playerdata.events.PlayerLevelUpEvent;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.util.ExperienceUtil;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerDataServiceLevelingTest {

    @Mock private MMOCraftPlugin mockPlugin;
    @Mock private PersistenceService mockPersistenceService;
    @Mock private LoggingUtil mockLogger;
    @Mock private EventBusService mockEventBusService;

    @Captor private ArgumentCaptor<PlayerLevelUpEvent> levelUpEventCaptor;

    private BasicPlayerDataService playerDataService;
    private UUID testPlayerUUID;
    private String testPlayerName;
    private PlayerProfile testProfile;

    @BeforeEach
    void setUp() {
        playerDataService = new BasicPlayerDataService(mockPlugin, mockPersistenceService, mockLogger, mockEventBusService);
        testPlayerUUID = UUID.randomUUID();
        testPlayerName = "Leveler";

        // Setup a base profile and cache it for tests
        testProfile = new PlayerProfile(testPlayerUUID, testPlayerName); // Starts at level 1, 0 XP
        playerDataService.cachePlayerProfile(testProfile);
    }

    @Test
    void addExperience_notEnoughToLevelUp_updatesExperience() {
        long xpToAdd = 50;
        long initialXP = testProfile.getExperience();

        playerDataService.addExperience(testPlayerUUID, xpToAdd);

        assertEquals(initialXP + xpToAdd, testProfile.getExperience());
        assertEquals(1, testProfile.getLevel()); // Still level 1
        verify(mockEventBusService, never()).call(any(PlayerLevelUpEvent.class));
    }

    @Test
    void addExperience_exactAmountToLevelUp_levelsUpOnce() {
        long xpToLevel2 = ExperienceUtil.getXPForNextLevel(1); // e.g., 100

        playerDataService.addExperience(testPlayerUUID, xpToLevel2);

        assertEquals(2, testProfile.getLevel());
        assertEquals(0, testProfile.getExperience()); // Exact amount consumed
        verify(mockEventBusService, times(1)).call(levelUpEventCaptor.capture());
        PlayerLevelUpEvent event = levelUpEventCaptor.getValue();
        assertEquals(testPlayerUUID, event.getPlayerUUID());
        assertEquals(1, event.getOldLevel());
        assertEquals(2, event.getNewLevel());
        assertNotNull(event.getProfileSnapshot());
        assertEquals(2, event.getProfileSnapshot().getLevel());
    }

    @Test
    void addExperience_moreThanEnoughToLevelUpOnce_levelsUpAndHasRemainderXP() {
        long xpToLevel2 = ExperienceUtil.getXPForNextLevel(1); // 100
        long extraXP = 50;

        playerDataService.addExperience(testPlayerUUID, xpToLevel2 + extraXP);

        assertEquals(2, testProfile.getLevel());
        assertEquals(extraXP, testProfile.getExperience());
        verify(mockEventBusService, times(1)).call(any(PlayerLevelUpEvent.class));
    }

    @Test
    void addExperience_enoughToLevelUpMultipleTimes() {
        // XP for L1->L2 = 100 * 1^1.5 = 100
        // XP for L2->L3 = 100 * 2^1.5 = 282
        // Total for L1->L3 = 100 + 282 = 382
        long xpToLevel3 = ExperienceUtil.getXPForNextLevel(1) + ExperienceUtil.getXPForNextLevel(2);
        long remainder = 20;

        playerDataService.addExperience(testPlayerUUID, xpToLevel3 + remainder);

        assertEquals(3, testProfile.getLevel());
        assertEquals(remainder, testProfile.getExperience());
        verify(mockEventBusService, times(2)).call(levelUpEventCaptor.capture());

        PlayerLevelUpEvent firstEvent = levelUpEventCaptor.getAllValues().get(0);
        assertEquals(1, firstEvent.getOldLevel());
        assertEquals(2, firstEvent.getNewLevel());

        PlayerLevelUpEvent secondEvent = levelUpEventCaptor.getAllValues().get(1);
        assertEquals(2, secondEvent.getOldLevel());
        assertEquals(3, secondEvent.getNewLevel());
    }

    @Test
    void addExperience_atMaxLevel_doesNotAddXPOrLevelUp() {
        testProfile.setLevel(ExperienceUtil.getMaxLevel());
        testProfile.setExperience(0); // Set to 0 for clarity at max level
        // PlayerProfile's setLevel calls recalculate, so derived stats are fine.

        playerDataService.addExperience(testPlayerUUID, 1000);

        assertEquals(ExperienceUtil.getMaxLevel(), testProfile.getLevel());
        assertEquals(0, testProfile.getExperience()); // Should remain 0 or be reset to 0
        verify(mockEventBusService, never()).call(any(PlayerLevelUpEvent.class));
        verify(mockLogger).fine(contains("is at max level. No XP gained."));
    }

    @Test
    void addExperience_levelUpToMaxLevel_setsXPToZero() {
        // Set player to one level below max, with almost enough XP to level
        int levelBeforeMax = ExperienceUtil.getMaxLevel() - 1;
        testProfile.setLevel(levelBeforeMax);
        long xpForNext = ExperienceUtil.getXPForNextLevel(levelBeforeMax);
        testProfile.setExperience(xpForNext - 1);

        playerDataService.addExperience(testPlayerUUID, 1); // Add 1 XP to hit max level

        assertEquals(ExperienceUtil.getMaxLevel(), testProfile.getLevel());
        assertEquals(0, testProfile.getExperience()); // XP should be zeroed out at max level
        verify(mockEventBusService, times(1)).call(any(PlayerLevelUpEvent.class));
        verify(mockLogger).info(contains("reached MAX LEVEL"));
    }


    @Test
    void addExperience_profileNotCached_logsWarningAndReturns() {
        UUID nonCachedUUID = UUID.randomUUID();
        playerDataService.addExperience(nonCachedUUID, 100);

        verify(mockLogger).warning("Cannot add experience: PlayerProfile not found in cache for UUID " + nonCachedUUID);
        verify(mockEventBusService, never()).call(any());
    }

    @Test
    void addExperience_negativeAmount_logsFineAndReturns() {
        playerDataService.addExperience(testPlayerUUID, -50);

        assertEquals(0, testProfile.getExperience()); // XP should not change
        assertEquals(1, testProfile.getLevel());       // Level should not change
        verify(mockLogger).fine(contains("Attempted to add non-positive XP"));
        verify(mockEventBusService, never()).call(any());
    }
}
