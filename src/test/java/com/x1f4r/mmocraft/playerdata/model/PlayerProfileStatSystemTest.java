package com.x1f4r.mmocraft.playerdata.model;

import com.x1f4r.mmocraft.config.gameplay.StatScalingConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerProfileStatSystemTest {

    private PlayerProfile profile;
    private StatScalingConfig originalConfig;
    private StatScalingConfig testConfig;

    @BeforeEach
    void setUp() {
        originalConfig = StatScalingConfig.defaults();
        testConfig = StatScalingConfig.builder()
                .defaultStatInvestment(0.0)
                .statRule(Stat.HEALTH, StatScalingConfig.StatRule.builder()
                        .baseValue(120.0)
                        .perPoint(10.0)
                        .perLevel(15.0)
                        .minValue(1.0)
                        .build())
                .statRule(Stat.INTELLIGENCE, StatScalingConfig.StatRule.builder()
                        .baseValue(60.0)
                        .perPoint(5.0)
                        .perLevel(5.0)
                        .minValue(0.0)
                        .build())
                .statRule(Stat.CRITICAL_CHANCE, StatScalingConfig.StatRule.builder()
                        .baseValue(20.0)
                        .perPoint(2.0)
                        .perLevel(1.0)
                        .minValue(0.0)
                        .maxValue(100.0)
                        .build())
                .statRule(Stat.CRITICAL_DAMAGE, StatScalingConfig.StatRule.builder()
                        .baseValue(50.0)
                        .perPoint(5.0)
                        .perLevel(2.0)
                        .minValue(0.0)
                        .build())
                .statRule(Stat.EVASION, StatScalingConfig.StatRule.builder()
                        .baseValue(5.0)
                        .perPoint(1.0)
                        .perLevel(0.5)
                        .minValue(0.0)
                        .maxValue(60.0)
                        .build())
                .statRule(Stat.DEFENSE, StatScalingConfig.StatRule.builder()
                        .baseValue(0.0)
                        .perPoint(3.0)
                        .perLevel(1.0)
                        .diminishingReturns(new StatScalingConfig.DiminishingReturns(300.0, 0.5))
                        .build())
                .statRule(Stat.TRUE_DEFENSE, StatScalingConfig.StatRule.builder()
                        .baseValue(0.0)
                        .perPoint(1.0)
                        .perLevel(0.2)
                        .minValue(0.0)
                        .build())
                .statRule(Stat.SPEED, StatScalingConfig.StatRule.builder()
                        .baseValue(100.0)
                        .perPoint(0.5)
                        .perLevel(0.2)
                        .minValue(0.0)
                        .maxValue(400.0)
                        .build())
                .statRule(Stat.ABILITY_POWER, StatScalingConfig.StatRule.builder()
                        .baseValue(0.0)
                        .perPoint(2.0)
                        .perLevel(1.0)
                        .minValue(0.0)
                        .build())
                .defenseReductionBase(100.0)
                .trueDefenseReductionBase(100.0)
                .maxDamageReduction(0.90)
                .maxEvasionChance(0.60)
                .build();

        PlayerProfile.setStatScalingConfig(testConfig);
        profile = new PlayerProfile(UUID.randomUUID(), "StatTestPlayer");
    }

    @AfterEach
    void tearDown() {
        PlayerProfile.setStatScalingConfig(originalConfig);
    }

    @Test
    void newPlayerProfile_initialAttributesCorrectlyCalculated() {
        assertEquals(120L, profile.getMaxHealth());
        assertEquals(profile.getMaxHealth(), profile.getCurrentHealth());
        assertEquals(60L, profile.getMaxMana());
        assertEquals(profile.getMaxMana(), profile.getCurrentMana());
        assertEquals(0.20, profile.getCriticalHitChance(), 0.0001);
        assertEquals(1.5, profile.getCriticalDamageBonus(), 0.0001);
        assertEquals(0.05, profile.getEvasionChance(), 0.0001);
        assertEquals(0.0, profile.getPhysicalDamageReduction(), 0.0001);
    }

    @Test
    void setLevel_shouldRecalculateScaling() {
        profile.setLevel(4);
        long expectedHealth = Math.round(120 + (3 * 15));
        long expectedMana = Math.round(60 + (3 * 5));
        assertEquals(expectedHealth, profile.getMaxHealth());
        assertEquals(expectedMana, profile.getMaxMana());
        double expectedCritDamage = 1.0 + ((50 + (3 * 2)) / 100.0);
        assertEquals(expectedCritDamage, profile.getCriticalDamageBonus(), 0.0001);
    }

    @Test
    void setStatValue_health_shouldRecalculateMaxHealth() {
        profile.setStatValue(Stat.HEALTH, 5.0);
        assertEquals(170L, profile.getMaxHealth());
        assertEquals(120L, profile.getCurrentHealth());
    }

    @Test
    void setStatValue_intelligence_shouldRecalculateMaxMana() {
        profile.setStatValue(Stat.INTELLIGENCE, 4.0);
        assertEquals(80L, profile.getMaxMana());
        assertEquals(60L, profile.getCurrentMana());
    }

    @Test
    void criticalChance_shouldRespectConfiguredCap() {
        profile.setStatValue(Stat.CRITICAL_CHANCE, 120.0);
        assertEquals(1.0, profile.getCriticalHitChance(), 0.0001);
    }

    @Test
    void defenseDiminishingReturns_shouldApply() {
        profile.setStatValue(Stat.DEFENSE, 200.0);
        double computedDefense = testConfig.getStatRule(Stat.DEFENSE).compute(200.0, profile.getLevel());
        double expectedReduction = computedDefense / (computedDefense + testConfig.getDefenseReductionBase());
        assertEquals(expectedReduction, profile.getPhysicalDamageReduction(), 0.0001);
    }

    @Test
    void speedStat_shouldClampToMax() {
        profile.setStatValue(Stat.SPEED, 1000.0);
        assertEquals(testConfig.getStatRule(Stat.SPEED).getMaxValue(), profile.getStatValue(Stat.SPEED));
    }

    @Test
    void equipmentModifiers_shouldAffectDerivedStats() {
        profile.addEquipmentStatModifier(Stat.HEALTH, 3.0);
        profile.recalculateDerivedAttributes();
        assertEquals(150L, profile.getMaxHealth());
        assertEquals(3.0, profile.getEquipmentStatModifier(Stat.HEALTH));
    }

    @Test
    void replacingConfig_shouldAdjustScaling() {
        StatScalingConfig updated = StatScalingConfig.builder(testConfig)
                .statRule(Stat.HEALTH, testConfig.getStatRule(Stat.HEALTH).toBuilder()
                        .perLevel(20.0)
                        .build())
                .build();
        PlayerProfile.setStatScalingConfig(updated);
        profile.recalculateDerivedAttributes();
        profile.setLevel(6);
        long expectedHealth = Math.round(120 + (5 * 20));
        assertEquals(expectedHealth, profile.getMaxHealth());
    }
}

