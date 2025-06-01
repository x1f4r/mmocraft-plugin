package com.x1f4r.mmocraft.loot.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LootTableEntryTest {

    @Test
    void constructor_validArgs_createsInstance() {
        LootTableEntry entry = new LootTableEntry("test_item", 0.5, 1, 5);
        assertEquals("test_item", entry.customItemId());
        assertEquals(0.5, entry.dropChance());
        assertEquals(1, entry.minAmount());
        assertEquals(5, entry.maxAmount());
    }

    @Test
    void constructor_nullItemId_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            new LootTableEntry(null, 0.5, 1, 1);
        });
    }

    @Test
    void constructor_dropChanceTooLow_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new LootTableEntry("test_item", -0.1, 1, 1);
        });
    }

    @Test
    void constructor_dropChanceTooHigh_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new LootTableEntry("test_item", 1.1, 1, 1);
        });
    }

    @Test
    void constructor_minAmountZeroOrLess_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new LootTableEntry("test_item", 0.5, 0, 1);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new LootTableEntry("test_item", 0.5, -1, 1);
        });
    }

    @Test
    void constructor_maxAmountLessThanMin_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new LootTableEntry("test_item", 0.5, 5, 4);
        });
    }

    @Test
    void constructor_maxAmountEqualsMin_isAllowed() {
        LootTableEntry entry = new LootTableEntry("test_item", 0.5, 3, 3);
        assertEquals(3, entry.minAmount());
        assertEquals(3, entry.maxAmount());
    }
}
