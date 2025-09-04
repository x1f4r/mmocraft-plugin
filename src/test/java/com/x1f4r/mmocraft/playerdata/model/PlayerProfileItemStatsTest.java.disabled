package com.x1f4r.mmocraft.playerdata.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

class PlayerProfileItemStatsTest {

    private PlayerProfile profile;
    private static final double DEFAULT_STAT_VAL = 10.0; // Assuming PlayerProfile initializes coreStats to 10.0

    @BeforeEach
    void setUp() {
        profile = new PlayerProfile(UUID.randomUUID(), "ItemStatTester");
        // Initialize core stats to a known value for consistent testing, if not already done by constructor
        Map<Stat, Double> baseStats = new EnumMap<>(Stat.class);
        for (Stat stat : Stat.values()) {
            baseStats.put(stat, DEFAULT_STAT_VAL);
        }
        profile.setCoreStats(baseStats); // This also calls recalculateDerivedAttributes
    }

    @Test
    void getEquipmentStatModifier_noModifiers_returnsZero() {
        assertEquals(0.0, profile.getEquipmentStatModifier(Stat.STRENGTH));
    }

    @Test
    void addEquipmentStatModifier_addsToCorrectStat() {
        profile.addEquipmentStatModifier(Stat.STRENGTH, 5.0);
        assertEquals(5.0, profile.getEquipmentStatModifier(Stat.STRENGTH));
        assertEquals(0.0, profile.getEquipmentStatModifier(Stat.AGILITY)); // Other stats unaffected

        profile.addEquipmentStatModifier(Stat.STRENGTH, 3.0); // Add more to same stat
        assertEquals(8.0, profile.getEquipmentStatModifier(Stat.STRENGTH));
    }

    @Test
    void addEquipmentStatModifier_negativeValue_subtractsFromStat() {
        profile.addEquipmentStatModifier(Stat.VITALITY, 10.0);
        profile.addEquipmentStatModifier(Stat.VITALITY, -3.0);
        assertEquals(7.0, profile.getEquipmentStatModifier(Stat.VITALITY));
    }

    @Test
    void clearEquipmentStatModifiers_removesAllModifiers() {
        profile.addEquipmentStatModifier(Stat.STRENGTH, 5.0);
        profile.addEquipmentStatModifier(Stat.AGILITY, 3.0);
        assertNotEquals(0.0, profile.getEquipmentStatModifier(Stat.STRENGTH));

        profile.clearEquipmentStatModifiers();
        assertEquals(0.0, profile.getEquipmentStatModifier(Stat.STRENGTH));
        assertEquals(0.0, profile.getEquipmentStatModifier(Stat.AGILITY));
    }

    @Test
    void addAllEquipmentStatModifiers_addsAllStatsFromMap() {
        Map<Stat, Double> modifiers = new EnumMap<>(Stat.class);
        modifiers.put(Stat.INTELLIGENCE, 7.0);
        modifiers.put(Stat.WISDOM, 3.5);
        modifiers.put(Stat.STRENGTH, 2.0); // To test merging with existing if any (none here)

        profile.addAllEquipmentStatModifiers(modifiers);

        assertEquals(7.0, profile.getEquipmentStatModifier(Stat.INTELLIGENCE));
        assertEquals(3.5, profile.getEquipmentStatModifier(Stat.WISDOM));
        assertEquals(2.0, profile.getEquipmentStatModifier(Stat.STRENGTH));
    }

    @Test
    void addAllEquipmentStatModifiers_mergesWithExistingModifiers() {
        profile.addEquipmentStatModifier(Stat.STRENGTH, 5.0); // Initial: STR 5.0

        Map<Stat, Double> newModifiers = new EnumMap<>(Stat.class);
        newModifiers.put(Stat.STRENGTH, 3.0); // Add 3.0 more to STR
        newModifiers.put(Stat.DEFENSE, 4.0);  // New stat DEF

        profile.addAllEquipmentStatModifiers(newModifiers);

        assertEquals(8.0, profile.getEquipmentStatModifier(Stat.STRENGTH)); // 5.0 + 3.0
        assertEquals(4.0, profile.getEquipmentStatModifier(Stat.DEFENSE));
    }


    @Test
    void getStatValue_noEquipmentModifiers_returnsBaseStatValue() {
        // Profile coreStats are initialized to DEFAULT_STAT_VAL (e.g. 10.0) in setUp
        assertEquals(DEFAULT_STAT_VAL, profile.getStatValue(Stat.STRENGTH));
        assertEquals(DEFAULT_STAT_VAL, profile.getBaseStatValue(Stat.STRENGTH)); // Base should be same
    }

    @Test
    void getStatValue_withEquipmentModifiers_returnsSumOfBaseAndEquipment() {
        profile.setStatValue(Stat.AGILITY, 15.0); // Sets base agility
        profile.addEquipmentStatModifier(Stat.AGILITY, 5.0);

        assertEquals(15.0, profile.getBaseStatValue(Stat.AGILITY));
        assertEquals(5.0, profile.getEquipmentStatModifier(Stat.AGILITY));
        assertEquals(20.0, profile.getStatValue(Stat.AGILITY)); // 15 (base) + 5 (equip)
    }

    @Test
    void recalculateDerivedAttributes_usesEquipmentModifiedStats() {
        // Assuming VITALITY affects maxHealth (e.g., 1 VITA = 5 HP, Base HP = 50, Level 1 = 2 HP)
        // Initial (default VITALITY is 12 from constructor, modified to 10 in setup)
        // Base VITALITY = 10.0
        // Max HP = 50 + (10.0 * 5) + (1 * 2) = 50 + 50 + 2 = 102
        profile.setStatValue(Stat.VITALITY, 10.0); // sets base and recalculates
        assertEquals(102, profile.getMaxHealth(), "Max health before equipment incorrect.");

        profile.addEquipmentStatModifier(Stat.VITALITY, 8.0); // Effective VITALITY = 10 + 8 = 18
        // addEquipmentStatModifier calls recalculateDerivedAttributes()

        // New Max HP = 50 + (18.0 * 5) + (1 * 2) = 50 + 90 + 2 = 142
        assertEquals(18.0, profile.getStatValue(Stat.VITALITY));
        assertEquals(142, profile.getMaxHealth(), "Max health after equipment vitality incorrect.");

        profile.clearEquipmentStatModifiers(); // VITALITY reverts to 10.0
        // clearEquipmentStatModifiers calls recalculateDerivedAttributes()
        assertEquals(10.0, profile.getStatValue(Stat.VITALITY));
        assertEquals(102, profile.getMaxHealth(), "Max health after clearing equipment vitality incorrect.");
    }
}
