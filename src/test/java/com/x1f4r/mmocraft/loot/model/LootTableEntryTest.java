package com.x1f4r.mmocraft.loot.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LootTableEntryTest {

    @Test
    void constructor_convenienceForCustom_createsInstanceCorrectly() {
        // This uses the convenience constructor: public LootTableEntry(String customItemId, ...)
        LootTableEntry entry = new LootTableEntry("test_custom_item", 0.5, 1, 5);

        assertEquals(LootType.CUSTOM, entry.type(), "Convenience constructor should default to CUSTOM type.");
        assertEquals("test_custom_item", entry.identifier());
        assertEquals(0.5, entry.dropChance());
        assertEquals(1, entry.minAmount());
        assertEquals(5, entry.maxAmount());
    }

    @Test
    void constructor_explicitVanilla_createsInstanceCorrectly() {
        LootTableEntry entry = new LootTableEntry(LootType.VANILLA, "DIAMOND", 0.1, 1, 1);

        assertEquals(LootType.VANILLA, entry.type());
        assertEquals("DIAMOND", entry.identifier());
        assertEquals(0.1, entry.dropChance());
    }

    @Test
    void constructor_nullIdentifier_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            new LootTableEntry(LootType.CUSTOM, null, 0.5, 1, 1);
        }, "Identifier cannot be null.");
    }

    @Test
    void constructor_nullType_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            new LootTableEntry(null, "some_id", 0.5, 1, 1);
        }, "LootType cannot be null.");
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
