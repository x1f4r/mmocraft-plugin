package com.x1f4r.mmocraft.config.gameplay;

import com.x1f4r.mmocraft.crafting.model.CustomRecipeIngredient;
import com.x1f4r.mmocraft.crafting.model.RecipeType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable configuration describing custom crafting recipes loaded from TOML.
 */
public final class CraftingConfig {

    private final List<CraftingRecipeDefinition> recipes;

    public CraftingConfig(List<CraftingRecipeDefinition> recipes) {
        this.recipes = List.copyOf(recipes);
    }

    public List<CraftingRecipeDefinition> getRecipes() {
        return recipes;
    }

    public static CraftingConfig defaults() {
        return new CraftingConfig(List.of());
    }

    public record CraftingRecipeDefinition(String id,
                                           boolean enabled,
                                           RecipeType type,
                                           OutputDefinition output,
                                           List<IngredientDefinition> shapelessIngredients,
                                           Map<Integer, IngredientDefinition> shapedIngredients,
                                           String permission) {
        public CraftingRecipeDefinition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(output, "output");
            shapelessIngredients = shapelessIngredients == null ? List.of() : List.copyOf(shapelessIngredients);
            if (shapedIngredients == null) {
                shapedIngredients = Map.of();
            } else {
                shapedIngredients = Collections.unmodifiableMap(new LinkedHashMap<>(shapedIngredients));
            }
        }
    }

    public record OutputDefinition(CustomRecipeIngredient.IngredientType type, String identifier, int amount) {
        public OutputDefinition {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(identifier, "identifier");
            if (amount <= 0) {
                throw new IllegalArgumentException("amount must be positive");
            }
        }
    }

    public record IngredientDefinition(CustomRecipeIngredient.IngredientType type,
                                       String identifier,
                                       int quantity,
                                       boolean matchNbt) {
        public IngredientDefinition {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(identifier, "identifier");
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be positive");
            }
        }
    }
}
