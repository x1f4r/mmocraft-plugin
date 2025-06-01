package com.x1f4r.mmocraft.combat.service;

import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DefaultMobStatProviderTest {

    private DefaultMobStatProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DefaultMobStatProvider();
    }

    @Test
    void getBaseHealth_knownMob_returnsSpecificValue() {
        assertEquals(20.0, provider.getBaseHealth(EntityType.ZOMBIE));
        assertEquals(40.0, provider.getBaseHealth(EntityType.ENDERMAN));
    }

    @Test
    void getBaseHealth_unknownMob_returnsDefaultValue() {
        assertEquals(10.0, provider.getBaseHealth(EntityType.VILLAGER)); // Assuming Villager is not in the map
        assertEquals(10.0, provider.getBaseHealth(EntityType.WANDERING_TRADER));
    }

    @Test
    void getBaseAttackDamage_knownMob_returnsSpecificValue() {
        assertEquals(3.0, provider.getBaseAttackDamage(EntityType.ZOMBIE));
        assertEquals(7.0, provider.getBaseAttackDamage(EntityType.ENDERMAN));
    }

    @Test
    void getBaseAttackDamage_unknownMob_returnsDefaultValue() {
        assertEquals(1.0, provider.getBaseAttackDamage(EntityType.VILLAGER));
    }

    @Test
    void getBaseAttackDamage_passiveMob_returnsZero() {
        assertEquals(0.0, provider.getBaseAttackDamage(EntityType.PIG));
        assertEquals(0.0, provider.getBaseAttackDamage(EntityType.COW));
    }


    @Test
    void getBaseDefense_knownMob_returnsSpecificValue() {
        assertEquals(2.0, provider.getBaseDefense(EntityType.ZOMBIE));
        assertEquals(1.0, provider.getBaseDefense(EntityType.ENDERMAN));
    }

    @Test
    void getBaseDefense_unknownMob_returnsDefaultValue() {
        assertEquals(0.0, provider.getBaseDefense(EntityType.VILLAGER));
    }
}
