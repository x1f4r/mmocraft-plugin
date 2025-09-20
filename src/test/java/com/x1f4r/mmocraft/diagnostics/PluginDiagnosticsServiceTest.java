package com.x1f4r.mmocraft.diagnostics;

import com.x1f4r.mmocraft.config.gameplay.GameplayConfigIssue;
import com.x1f4r.mmocraft.config.gameplay.GameplayConfigService;
import com.x1f4r.mmocraft.config.gameplay.StatScalingConfig;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.persistence.PersistenceService;
import com.x1f4r.mmocraft.skill.model.Skill;
import com.x1f4r.mmocraft.skill.service.SkillRegistryService;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.world.resourcegathering.model.ActiveResourceNode;
import com.x1f4r.mmocraft.world.resourcegathering.service.ActiveNodeManager;
import com.x1f4r.mmocraft.world.resourcegathering.service.ResourceNodeRegistryService;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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
    private StatScalingConfig statScalingConfig;
    @Mock
    private PersistenceService persistenceService;
    @Mock
    private Connection connection;

    private PluginDiagnosticsService diagnosticsService;

    @BeforeEach
    void setUp() throws SQLException {
        lenient().when(skillRegistryService.getAllSkills()).thenReturn(defaultSkills());
        lenient().when(customItemRegistry.getAllItems()).thenReturn(defaultItems());
        lenient().when(gameplayConfigService.getStatScalingConfig()).thenReturn(statScalingConfig);
        lenient().when(statScalingConfig.getBaseHealth()).thenReturn(100L);
        lenient().when(gameplayConfigService.getIssues()).thenReturn(List.of());
        lenient().when(persistenceService.getConnection()).thenReturn(connection);
        lenient().when(connection.isClosed()).thenReturn(false);
        lenient().when(activeNodeManager.getAllActiveNodesView()).thenReturn(Collections.emptyMap());

        diagnosticsService = new PluginDiagnosticsService(
                loggingUtil,
                customItemRegistry,
                skillRegistryService,
                activeNodeManager,
                resourceNodeRegistryService,
                gameplayConfigService,
                persistenceService
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

    private Collection<Skill> defaultSkills() {
        return List.of(mock(Skill.class));
    }

    private Collection<CustomItem> defaultItems() {
        return List.of(mock(CustomItem.class));
    }
}
