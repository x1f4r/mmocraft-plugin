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
import com.x1f4r.mmocraft.core.MMOCraftPlugin;
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
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Centralised diagnostics utility that inspects the most important runtime services and
 * produces human-readable health reports. The command layer can render these entries to players
 * and also forward them to the server log for persistent tracking.
 */
public class PluginDiagnosticsService {

    private static final List<Class<? extends Listener>> REQUIRED_LISTENERS = List.of(
            PlayerJoinQuitListener.class,
            PlayerRuntimeAttributeListener.class,
            PlayerZoneTrackerListener.class,
            PlayerCombatListener.class,
            PlayerEquipmentListener.class,
            ResourceNodeInteractionListener.class,
            MobDeathLootListener.class
    );

    private final LoggingUtil logger;
    private final CustomItemRegistry customItemRegistry;
    private final SkillRegistryService skillRegistryService;
    private final ActiveNodeManager activeNodeManager;
    private final ResourceNodeRegistryService resourceNodeRegistryService;
    private final GameplayConfigService gameplayConfigService;
    private final PersistenceService persistenceService;
    private final RecipeRegistryService recipeRegistryService;
    private final Supplier<DemoContentSettings> demoSettingsSupplier;
    private final Plugin plugin;
    private final Function<Plugin, Collection<RegisteredListener>> listenerLookup;

    public PluginDiagnosticsService(
            LoggingUtil logger,
            CustomItemRegistry customItemRegistry,
            SkillRegistryService skillRegistryService,
            ActiveNodeManager activeNodeManager,
            ResourceNodeRegistryService resourceNodeRegistryService,
            GameplayConfigService gameplayConfigService,
            PersistenceService persistenceService,
            RecipeRegistryService recipeRegistryService,
            Supplier<DemoContentSettings> demoSettingsSupplier,
            Plugin plugin,
            Function<Plugin, Collection<RegisteredListener>> listenerLookup
    ) {
        this.logger = logger;
        this.customItemRegistry = customItemRegistry;
        this.skillRegistryService = skillRegistryService;
        this.activeNodeManager = activeNodeManager;
        this.resourceNodeRegistryService = resourceNodeRegistryService;
        this.gameplayConfigService = gameplayConfigService;
        this.persistenceService = persistenceService;
        this.recipeRegistryService = recipeRegistryService;
        this.demoSettingsSupplier = demoSettingsSupplier;
        this.plugin = plugin;
        this.listenerLookup = listenerLookup != null ? listenerLookup : HandlerList::getRegisteredListeners;
    }

    public List<DiagnosticEntry> runDiagnostics() {
        List<DiagnosticEntry> results = new ArrayList<>();
        checkCustomItems(results);
        checkSkills(results);
        checkConfig(results);
        checkPersistence(results);
        checkResourceNodes(results);
        checkDemoContent(results);
        checkCraftingRecipes(results);
        checkEventListeners(results);
        return results;
    }

    public List<DiagnosticEntry> findIssues() {
        return runDiagnostics().stream()
                .filter(DiagnosticEntry::isIssue)
                .collect(Collectors.toList());
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

        auditCraftingConfig(results, gameplayConfigService.getCraftingConfig());
        auditLootAssignments(results, gameplayConfigService.getLootTablesConfig());
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

    private void checkDemoContent(List<DiagnosticEntry> results) {
        if (gameplayConfigService == null) {
            return;
        }
        DemoContentConfig demoConfig = gameplayConfigService.getDemoContentConfig();
        if (demoConfig == null) {
            results.add(entry(Severity.WARNING, "Demo content configuration could not be loaded.",
                    "Ensure demo-content.toml is present in the config directory."));
            return;
        }

        DemoContentConfig.DemoToggles toggles = demoConfig.getToggles();
        boolean configMaster = toggles != null && toggles.master();
        DemoContentSettings runtimeSettings = demoSettingsSupplier != null ? demoSettingsSupplier.get() : null;
        boolean runtimeMaster = runtimeSettings != null && runtimeSettings.masterEnabled();

        if (!configMaster) {
            results.add(entry(Severity.INFO, "Demo content disabled via configuration."));
            return;
        }

        if (!runtimeMaster) {
            results.add(entry(Severity.WARNING, "Demo content enabled in configuration but disabled at runtime.",
                    "Check setup overrides or apply settings with /mmocadm demo."));
            return;
        }

        results.add(entry(Severity.INFO, "Demo content active features: " + runtimeSettings.describeEnabledFeatures()));

        if (runtimeSettings.itemsEnabled() && (customItemRegistry == null || isEmpty(customItemRegistry.getAllItems()))) {
            results.add(entry(Severity.WARNING, "Demo items enabled but no custom items are registered.",
                    "Load the demo items or configure custom items in items.yml."));
        }
        if (runtimeSettings.skillsEnabled() && (skillRegistryService == null || isEmpty(skillRegistryService.getAllSkills()))) {
            results.add(entry(Severity.WARNING, "Demo skills enabled but no skills are registered.",
                    "Ensure demo skills are registered or review skill registry startup."));
        }
        if (runtimeSettings.resourceNodesEnabled()) {
            int nodeTypes = resourceNodeRegistryService != null
                    ? resourceNodeRegistryService.getAllNodeTypes().size()
                    : 0;
            if (resourceNodeRegistryService == null || nodeTypes == 0) {
                results.add(entry(Severity.WARNING, "Demo resource nodes enabled but no node types are registered.",
                        "Register resource node types or enable demo resource nodes."));
            }
            if (demoConfig.getResourceNodeTypes().isEmpty()) {
                results.add(entry(Severity.WARNING, "Demo content configuration defines no resource node types.",
                        "Add entries under resource-nodes in demo-content.toml."));
            }
        }
        if (runtimeSettings.lootTablesEnabled()
                && demoConfig.getGenericLootTables().isEmpty()
                && demoConfig.getMobLootTables().isEmpty()) {
            results.add(entry(Severity.WARNING, "Demo loot tables enabled but no loot tables configured.",
                    "Populate demo-content loot tables or disable the feature."));
        }
        if (runtimeSettings.customSpawnsEnabled() && demoConfig.getCustomSpawnRules().isEmpty()) {
            results.add(entry(Severity.WARNING, "Demo custom spawns enabled but no spawn rules are defined.",
                    "Add custom spawn rules to demo-content.toml."));
        }
    }

    private void checkCraftingRecipes(List<DiagnosticEntry> results) {
        if (recipeRegistryService == null) {
            results.add(entry(Severity.WARNING, "Recipe registry service is unavailable.",
                    "Custom crafting recipes cannot be used."));
            return;
        }
        List<CustomRecipe> recipes = recipeRegistryService.getAllRecipes();
        if (recipes == null || recipes.isEmpty()) {
            results.add(entry(Severity.INFO, "No custom crafting recipes are currently registered."));
            return;
        }
        recipes.forEach(recipe -> inspectRecipe(results, recipe));
        results.add(entry(Severity.INFO, "Runtime crafting recipes registered: " + recipes.size()));
    }

    private void inspectRecipe(List<DiagnosticEntry> results, CustomRecipe recipe) {
        if (recipe == null) {
            return;
        }
        String recipeId = recipe.getRecipeId();
        if (plugin instanceof MMOCraftPlugin mmocraftPlugin) {
            String outputCustomId = CustomItem.getItemId(recipe.getOutputItemStack(), mmocraftPlugin);
            if (outputCustomId != null
                    && (customItemRegistry == null || customItemRegistry.getCustomItem(outputCustomId).isEmpty())) {
                results.add(entry(Severity.WARNING, "Recipe '" + recipeId + "' outputs missing custom item '" + outputCustomId + "'.",
                        "Update crafting.toml or register the item."));
            }
        }

        Collection<CustomRecipeIngredient> ingredients = recipe.isShaped()
                ? recipe.getShapedIngredients().values()
                : recipe.getShapelessIngredients();
        for (CustomRecipeIngredient ingredient : ingredients) {
            if (ingredient == null) {
                continue;
            }
            if (ingredient.getType() == CustomRecipeIngredient.IngredientType.CUSTOM_ITEM) {
                if (customItemRegistry == null || customItemRegistry.getCustomItem(ingredient.getIdentifier()).isEmpty()) {
                    results.add(entry(Severity.ERROR, "Recipe '" + recipeId + "' requires missing custom item '" + ingredient.getIdentifier() + "'.",
                            "Players cannot craft this recipe until the item is registered."));
                }
            } else if (ingredient.getType() == CustomRecipeIngredient.IngredientType.VANILLA_MATERIAL) {
                if (Material.matchMaterial(ingredient.getIdentifier()) == null) {
                    results.add(entry(Severity.ERROR, "Recipe '" + recipeId + "' references unknown material '" + ingredient.getIdentifier() + "'.",
                            "Correct the recipe definition to use a valid material."));
                }
            }
        }
    }

    private void checkEventListeners(List<DiagnosticEntry> results) {
        if (plugin == null) {
            results.add(entry(Severity.WARNING, "Plugin reference unavailable to audit Bukkit listeners."));
            return;
        }
        Collection<RegisteredListener> registeredListeners = listenerLookup.apply(plugin);
        if (registeredListeners == null || registeredListeners.isEmpty()) {
            results.add(entry(Severity.ERROR, "No Bukkit event listeners are registered for MMOCraft.",
                    "Ensure registerListeners() runs during plugin startup."));
            return;
        }
        Set<Class<?>> registeredTypes = registeredListeners.stream()
                .map(RegisteredListener::getListener)
                .filter(Objects::nonNull)
                .map(Object::getClass)
                .collect(Collectors.toSet());
        for (Class<? extends Listener> expected : REQUIRED_LISTENERS) {
            boolean found = registeredTypes.stream().anyMatch(expected::isAssignableFrom);
            if (!found) {
                results.add(entry(Severity.WARNING, "Listener " + expected.getSimpleName() + " is not registered.",
                        "Gameplay systems tied to this listener may not function."));
            }
        }
        results.add(entry(Severity.INFO, "Bukkit listeners registered: " + registeredTypes.size()));
    }

    private void auditCraftingConfig(List<DiagnosticEntry> results, CraftingConfig craftingConfig) {
        if (craftingConfig == null) {
            results.add(entry(Severity.WARNING, "Crafting configuration failed to load.",
                    "Check crafting.toml for syntax errors."));
            return;
        }
        List<CraftingConfig.CraftingRecipeDefinition> definitions = craftingConfig.getRecipes();
        if (definitions.isEmpty()) {
            results.add(entry(Severity.INFO, "No crafting recipes defined in crafting.toml."));
            return;
        }
        for (CraftingConfig.CraftingRecipeDefinition definition : definitions) {
            if (definition == null || !definition.enabled()) {
                continue;
            }
            CraftingConfig.OutputDefinition output = definition.output();
            if (output != null && output.type() == CustomRecipeIngredient.IngredientType.CUSTOM_ITEM) {
                if (customItemRegistry == null || customItemRegistry.getCustomItem(output.identifier()).isEmpty()) {
                    results.add(entry(Severity.WARNING,
                            "Crafting recipe '" + definition.id() + "' outputs missing custom item '" + output.identifier() + "'.",
                            "Register the item or disable the recipe."));
                }
            }
            definition.shapelessIngredients().forEach(ingredient ->
                    validateIngredientDefinition(results, definition.id(), ingredient));
            definition.shapedIngredients().values().forEach(ingredient ->
                    validateIngredientDefinition(results, definition.id(), ingredient));
        }
    }

    private void validateIngredientDefinition(List<DiagnosticEntry> results,
                                              String recipeId,
                                              CraftingConfig.IngredientDefinition ingredient) {
        if (ingredient == null) {
            return;
        }
        if (ingredient.type() == CustomRecipeIngredient.IngredientType.CUSTOM_ITEM) {
            if (customItemRegistry == null || customItemRegistry.getCustomItem(ingredient.identifier()).isEmpty()) {
                results.add(entry(Severity.WARNING,
                        "Crafting recipe '" + recipeId + "' references missing custom item ingredient '" + ingredient.identifier() + "'.",
                        "Register the custom item referenced in crafting.toml."));
            }
        } else if (ingredient.type() == CustomRecipeIngredient.IngredientType.VANILLA_MATERIAL) {
            if (Material.matchMaterial(ingredient.identifier()) == null) {
                results.add(entry(Severity.ERROR,
                        "Crafting recipe '" + recipeId + "' references unknown material '" + ingredient.identifier() + "'.",
                        "Correct the material name in crafting.toml."));
            }
        }
    }

    private void auditLootAssignments(List<DiagnosticEntry> results, LootTablesConfig lootTablesConfig) {
        if (lootTablesConfig == null) {
            results.add(entry(Severity.WARNING, "Loot tables configuration failed to load.",
                    "Verify loot-tables.toml exists and is valid."));
            return;
        }
        Map<String, LootTablesConfig.LootTableDefinition> tables = lootTablesConfig.getTablesById();
        if (tables.isEmpty() && lootTablesConfig.getMobAssignments().isEmpty()) {
            results.add(entry(Severity.INFO, "No loot tables configured."));
        }
        for (Map.Entry<EntityType, String> assignment : lootTablesConfig.getMobAssignments().entrySet()) {
            if (!tables.containsKey(assignment.getValue())) {
                results.add(entry(Severity.WARNING,
                        "Mob loot assignment references unknown table '" + assignment.getValue() + "'.",
                        "Entity: " + assignment.getKey().name()));
            }
        }
    }

    private boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
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
