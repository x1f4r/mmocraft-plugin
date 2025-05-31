package com.x1f4r.mmocraft.playerdata.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

class PlayerProfileStatSystemTest {

    private PlayerProfile profile;
    private UUID testUUID;
    private String testPlayerName;

    // Constants from PlayerProfile for test reference (consider making them public in PlayerProfile or testable some other way)
    private static final long BASE_HEALTH = 50;
    private static final double HEALTH_PER_VITALITY = 5.0;
    private static final double HEALTH_PER_LEVEL = 2.0;
    private static final long BASE_MANA = 20;
    private static final double MANA_PER_WISDOM = 3.0;
    private static final double MANA_PER_LEVEL = 1.0;
    private static final double BASE_CRITICAL_HIT_CHANCE = 0.05;
    private static final double CRIT_CHANCE_PER_AGILITY = 0.005;
    private static final double CRIT_CHANCE_PER_LUCK = 0.002;
    private static final double BASE_CRITICAL_DAMAGE_BONUS = 1.5;
    private static final double CRIT_DAMAGE_BONUS_PER_STRENGTH = 0.01;
    private static final double BASE_EVASION_CHANCE = 0.02;
    private static final double EVASION_PER_AGILITY = 0.004;
    private static final double EVASION_PER_LUCK = 0.001;
    private static final double PHYS_REDUCTION_PER_DEFENSE = 0.005;
    private static final double MAX_PHYS_REDUCTION = 0.80;
    private static final double MAGIC_REDUCTION_PER_WISDOM = 0.003;
    private static final double MAX_MAGIC_REDUCTION = 0.80;


    @BeforeEach
    void setUp() {
        testUUID = UUID.randomUUID();
        testPlayerName = "StatTestPlayer";
        // Uses the minimal constructor, which initializes default stats and calls recalculateDerivedAttributes()
        profile = new PlayerProfile(testUUID, testPlayerName);
    }

    @Test
    void newPlayerProfile_initialAttributesCorrectlyCalculated() {
        // Default level is 1. Default stats are 10, VITALITY 12, WISDOM 11.
        double expectedMaxHealth = BASE_HEALTH + (12.0 * HEALTH_PER_VITALITY) + (1 * HEALTH_PER_LEVEL); // 50 + 60 + 2 = 112
        double expectedMaxMana = BASE_MANA + (11.0 * MANA_PER_WISDOM) + (1 * MANA_PER_LEVEL);     // 20 + 33 + 1 = 54

        assertEquals((long)expectedMaxHealth, profile.getMaxHealth(), "Initial Max Health calculation incorrect.");
        assertEquals(profile.getMaxHealth(), profile.getCurrentHealth(), "Initial Current Health should be Max Health.");
        assertEquals((long)expectedMaxMana, profile.getMaxMana(), "Initial Max Mana calculation incorrect.");
        assertEquals(profile.getMaxMana(), profile.getCurrentMana(), "Initial Current Mana should be Max Mana.");

        // Check one secondary stat (e.g., crit chance with default Agility 10, Luck 10)
        double expectedCrit = BASE_CRITICAL_HIT_CHANCE + (10.0 * CRIT_CHANCE_PER_AGILITY) + (10.0 * CRIT_CHANCE_PER_LUCK);
        assertEquals(expectedCrit, profile.getCriticalHitChance(), 0.0001, "Initial Crit Chance incorrect.");
    }

    @Test
    void setLevel_shouldRecalculateMaxHealthAndMana() {
        long oldMaxHealth = profile.getMaxHealth();
        long oldMaxMana = profile.getMaxMana();

        profile.setLevel(5); // Level up from 1 to 5 (diff of 4 levels)

        long expectedNewMaxHealth = (long)(oldMaxHealth + (4 * HEALTH_PER_LEVEL));
        long expectedNewMaxMana = (long)(oldMaxMana + (4 * MANA_PER_LEVEL));

        assertEquals(expectedNewMaxHealth, profile.getMaxHealth());
        assertEquals(expectedNewMaxMana, profile.getMaxMana());
    }

    @Test
    void setStatValue_vitality_shouldRecalculateMaxHealthAndClampCurrentHealth() {
        profile.setStatValue(Stat.VITALITY, 20.0); // Default was 12.0
        // Expected: 50 (base) + (20 * 5) + (1 * 2) = 50 + 100 + 2 = 152
        assertEquals(152, profile.getMaxHealth());

        // Test clamping currentHealth if it was higher than new max (not directly testable here easily without setting high current)
        // but ensure currentHealth is not above new maxHealth.
        profile.setCurrentHealth(200); // Try to set current health higher than new max
        assertEquals(profile.getMaxHealth(), profile.getCurrentHealth(), "Current health should be clamped to new max health.");
    }

    @Test
    void setStatValue_wisdom_shouldRecalculateMaxMana() {
        profile.setStatValue(Stat.WISDOM, 20.0); // Default was 11.0
        // Expected: 20 (base) + (20 * 3) + (1 * 1) = 20 + 60 + 1 = 81
        assertEquals(81, profile.getMaxMana());
    }

    @Test
    void setStatValue_agilityAndLuck_shouldRecalculateCritAndEvasion() {
        profile.setStatValue(Stat.AGILITY, 20.0); // Default 10.0
        profile.setStatValue(Stat.LUCK, 15.0);   // Default 10.0

        double expectedCrit = BASE_CRITICAL_HIT_CHANCE +
                              (20.0 * CRIT_CHANCE_PER_AGILITY) +
                              (15.0 * CRIT_CHANCE_PER_LUCK);
        assertEquals(expectedCrit, profile.getCriticalHitChance(), 0.0001);

        double expectedEvasion = BASE_EVASION_CHANCE +
                                 (20.0 * EVASION_PER_AGILITY) +
                                 (15.0 * EVASION_PER_LUCK);
        assertEquals(expectedEvasion, profile.getEvasionChance(), 0.0001);
    }

    @Test
    void setStatValue_strength_shouldRecalculateCritDamageBonus() {
        profile.setStatValue(Stat.STRENGTH, 30.0); // Default 10.0
        double expectedCritDmg = BASE_CRITICAL_DAMAGE_BONUS + (30.0 * CRIT_DAMAGE_BONUS_PER_STRENGTH);
        assertEquals(expectedCritDmg, profile.getCriticalDamageBonus(), 0.0001);
    }

    @Test
    void setStatValue_defense_shouldRecalculatePhysicalDamageReduction() {
        profile.setStatValue(Stat.DEFENSE, 50.0); // Default 10.0
        double expectedReduction = 50.0 * PHYS_REDUCTION_PER_DEFENSE;
        expectedReduction = Math.min(expectedReduction, MAX_PHYS_REDUCTION);
        assertEquals(expectedReduction, profile.getPhysicalDamageReduction(), 0.0001);
    }

    @Test
    void physicalDamageReduction_shouldBeCapped() {
        profile.setStatValue(Stat.DEFENSE, 10000.0); // Very high defense
        assertEquals(MAX_PHYS_REDUCTION, profile.getPhysicalDamageReduction(), 0.0001);
    }

    @Test
    void fullConstructor_shouldCalculateAndClampCorrectly() {
        Map<Stat, Double> loadedStats = new EnumMap<>(Stat.class);
        loadedStats.put(Stat.VITALITY, 30.0); // Max Health = 50 + 30*5 + 10*2 = 50 + 150 + 20 = 220
        loadedStats.put(Stat.WISDOM, 20.0);   // Max Mana = 20 + 20*3 + 10*1 = 20 + 60 + 10 = 90
        for (Stat s : Stat.values()) { // Ensure all stats are present
            loadedStats.putIfAbsent(s, 10.0);
        }

        PlayerProfile loadedProfile = new PlayerProfile(
            UUID.randomUUID(), "LoadedPlayer",
            500, 500, // Current health/mana (will be clamped)
            300, 300, // Max health/mana from DB (ignored, will be recalculated)
            10, 0, 0, // level, exp, currency
            loadedStats,
            LocalDateTime.now(), LocalDateTime.now()
        );

        assertEquals(220, loadedProfile.getMaxHealth());
        assertEquals(220, loadedProfile.getCurrentHealth(), "Current health should be clamped to calculated max health.");
        assertEquals(90, loadedProfile.getMaxMana());
        assertEquals(90, loadedProfile.getCurrentMana(), "Current mana should be clamped to calculated max mana.");
    }

    @Test
    void setCoreStats_shouldTriggerRecalculation() {
        Map<Stat, Double> newStats = new EnumMap<>(Stat.class);
        newStats.put(Stat.VITALITY, 25.0); // Expected HP: 50 + 25*5 + 1*2 = 177
        newStats.put(Stat.WISDOM, 5.0);   // Expected MP: 20 + 5*3 + 1*1 = 36
         for (Stat s : Stat.values()) {
            newStats.putIfAbsent(s, 8.0); // Lower other stats
        }

        profile.setCoreStats(newStats);

        assertEquals(177, profile.getMaxHealth());
        assertEquals(36, profile.getMaxMana());
        // Check a secondary stat
        double expectedCrit = BASE_CRITICAL_HIT_CHANCE +
                              (8.0 * CRIT_CHANCE_PER_AGILITY) +
                              (8.0 * CRIT_CHANCE_PER_LUCK);
        assertEquals(expectedCrit, profile.getCriticalHitChance(), 0.0001, "Crit chance after setCoreStats incorrect.");
    }
}
