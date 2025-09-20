package com.x1f4r.mmocraft.crafting.config;

import com.x1f4r.mmocraft.config.gameplay.CraftingConfig;
import com.x1f4r.mmocraft.crafting.model.CustomRecipeIngredient;
import com.x1f4r.mmocraft.crafting.recipe.CustomRecipe;
import com.x1f4r.mmocraft.crafting.service.RecipeRegistryService;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Applies {@link CraftingConfig} definitions to the runtime {@link RecipeRegistryService}.
 */
public class CraftingRecipeLoader {

    private final RecipeRegistryService recipeRegistryService;
    private final CustomItemRegistry customItemRegistry;
    private final LoggingUtil logger;
    private final Set<String> managedRecipeIds = new HashSet<>();

    public CraftingRecipeLoader(RecipeRegistryService recipeRegistryService,
                                CustomItemRegistry customItemRegistry,
                                LoggingUtil logger) {
        this.recipeRegistryService = Objects.requireNonNull(recipeRegistryService, "recipeRegistryService");
        this.customItemRegistry = Objects.requireNonNull(customItemRegistry, "customItemRegistry");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public synchronized void applyConfig(CraftingConfig config) {
        Objects.requireNonNull(config, "config");
        for (String recipeId : new HashSet<>(managedRecipeIds)) {
            recipeRegistryService.unregisterRecipe(recipeId);
            managedRecipeIds.remove(recipeId);
        }

        for (CraftingConfig.CraftingRecipeDefinition definition : config.getRecipes()) {
            if (!definition.enabled()) {
                continue;
            }
            try {
                CustomRecipe recipe = buildRecipe(definition);
                recipeRegistryService.registerRecipe(recipe);
                managedRecipeIds.add(recipe.getRecipeId().toLowerCase(Locale.ROOT));
                logger.info("Registered config recipe: " + recipe.getRecipeId());
            } catch (Exception ex) {
                logger.warning("Failed to register recipe '" + definition.id() + "': " + ex.getMessage());
            }
        }
    }

    private CustomRecipe buildRecipe(CraftingConfig.CraftingRecipeDefinition definition) {
        ItemStack output = resolveOutput(definition.output());
        if (definition.type().isShaped()) {
            Map<Integer, CustomRecipeIngredient> shaped = new HashMap<>();
            definition.shapedIngredients().forEach((slot, ingredient) ->
                    shaped.put(slot, toIngredient(ingredient)));
            if (shaped.isEmpty()) {
                throw new IllegalArgumentException("Shaped recipe '" + definition.id() + "' has no shaped ingredients defined.");
            }
            return new CustomRecipe(definition.id(), output, definition.type(), shaped, definition.permission());
        }
        List<CustomRecipeIngredient> shapeless = new ArrayList<>();
        for (CraftingConfig.IngredientDefinition ingredient : definition.shapelessIngredients()) {
            shapeless.add(toIngredient(ingredient));
        }
        if (shapeless.isEmpty()) {
            throw new IllegalArgumentException("Shapeless recipe '" + definition.id() + "' has no ingredients defined.");
        }
        return new CustomRecipe(definition.id(), output, definition.type(), shapeless, definition.permission());
    }

    private ItemStack resolveOutput(CraftingConfig.OutputDefinition outputDefinition) {
        CustomRecipeIngredient.IngredientType type = outputDefinition.type();
        String identifier = outputDefinition.identifier().toUpperCase(Locale.ROOT);
        int amount = outputDefinition.amount();
        return switch (type) {
            case CUSTOM_ITEM -> customItemRegistry.createItemStack(identifier, amount);
            case VANILLA_MATERIAL -> {
                Material material = Material.valueOf(identifier);
                yield new ItemStack(material, amount);
            }
            default -> throw new IllegalArgumentException("Unsupported output type: " + type);
        };
    }

    private CustomRecipeIngredient toIngredient(CraftingConfig.IngredientDefinition definition) {
        CustomRecipeIngredient.IngredientType type = definition.type();
        String identifier = definition.identifier().toUpperCase(Locale.ROOT);
        return new CustomRecipeIngredient(type, identifier, definition.quantity(), definition.matchNbt());
    }
}
