package com.x1f4r.mmocraft.crafting.model;

/**
 * Defines the type of crafting recipe, primarily distinguishing between
 * shaped (ingredients must be in a specific pattern) and shapeless (order doesn't matter)
 * and where the crafting occurs (vanilla workbench vs. custom UI).
 */
public enum RecipeType {
    /**
     * A recipe where ingredients must be placed in a specific pattern within a custom crafting UI.
     * The grid size (e.g., 3x3, 5x5) would be defined by the UI itself.
     */
    CUSTOM_SHAPED,

    /**
     * A recipe where the order and placement of ingredients do not matter,
     * only the correct items and their quantities, within a custom crafting UI.
     */
    CUSTOM_SHAPELESS,

    /**
     * (Future Use) A shaped recipe for the vanilla 3x3 workbench.
     * Would require integration with Bukkit's recipe system.
     */
    WORKBENCH_SHAPED,

    /**
     * (Future Use) A shapeless recipe for the vanilla workbench or 2x2 player inventory grid.
     * Would require integration with Bukkit's recipe system.
     */
    WORKBENCH_SHAPELESS;

    /**
     * Checks if this recipe type implies a shaped pattern.
     * @return true if the recipe is shaped, false otherwise.
     */
    public boolean isShaped() {
        return this == CUSTOM_SHAPED || this == WORKBENCH_SHAPED;
    }

    /**
     * Checks if this recipe type is intended for a custom UI rather than vanilla workbench.
     * @return true if for a custom UI.
     */
    public boolean isCustomUI() {
        return this == CUSTOM_SHAPED || this == CUSTOM_SHAPELESS;
    }
}
