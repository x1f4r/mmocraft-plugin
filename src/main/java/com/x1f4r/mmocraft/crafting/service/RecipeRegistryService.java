package com.x1f4r.mmocraft.crafting.service;

import com.x1f4r.mmocraft.crafting.model.RecipeType;
import com.x1f4r.mmocraft.crafting.recipe.CustomRecipe;
import org.bukkit.inventory.Inventory; // Represents the crafting grid
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing custom crafting recipes.
 */
public interface RecipeRegistryService {

    /**
     * Registers a new custom recipe.
     * @param recipe The {@link CustomRecipe} to register.
     */
    void registerRecipe(CustomRecipe recipe);

    /**
     * Retrieves a custom recipe by its unique ID.
     * @param recipeId The ID of the recipe.
     * @return An {@link Optional} containing the {@link CustomRecipe} if found, otherwise empty.
     */
    Optional<CustomRecipe> getRecipeById(String recipeId);

    /**
     * Finds all recipes that produce an item similar to the given outputMatcher.
     * Similarity might be based on item type, custom item ID, or other NBT data in the future.
     * For now, it might compare based on Material and custom item ID if present.
     *
     * @param outputMatcher An ItemStack to match against recipe outputs.
     * @return A list of matching {@link CustomRecipe}s.
     */
    List<CustomRecipe> getRecipesForOutput(ItemStack outputMatcher);

    /**
     * Finds a custom recipe that matches the items placed in the provided crafting grid.
     *
     * @param type The {@link RecipeType} to check against (e.g., CUSTOM_SHAPED, CUSTOM_SHAPELESS).
     *             This helps the method to know how to interpret the grid and recipe.
     * @param craftingGridInventory An {@link Inventory} instance representing the crafting grid
     *                              (e.g., top part of a CraftingTable or a custom GUI's grid).
     *                              The size of the grid (e.g., 3x3) is implicitly handled by how
     *                              recipes are defined and how this method iterates the inventory.
     * @return An {@link Optional} containing the matching {@link CustomRecipe} if one is found, otherwise empty.
     */
    Optional<CustomRecipe> findMatchingRecipe(RecipeType type, Inventory craftingGridInventory);

    /**
     * Retrieves all registered custom recipes.
     * @return An unmodifiable list of all recipes.
     */
    List<CustomRecipe> getAllRecipes();

    /**
     * Unregisters a recipe by its ID.
     * @param recipeId The ID of the recipe to remove.
     * @return True if a recipe was removed, false otherwise.
     */
    boolean unregisterRecipe(String recipeId);
}
