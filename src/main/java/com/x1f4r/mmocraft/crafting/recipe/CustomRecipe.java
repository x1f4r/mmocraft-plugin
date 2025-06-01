package com.x1f4r.mmocraft.crafting.recipe;

import com.x1f4r.mmocraft.crafting.model.CustomRecipeIngredient;
import com.x1f4r.mmocraft.crafting.model.RecipeType;
import org.bukkit.inventory.Inventory; // For getRemainingItems, not fully implemented yet
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList; // Added
import java.util.Collections;
import java.util.HashMap; // Added
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a custom crafting recipe.
 */
public class CustomRecipe {

    private final String recipeId;
    private final ItemStack outputItemStack;
    private final RecipeType recipeType;
    private final String permissionRequired; // Optional permission

    // For shapeless recipes: List of ingredients
    private final List<CustomRecipeIngredient> shapelessIngredients;

    // For shaped recipes: Map of slot index (0-8 for 3x3) to ingredient
    // Or a List<CustomRecipeIngredient> of size 9 (for 3x3), with nulls for empty slots.
    private final Map<Integer, CustomRecipeIngredient> shapedIngredients; // Key: slot index

    /**
     * Constructor for shapeless recipes.
     */
    public CustomRecipe(String recipeId, ItemStack outputItemStack, RecipeType recipeType,
                        List<CustomRecipeIngredient> ingredients, String permissionRequired) {
        this.recipeId = Objects.requireNonNull(recipeId, "Recipe ID cannot be null.");
        this.outputItemStack = Objects.requireNonNull(outputItemStack, "Output ItemStack cannot be null.");
        if (outputItemStack.getType().isAir() || outputItemStack.getAmount() <= 0) {
            throw new IllegalArgumentException("Output ItemStack must be a valid, non-empty item.");
        }
        this.recipeType = Objects.requireNonNull(recipeType, "Recipe type cannot be null.");
        if (recipeType.isShaped()) {
            throw new IllegalArgumentException("This constructor is for shapeless recipes. Use shaped constructor for " + recipeType);
        }
        this.shapelessIngredients = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(ingredients, "Ingredients list cannot be null.")));
        this.shapedIngredients = Collections.emptyMap();
        this.permissionRequired = permissionRequired; // Can be null
    }

    /**
     * Constructor for shaped recipes.
     * The shapedIngredients map uses Integer keys from 0 to (gridSize*gridSize - 1).
     * For a 3x3 grid, keys are 0-8.
     */
    public CustomRecipe(String recipeId, ItemStack outputItemStack, RecipeType recipeType,
                        Map<Integer, CustomRecipeIngredient> shapedIngredients, String permissionRequired) {
        this.recipeId = Objects.requireNonNull(recipeId, "Recipe ID cannot be null.");
        this.outputItemStack = Objects.requireNonNull(outputItemStack, "Output ItemStack cannot be null.");
         if (outputItemStack.getType().isAir() || outputItemStack.getAmount() <= 0) {
            throw new IllegalArgumentException("Output ItemStack must be a valid, non-empty item.");
        }
        this.recipeType = Objects.requireNonNull(recipeType, "Recipe type cannot be null.");
        if (!recipeType.isShaped()) {
            throw new IllegalArgumentException("This constructor is for shaped recipes. Use shapeless constructor for " + recipeType);
        }
        this.shapedIngredients = Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(shapedIngredients, "Shaped ingredients map cannot be null.")));
        this.shapelessIngredients = Collections.emptyList();
        this.permissionRequired = permissionRequired; // Can be null
    }


    // Getters
    public String getRecipeId() { return recipeId; }
    public ItemStack getOutputItemStack() { return outputItemStack.clone(); } // Return a clone for safety
    public RecipeType getRecipeType() { return recipeType; }
    public String getPermissionRequired() { return permissionRequired; }
    public boolean hasPermission() { return permissionRequired != null && !permissionRequired.trim().isEmpty(); }

    public boolean isShaped() {
        return recipeType.isShaped();
    }

    /**
     * Gets the list of ingredients for a shapeless recipe.
     * @return Unmodifiable list of ingredients. Empty if it's a shaped recipe.
     */
    public List<CustomRecipeIngredient> getShapelessIngredients() {
        return shapelessIngredients; // Already unmodifiable
    }

    /**
     * Gets the map of ingredients for a shaped recipe (slot index to ingredient).
     * @return Unmodifiable map of ingredients. Empty if it's a shapeless recipe.
     */
    public Map<Integer, CustomRecipeIngredient> getShapedIngredients() {
        return shapedIngredients; // Already unmodifiable
    }

    /**
     * (Advanced Feature - Placeholder)
     * Calculates remaining items after crafting (e.g., empty buckets).
     * For now, returns an empty list.
     *
     * @param craftingInventory The inventory where crafting took place.
     * @return A list of ItemStacks to be returned to the player (e.g., empty buckets).
     */
    public List<ItemStack> getRemainingItems(Inventory craftingInventory) {
        // TODO: Implement logic for items like water buckets returning empty buckets.
        // This would involve checking ingredient types and their .containerItem() in Bukkit API if applicable,
        // or custom logic for CustomItems.
        return Collections.emptyList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomRecipe that = (CustomRecipe) o;
        return recipeId.equals(that.recipeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recipeId);
    }

    @Override
    public String toString() {
        return "CustomRecipe{" +
               "recipeId='" + recipeId + '\'' +
               ", output=" + outputItemStack.getType() + "x" + outputItemStack.getAmount() +
               ", type=" + recipeType +
               (isShaped() ? ", ingredientsShape=" + shapedIngredients.size() + " slots"
                           : ", ingredients=" + shapelessIngredients.size() + " items") +
               (permissionRequired != null ? ", permission='" + permissionRequired + '\'' : "") +
               '}';
    }
}
