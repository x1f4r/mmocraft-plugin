package com.x1f4r.mmocraft.loot.registry;

import com.x1f4r.mmocraft.config.gameplay.LootTablesConfig;
import com.x1f4r.mmocraft.loot.model.LootTable;
import com.x1f4r.mmocraft.loot.model.LootTableEntry;
import com.x1f4r.mmocraft.loot.model.LootType;
import com.x1f4r.mmocraft.loot.service.LootService;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LootTableRegistryTest {

    @Mock
    private LootService lootService;
    @Mock
    private LoggingUtil loggingUtil;

    private LootTableRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new LootTableRegistry(lootService, loggingUtil);
    }

    @Test
    void applyConfig_registersTablesAndAssignments() {
        LootTablesConfig config = new LootTablesConfig(
                Map.of(
                        "demo", new LootTablesConfig.LootTableDefinition("demo",
                                List.of(new LootTablesConfig.LootEntryDefinition(LootType.VANILLA, "STONE", 1.0, 1, 2)))
                ),
                Map.of(EntityType.ZOMBIE, "demo")
        );

        registry.applyConfig(config);

        ArgumentCaptor<LootTable> genericCaptor = ArgumentCaptor.forClass(LootTable.class);
        verify(lootService).registerLootTableById(genericCaptor.capture());
        LootTable table = genericCaptor.getValue();
        assertEquals("demo", table.getLootTableId());
        assertEquals(1, table.getEntries().size());
        LootTableEntry entry = table.getEntries().get(0);
        assertEquals("STONE", entry.identifier());
        assertEquals(1.0, entry.dropChance());

        verify(lootService).registerLootTable(eq(EntityType.ZOMBIE), any(LootTable.class));
    }

    @Test
    void applyConfig_unregistersPreviousTablesBeforeRegistering() {
        LootTablesConfig initial = new LootTablesConfig(
                Map.of("alpha", new LootTablesConfig.LootTableDefinition("alpha", List.of())),
                Map.of(EntityType.SKELETON, "alpha")
        );
        registry.applyConfig(initial);

        LootTablesConfig updated = new LootTablesConfig(Map.of(), Map.of());
        registry.applyConfig(updated);

        verify(lootService).unregisterLootTableById("alpha");
        verify(lootService).unregisterLootTable(EntityType.SKELETON);
    }
}
