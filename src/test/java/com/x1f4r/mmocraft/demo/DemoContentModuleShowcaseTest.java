package com.x1f4r.mmocraft.demo;

import com.x1f4r.mmocraft.config.gameplay.DemoContentConfig;
import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.crafting.recipe.CustomRecipe;
import com.x1f4r.mmocraft.crafting.service.RecipeRegistryService;
import com.x1f4r.mmocraft.demo.item.AnglersTidalRod;
import com.x1f4r.mmocraft.demo.item.BerserkerGauntlet;
import com.x1f4r.mmocraft.demo.item.BlazingEmberRod;
import com.x1f4r.mmocraft.demo.item.ForagersHatchet;
import com.x1f4r.mmocraft.demo.item.GuardianBulwark;
import com.x1f4r.mmocraft.demo.item.HarvestersScythe;
import com.x1f4r.mmocraft.demo.item.LuckyCharmTalisman;
import com.x1f4r.mmocraft.demo.item.ProspectorsDrill;
import com.x1f4r.mmocraft.demo.item.WindrunnerBoots;
import com.x1f4r.mmocraft.demo.skill.BerserkerRageSkill;
import com.x1f4r.mmocraft.demo.skill.GaleForceDashSkill;
import com.x1f4r.mmocraft.demo.skill.HarvestRallySkill;
import com.x1f4r.mmocraft.demo.skill.InfernoBurstSkill;
import com.x1f4r.mmocraft.demo.skill.ProspectorPulseSkill;
import com.x1f4r.mmocraft.demo.skill.TidalSurgeSkill;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.skill.model.Skill;
import com.x1f4r.mmocraft.skill.service.SkillRegistryService;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DemoContentModuleShowcaseTest {

    @Mock
    private MMOCraftPlugin plugin;
    @Mock
    private LoggingUtil loggingUtil;
    @Mock
    private CustomItemRegistry itemRegistry;
    @Mock
    private SkillRegistryService skillRegistry;
    @Mock
    private RecipeRegistryService recipeRegistry;

    private DemoContentModule module;

    private Map<String, CustomItem> registeredItems;
    private Map<String, Skill> registeredSkills;
    private Map<String, CustomRecipe> registeredRecipes;
    private Map<String, ItemStack> itemStacks;

    @BeforeEach
    void setUp() {
        DemoContentConfig config = new DemoContentConfig(
                new DemoContentConfig.DemoToggles(true, true, true, false, false, false, false),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        when(plugin.getLoggingUtil()).thenReturn(loggingUtil);
        when(plugin.getCustomItemRegistry()).thenReturn(itemRegistry);
        when(plugin.getSkillRegistryService()).thenReturn(skillRegistry);
        when(plugin.getRecipeRegistryService()).thenReturn(recipeRegistry);
        when(plugin.getName()).thenReturn("mmocraft");

        registeredItems = new HashMap<>();
        itemStacks = new HashMap<>();
        doAnswer(invocation -> {
            CustomItem item = invocation.getArgument(0);
            CustomItem spyItem = spy(item);
            ItemStack stack = mock(ItemStack.class);
            Material materialStub = mock(Material.class);
            when(materialStub.name()).thenReturn(item.getItemId().toUpperCase(Locale.ROOT));
            when(materialStub.isAir()).thenReturn(false);
            when(stack.getType()).thenReturn(materialStub);
            when(stack.getAmount()).thenReturn(1);
            when(stack.clone()).thenReturn(stack);
            doReturn(stack).when(spyItem).createItemStack(anyInt());
            registeredItems.put(spyItem.getItemId(), spyItem);
            itemStacks.put(spyItem.getItemId(), stack);
            return null;
        }).when(itemRegistry).registerItem(any(CustomItem.class));
        when(itemRegistry.getCustomItem(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return Optional.ofNullable(registeredItems.get(id));
        });
        when(itemRegistry.unregisterItem(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            itemStacks.remove(id);
            return registeredItems.remove(id) != null;
        });

        registeredSkills = new HashMap<>();
        doAnswer(invocation -> {
            Skill skill = invocation.getArgument(0);
            registeredSkills.put(skill.getSkillId(), skill);
            return null;
        }).when(skillRegistry).registerSkill(any(Skill.class));
        when(skillRegistry.getSkill(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return Optional.ofNullable(registeredSkills.get(id));
        });
        when(skillRegistry.unregisterSkill(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return registeredSkills.remove(id) != null;
        });

        registeredRecipes = new HashMap<>();
        doAnswer(invocation -> {
            CustomRecipe recipe = invocation.getArgument(0);
            registeredRecipes.put(recipe.getRecipeId().toLowerCase(Locale.ROOT), recipe);
            return null;
        }).when(recipeRegistry).registerRecipe(any(CustomRecipe.class));
        when(recipeRegistry.getRecipeById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return Optional.ofNullable(registeredRecipes.get(id.toLowerCase(Locale.ROOT)));
        });
        when(recipeRegistry.unregisterRecipe(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return registeredRecipes.remove(id.toLowerCase(Locale.ROOT)) != null;
        });

        module = new DemoContentModule(plugin, loggingUtil, config);
    }

    @Test
    void applySettings_registersShowcaseContentWithoutIdCollisions() {
        DemoContentSettings settings = new DemoContentSettings(true, true, true, false, false, false, false);

        module.applySettings(settings);

        Set<String> expectedItems = Set.of(
                "simple_sword",
                "training_chestplate",
                BlazingEmberRod.ITEM_ID,
                WindrunnerBoots.ITEM_ID,
                GuardianBulwark.ITEM_ID,
                BerserkerGauntlet.ITEM_ID,
                LuckyCharmTalisman.ITEM_ID,
                ProspectorsDrill.ITEM_ID,
                HarvestersScythe.ITEM_ID,
                ForagersHatchet.ITEM_ID,
                AnglersTidalRod.ITEM_ID
        );
        assertEquals(expectedItems, registeredItems.keySet());

        Set<String> expectedSkills = Set.of(
                "strong_strike",
                "minor_heal",
                InfernoBurstSkill.SKILL_ID,
                GaleForceDashSkill.SKILL_ID,
                BerserkerRageSkill.SKILL_ID,
                ProspectorPulseSkill.SKILL_ID,
                HarvestRallySkill.SKILL_ID,
                TidalSurgeSkill.SKILL_ID
        );
        assertEquals(expectedSkills, registeredSkills.keySet());

        Map<String, String> expectedRecipes = Map.of(
                "infusion_blazing_ember_rod", BlazingEmberRod.ITEM_ID,
                "infusion_berserker_gauntlet", BerserkerGauntlet.ITEM_ID,
                "infusion_prospectors_drill", ProspectorsDrill.ITEM_ID,
                "infusion_harvesters_scythe", HarvestersScythe.ITEM_ID,
                "infusion_anglers_rod", AnglersTidalRod.ITEM_ID
        );
        assertEquals(expectedRecipes.keySet(), registeredRecipes.keySet());

        expectedRecipes.forEach((recipeId, itemId) -> {
            CustomRecipe recipe = registeredRecipes.get(recipeId);
            assertNotNull(recipe, "Recipe not registered: " + recipeId);
            assertSame(itemStacks.get(itemId), recipe.getOutputItemStack(), "Recipe output mismatch for " + recipeId);
        });

        module.applySettings(settings);

        assertEquals(expectedItems, registeredItems.keySet());
        assertEquals(expectedSkills, registeredSkills.keySet());
        assertEquals(expectedRecipes.keySet(), registeredRecipes.keySet());

        verify(itemRegistry, times(expectedItems.size() * 2)).registerItem(any(CustomItem.class));
        verify(skillRegistry, times(expectedSkills.size() * 2)).registerSkill(any(Skill.class));
        verify(recipeRegistry, times(expectedRecipes.size() * 2)).registerRecipe(any(CustomRecipe.class));
    }
}
