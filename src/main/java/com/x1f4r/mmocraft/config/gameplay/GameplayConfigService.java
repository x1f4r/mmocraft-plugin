package com.x1f4r.mmocraft.config.gameplay;

import com.x1f4r.mmocraft.crafting.model.CustomRecipeIngredient;
import com.x1f4r.mmocraft.crafting.model.RecipeType;
import com.x1f4r.mmocraft.loot.model.LootType;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Loads and hot-reloads gameplay configuration files backed by YAML/TOML documents.
 */
public class GameplayConfigService {

    private final Path configDirectory;
    private final Function<String, InputStream> resourceSupplier;
    private final LoggingUtil logger;

    private StatScalingConfig statScalingConfig = StatScalingConfig.defaults();
    private RuntimeStatConfig runtimeStatConfig = RuntimeStatConfig.defaults();
    private LootTablesConfig lootTablesConfig = LootTablesConfig.defaults();
    private DemoContentConfig demoContentConfig = DemoContentConfig.defaults();
    private CraftingConfig craftingConfig = CraftingConfig.defaults();
    private final List<GameplayConfigIssue> issues = new ArrayList<>();

    public GameplayConfigService(Path configDirectory,
                                 Function<String, InputStream> resourceSupplier,
                                 LoggingUtil logger) {
        this.configDirectory = Objects.requireNonNull(configDirectory, "configDirectory");
        this.resourceSupplier = Objects.requireNonNull(resourceSupplier, "resourceSupplier");
        this.logger = Objects.requireNonNull(logger, "logger");
        reload();
    }

    public synchronized void reload() {
        issues.clear();
        ensureDirectory();
        statScalingConfig = loadStatScalingConfig();
        lootTablesConfig = loadLootTablesConfig();
        demoContentConfig = loadDemoContentConfig();
        craftingConfig = loadCraftingConfig();
    }

    public StatScalingConfig getStatScalingConfig() {
        return statScalingConfig;
    }

    public LootTablesConfig getLootTablesConfig() {
        return lootTablesConfig;
    }

    public RuntimeStatConfig getRuntimeStatConfig() {
        return runtimeStatConfig;
    }

    public DemoContentConfig getDemoContentConfig() {
        return demoContentConfig;
    }

    public CraftingConfig getCraftingConfig() {
        return craftingConfig;
    }

    public List<GameplayConfigIssue> getIssues() {
        return List.copyOf(issues);
    }

    private void ensureDirectory() {
        try {
            Files.createDirectories(configDirectory);
        } catch (IOException e) {
            issues.add(GameplayConfigIssue.error("Failed to create gameplay config directory", e.getMessage()));
            logger.severe("Unable to create gameplay config directory: " + e.getMessage(), e);
        }
    }

    private Path ensureFile(String resourceName) {
        Path target = configDirectory.resolve(resourceName);
        if (Files.exists(target)) {
            return target;
        }
        try (InputStream in = resourceSupplier.apply(resourceName)) {
            if (in == null) {
                issues.add(GameplayConfigIssue.error("Missing bundled config resource", resourceName));
                return target;
            }
            Files.createDirectories(target.getParent());
            Files.copy(in, target);
            logger.info("Created default gameplay config: " + resourceName);
        } catch (IOException ex) {
            issues.add(GameplayConfigIssue.error("Failed to write default config " + resourceName, ex.getMessage()));
            logger.severe("Unable to copy default config " + resourceName + ": " + ex.getMessage(), ex);
        }
        return target;
    }

    private StatScalingConfig loadStatScalingConfig() {
        Path file = ensureFile("stats.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        runtimeStatConfig = loadRuntimeStatConfig(yaml);
        StatScalingConfig defaults = StatScalingConfig.defaults();
        StatScalingConfig.Builder builder = StatScalingConfig.builder(defaults);

        builder.defaultStatInvestment(yaml.getDouble("defaults.base-investment", defaults.getDefaultStatInvestment()));
        ConfigurationSection overrides = yaml.getConfigurationSection("defaults.overrides");
        if (overrides != null) {
            Map<Stat, Double> overrideMap = new EnumMap<>(Stat.class);
            for (String key : overrides.getKeys(false)) {
                try {
                    Stat stat = Stat.valueOf(key.toUpperCase(Locale.ROOT));
                    overrideMap.put(stat, overrides.getDouble(key));
                } catch (IllegalArgumentException ex) {
                    issues.add(GameplayConfigIssue.warn("Unknown stat override in stats.yml", key));
                }
            }
            builder.overrides(overrideMap);
        }

        ConfigurationSection scalingSection = yaml.getConfigurationSection("scaling");
        if (scalingSection != null) {
            for (String key : scalingSection.getKeys(false)) {
                try {
                    Stat stat = Stat.valueOf(key.toUpperCase(Locale.ROOT));
                    ConfigurationSection statSection = scalingSection.getConfigurationSection(key);
                    if (statSection != null) {
                        StatScalingConfig.StatRule baseRule = defaults.getStatRule(stat);
                        StatScalingConfig.StatRule parsed = parseStatRule(statSection, baseRule);
                        builder.statRule(stat, parsed);
                    }
                } catch (IllegalArgumentException ex) {
                    issues.add(GameplayConfigIssue.warn("Unknown stat entry in stats.yml scaling section", key));
                }
            }
        }

        builder.defenseReductionBase(yaml.getDouble("combat.defense-reduction-base", defaults.getDefenseReductionBase()));
        builder.trueDefenseReductionBase(yaml.getDouble("combat.true-defense-reduction-base", defaults.getTrueDefenseReductionBase()));
        builder.maxDamageReduction(yaml.getDouble("combat.max-damage-reduction", defaults.getMaxDamageReduction()));
        builder.maxEvasionChance(yaml.getDouble("combat.max-evasion-chance", defaults.getMaxEvasionChance()));

        return builder.build();
    }

    private RuntimeStatConfig loadRuntimeStatConfig(YamlConfiguration yaml) {
        RuntimeStatConfig defaults = RuntimeStatConfig.defaults();
        RuntimeStatConfig.Builder builder = defaults.toBuilder();

        ConfigurationSection movement = yaml.getConfigurationSection("runtime.movement");
        if (movement != null) {
            builder.movementSettings(RuntimeStatConfig.MovementSettings.builder()
                    .baseWalkSpeed(movement.getDouble("base-walk-speed", defaults.getMovementSettings().getBaseWalkSpeed()))
                    .maxWalkSpeed(movement.getDouble("max-walk-speed", defaults.getMovementSettings().getMaxWalkSpeed()))
                    .minWalkSpeed(movement.getDouble("min-walk-speed", defaults.getMovementSettings().getMinWalkSpeed()))
                    .speedBaseline(movement.getDouble("speed-baseline", defaults.getMovementSettings().getSpeedBaseline())));
        }

        ConfigurationSection combat = yaml.getConfigurationSection("runtime.combat");
        if (combat != null) {
            builder.combatSettings(RuntimeStatConfig.CombatSettings.builder()
                    .baseAttackSpeed(combat.getDouble("base-attack-speed", defaults.getCombatSettings().getBaseAttackSpeed()))
                    .attackSpeedPerPoint(combat.getDouble("attack-speed-per-point", defaults.getCombatSettings().getAttackSpeedPerPoint()))
                    .maxAttackSpeed(combat.getDouble("max-attack-speed", defaults.getCombatSettings().getMaxAttackSpeed()))
                    .strengthPhysicalScaling(combat.getDouble("strength-physical-scaling", defaults.getCombatSettings().getStrengthPhysicalScaling()))
                    .intelligenceMagicalScaling(combat.getDouble("intelligence-magical-scaling", defaults.getCombatSettings().getIntelligenceMagicalScaling()))
                    .abilityPowerPercentPerPoint(combat.getDouble("ability-power-percent-per-point", defaults.getCombatSettings().getAbilityPowerPercentPerPoint()))
                    .ferocityPerExtraHit(combat.getDouble("ferocity-per-extra-hit", defaults.getCombatSettings().getFerocityPerExtraHit()))
                    .ferocityMaxExtraHits(combat.getInt("ferocity-max-extra-hits", defaults.getCombatSettings().getFerocityMaxExtraHits()))
                    .mobDefenseReductionFactor(combat.getDouble("mob-defense-reduction-factor", defaults.getCombatSettings().getMobDefenseReductionFactor())));
        }

        ConfigurationSection ability = yaml.getConfigurationSection("runtime.ability");
        if (ability != null) {
            builder.abilitySettings(RuntimeStatConfig.AbilitySettings.builder()
                    .cooldownReductionPerAttackSpeedPoint(ability.getDouble("cooldown-reduction-per-attack-speed-point", defaults.getAbilitySettings().getCooldownReductionPerAttackSpeedPoint()))
                    .cooldownReductionPerIntelligencePoint(ability.getDouble("cooldown-reduction-per-intelligence-point", defaults.getAbilitySettings().getCooldownReductionPerIntelligencePoint()))
                    .minimumCooldownSeconds(ability.getDouble("minimum-cooldown-seconds", defaults.getAbilitySettings().getMinimumCooldownSeconds()))
                    .manaCostReductionPerIntelligencePoint(ability.getDouble("mana-cost-reduction-per-intelligence-point", defaults.getAbilitySettings().getManaCostReductionPerIntelligencePoint()))
                    .manaCostReductionPerAbilityPowerPoint(ability.getDouble("mana-cost-reduction-per-ability-power-point", defaults.getAbilitySettings().getManaCostReductionPerAbilityPowerPoint()))
                    .minimumManaCostMultiplier(ability.getDouble("minimum-mana-cost-multiplier", defaults.getAbilitySettings().getMinimumManaCostMultiplier()))
                    .minimumManaCost(ability.getDouble("minimum-mana-cost", defaults.getAbilitySettings().getMinimumManaCost())));
        }

        ConfigurationSection gathering = yaml.getConfigurationSection("runtime.gathering");
        if (gathering != null) {
            RuntimeStatConfig.GatheringSettings.Builder gatheringBuilder = RuntimeStatConfig.GatheringSettings.builder()
                    .baseGatherDelaySeconds(gathering.getDouble("base-gather-delay-seconds", defaults.getGatheringSettings().getBaseGatherDelaySeconds()))
                    .minimumGatherDelaySeconds(gathering.getDouble("minimum-gather-delay-seconds", defaults.getGatheringSettings().getMinimumGatherDelaySeconds()))
                    .miningSpeedDelayDivisor(gathering.getDouble("mining-speed-delay-divisor", defaults.getGatheringSettings().getMiningSpeedDelayDivisor()))
                    .miningSpeedHastePerTier(gathering.getDouble("mining-speed-haste-per-tier", defaults.getGatheringSettings().getMiningSpeedHastePerTier()))
                    .miningSpeedMaxHasteTier(gathering.getInt("mining-speed-max-haste-tier", defaults.getGatheringSettings().getMiningSpeedMaxHasteTier()));

            Map<Stat, Double> fortuneDefaults = new EnumMap<>(Stat.class);
            fortuneDefaults.put(Stat.MINING_FORTUNE, defaults.getGatheringSettings().getFortunePerPoint(Stat.MINING_FORTUNE));
            fortuneDefaults.put(Stat.FARMING_FORTUNE, defaults.getGatheringSettings().getFortunePerPoint(Stat.FARMING_FORTUNE));
            fortuneDefaults.put(Stat.FORAGING_FORTUNE, defaults.getGatheringSettings().getFortunePerPoint(Stat.FORAGING_FORTUNE));
            fortuneDefaults.put(Stat.FISHING_FORTUNE, defaults.getGatheringSettings().getFortunePerPoint(Stat.FISHING_FORTUNE));

            ConfigurationSection fortuneSection = gathering.getConfigurationSection("fortune-per-point");
            if (fortuneSection != null) {
                for (Map.Entry<Stat, Double> entry : fortuneDefaults.entrySet()) {
                    String key = entry.getKey().name().toLowerCase();
                    double value = fortuneSection.getDouble(key, entry.getValue());
                    gatheringBuilder.fortunePerPoint(entry.getKey(), value);
                }
            } else {
                for (Map.Entry<Stat, Double> entry : fortuneDefaults.entrySet()) {
                    gatheringBuilder.fortunePerPoint(entry.getKey(), entry.getValue());
                }
            }

            builder.gatheringSettings(gatheringBuilder);
        }

        ConfigurationSection mobs = yaml.getConfigurationSection("runtime.mobs");
        if (mobs != null) {
            builder.mobScalingSettings(RuntimeStatConfig.MobScalingSettings.builder()
                    .healthPerLevelPercent(mobs.getDouble("health-per-level-percent", defaults.getMobScalingSettings().getHealthPerLevelPercent()))
                    .damagePerLevelPercent(mobs.getDouble("damage-per-level-percent", defaults.getMobScalingSettings().getDamagePerLevelPercent()))
                    .defensePerLevel(mobs.getDouble("defense-per-level", defaults.getMobScalingSettings().getDefensePerLevel()))
                    .maxHealthMultiplier(mobs.getDouble("max-health-multiplier", defaults.getMobScalingSettings().getMaxHealthMultiplier()))
                    .maxDamageMultiplier(mobs.getDouble("max-damage-multiplier", defaults.getMobScalingSettings().getMaxDamageMultiplier()))
                    .maxDefenseBonus(mobs.getDouble("max-defense-bonus", defaults.getMobScalingSettings().getMaxDefenseBonus())));
        }

        return builder.build();
    }

    private StatScalingConfig.StatRule parseStatRule(ConfigurationSection section, StatScalingConfig.StatRule defaults) {
        StatScalingConfig.StatRule.Builder ruleBuilder = defaults == null
                ? StatScalingConfig.StatRule.builder()
                : defaults.toBuilder();

        StatScalingConfig.StatRule template = defaults != null ? defaults : ruleBuilder.build();
        double defaultBase = template.getBaseValue();
        double defaultPerPoint = template.getPerPoint();
        double defaultPerLevel = template.getPerLevel();
        double defaultMin = template.getMinValue();
        Double defaultMax = template.getMaxValue();

        if (section.contains("base")) {
            ruleBuilder.baseValue(section.getDouble("base", defaultBase));
        }
        if (section.contains("per-point")) {
            ruleBuilder.perPoint(section.getDouble("per-point", defaultPerPoint));
        }
        if (section.contains("per-level")) {
            ruleBuilder.perLevel(section.getDouble("per-level", defaultPerLevel));
        }
        if (section.contains("min")) {
            ruleBuilder.minValue(section.getDouble("min", defaultMin));
        }
        if (section.contains("max")) {
            Object maxObj = section.get("max");
            if (maxObj == null) {
                ruleBuilder.maxValue(null);
            } else {
                ruleBuilder.maxValue(section.getDouble("max", defaultMax != null ? defaultMax : 0.0));
            }
        }

        if (section.isConfigurationSection("diminishing")) {
            ConfigurationSection dim = section.getConfigurationSection("diminishing");
            double threshold = dim.getDouble("threshold",
                    defaults != null && defaults.getDiminishingReturns() != null
                            ? defaults.getDiminishingReturns().threshold()
                            : Double.MAX_VALUE);
            double multiplier = dim.getDouble("multiplier",
                    defaults != null && defaults.getDiminishingReturns() != null
                            ? defaults.getDiminishingReturns().multiplier()
                            : 1.0);
            ruleBuilder.diminishingReturns(new StatScalingConfig.DiminishingReturns(threshold, multiplier));
        }

        return ruleBuilder.build();
    }

    private LootTablesConfig loadLootTablesConfig() {
        Path file = ensureFile("loot_tables.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        Map<String, LootTablesConfig.LootTableDefinition> tables = new HashMap<>();
        ConfigurationSection tablesSection = yaml.getConfigurationSection("tables");
        if (tablesSection != null) {
            for (String key : tablesSection.getKeys(false)) {
                List<LootTablesConfig.LootEntryDefinition> entries = new ArrayList<>();
                List<Map<?, ?>> rawEntries = tablesSection.getMapList(key + ".entries");
                for (Map<?, ?> raw : rawEntries) {
                    LootTablesConfig.LootEntryDefinition entry = parseLootEntry("loot_tables.yml", key, raw);
                    if (entry != null) {
                        entries.add(entry);
                    }
                }
                tables.put(key, new LootTablesConfig.LootTableDefinition(key, entries));
            }
        }
        Map<EntityType, String> mobAssignments = new EnumMap<>(EntityType.class);
        ConfigurationSection mobs = yaml.getConfigurationSection("mob-assignments");
        if (mobs != null) {
            for (String key : mobs.getKeys(false)) {
                try {
                    EntityType type = EntityType.valueOf(key.toUpperCase(Locale.ROOT));
                    mobAssignments.put(type, mobs.getString(key));
                } catch (IllegalArgumentException ex) {
                    issues.add(GameplayConfigIssue.warn("Unknown EntityType in loot_tables.yml mob-assignments", key));
                }
            }
        }
        return new LootTablesConfig(tables, mobAssignments);
    }

    private LootTablesConfig.LootEntryDefinition parseLootEntry(String fileName, String tableId, Map<?, ?> raw) {
        try {
            String typeName = Objects.toString(raw.get("type"));
            LootType type = LootType.valueOf(typeName.toUpperCase(Locale.ROOT));
            String identifier = Objects.toString(raw.get("id"));
            double chance = raw.containsKey("chance") ? ((Number) raw.get("chance")).doubleValue() : 1.0;
            int min = raw.containsKey("min") ? ((Number) raw.get("min")).intValue() : 1;
            int max = raw.containsKey("max") ? ((Number) raw.get("max")).intValue() : min;
            return new LootTablesConfig.LootEntryDefinition(type, identifier, chance, min, max);
        } catch (Exception ex) {
            issues.add(GameplayConfigIssue.warn("Invalid loot entry in " + fileName + " for table " + tableId, ex.getMessage()));
            return null;
        }
    }

    private DemoContentConfig loadDemoContentConfig() {
        Path file = ensureFile("demo-content.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        DemoContentConfig.DemoToggles defaults = DemoContentConfig.DemoToggles.defaults();
        DemoContentConfig.DemoToggles toggles = new DemoContentConfig.DemoToggles(
                yaml.getBoolean("toggles.master", defaults.master()),
                yaml.getBoolean("toggles.items", defaults.items()),
                yaml.getBoolean("toggles.skills", defaults.skills()),
                yaml.getBoolean("toggles.loot-tables", defaults.lootTables()),
                yaml.getBoolean("toggles.custom-spawns", defaults.customSpawns()),
                yaml.getBoolean("toggles.resource-nodes", defaults.resourceNodes()),
                yaml.getBoolean("toggles.zones", defaults.zones())
        );

        List<Map<?, ?>> genericLootRaw = yaml.getMapList("loot-tables.generic");
        List<DemoContentConfig.LootTableDefinition> genericTables = new ArrayList<>();
        for (Map<?, ?> rawTable : genericLootRaw) {
            DemoContentConfig.LootTableDefinition definition = parseDemoLootTable(rawTable, "generic");
            if (definition != null) {
                genericTables.add(definition);
            }
        }

        List<Map<?, ?>> mobLootRaw = yaml.getMapList("loot-tables.mobs");
        List<DemoContentConfig.MobLootTableDefinition> mobTables = new ArrayList<>();
        for (Map<?, ?> raw : mobLootRaw) {
            try {
                String entityName = Objects.toString(raw.get("entity"));
                EntityType type = EntityType.valueOf(entityName.toUpperCase(Locale.ROOT));
                DemoContentConfig.LootTableDefinition table = parseDemoLootTable(raw, entityName);
                if (table != null) {
                    mobTables.add(new DemoContentConfig.MobLootTableDefinition(type, table));
                }
            } catch (Exception ex) {
                issues.add(GameplayConfigIssue.warn("Invalid mob loot table in demo-content.yml", ex.getMessage()));
            }
        }

        List<Map<?, ?>> resourceTypeRaw = yaml.getMapList("resource-nodes.types");
        List<DemoContentConfig.ResourceNodeTypeConfig> resourceTypes = new ArrayList<>();
        for (Map<?, ?> raw : resourceTypeRaw) {
            DemoContentConfig.ResourceNodeTypeConfig config = parseResourceNodeType(raw);
            if (config != null) {
                resourceTypes.add(config);
            }
        }

        List<Map<?, ?>> placementRaw = yaml.getMapList("resource-nodes.placements");
        List<DemoContentConfig.ResourceNodePlacementConfig> placements = new ArrayList<>();
        for (Map<?, ?> raw : placementRaw) {
            try {
                String typeId = Objects.toString(raw.get("node-type-id"));
                String world = raw.containsKey("world") ? Objects.toString(raw.get("world")) : null;
                int x = raw.containsKey("x") ? ((Number) raw.get("x")).intValue() : 0;
                int y = raw.containsKey("y") ? ((Number) raw.get("y")).intValue() : 0;
                int z = raw.containsKey("z") ? ((Number) raw.get("z")).intValue() : 0;
                placements.add(new DemoContentConfig.ResourceNodePlacementConfig(typeId, world, x, y, z));
            } catch (Exception ex) {
                issues.add(GameplayConfigIssue.warn("Invalid resource node placement in demo-content.yml", ex.getMessage()));
            }
        }

        List<Map<?, ?>> customSpawnRaw = yaml.getMapList("custom-spawns");
        List<DemoContentConfig.CustomSpawnRuleConfig> customSpawns = new ArrayList<>();
        for (Map<?, ?> raw : customSpawnRaw) {
            DemoContentConfig.CustomSpawnRuleConfig config = parseCustomSpawnRule(raw);
            if (config != null) {
                customSpawns.add(config);
            }
        }

        return new DemoContentConfig(toggles, genericTables, mobTables, resourceTypes, placements, customSpawns);
    }

    private DemoContentConfig.LootTableDefinition parseDemoLootTable(Map<?, ?> rawTable, String context) {
        try {
            String tableId = Objects.toString(rawTable.get("table-id"));
            List<Map<?, ?>> entriesRaw = (List<Map<?, ?>>) rawTable.get("entries");
            List<DemoContentConfig.LootEntryDefinition> entries = new ArrayList<>();
            if (entriesRaw != null) {
                for (Map<?, ?> rawEntry : entriesRaw) {
                    LootTablesConfig.LootEntryDefinition entry = parseLootEntry("demo-content.yml", tableId, rawEntry);
                    if (entry != null) {
                        entries.add(new DemoContentConfig.LootEntryDefinition(entry.type(), entry.identifier(), entry.chance(), entry.minAmount(), entry.maxAmount()));
                    }
                }
            }
            return new DemoContentConfig.LootTableDefinition(tableId, entries);
        } catch (Exception ex) {
            issues.add(GameplayConfigIssue.warn("Invalid loot table in demo-content.yml", context + ": " + ex.getMessage()));
            return null;
        }
    }

    private DemoContentConfig.ResourceNodeTypeConfig parseResourceNodeType(Map<?, ?> raw) {
        try {
            String typeId = Objects.toString(raw.get("type-id"));
            Material material = Material.valueOf(Objects.toString(raw.get("block")).toUpperCase(Locale.ROOT));
            double harvestSeconds = raw.containsKey("harvest-seconds")
                    ? ((Number) raw.get("harvest-seconds")).doubleValue()
                    : 0.0;
            Object toolsObject = raw.get("required-tools");
            List<?> toolsRaw = toolsObject instanceof List ? (List<?>) toolsObject : List.of();
            Set<Material> tools = EnumSet.noneOf(Material.class);
            for (Object toolObj : toolsRaw) {
                String tool = Objects.toString(toolObj);
                try {
                    tools.add(Material.valueOf(tool.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    issues.add(GameplayConfigIssue.warn("Unknown material in demo resource node tools", tool));
                }
            }
            String lootTableId = Objects.toString(raw.get("loot-table-id"));
            int respawnSeconds = raw.containsKey("respawn-seconds")
                    ? ((Number) raw.get("respawn-seconds")).intValue()
                    : 0;
            String displayName = Objects.toString(raw.get("display-name"));
            return new DemoContentConfig.ResourceNodeTypeConfig(typeId, material, harvestSeconds, tools, lootTableId, respawnSeconds, displayName);
        } catch (Exception ex) {
            issues.add(GameplayConfigIssue.warn("Invalid resource node type in demo-content.yml", ex.getMessage()));
            return null;
        }
    }

    private DemoContentConfig.CustomSpawnRuleConfig parseCustomSpawnRule(Map<?, ?> raw) {
        try {
            String ruleId = Objects.toString(raw.get("rule-id"));
            String mobId = Objects.toString(raw.get("mob-id"));
            EntityType entityType = EntityType.valueOf(Objects.toString(raw.get("entity-type")).toUpperCase(Locale.ROOT));
            String displayName = Objects.toString(raw.get("display-name"));
            String statProfileId = Objects.toString(raw.get("stat-profile-id"));
            String lootTableId = Objects.toString(raw.get("loot-table-id"));

            Object equipmentObject = raw.get("equipment");
            Map<?, ?> equipmentRaw = equipmentObject instanceof Map ? (Map<?, ?>) equipmentObject : Map.of();
            Map<EquipmentSlot, Material> equipment = new EnumMap<>(EquipmentSlot.class);
            for (Map.Entry<?, ?> entry : equipmentRaw.entrySet()) {
                try {
                    EquipmentSlot slot = EquipmentSlot.valueOf(Objects.toString(entry.getKey()).toUpperCase(Locale.ROOT));
                    Material mat = Material.valueOf(Objects.toString(entry.getValue()).toUpperCase(Locale.ROOT));
                    equipment.put(slot, mat);
                } catch (IllegalArgumentException ex) {
                    issues.add(GameplayConfigIssue.warn("Invalid equipment entry in demo-content.yml",
                            Objects.toString(entry.getKey())));
                }
            }

            Object biomesObject = raw.get("biomes");
            List<?> biomesRaw = biomesObject instanceof List ? (List<?>) biomesObject : List.of();
            Set<Biome> biomes = new HashSet<>();
            for (Object biomeObj : biomesRaw) {
                String biomeName = Objects.toString(biomeObj);
                try {
                    biomes.add(Biome.valueOf(biomeName.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    issues.add(GameplayConfigIssue.warn("Unknown biome in demo-content.yml", biomeName));
                }
            }

            Object timeWindowObject = raw.get("time-window");
            Map<?, ?> timeWindowRaw = timeWindowObject instanceof Map ? (Map<?, ?>) timeWindowObject : null;
            DemoContentConfig.TimeWindow timeWindow = null;
            if (timeWindowRaw != null) {
                int start = timeWindowRaw.containsKey("start") ? ((Number) timeWindowRaw.get("start")).intValue() : 0;
                int end = timeWindowRaw.containsKey("end") ? ((Number) timeWindowRaw.get("end")).intValue() : 0;
                timeWindow = new DemoContentConfig.TimeWindow(start, end);
            } else {
                timeWindow = new DemoContentConfig.TimeWindow(0, 23999);
            }

            double spawnChance = raw.containsKey("spawn-chance")
                    ? ((Number) raw.get("spawn-chance")).doubleValue()
                    : 0.0;
            int minY = raw.containsKey("min-y") ? ((Number) raw.get("min-y")).intValue() : 0;
            int maxY = raw.containsKey("max-y") ? ((Number) raw.get("max-y")).intValue() : 256;
            int maxNearby = raw.containsKey("max-nearby") ? ((Number) raw.get("max-nearby")).intValue() : 0;
            double radius = raw.containsKey("radius") ? ((Number) raw.get("radius")).doubleValue() : 0.0;
            long intervalTicks = raw.containsKey("interval-ticks")
                    ? ((Number) raw.get("interval-ticks")).longValue()
                    : 0L;

            return new DemoContentConfig.CustomSpawnRuleConfig(ruleId, mobId, entityType, displayName, statProfileId,
                    lootTableId, equipment, biomes, timeWindow, spawnChance, minY, maxY, maxNearby, radius, intervalTicks);
        } catch (Exception ex) {
            issues.add(GameplayConfigIssue.warn("Invalid custom spawn rule in demo-content.yml", ex.getMessage()));
            return null;
        }
    }

    private CraftingConfig loadCraftingConfig() {
        Path file = ensureFile("crafting.toml");
        TomlParseResult result;
        try {
            result = Toml.parse(file);
        } catch (IOException e) {
            issues.add(GameplayConfigIssue.error("Failed to read crafting.toml", e.getMessage()));
            logger.severe("Unable to parse crafting.toml: " + e.getMessage(), e);
            return CraftingConfig.defaults();
        }
        if (result.hasErrors()) {
            result.errors().forEach(error ->
                    issues.add(GameplayConfigIssue.error("crafting.toml parse error", error.toString())));
        }
        List<CraftingConfig.CraftingRecipeDefinition> recipes = new ArrayList<>();
        TomlArray recipeArray = result.getArray("recipes");
        if (recipeArray != null) {
            for (int i = 0; i < recipeArray.size(); i++) {
                TomlTable table = recipeArray.getTable(i);
                if (table == null) continue;
                CraftingConfig.CraftingRecipeDefinition definition = parseCraftingRecipe(table);
                if (definition != null) {
                    recipes.add(definition);
                }
            }
        }
        return new CraftingConfig(recipes);
    }

    private CraftingConfig.CraftingRecipeDefinition parseCraftingRecipe(TomlTable table) {
        try {
            String id = table.getString("id");
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Recipe id missing");
            }
            boolean enabled = table.getBoolean("enabled") != null ? table.getBoolean("enabled") : true;
            String typeName = Objects.requireNonNull(table.getString("type"), "type");
            RecipeType type = RecipeType.valueOf(typeName.toUpperCase(Locale.ROOT));
            String permission = table.getString("permission");

            TomlTable outputTable = table.getTable("output");
            if (outputTable == null) {
                throw new IllegalArgumentException("output section missing");
            }
            CraftingConfig.OutputDefinition output = parseOutputDefinition(outputTable);

            List<CraftingConfig.IngredientDefinition> shapeless = new ArrayList<>();
            TomlArray ingredientsArray = table.getArray("ingredients");
            if (ingredientsArray != null) {
                for (int i = 0; i < ingredientsArray.size(); i++) {
                    TomlTable ingredientTable = ingredientsArray.getTable(i);
                    if (ingredientTable != null) {
                        shapeless.add(parseIngredientDefinition(ingredientTable));
                    }
                }
            }

            Map<Integer, CraftingConfig.IngredientDefinition> shaped = new HashMap<>();
            TomlTable shapedTable = table.getTable("shaped");
            if (shapedTable != null) {
                for (String key : shapedTable.keySet()) {
                    try {
                        int slot = Integer.parseInt(key);
                        TomlTable ingredientTable = shapedTable.getTable(key);
                        if (ingredientTable != null) {
                            shaped.put(slot, parseIngredientDefinition(ingredientTable));
                        }
                    } catch (NumberFormatException ex) {
                        issues.add(GameplayConfigIssue.warn("Invalid shaped slot in crafting.toml", key));
                    }
                }
            }

            return new CraftingConfig.CraftingRecipeDefinition(id, enabled, type, output, shapeless, shaped, permission);
        } catch (Exception ex) {
            issues.add(GameplayConfigIssue.warn("Invalid crafting recipe in crafting.toml", ex.getMessage()));
            return null;
        }
    }

    private CraftingConfig.OutputDefinition parseOutputDefinition(TomlTable table) {
        String typeName = Objects.requireNonNull(table.getString("type"), "output.type");
        CustomRecipeIngredient.IngredientType type = parseIngredientType(typeName, "output.type");
        String identifier = Objects.requireNonNull(table.getString("id"), "output.id");
        int amount = table.getLong("amount") != null ? Math.toIntExact(table.getLong("amount")) : 1;
        return new CraftingConfig.OutputDefinition(type, identifier, amount);
    }

    private CraftingConfig.IngredientDefinition parseIngredientDefinition(TomlTable table) {
        String typeName = Objects.requireNonNull(table.getString("type"), "ingredient.type");
        CustomRecipeIngredient.IngredientType type = parseIngredientType(typeName, "ingredient.type");
        String identifier = Objects.requireNonNull(table.getString("id"), "ingredient.id");
        int quantity = table.getLong("quantity") != null ? Math.toIntExact(table.getLong("quantity")) : 1;
        boolean matchNbt = table.getBoolean("match-nbt") != null && table.getBoolean("match-nbt");
        return new CraftingConfig.IngredientDefinition(type, identifier, quantity, matchNbt);
    }

    private CustomRecipeIngredient.IngredientType parseIngredientType(String rawType, String context) {
        String normalized = rawType.trim().toUpperCase(Locale.ROOT);
        try {
            return switch (normalized) {
                case "CUSTOM", "CUSTOM_ITEM" -> CustomRecipeIngredient.IngredientType.CUSTOM_ITEM;
                case "VANILLA", "VANILLA_MATERIAL" -> CustomRecipeIngredient.IngredientType.VANILLA_MATERIAL;
                default -> CustomRecipeIngredient.IngredientType.valueOf(normalized);
            };
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(context + " has unknown ingredient type '" + rawType + "'", ex);
        }
    }
}
