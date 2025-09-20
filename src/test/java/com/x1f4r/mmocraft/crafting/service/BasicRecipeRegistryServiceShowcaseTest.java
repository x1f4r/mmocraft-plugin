package com.x1f4r.mmocraft.crafting.service;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.crafting.model.CustomRecipeIngredient;
import com.x1f4r.mmocraft.crafting.model.RecipeType;
import com.x1f4r.mmocraft.crafting.recipe.CustomRecipe;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BasicRecipeRegistryServiceShowcaseTest {

    @Mock
    private MMOCraftPlugin plugin;
    @Mock
    private LoggingUtil loggingUtil;
    @Mock
    private CustomItemRegistry itemRegistry;

    private BasicRecipeRegistryService recipeRegistryService;

    @BeforeEach
    void setUp() {
        recipeRegistryService = new BasicRecipeRegistryService(plugin, loggingUtil, itemRegistry);
    }

    @Test
    void findMatchingRecipe_windrunnerBootsMatchesRegardlessOfOrder() {
        recipeRegistryService.registerRecipe(createWindrunnerRecipe());

        Inventory grid = mock(Inventory.class);
        ItemStack[] contents = new ItemStack[9];
        contents[0] = mockStack("FEATHER", 4);
        contents[1] = mockStack("SUGAR", 4);
        contents[2] = mockStack("LEATHER_BOOTS", 1);
        contents[3] = mockStack("PHANTOM_MEMBRANE", 2);
        when(grid.getContents()).thenReturn(contents);
        when(grid.getSize()).thenReturn(contents.length);

        Optional<CustomRecipe> match = recipeRegistryService.findMatchingRecipe(RecipeType.CUSTOM_SHAPELESS, grid);
        assertTrue(match.isPresent());
        assertEquals("windrunner_boots", match.get().getRecipeId());
    }

    @Test
    void findMatchingRecipe_windrunnerBootsFailsWhenQuantitiesMissing() {
        recipeRegistryService.registerRecipe(createWindrunnerRecipe());

        Inventory grid = mock(Inventory.class);
        ItemStack[] contents = new ItemStack[9];
        contents[0] = mockStack("FEATHER", 4);
        contents[1] = mockStack("SUGAR", 4);
        contents[2] = mockStack("LEATHER_BOOTS", 1);
        contents[3] = mockStack("PHANTOM_MEMBRANE", 1); // Not enough membranes
        when(grid.getContents()).thenReturn(contents);
        when(grid.getSize()).thenReturn(contents.length);

        Optional<CustomRecipe> match = recipeRegistryService.findMatchingRecipe(RecipeType.CUSTOM_SHAPELESS, grid);
        assertFalse(match.isPresent());
    }

    private CustomRecipe createWindrunnerRecipe() {
        ItemStack output = mockStack("LEATHER_BOOTS", 1);
        List<CustomRecipeIngredient> ingredients = List.of(
                new CustomRecipeIngredient(CustomRecipeIngredient.IngredientType.VANILLA_MATERIAL, "LEATHER_BOOTS", 1),
                new CustomRecipeIngredient(CustomRecipeIngredient.IngredientType.VANILLA_MATERIAL, "FEATHER", 4),
                new CustomRecipeIngredient(CustomRecipeIngredient.IngredientType.VANILLA_MATERIAL, "SUGAR", 4),
                new CustomRecipeIngredient(CustomRecipeIngredient.IngredientType.VANILLA_MATERIAL, "PHANTOM_MEMBRANE", 2)
        );
        return new CustomRecipe("windrunner_boots", output, RecipeType.CUSTOM_SHAPELESS, ingredients, null);
    }

    private ItemStack mockStack(String materialName, int amount) {
        ItemStack stack = mock(ItemStack.class);
        Material material = mock(Material.class);
        when(material.name()).thenReturn(materialName);
        when(material.isAir()).thenReturn(false);
        when(stack.getType()).thenReturn(material);
        when(stack.getAmount()).thenReturn(amount);
        when(stack.clone()).thenReturn(stack);
        return stack;
    }
}
