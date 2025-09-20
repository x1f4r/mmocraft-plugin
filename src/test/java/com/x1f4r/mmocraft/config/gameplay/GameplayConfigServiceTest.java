package com.x1f4r.mmocraft.config.gameplay;

import com.x1f4r.mmocraft.crafting.model.CustomRecipeIngredient;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GameplayConfigServiceTest {

    private static final String STATS_YAML = """
            defaults:
              base-investment: 4.0
              overrides:
                strength: 12.5
            scaling:
              health:
                base: 140.0
                per-point: 2.5
                per-level: 8.0
              critical_chance:
                base: 25.0
                per-point: 0.75
                max: 95.0
              defense:
                base: 5.0
                diminishing:
                  threshold: 750.0
                  multiplier: 0.4
            combat:
              defense-reduction-base: 120.0
              max-damage-reduction: 0.92
              max-evasion-chance: 0.55
            """;

    private static final String LOOT_YAML = """
            tables:
              config_test:
                entries:
                  - type: VANILLA
                    id: STONE
                    chance: 0.5
                    min: 1
                    max: 3
            mob-assignments:
              ZOMBIE: config_test
            """;

    private static final String DEMO_YAML = """
            toggles:
              master: true
              items: false
              skills: false
              loot-tables: false
              custom-spawns: false
              resource-nodes: false
              zones: false
            loot-tables:
              mobs: []
              generic: []
            resource-nodes:
              types: []
              placements: []
            custom-spawns: []
            """;

    private static final String CRAFTING_TOML = """
            [[recipes]]
            id = "shapeless_recipe"
            enabled = true
            type = "CUSTOM_SHAPELESS"

            [recipes.output]
            type = "vanilla"
            id = "STONE"
            amount = 2

            [[recipes.ingredients]]
            type = "custom"
            id = "demo_item"
            quantity = 1
            match-nbt = true

            [[recipes]]
            id = "shaped_recipe"
            enabled = true
            type = "CUSTOM_SHAPED"

            [recipes.output]
            type = "VANILLA_MATERIAL"
            id = "IRON_SWORD"
            amount = 1

            [recipes.shaped."0"]
            type = "vanilla"
            id = "STICK"
            quantity = 1

            [recipes.shaped."4"]
            type = "CUSTOM_ITEM"
            id = "demo_item"
            quantity = 1
            """;

    @TempDir
    Path tempDir;

    @Mock
    private LoggingUtil loggingUtil;

    @Test
    void reload_withValidConfigs_parsesAllSections() {
        GameplayConfigService service = createService(Map.of(
                "stats.yml", STATS_YAML,
                "loot_tables.yml", LOOT_YAML,
                "demo-content.yml", DEMO_YAML,
                "crafting.toml", CRAFTING_TOML
        ));

        StatScalingConfig stats = service.getStatScalingConfig();
        assertEquals(4.0, stats.getDefaultStatInvestment());
        assertEquals(12.5, stats.getDefaultStatValue(Stat.STRENGTH));
        assertEquals(140.0, stats.getStatRule(Stat.HEALTH).getBaseValue());
        assertEquals(2.5, stats.getStatRule(Stat.HEALTH).getPerPoint());
        assertEquals(0.75, stats.getStatRule(Stat.CRITICAL_CHANCE).getPerPoint());
        assertEquals(95.0, stats.getStatRule(Stat.CRITICAL_CHANCE).getMaxValue());
        assertEquals(120.0, stats.getDefenseReductionBase());
        assertEquals(0.92, stats.getMaxDamageReduction());
        assertEquals(0.55, stats.getMaxEvasionChance());
        assertEquals(750.0, stats.getStatRule(Stat.DEFENSE).getDiminishingReturns().threshold());
        assertEquals(0.4, stats.getStatRule(Stat.DEFENSE).getDiminishingReturns().multiplier());

        LootTablesConfig lootTables = service.getLootTablesConfig();
        assertEquals(1, lootTables.getTablesById().size());
        assertEquals("config_test", lootTables.getTable("config_test").tableId());
        assertEquals("config_test", lootTables.getMobAssignments().get(EntityType.ZOMBIE));

        DemoContentConfig demoConfig = service.getDemoContentConfig();
        assertTrue(demoConfig.getToggles().master());
        assertTrue(demoConfig.getGenericLootTables().isEmpty());

        CraftingConfig crafting = service.getCraftingConfig();
        assertEquals(2, crafting.getRecipes().size());
        CraftingConfig.CraftingRecipeDefinition shapeless = crafting.getRecipes().get(0);
        assertEquals(CustomRecipeIngredient.IngredientType.CUSTOM_ITEM,
                shapeless.shapelessIngredients().get(0).type());
        assertTrue(shapeless.shapelessIngredients().get(0).matchNbt());
        CraftingConfig.CraftingRecipeDefinition shaped = crafting.getRecipes().get(1);
        assertEquals(CustomRecipeIngredient.IngredientType.VANILLA_MATERIAL,
                shaped.shapedIngredients().get(0).type());
        assertEquals(CustomRecipeIngredient.IngredientType.CUSTOM_ITEM,
                shaped.shapedIngredients().get(4).type());

        assertTrue(service.getIssues().isEmpty());
    }

    @Test
    void reload_missingResources_usesDefaultsAndRecordsIssues() {
        GameplayConfigService service = createService(Map.of());

        StatScalingConfig stats = service.getStatScalingConfig();
        assertEquals(StatScalingConfig.defaults().getStatRule(Stat.HEALTH).getBaseValue(),
                stats.getStatRule(Stat.HEALTH).getBaseValue());
        assertTrue(service.getCraftingConfig().getRecipes().isEmpty());

        List<GameplayConfigIssue> issues = service.getIssues();
        assertFalse(issues.isEmpty());
        assertTrue(issues.stream()
                .anyMatch(issue -> issue.message().contains("Missing bundled config resource")));
    }

    private GameplayConfigService createService(Map<String, String> resources) {
        Function<String, InputStream> supplier = name -> {
            String content = resources.get(name);
            return content != null
                    ? new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
                    : null;
        };
        return new GameplayConfigService(tempDir.resolve("config"), supplier, loggingUtil);
    }
}
