package com.x1f4r.mmocraft.diagnostics;

import com.x1f4r.mmocraft.config.gameplay.GameplayConfigIssue;
import com.x1f4r.mmocraft.config.gameplay.GameplayConfigService;
import com.x1f4r.mmocraft.config.gameplay.StatScalingConfig;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.persistence.PersistenceService;
import com.x1f4r.mmocraft.skill.model.Skill;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.skill.service.SkillRegistryService;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.world.resourcegathering.model.ActiveResourceNode;
import com.x1f4r.mmocraft.world.resourcegathering.service.ActiveNodeManager;
import com.x1f4r.mmocraft.world.resourcegathering.service.ResourceNodeRegistryService;
import org.bukkit.Location;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Centralised diagnostics utility that inspects the most important runtime services and
 * produces human-readable health reports. The command layer can render these entries to players
 * and also forward them to the server log for persistent tracking.
 */
public class PluginDiagnosticsService {

    private final LoggingUtil logger;
    private final CustomItemRegistry customItemRegistry;
    private final SkillRegistryService skillRegistryService;
    private final ActiveNodeManager activeNodeManager;
    private final ResourceNodeRegistryService resourceNodeRegistryService;
    private final GameplayConfigService gameplayConfigService;
    private final PersistenceService persistenceService;

    public PluginDiagnosticsService(
            LoggingUtil logger,
            CustomItemRegistry customItemRegistry,
            SkillRegistryService skillRegistryService,
            ActiveNodeManager activeNodeManager,
            ResourceNodeRegistryService resourceNodeRegistryService,
            GameplayConfigService gameplayConfigService,
            PersistenceService persistenceService
    ) {
        this.logger = logger;
        this.customItemRegistry = customItemRegistry;
        this.skillRegistryService = skillRegistryService;
        this.activeNodeManager = activeNodeManager;
        this.resourceNodeRegistryService = resourceNodeRegistryService;
        this.gameplayConfigService = gameplayConfigService;
        this.persistenceService = persistenceService;
    }

    public List<DiagnosticEntry> runDiagnostics() {
        List<DiagnosticEntry> results = new ArrayList<>();
        checkCustomItems(results);
        checkSkills(results);
        checkConfig(results);
        checkPersistence(results);
        checkResourceNodes(results);
        return results;
    }

    private void checkCustomItems(List<DiagnosticEntry> results) {
        if (customItemRegistry == null) {
            results.add(entry(Severity.ERROR, "Custom item registry is unavailable.",
                    "Players will not be able to receive or craft MMOCraft items."));
            return;
        }

        Collection<CustomItem> items = customItemRegistry.getAllItems();
        int count = items == null ? 0 : items.size();
        if (count == 0) {
            results.add(entry(Severity.WARNING, "No custom items are registered.",
                    "Enable the demo content or register custom items to showcase MMO equipment."));
        } else {
            results.add(entry(Severity.INFO, "Custom items registered: " + count));
        }
    }

    private void checkSkills(List<DiagnosticEntry> results) {
        if (skillRegistryService == null) {
            results.add(entry(Severity.ERROR, "Skill registry is unavailable.",
                    "Active abilities and combat skills cannot be used."));
            return;
        }

        Collection<Skill> skills = skillRegistryService.getAllSkills();
        int count = skills == null ? 0 : skills.size();
        if (count == 0) {
            results.add(entry(Severity.WARNING, "No skills are registered.",
                    "Register demo skills so players can explore the ability system."));
        } else {
            results.add(entry(Severity.INFO, "Skills registered: " + count));
        }
    }

    private void checkConfig(List<DiagnosticEntry> results) {
        if (gameplayConfigService == null) {
            results.add(entry(Severity.WARNING, "Gameplay configuration service is unavailable.",
                    "Default values will be used and tuning changes cannot be applied."));
            return;
        }

        StatScalingConfig stats = gameplayConfigService.getStatScalingConfig();
        double baseHealth = stats.getStatRule(Stat.HEALTH).getBaseValue();
        if (baseHealth <= 0) {
            results.add(entry(Severity.ERROR, "Base health from stats.yml is not positive.",
                    "Update the health scaling to use a value greater than zero."));
        } else {
            results.add(entry(Severity.INFO, "Base health configured to " + baseHealth));
        }

        for (GameplayConfigIssue issue : gameplayConfigService.getIssues()) {
            Severity severity = switch (issue.severity()) {
                case ERROR -> Severity.ERROR;
                case WARNING -> Severity.WARNING;
                default -> Severity.INFO;
            };
            results.add(entry(severity, issue.message(), issue.detail()));
        }
    }

    private void checkPersistence(List<DiagnosticEntry> results) {
        if (persistenceService == null) {
            results.add(entry(Severity.WARNING, "Persistence service is unavailable.",
                    "Player progress cannot be saved to the database."));
            return;
        }

        try {
            Connection connection = persistenceService.getConnection();
            if (connection == null) {
                results.add(entry(Severity.ERROR, "Database connection could not be established.",
                        "Check JDBC driver availability and file permissions."));
                return;
            }
            if (connection.isClosed()) {
                results.add(entry(Severity.ERROR, "Database connection is closed.",
                        "Call stack should reopen or re-initialise the persistence service."));
            } else {
                results.add(entry(Severity.INFO, "Database connection is healthy."));
            }
        } catch (SQLException ex) {
            logger.severe("Diagnostics failed to verify the SQLite connection: " + ex.getMessage(), ex);
            results.add(entry(Severity.ERROR, "Database connectivity test failed.", ex.getMessage()));
        }
    }

    private void checkResourceNodes(List<DiagnosticEntry> results) {
        if (activeNodeManager == null || resourceNodeRegistryService == null) {
            results.add(entry(Severity.WARNING, "Resource node services are not initialised.",
                    "Resource gathering mechanics are currently unavailable."));
            return;
        }

        Map<Location, ActiveResourceNode> nodes = activeNodeManager.getAllActiveNodesView();
        if (nodes.isEmpty()) {
            results.add(entry(Severity.INFO, "No active resource nodes are currently tracked."));
            return;
        }

        long missingTypes = nodes.values().stream()
                .filter(node -> resourceNodeRegistryService.getNodeType(node.getNodeTypeId()).isEmpty())
                .count();
        long nullWorlds = nodes.values().stream()
                .map(ActiveResourceNode::getLocation)
                .map(Location::getWorld)
                .filter(Objects::isNull)
                .count();

        if (missingTypes > 0) {
            results.add(entry(Severity.WARNING, missingTypes + " resource node(s) reference unknown node types.",
                    "Ensure demo content is enabled or remove stale nodes."));
        }
        if (nullWorlds > 0) {
            results.add(entry(Severity.ERROR, nullWorlds + " resource node(s) are bound to unloaded worlds.",
                    "Remove or relocate these nodes to prevent scheduler errors."));
        }

        results.add(entry(Severity.INFO, "Tracked active resource nodes: " + nodes.size()));
    }

    private DiagnosticEntry entry(Severity severity, String message) {
        return entry(severity, message, null);
    }

    private DiagnosticEntry entry(Severity severity, String message, String detail) {
        return new DiagnosticEntry(severity, message, detail);
    }

    public enum Severity {
        INFO,
        WARNING,
        ERROR;

        public boolean isIssue() {
            return this != INFO;
        }
    }

    public static final class DiagnosticEntry {
        private final Severity severity;
        private final String message;
        private final String detail;

        private DiagnosticEntry(Severity severity, String message, String detail) {
            this.severity = Objects.requireNonNull(severity, "severity");
            this.message = Objects.requireNonNull(message, "message");
            this.detail = detail;
        }

        public Severity getSeverity() {
            return severity;
        }

        public String getMessage() {
            return message;
        }

        public Optional<String> getDetail() {
            return Optional.ofNullable(detail);
        }

        public boolean isIssue() {
            return severity.isIssue();
        }

        @Override
        public String toString() {
            return "DiagnosticEntry{" +
                    "severity=" + severity +
                    ", message='" + message + '\'' +
                    (detail != null ? ", detail='" + detail + '\'' : "") +
                    '}';
        }
    }
}
