package com.x1f4r.mmocraft.loot.service;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.loot.model.LootTable;
import com.x1f4r.mmocraft.util.LoggingUtil;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicLootServiceTest {

    @Mock private MMOCraftPlugin mockPlugin;
    @Mock private LoggingUtil mockLogger;
    // CustomItemRegistry is not directly used by BasicLootService, but by LootTable.

    @Mock private LootTable mockLootTable;
    @Mock private Location mockLocation;
    @Mock private World mockWorld;

    private BasicLootService lootService;

    @BeforeEach
    void setUp() {
        lootService = new BasicLootService(mockPlugin, mockLogger);
        when(mockLocation.getWorld()).thenReturn(mockWorld); // For spawnLoot
    }

    @Test
    void registerLootTable_forMob_registersSuccessfully() {
        when(mockLootTable.getLootTableId()).thenReturn("zombie_drops");
        lootService.registerLootTable(EntityType.ZOMBIE, mockLootTable);
        Optional<LootTable> retrieved = lootService.getLootTable(EntityType.ZOMBIE);
        assertTrue(retrieved.isPresent());
        assertSame(mockLootTable, retrieved.get());
        verify(mockLogger).info("Registered loot table for mob type: ZOMBIE (ID: zombie_drops)");
    }

    @Test
    void registerLootTable_forMob_replaceExisting_logsWarning() {
        LootTable oldTable = mock(LootTable.class);
        when(oldTable.getLootTableId()).thenReturn("old_zombie_drops");
        when(mockLootTable.getLootTableId()).thenReturn("new_zombie_drops");

        lootService.registerLootTable(EntityType.ZOMBIE, oldTable); // Initial registration
        lootService.registerLootTable(EntityType.ZOMBIE, mockLootTable); // Replace

        verify(mockLogger).info("Replaced existing loot table for mob type: ZOMBIE");
    }

    @Test
    void registerLootTable_nullMobType_logsWarning() {
        lootService.registerLootTable(null, mockLootTable);
        verify(mockLogger).warning("Attempted to register null mobType or lootTable.");
    }

    @Test
    void getLootTable_forRegisteredMob_returnsTable() {
        lootService.registerLootTable(EntityType.SKELETON, mockLootTable);
        assertTrue(lootService.getLootTable(EntityType.SKELETON).isPresent());
    }

    @Test
    void getLootTable_forUnregisteredMob_returnsEmpty() {
        assertFalse(lootService.getLootTable(EntityType.CREEPER).isPresent());
    }

    @Test
    void getLootTable_nullMobType_returnsEmpty() {
        assertFalse(lootService.getLootTable(null).isPresent());
    }

    @Test
    void registerLootTableById_registersSuccessfully() {
        when(mockLootTable.getLootTableId()).thenReturn("treasure_chest_common");
        lootService.registerLootTableById(mockLootTable);
        Optional<LootTable> retrieved = lootService.getLootTableById("treasure_chest_common");
        assertTrue(retrieved.isPresent());
        assertSame(mockLootTable, retrieved.get());
        verify(mockLogger).info("Registered generic loot table with ID: treasure_chest_common");
    }

    @Test
    void getLootTableById_nonExistentId_returnsEmpty() {
        assertFalse(lootService.getLootTableById("fake_id").isPresent());
    }


    @Test
    void spawnLoot_validItems_dropsItemsInWorld() {
        ItemStack item1 = new ItemStack(Material.DIAMOND, 1);
        ItemStack item2 = new ItemStack(Material.GOLD_INGOT, 5);
        List<ItemStack> items = List.of(item1, item2);

        lootService.spawnLoot(mockLocation, items);

        verify(mockWorld).dropItemNaturally(mockLocation, item1);
        verify(mockWorld).dropItemNaturally(mockLocation, item2);
        verify(mockLogger, times(2)).finer(contains("Dropped item:"));
    }

    @Test
    void spawnLoot_emptyList_doesNothing() {
        lootService.spawnLoot(mockLocation, Collections.emptyList());
        verify(mockWorld, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
    }

    @Test
    void spawnLoot_nullLocationOrList_doesNothing() {
        lootService.spawnLoot(null, List.of(new ItemStack(Material.STONE)));
        lootService.spawnLoot(mockLocation, null);
        verify(mockWorld, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
    }

    @Test
    void spawnLoot_locationWithNullWorld_logsWarning() {
        when(mockLocation.getWorld()).thenReturn(null);
        lootService.spawnLoot(mockLocation, List.of(new ItemStack(Material.STONE)));
        verify(mockLogger).warning("Cannot spawn loot, location has no world.");
        verify(mockWorld, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
    }

    @Test
    void spawnLoot_itemStackIsAirOrNull_doesNotDrop() {
        List<ItemStack> items = List.of(new ItemStack(Material.AIR), null, new ItemStack(Material.IRON_INGOT));
        lootService.spawnLoot(mockLocation, items);
        verify(mockWorld, times(1)).dropItemNaturally(eq(mockLocation), any(ItemStack.class)); // Only iron ingot
        verify(mockWorld).dropItemNaturally(mockLocation, new ItemStack(Material.IRON_INGOT));
    }
}
