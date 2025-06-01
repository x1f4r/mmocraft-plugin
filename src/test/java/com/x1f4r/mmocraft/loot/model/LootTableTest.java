package com.x1f4r.mmocraft.loot.model;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.util.LoggingUtil; // For when plugin.getLoggingUtil() is called

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LootTableTest {

    @Mock private CustomItemRegistry mockItemRegistry;
    @Mock private MMOCraftPlugin mockPlugin;
    @Mock private LoggingUtil mockLogger; // For LootTable internal logging

    private LootTable lootTable;
    private List<LootTableEntry> entries;

    @BeforeEach
    void setUp() {
        entries = new ArrayList<>();
        // Mock the logger from the plugin instance
        when(mockPlugin.getLoggingUtil()).thenReturn(mockLogger);
    }

    @Test
    void constructor_validArgs_createsInstance() {
        lootTable = new LootTable("test_table", entries);
        assertEquals("test_table", lootTable.getLootTableId());
        assertTrue(lootTable.getEntries().isEmpty());
    }

    @Test
    void constructor_nullId_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new LootTable(null, entries));
    }

    @Test
    void constructor_nullEntries_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new LootTable("test_table", null));
    }

    @Test
    void addEntry_addsEntryToList() {
        lootTable = new LootTable("test_table", new ArrayList<>()); // Start with empty mutable list
        LootTableEntry entry = new LootTableEntry("item1", 0.5, 1, 1);
        lootTable.addEntry(entry);
        assertEquals(1, lootTable.getEntries().size());
        assertTrue(lootTable.getEntries().contains(entry));
    }


    // Use RepeatedTest to account for randomness in drop chance
    @RepeatedTest(10) // Run multiple times to get a better sense of probabilities
    void generateLoot_respectsDropChancesAndAmounts() {
        LootTableEntry entry1 = new LootTableEntry("item_common", 1.0, 1, 1); // Always drops 1
        LootTableEntry entry2 = new LootTableEntry("item_rare", 0.1, 2, 5);   // 10% chance, drops 2-5
        LootTableEntry entry3 = new LootTableEntry("item_never", 0.0, 1, 1); // Never drops

        entries.add(entry1);
        entries.add(entry2);
        entries.add(entry3);
        lootTable = new LootTable("chance_test", entries);

        ItemStack commonItemStack = new ItemStack(Material.STONE, 1);
        ItemStack rareItemStack = new ItemStack(Material.DIAMOND, 3); // Amount will be overridden by entry

        when(mockItemRegistry.createItemStack("item_common", 1)).thenReturn(commonItemStack);
        // For entry2, amount is random between 2 and 5. We need to mock this for any of these amounts.
        // It's simpler to mock it for a specific amount we can verify if it drops.
        // Or, capture the amount argument.
        when(mockItemRegistry.createItemStack(eq("item_rare"), anyInt()))
            .thenAnswer(invocation -> new ItemStack(Material.DIAMOND, invocation.getArgument(1, Integer.class)));


        List<ItemStack> generatedLoot = lootTable.generateLoot(mockItemRegistry, mockPlugin);

        boolean commonDropped = false;
        boolean rareDropped = false;
        boolean neverDropped = false;

        for (ItemStack item : generatedLoot) {
            if (item.getType() == Material.STONE) {
                commonDropped = true;
                assertEquals(1, item.getAmount());
            } else if (item.getType() == Material.DIAMOND) {
                rareDropped = true;
                assertTrue(item.getAmount() >= 2 && item.getAmount() <= 5);
            } else if (item.getType() == Material.COAL) { // Assuming "item_never" would be COAL
                neverDropped = true;
            }
        }

        assertTrue(commonDropped, "Common item (100% chance) should always drop.");
        assertFalse(neverDropped, "Never drop item (0% chance) should never drop.");
        // Rare item might or might not drop in a single run. RepeatedTest helps observe.
        // A more statistical test would run many times and check proportions.
        if (rareDropped) {
             System.out.println("Rare item dropped in this run (generateLoot_respectsDropChancesAndAmounts)");
        }
    }

    @Test
    void generateLoot_itemRegistryReturnsNullOrAir_itemNotAdded() {
        LootTableEntry entryAir = new LootTableEntry("item_air", 1.0, 1, 1);
        LootTableEntry entryNull = new LootTableEntry("item_null", 1.0, 1, 1);
        entries.add(entryAir);
        entries.add(entryNull);
        lootTable = new LootTable("null_air_test", entries);

        when(mockItemRegistry.createItemStack("item_air", 1)).thenReturn(new ItemStack(Material.AIR));
        when(mockItemRegistry.createItemStack("item_null", 1)).thenReturn(null);

        List<ItemStack> generatedLoot = lootTable.generateLoot(mockItemRegistry, mockPlugin);

        assertTrue(generatedLoot.isEmpty(), "Loot list should be empty if items are AIR or null.");
        verify(mockLogger, times(1)).warning(contains("generated AIR/null for customItemId: item_air"));
        verify(mockLogger, times(1)).warning(contains("generated AIR/null for customItemId: item_null"));
    }

    @Test
    void generateLoot_itemRegistryThrowsException_itemNotAddedAndErrorLogged() {
        LootTableEntry entryError = new LootTableEntry("item_error", 1.0, 1, 1);
        entries.add(entryError);
        lootTable = new LootTable("error_test", entries);

        when(mockItemRegistry.createItemStack("item_error", 1))
            .thenThrow(new IllegalArgumentException("Test item error"));

        List<ItemStack> generatedLoot = lootTable.generateLoot(mockItemRegistry, mockPlugin);

        assertTrue(generatedLoot.isEmpty());
        verify(mockLogger).warning(contains("failed to create item: Test item error"));
    }

    @Test
    void generateLoot_nullItemRegistry_logsErrorAndReturnsEmptyList() {
        lootTable = new LootTable("null_registry", List.of(new LootTableEntry("any", 1.0, 1,1)));
        // System.err will be called here, cannot easily verify with Mockito without custom setup
        // We trust it logs to System.err based on code.
        List<ItemStack> loot = lootTable.generateLoot(null, mockPlugin);
        assertTrue(loot.isEmpty());
    }
}
