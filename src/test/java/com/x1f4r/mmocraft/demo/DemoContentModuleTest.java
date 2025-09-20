package com.x1f4r.mmocraft.demo;

import com.x1f4r.mmocraft.config.gameplay.DemoContentConfig;
import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.loot.model.LootTable;
import com.x1f4r.mmocraft.loot.service.LootService;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.util.StringUtil;
import com.x1f4r.mmocraft.world.resourcegathering.model.ResourceNodeType;
import com.x1f4r.mmocraft.world.resourcegathering.service.ActiveNodeManager;
import com.x1f4r.mmocraft.world.resourcegathering.service.ResourceNodeRegistryService;
import com.x1f4r.mmocraft.world.spawning.conditions.TimeCondition;
import com.x1f4r.mmocraft.world.spawning.model.CustomSpawnRule;
import com.x1f4r.mmocraft.world.spawning.service.CustomSpawningService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoContentModuleTest {

    @Mock
    private MMOCraftPlugin plugin;
    @Mock
    private LoggingUtil loggingUtil;
    @Mock
    private LootService lootService;
    @Mock
    private ResourceNodeRegistryService resourceNodeRegistryService;
    @Mock
    private ActiveNodeManager activeNodeManager;
    @Mock
    private CustomSpawningService customSpawningService;

    @BeforeEach
    void setUp() {
        lenient().when(plugin.getLootService()).thenReturn(lootService);
        lenient().when(plugin.getResourceNodeRegistryService()).thenReturn(resourceNodeRegistryService);
        lenient().when(plugin.getActiveNodeManager()).thenReturn(activeNodeManager);
    }

    @Test
    void applySettings_resourceNodesEnabled_placesNodesFromConfig() {
        DemoContentConfig config = new DemoContentConfig(
                new DemoContentConfig.DemoToggles(true, false, false, false, false, true, false),
                List.of(),
                List.of(),
                List.of(new DemoContentConfig.ResourceNodeTypeConfig("stone_node", Material.STONE, 5.0,
                        Set.of(), "stone_loot", 30, "&7Stone Node")),
                List.of(new DemoContentConfig.ResourceNodePlacementConfig("stone_node", null, 5, 64, 5)),
                List.of()
        );
        when(lootService.getLootTableById("stone_loot")).thenReturn(Optional.of(mock(LootTable.class)));
        when(resourceNodeRegistryService.getNodeType("stone_node")).thenReturn(Optional.empty());
        when(activeNodeManager.getActiveNode(any())).thenReturn(Optional.empty());
        lenient().when(activeNodeManager.countNodesOfType("stone_node")).thenReturn(0L);

        DemoContentModule module = new DemoContentModule(plugin, loggingUtil, config);

        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
            World world = mock(World.class);
            when(world.getName()).thenReturn("world");
            mockedBukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
            mockedBukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));

            DemoContentSettings settings = new DemoContentSettings(true, false, false, false, false, true, false);
            module.applySettings(settings);
        }

        ArgumentCaptor<ResourceNodeType> nodeTypeCaptor = ArgumentCaptor.forClass(ResourceNodeType.class);
        verify(resourceNodeRegistryService).registerNodeType(nodeTypeCaptor.capture());
        assertEquals("stone_node", nodeTypeCaptor.getValue().getTypeId());

        ArgumentCaptor<Location> locationCaptor = ArgumentCaptor.forClass(Location.class);
        verify(activeNodeManager).placeNewNode(locationCaptor.capture(), eq("stone_node"));
        Location placed = locationCaptor.getValue();
        assertEquals(5, placed.getBlockX());
        assertEquals(64, placed.getBlockY());
        assertEquals(5, placed.getBlockZ());
    }

    @Test
    void applySettings_customSpawnsEnabled_registersRules() {
        DemoContentConfig config = new DemoContentConfig(
                new DemoContentConfig.DemoToggles(true, false, false, false, true, false, false),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new DemoContentConfig.CustomSpawnRuleConfig(
                        "rule_one",
                        "mob_one",
                        EntityType.ZOMBIE,
                        "&aTest Mob",
                        "profile",
                        "table",
                        Map.of(),
                        Set.of(),
                        new DemoContentConfig.TimeWindow(0, 2000),
                        0.5,
                        0,
                        100,
                        3,
                        8.0,
                        120L
                ))
        );
        when(plugin.getCustomSpawningService()).thenReturn(customSpawningService);
        when(customSpawningService.getAllRules()).thenReturn(List.of());

        DemoContentModule module = new DemoContentModule(plugin, loggingUtil, config);
        DemoContentSettings settings = new DemoContentSettings(true, false, false, false, true, false, false);
        module.applySettings(settings);

        ArgumentCaptor<CustomSpawnRule> ruleCaptor = ArgumentCaptor.forClass(CustomSpawnRule.class);
        verify(customSpawningService).registerRule(ruleCaptor.capture());
        CustomSpawnRule rule = ruleCaptor.getValue();
        assertEquals("rule_one", rule.getRuleId());
        assertEquals("mob_one", rule.getMobSpawnDefinition().getDefinitionId());
        assertEquals(StringUtil.colorize("&aTest Mob"),
                rule.getMobSpawnDefinition().getDisplayName().orElse(""));
        assertTrue(rule.getConditions().stream().anyMatch(condition -> condition instanceof TimeCondition));
    }
}
