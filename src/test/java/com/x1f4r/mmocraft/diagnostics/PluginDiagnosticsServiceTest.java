package com.x1f4r.mmocraft.diagnostics;

import com.x1f4r.mmocraft.combat.listeners.PlayerCombatListener;
import com.x1f4r.mmocraft.config.gameplay.CraftingConfig;
import com.x1f4r.mmocraft.config.gameplay.DemoContentConfig;
import com.x1f4r.mmocraft.config.gameplay.GameplayConfigIssue;
import com.x1f4r.mmocraft.config.gameplay.GameplayConfigService;
import com.x1f4r.mmocraft.config.gameplay.LootTablesConfig;
import com.x1f4r.mmocraft.config.gameplay.StatScalingConfig;
import com.x1f4r.mmocraft.crafting.model.CustomRecipeIngredient;
import com.x1f4r.mmocraft.crafting.recipe.CustomRecipe;
import com.x1f4r.mmocraft.crafting.service.RecipeRegistryService;
import com.x1f4r.mmocraft.demo.DemoContentSettings;
import com.x1f4r.mmocraft.item.equipment.listeners.PlayerEquipmentListener;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.loot.listeners.MobDeathLootListener;
import com.x1f4r.mmocraft.persistence.PersistenceService;
import com.x1f4r.mmocraft.playerdata.listeners.PlayerJoinQuitListener;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.playerdata.runtime.PlayerRuntimeAttributeListener;
import com.x1f4r.mmocraft.skill.model.Skill;
import com.x1f4r.mmocraft.skill.service.SkillRegistryService;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.world.resourcegathering.listeners.ResourceNodeInteractionListener;
import com.x1f4r.mmocraft.world.resourcegathering.model.ActiveResourceNode;
import com.x1f4r.mmocraft.world.resourcegathering.service.ActiveNodeManager;
import com.x1f4r.mmocraft.world.resourcegathering.service.ResourceNodeRegistryService;
import com.x1f4r.mmocraft.world.zone.listeners.PlayerZoneTrackerListener;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PluginDiagnosticsServiceTest {

    @Mock
    private LoggingUtil loggingUtil;
    @Mock
    private CustomItemRegistry customItemRegistry;
    @Mock
    private SkillRegistryService skillRegistryService;
    @Mock
    private ActiveNodeManager activeNodeManager;
    @Mock
    private ResourceNodeRegistryService resourceNodeRegistryService;
    @Mock
    private GameplayConfigService gameplayConfigService;
    @Mock
    private PersistenceService persistenceService;
    @Mock
    private Connection connection;
    @Mock
    private RecipeRegistryService recipeRegistryService;
    @Mock
    private Plugin plugin;

    private PluginDiagnosticsService diagnosticsService;
    private DemoContentSettings demoSettings;
    private List<RegisteredListener> currentListeners;

    @BeforeEach
    void setUp() throws SQLException {
        currentListeners = listenersFor(
                PlayerJoinQuitListener.class,
                PlayerRuntimeAttributeListener.class,
                PlayerZoneTrackerListener.class,
                PlayerCombatListener.class,
                PlayerEquipmentListener.class,
                ResourceNodeInteractionListener.class,
                MobDeathLootListener.class
        );

        lenient().when(skillRegistryService.getAllSkills()).thenReturn(defaultSkills());
        lenient().when(customItemRegistry.getAllItems()).thenReturn(defaultItems());
        StatScalingConfig statScalingConfig = StatScalingConfig.builder()
                .statRule(Stat.HEALTH, StatScalingConfig.StatRule.builder().baseValue(100.0).build())
                .build();
        lenient().when(gameplayConfigService.getStatScalingConfig()).thenReturn(statScalingConfig);
        lenient().when(gameplayConfigService.getIssues()).thenReturn(List.of());
        lenient().when(gameplayConfigService.getCraftingConfig()).thenReturn(new CraftingConfig(List.of()));
        lenient().when(gameplayConfigService.getLootTablesConfig()).thenReturn(LootTablesConfig.defaults());
        lenient().when(gameplayConfigService.getDemoContentConfig()).thenReturn(DemoContentConfig.defaults());
        lenient().when(persistenceService.getConnection()).thenReturn(connection);
        lenient().when(connection.isClosed()).thenReturn(false);
        lenient().when(activeNodeManager.getAllActiveNodesView()).thenReturn(Collections.emptyMap());
        lenient().when(resourceNodeRegistryService.getAllNodeTypes()).thenReturn(Collections.emptyList());
        lenient().when(recipeRegistryService.getAllRecipes()).thenReturn(Collections.emptyList());

        demoSettings = DemoContentSettings.disabled();

        diagnosticsService = new PluginDiagnosticsService(
                loggingUtil,
                customItemRegistry,
                skillRegistryService,
                activeNodeManager,
                resourceNodeRegistryService,
                gameplayConfigService,
                persistenceService,
                recipeRegistryService,
                () -> demoSettings,
                plugin,
                ignored -> currentListeners
        );
    }

    @Test
    void runDiagnostics_whenNoCustomItems_reportsWarning() {
        when(customItemRegistry.getAllItems()).thenReturn(Collections.emptyList());

        List<PluginDiagnosticsService.DiagnosticEntry> entries = diagnosticsService.runDiagnostics();

        assertTrue(entries.stream().anyMatch(entry ->
                entry.getSeverity() == PluginDiagnosticsService.Severity.WARNING
                        && entry.getMessage().contains("No custom items")));
    }

    @Test
    void runDiagnostics_whenResourceNodeTypeMissing_reportsWarning() {
        World world = mock(World.class);
        Location location = new Location(world, 0, 64, 0);
        ActiveResourceNode orphanedNode = new ActiveResourceNode(location, "missing_type");

        lenient().when(activeNodeManager.getAllActiveNodesView()).thenReturn(Map.of(location, orphanedNode));
        lenient().when(resourceNodeRegistryService.getNodeType("missing_type")).thenReturn(Optional.empty());

        List<PluginDiagnosticsService.DiagnosticEntry> entries = diagnosticsService.runDiagnostics();

        assertTrue(entries.stream().anyMatch(entry ->
                entry.getSeverity() == PluginDiagnosticsService.Severity.WARNING
                        && entry.getMessage().contains("reference unknown node types")));
    }

    @Test
    void runDiagnostics_whenDatabaseCheckFails_reportsError() throws SQLException {
        SQLException failure = new SQLException("boom");
        when(persistenceService.getConnection()).thenThrow(failure);

        List<PluginDiagnosticsService.DiagnosticEntry> entries = diagnosticsService.runDiagnostics();

        assertTrue(entries.stream().anyMatch(entry ->
                entry.getSeverity() == PluginDiagnosticsService.Severity.ERROR
                        && entry.getMessage().contains("Database connectivity test failed")));
    }

    @Test
    void runDiagnostics_whenConfigIssuesPresent_reportsMessages() {
        GameplayConfigIssue issue = GameplayConfigIssue.warn("Crafting config", "invalid recipe");
        when(gameplayConfigService.getIssues()).thenReturn(List.of(issue));

        List<PluginDiagnosticsService.DiagnosticEntry> entries = diagnosticsService.runDiagnostics();

        assertTrue(entries.stream().anyMatch(entry ->
                entry.getMessage().contains("Crafting config")
                        && entry.getDetail().orElse("").contains("invalid recipe")));
    }

    @Test
    void findIssues_returnsOnlyWarningsAndErrors() {
        when(customItemRegistry.getAllItems()).thenReturn(Collections.emptyList());

        List<PluginDiagnosticsService.DiagnosticEntry> issues = diagnosticsService.findIssues();

        assertFalse(issues.isEmpty());
        assertTrue(issues.stream().allMatch(PluginDiagnosticsService.DiagnosticEntry::isIssue));
    }

    @Test
    void runDiagnostics_whenDemoItemsEnabledWithoutContent_reportsWarning() {
        demoSettings = new DemoContentSettings(true, true, false, false, false, false, false);
        DemoContentConfig demoConfig = new DemoContentConfig(
                new DemoContentConfig.DemoToggles(true, true, false, false, false, false, false),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        when(gameplayConfigService.getDemoContentConfig()).thenReturn(demoConfig);
        when(customItemRegistry.getAllItems()).thenReturn(Collections.emptyList());

        List<PluginDiagnosticsService.DiagnosticEntry> entries = diagnosticsService.runDiagnostics();

        assertTrue(entries.stream().anyMatch(entry ->
                entry.getSeverity() == PluginDiagnosticsService.Severity.WARNING
                        && entry.getMessage().contains("Demo items enabled")));
    }

    @Test
    void runDiagnostics_whenRecipeUsesMissingCustomItem_reportsError() {
        CustomRecipeIngredient missing = new CustomRecipeIngredient(
                CustomRecipeIngredient.IngredientType.CUSTOM_ITEM,
                "MISSING_ITEM",
                1
        );
        CustomRecipe recipe = mock(CustomRecipe.class);
        when(recipe.getRecipeId()).thenReturn("stale_recipe");
        when(recipe.isShaped()).thenReturn(false);
        when(recipe.getShapelessIngredients()).thenReturn(List.of(missing));
        when(recipeRegistryService.getAllRecipes()).thenReturn(List.of(recipe));
        when(customItemRegistry.getCustomItem("MISSING_ITEM")).thenReturn(Optional.empty());

        List<PluginDiagnosticsService.DiagnosticEntry> entries = diagnosticsService.runDiagnostics();

        assertTrue(entries.stream().anyMatch(entry ->
                entry.getSeverity() == PluginDiagnosticsService.Severity.ERROR
                        && entry.getMessage().contains("requires missing custom item")));
    }

    @Test
    void runDiagnostics_whenListenerMissing_reportsWarning() {
        currentListeners = listenersFor(PlayerJoinQuitListener.class);

        List<PluginDiagnosticsService.DiagnosticEntry> entries = diagnosticsService.runDiagnostics();

        assertTrue(entries.stream().anyMatch(entry ->
                entry.getSeverity() == PluginDiagnosticsService.Severity.WARNING
                        && entry.getMessage().contains("PlayerCombatListener")));
    }

    private Collection<Skill> defaultSkills() {
        return List.of(mock(Skill.class));
    }

    private Collection<CustomItem> defaultItems() {
        return List.of(mock(CustomItem.class));
    }

    @SafeVarargs
    private List<RegisteredListener> listenersFor(Class<? extends Listener>... listenerClasses) {
        List<RegisteredListener> listeners = new ArrayList<>();
        for (Class<? extends Listener> clazz : listenerClasses) {
            RegisteredListener registeredListener = mock(RegisteredListener.class);
            Listener listener = mock(clazz);
            when(registeredListener.getListener()).thenReturn(listener);
            listeners.add(registeredListener);
        }
        return listeners;
    }
}
