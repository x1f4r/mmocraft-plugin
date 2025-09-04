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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicRecipeRegistryServiceTest {

    @Mock private MMOCraftPlugin mockPlugin;
    @Mock private LoggingUtil mockLogger;
    @Mock private CustomItemRegistry mockItemRegistry; // Needed for findMatchingRecipe context

    private BasicRecipeRegistryService recipeRegistryService;

    private CustomRecipe shapelessRecipe1;
    private CustomRecipe shapedRecipe1;

    @BeforeEach
    void setUp() {
        recipeRegistryService = new BasicRecipeRegistryService(mockPlugin, mockLogger, mockItemRegistry);

        // Setup sample recipes
        ItemStack output1 = new ItemStack(Material.DIAMOND_PICKAXE);
        List<CustomRecipeIngredient> ingredients1 = List.of(
            new CustomRecipeIngredient(CustomRecipeIngredient.IngredientType.VANILLA_MATERIAL, "DIAMOND", 3),
            new CustomRecipeIngredient(CustomRecipeIngredient.IngredientType.VANILLA_MATERIAL, "STICK", 2)
        );
        shapelessRecipe1 = new CustomRecipe("diamond_pick_shapeless", output1, RecipeType.CUSTOM_SHAPELESS, ingredients1, null);

        // Shaped recipe would need a Map<Integer, CustomRecipeIngredient>
        // For now, BasicRecipeRegistryService.findMatchingRecipe only has basic shapeless logic
    }

    @Test
    void registerRecipe_newRecipe_registersSuccessfully() {
        recipeRegistryService.registerRecipe(shapelessRecipe1);
        Optional<CustomRecipe> retrieved = recipeRegistryService.getRecipeById("diamond_pick_shapeless");
        assertTrue(retrieved.isPresent());
        assertEquals(shapelessRecipe1, retrieved.get());
        verify(mockLogger).info("Registered recipe: diamond_pick_shapeless (Output: DIAMOND_PICKAXE)");
    }

    @Test
    void registerRecipe_duplicateId_overwritesAndLogsWarning() {
        recipeRegistryService.registerRecipe(shapelessRecipe1);
        CustomRecipe newRecipeWithSameId = new CustomRecipe(
            "diamond_pick_shapeless", new ItemStack(Material.GOLDEN_PICKAXE),
            RecipeType.CUSTOM_SHAPELESS, shapelessRecipe1.getShapelessIngredients(), null
        );
        recipeRegistryService.registerRecipe(newRecipeWithSameId);

        Optional<CustomRecipe> retrieved = recipeRegistryService.getRecipeById("diamond_pick_shapeless");
        assertTrue(retrieved.isPresent());
        assertEquals(Material.GOLDEN_PICKAXE, retrieved.get().getOutputItemStack().getType());
        verify(mockLogger).warning("Recipe ID 'diamond_pick_shapeless' was already registered. Overwriting.");
    }

    @Test
    void getRecipeById_caseInsensitive_returnsRecipe() {
        recipeRegistryService.registerRecipe(shapelessRecipe1);
        Optional<CustomRecipe> retrieved = recipeRegistryService.getRecipeById("DIAMOND_PICK_SHAPELESS");
        assertTrue(retrieved.isPresent());
    }


    @Test
    void getRecipesForOutput_matchesMaterial_returnsRecipe() {
        recipeRegistryService.registerRecipe(shapelessRecipe1);
        List<CustomRecipe> found = recipeRegistryService.getRecipesForOutput(new ItemStack(Material.DIAMOND_PICKAXE));
        assertEquals(1, found.size());
        assertTrue(found.contains(shapelessRecipe1));
    }

    @Test
    void getRecipesForOutput_noMatch_returnsEmptyList() {
        recipeRegistryService.registerRecipe(shapelessRecipe1);
        List<CustomRecipe> found = recipeRegistryService.getRecipesForOutput(new ItemStack(Material.IRON_PICKAXE));
        assertTrue(found.isEmpty());
    }

    // --- findMatchingRecipe Tests (currently placeholder logic for shapeless) ---
    @Test
    void findMatchingRecipe_shapeless_matchesCorrectly() {
        recipeRegistryService.registerRecipe(shapelessRecipe1);

        Inventory mockGrid = mock(Inventory.class);
        ItemStack[] gridContents = new ItemStack[9]; // Assuming 3x3 for this test
        gridContents[0] = new ItemStack(Material.DIAMOND, 3);
        gridContents[1] = new ItemStack(Material.STICK, 2);
        when(mockGrid.getContents()).thenReturn(gridContents);

        Optional<CustomRecipe> found = recipeRegistryService.findMatchingRecipe(RecipeType.CUSTOM_SHAPELESS, mockGrid);
        assertTrue(found.isPresent());
        assertEquals(shapelessRecipe1, found.get());
    }

    @Test
    void findMatchingRecipe_shapeless_ingredientQuantityMismatch_returnsEmpty() {
        recipeRegistryService.registerRecipe(shapelessRecipe1);
        Inventory mockGrid = mock(Inventory.class);
        ItemStack[] gridContents = new ItemStack[9];
        gridContents[0] = new ItemStack(Material.DIAMOND, 1); // Not enough diamonds
        gridContents[1] = new ItemStack(Material.STICK, 2);
        when(mockGrid.getContents()).thenReturn(gridContents);

        Optional<CustomRecipe> found = recipeRegistryService.findMatchingRecipe(RecipeType.CUSTOM_SHAPELESS, mockGrid);
        assertFalse(found.isPresent());
    }

    @Test
    void findMatchingRecipe_shapeless_extraIngredient_returnsEmpty() {
        recipeRegistryService.registerRecipe(shapelessRecipe1);
        Inventory mockGrid = mock(Inventory.class);
        ItemStack[] gridContents = new ItemStack[9];
        gridContents[0] = new ItemStack(Material.DIAMOND, 3);
        gridContents[1] = new ItemStack(Material.STICK, 2);
        gridContents[2] = new ItemStack(Material.IRON_INGOT, 1); // Extra item
        when(mockGrid.getContents()).thenReturn(gridContents);

        Optional<CustomRecipe> found = recipeRegistryService.findMatchingRecipe(RecipeType.CUSTOM_SHAPELESS, mockGrid);
        assertFalse(found.isPresent());
    }


    @Test
    void findMatchingRecipe_shaped_logsNotImplemented() {
        // No shaped recipes registered, just testing the path
        Inventory mockGrid = mock(Inventory.class);
        when(mockGrid.getContents()).thenReturn(new ItemStack[9]); // Empty grid

        recipeRegistryService.findMatchingRecipe(RecipeType.CUSTOM_SHAPED, mockGrid);
        verify(mockLogger).warning("Shaped recipe matching is not yet fully implemented in BasicRecipeRegistryService.");
    }

    @Test
    void getAllRecipes_returnsAll() {
        recipeRegistryService.registerRecipe(shapelessRecipe1);
        // recipeRegistryService.registerRecipe(shapedRecipe1); // if we had one
        assertEquals(1, recipeRegistryService.getAllRecipes().size());
    }

    @Test
    void unregisterRecipe_removesRecipe() {
        recipeRegistryService.registerRecipe(shapelessRecipe1);
        assertTrue(recipeRegistryService.unregisterRecipe("diamond_pick_shapeless"));
        assertFalse(recipeRegistryService.getRecipeById("diamond_pick_shapeless").isPresent());
    }
}
