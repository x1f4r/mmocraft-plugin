package com.x1f4r.mmocraft.statuseffect.manager;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.statuseffect.model.ActiveStatusEffect;
import com.x1f4r.mmocraft.statuseffect.model.StatusEffect;
import com.x1f4r.mmocraft.statuseffect.model.StatusEffectType;
import com.x1f4r.mmocraft.util.LoggingUtil;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicStatusEffectManagerTest {

    @Mock private MMOCraftPlugin mockPlugin;
    @Mock private LoggingUtil mockLogger;
    @Mock private PlayerDataService mockPlayerDataService;
    @Mock private LivingEntity mockTargetEntity;
    @Mock private Player mockTargetPlayer;
    @Mock private PlayerProfile mockPlayerProfile;
    @Mock private Server mockServer; // For Bukkit.getEntity

    private BasicStatusEffectManager statusEffectManager;
    private UUID targetEntityUUID = UUID.randomUUID();
    private UUID targetPlayerUUID = UUID.randomUUID();

    private MockedStatic<Bukkit> mockedBukkit;


    // Dummy StatusEffect for testing
    private static class TestStatusEffect extends StatusEffect {
        boolean applied = false;
        boolean ticked = false;
        boolean expired = false;
        boolean removed = false;
        boolean statModified = false;

        public TestStatusEffect(MMOCraftPlugin plugin, StatusEffectType type, double duration, double tickInterval, double potency) {
            super(plugin, type, duration, potency, tickInterval, null);
        }

        public TestStatusEffect(MMOCraftPlugin plugin, StatusEffectType type, double duration, double tickInterval) {
            this(plugin, type, duration, tickInterval, 0);
        }

        @Override public void onApply(LivingEntity target, PlayerProfile profile) { applied = true;
            if (this.effectType == StatusEffectType.STAT_BUFF_STRENGTH && profile != null) {
                profile.setStatValue(Stat.STRENGTH, profile.getStatValue(Stat.STRENGTH) + potency);
                statModified = true;
            }
        }
        @Override public void onTick(LivingEntity target, PlayerProfile profile) { ticked = true; }
        @Override public void onExpire(LivingEntity target, PlayerProfile profile) { expired = true;
            if (this.effectType == StatusEffectType.STAT_BUFF_STRENGTH && profile != null && statModified) {
                 profile.setStatValue(Stat.STRENGTH, profile.getStatValue(Stat.STRENGTH) - potency);
            }
        }
        @Override public void onRemove(LivingEntity target, PlayerProfile profile) {
            removed = true;
            // Typically delegates to onExpire for cleanup for non- razlikovanje
            if (this.effectType == StatusEffectType.STAT_BUFF_STRENGTH && profile != null && statModified) {
                 profile.setStatValue(Stat.STRENGTH, profile.getStatValue(Stat.STRENGTH) - potency);
            }
        }
    }

    @BeforeEach
    void setUp() {
        //MockitoAnnotations.openMocks(this); // For non-ExtendWith usage
        statusEffectManager = new BasicStatusEffectManager(mockPlugin, mockLogger, mockPlayerDataService);

        when(mockTargetEntity.getUniqueId()).thenReturn(targetEntityUUID);
        when(mockTargetEntity.getName()).thenReturn("TestMob");
        lenient().when(mockTargetEntity.isDead()).thenReturn(false);


        when(mockTargetPlayer.getUniqueId()).thenReturn(targetPlayerUUID);
        when(mockTargetPlayer.getName()).thenReturn("TestPlayer");
        lenient().when(mockTargetPlayer.isDead()).thenReturn(false);
        lenient().when(mockPlayerDataService.getPlayerProfile(targetPlayerUUID)).thenReturn(mockPlayerProfile);

        // Mock Bukkit.getEntity() which is used in tickAllActiveEffects
        mockedBukkit = mockStatic(Bukkit.class);
        lenient().when(Bukkit.getEntity(targetEntityUUID)).thenReturn(mockTargetEntity);
        lenient().when(Bukkit.getEntity(targetPlayerUUID)).thenReturn(mockTargetPlayer);

    }

    @AfterEach
    void tearDown() {
        mockedBukkit.close(); // Close static mock
    }


    @Test
    void applyEffect_newEffect_appliesAndCallsOnApply() {
        TestStatusEffect effect = new TestStatusEffect(mockPlugin, StatusEffectType.POISON, 10, 1);
        statusEffectManager.applyEffect(mockTargetEntity, effect);

        assertTrue(effect.applied);
        List<ActiveStatusEffect> active = statusEffectManager.getActiveEffectsOnEntity(mockTargetEntity);
        assertEquals(1, active.size());
        assertSame(effect, active.get(0).getStatusEffect());
        verify(mockLogger).fine("Applied status effect POISON to TestMob");
    }

    @Test
    void applyEffect_statBuff_callsRecalculateOnProfile() {
        TestStatusEffect statBuff = new TestStatusEffect(mockPlugin, StatusEffectType.STAT_BUFF_STRENGTH, 10, 0, 5.0);
        // Simulate getStatValue before and after for mockPlayerProfile
        when(mockPlayerProfile.getStatValue(Stat.STRENGTH)).thenReturn(10.0).thenReturn(15.0);

        statusEffectManager.applyEffect(mockTargetPlayer, statBuff);

        assertTrue(statBuff.applied);
        assertTrue(statBuff.statModified);
        verify(mockPlayerProfile).setStatValue(Stat.STRENGTH, 15.0); // 10 (original) + 5 (potency)
        verify(mockPlayerProfile, times(1)).recalculateDerivedAttributes(); // Called due to setStatValue
    }


    @Test
    void removeEffect_removesAndCallsOnRemove() {
        TestStatusEffect effect = new TestStatusEffect(mockPlugin, StatusEffectType.POISON, 10, 1);
        statusEffectManager.applyEffect(mockTargetEntity, effect);
        assertTrue(statusEffectManager.hasEffect(mockTargetEntity, StatusEffectType.POISON));

        statusEffectManager.removeEffect(mockTargetEntity, StatusEffectType.POISON);

        assertTrue(effect.removed || effect.expired); // onRemove often calls onExpire
        assertFalse(statusEffectManager.hasEffect(mockTargetEntity, StatusEffectType.POISON));
        verify(mockLogger).fine("Removed status effect POISON from TestMob");
    }

    @Test
    void removeEffect_statBuff_revertsStatAndRecalculates() {
        TestStatusEffect statBuff = new TestStatusEffect(mockPlugin, StatusEffectType.STAT_BUFF_STRENGTH, 10, 0, 5.0);
        // Initial stat value
        when(mockPlayerProfile.getStatValue(Stat.STRENGTH)).thenReturn(10.0);
        statusEffectManager.applyEffect(mockTargetPlayer, statBuff); // Applies 10+5=15

        // Mock return value for when stat is reverted
        when(mockPlayerProfile.getStatValue(Stat.STRENGTH)).thenReturn(15.0); // Value before removing potency

        statusEffectManager.removeEffect(mockTargetPlayer, StatusEffectType.STAT_BUFF_STRENGTH);

        assertTrue(statBuff.removed || statBuff.expired);
        verify(mockPlayerProfile).setStatValue(Stat.STRENGTH, 10.0); // 15 (current) - 5 (potency)
        // recalculate is called twice: once for apply (via setStatValue), once for remove (via setStatValue)
        verify(mockPlayerProfile, times(2)).recalculateDerivedAttributes();
    }


    @Test
    void tickAllActiveEffects_callsOnTickForReadyEffects() {
        TestStatusEffect tickingEffect = new TestStatusEffect(mockPlugin, StatusEffectType.HEALTH_REGEN, 10, 0.01); // Ticks very fast
        statusEffectManager.applyEffect(mockTargetEntity, tickingEffect);

        // Simulate time passing enough for a tick
        try { TimeUnit.MILLISECONDS.sleep(20); } catch (InterruptedException ignored) {}

        statusEffectManager.tickAllActiveEffects();
        assertTrue(tickingEffect.ticked);
        verify(mockLogger, atLeastOnce()).finest(contains("Ticked status effect HEALTH_REGEN"));
    }

    @Test
    void tickAllActiveEffects_removesExpiredEffectsAndCallsOnExpire() {
        TestStatusEffect shortEffect = new TestStatusEffect(mockPlugin, StatusEffectType.SLOW, 0.01, 0); // Expires very fast
        statusEffectManager.applyEffect(mockTargetEntity, shortEffect);

        // Simulate time passing enough for expiry
        try { TimeUnit.MILLISECONDS.sleep(20); } catch (InterruptedException ignored) {}

        statusEffectManager.tickAllActiveEffects();
        assertTrue(shortEffect.expired);
        assertFalse(statusEffectManager.hasEffect(mockTargetEntity, StatusEffectType.SLOW));
        verify(mockLogger).finer(contains("Expired status effect SLOW"));
    }

    @Test
    void tickAllActiveEffects_targetDeadOrInvalid_clearsEffects() {
        TestStatusEffect effect = new TestStatusEffect(mockPlugin, StatusEffectType.POISON, 10, 1);
        statusEffectManager.applyEffect(mockTargetEntity, effect); // Target is mockTargetEntity
        assertTrue(statusEffectManager.hasEffect(mockTargetEntity, StatusEffectType.POISON));

        // Make Bukkit.getEntity return null for this UUID, simulating player logged off or entity removed
        mockedBukkit.when(() -> Bukkit.getEntity(targetEntityUUID)).thenReturn(null);

        statusEffectManager.tickAllActiveEffects();

        assertFalse(statusEffectManager.hasEffect(mockTargetEntity, StatusEffectType.POISON)); // Effects should be gone
        assertTrue(statusEffectManager.getActiveEffectsOnEntity(mockTargetEntity).isEmpty());
        verify(mockLogger).fine("Target UUID " + targetEntityUUID + " no longer valid, clearing effects.");
    }


    @Test
    void shutdown_clearsAllEffectsAndCallsRemove() {
        TestStatusEffect effect1OnTarget1 = new TestStatusEffect(mockPlugin, StatusEffectType.POISON, 10, 1);
        TestStatusEffect effect2OnTarget1 = new TestStatusEffect(mockPlugin, StatusEffectType.STUN, 5, 0);
        TestStatusEffect effect1OnTarget2 = new TestStatusEffect(mockPlugin, StatusEffectType.HEALTH_REGEN, 10, 1);

        statusEffectManager.applyEffect(mockTargetEntity, effect1OnTarget1);
        statusEffectManager.applyEffect(mockTargetEntity, effect2OnTarget1);
        statusEffectManager.applyEffect(mockTargetPlayer, effect1OnTarget2);

        statusEffectManager.shutdown();

        assertTrue(effect1OnTarget1.removed || effect1OnTarget1.expired);
        assertTrue(effect2OnTarget1.removed || effect2OnTarget1.expired);
        assertTrue(effect1OnTarget2.removed || effect1OnTarget2.expired);

        assertTrue(statusEffectManager.getActiveEffectsOnEntity(mockTargetEntity).isEmpty());
        assertTrue(statusEffectManager.getActiveEffectsOnEntity(mockTargetPlayer).isEmpty());
        verify(mockLogger).info("All active status effects cleared.");
    }
}
