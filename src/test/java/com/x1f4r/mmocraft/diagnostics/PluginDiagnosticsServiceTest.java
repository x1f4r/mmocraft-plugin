package com.x1f4r.mmocraft.diagnostics;

import com.x1f4r.mmocraft.config.ConfigService;
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
    private ConfigService configService;
    @Mock
    private PersistenceService persistenceService;
    @Mock
    private Connection connection;

    private PluginDiagnosticsService diagnosticsService;

    @BeforeEach
    void setUp() throws SQLException {
        lenient().when(skillRegistryService.getAllSkills()).thenReturn(defaultSkills());
        lenient().when(customItemRegistry.getAllItems()).thenReturn(defaultItems());
        lenient().when(configService.getInt("stats.max-health")).thenReturn(100);
        lenient().when(configService.getDouble("stats.base-damage")).thenReturn(5.0);
        lenient().when(persistenceService.getConnection()).thenReturn(connection);
        lenient().when(connection.isClosed()).thenReturn(false);
        lenient().when(activeNodeManager.getAllActiveNodesView()).thenReturn(Collections.emptyMap());

        diagnosticsService = new PluginDiagnosticsService(
                loggingUtil,
                customItemRegistry,
                skillRegistryService,
                activeNodeManager,
                resourceNodeRegistryService,
                configService,
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

    private Collection<Skill> defaultSkills() {
        return List.of(mock(Skill.class));
    }

    private Collection<CustomItem> defaultItems() {
        return List.of(mock(CustomItem.class));
    }
}
