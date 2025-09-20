package com.x1f4r.mmocraft.crafting.config;

import com.x1f4r.mmocraft.config.gameplay.CraftingConfig;
import com.x1f4r.mmocraft.crafting.model.CustomRecipeIngredient;
import com.x1f4r.mmocraft.crafting.model.RecipeType;
import com.x1f4r.mmocraft.crafting.recipe.CustomRecipe;
import com.x1f4r.mmocraft.crafting.service.RecipeRegistryService;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
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
class CraftingRecipeLoaderTest {

    @Mock
    private RecipeRegistryService recipeRegistryService;
    @Mock
    private CustomItemRegistry customItemRegistry;
    @Mock
    private LoggingUtil loggingUtil;

    private CraftingRecipeLoader loader;

    @BeforeEach
    void setUp() {
        loader = new CraftingRecipeLoader(recipeRegistryService, customItemRegistry, loggingUtil);
        when(customItemRegistry.createItemStack(anyString(), anyInt())).thenAnswer(invocation -> {
            int amount = invocation.getArgument(1);
            ItemStack mockedItemStack = mock(ItemStack.class);
            Material material = mock(Material.class);
            when(material.isAir()).thenReturn(false);
            when(mockedItemStack.getType()).thenReturn(material);
            when(mockedItemStack.getAmount()).thenReturn(amount);
            return mockedItemStack;
        });
    }

    @Test
    void applyConfig_registersShapelessAndShapedRecipes() {
        CraftingConfig.CraftingRecipeDefinition shapeless = new CraftingConfig.CraftingRecipeDefinition(
                "shapeless",
                true,
                RecipeType.CUSTOM_SHAPELESS,
                new CraftingConfig.OutputDefinition(CustomRecipeIngredient.IngredientType.CUSTOM_ITEM, "shapeless_item", 2),
                List.of(new CraftingConfig.IngredientDefinition(CustomRecipeIngredient.IngredientType.CUSTOM_ITEM,
                        "demo_item", 1, true)),
                Map.of(),
                null
        );
        CraftingConfig.CraftingRecipeDefinition shaped = new CraftingConfig.CraftingRecipeDefinition(
                "shaped",
                true,
                RecipeType.CUSTOM_SHAPED,
                new CraftingConfig.OutputDefinition(CustomRecipeIngredient.IngredientType.CUSTOM_ITEM, "shaped_item", 1),
                List.of(),
                Map.of(
                        0, new CraftingConfig.IngredientDefinition(CustomRecipeIngredient.IngredientType.VANILLA_MATERIAL,
                                "STICK", 1, false),
                        4, new CraftingConfig.IngredientDefinition(CustomRecipeIngredient.IngredientType.VANILLA_MATERIAL,
                                "IRON_INGOT", 2, false)
                ),
                "mmocraft.craft.test"
        );
        CraftingConfig config = new CraftingConfig(List.of(shapeless, shaped));

        loader.applyConfig(config);

        ArgumentCaptor<CustomRecipe> recipeCaptor = ArgumentCaptor.forClass(CustomRecipe.class);
        verify(recipeRegistryService, times(2)).registerRecipe(recipeCaptor.capture());

        List<CustomRecipe> recipes = recipeCaptor.getAllValues();
        CustomRecipe shapelessRecipe = recipes.get(0);
        assertFalse(shapelessRecipe.isShaped());
        assertEquals(1, shapelessRecipe.getShapelessIngredients().size());
        assertTrue(shapelessRecipe.getShapelessIngredients().get(0).shouldMatchNBT());
        verify(customItemRegistry).createItemStack("SHAPELESS_ITEM", 2);

        CustomRecipe shapedRecipe = recipes.get(1);
        assertTrue(shapedRecipe.isShaped());
        assertEquals(2, shapedRecipe.getShapedIngredients().size());
        assertEquals("mmocraft.craft.test", shapedRecipe.getPermissionRequired());
        verify(customItemRegistry).createItemStack("SHAPED_ITEM", 1);
    }

    @Test
    void applyConfig_unregistersManagedRecipesOnReload() {
        CraftingConfig initial = new CraftingConfig(List.of(
                new CraftingConfig.CraftingRecipeDefinition(
                        "alpha",
                        true,
                        RecipeType.CUSTOM_SHAPELESS,
                        new CraftingConfig.OutputDefinition(CustomRecipeIngredient.IngredientType.CUSTOM_ITEM, "alpha_item", 1),
                        List.of(new CraftingConfig.IngredientDefinition(CustomRecipeIngredient.IngredientType.VANILLA_MATERIAL,
                                "COBBLESTONE", 1, false)),
                        Map.of(),
                        null
                )
        ));

        loader.applyConfig(initial);
        loader.applyConfig(new CraftingConfig(List.of()));

        verify(recipeRegistryService).unregisterRecipe("alpha");
    }
}
