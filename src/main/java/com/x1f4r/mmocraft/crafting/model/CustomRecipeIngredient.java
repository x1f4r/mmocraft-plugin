package com.x1f4r.mmocraft.crafting.model;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.Objects;

public class CustomRecipeIngredient {

    public enum IngredientType {
        VANILLA_MATERIAL,
        CUSTOM_ITEM
    }

    private final IngredientType type;
    private final String identifier; // Material.name() or customItemId
    private final int quantity;
    private final boolean matchNBT; // For future advanced matching, default false

    public CustomRecipeIngredient(IngredientType type, String identifier, int quantity, boolean matchNBT) {
        this.type = Objects.requireNonNull(type, "Ingredient type cannot be null.");
        this.identifier = Objects.requireNonNull(identifier, "Identifier cannot be null.").toUpperCase(); // Store Material names uppercase
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        this.quantity = quantity;
        this.matchNBT = matchNBT;
    }

    public CustomRecipeIngredient(IngredientType type, String identifier, int quantity) {
        this(type, identifier, quantity, false);
    }

    // Getters
    public IngredientType getType() { return type; }
    public String getIdentifier() { return identifier; }
    public int getQuantity() { return quantity; }
    public boolean shouldMatchNBT() { return matchNBT; }

    /**
     * Checks if the given ItemStack matches this ingredient definition.
     *
     * @param itemStack The ItemStack to check. Can be null or AIR.
     * @param itemRegistry The registry to look up custom item definitions.
     * @param plugin The main plugin instance for NBT operations.
     * @return True if the ItemStack matches, false otherwise.
     */
    public boolean matches(ItemStack itemStack, CustomItemRegistry itemRegistry, MMOCraftPlugin plugin) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return false; // Cannot match an empty or null item
        }

        if (itemStack.getAmount() < this.quantity) {
            return false; // Not enough quantity
        }

        switch (this.type) {
            case VANILLA_MATERIAL:
                try {
                    Material expectedMaterial = Material.valueOf(this.identifier);
                    return itemStack.getType() == expectedMaterial;
                    // Note: Does not check NBT for vanilla items unless matchNBT is true and implemented
                } catch (IllegalArgumentException e) {
                    // Invalid material name in recipe definition
                    if (plugin != null && plugin.getLoggingUtil() != null) {
                         plugin.getLoggingUtil().warning("Invalid material identifier in recipe ingredient: " + this.identifier);
                    }
                    return false;
                }
            case CUSTOM_ITEM:
                String customItemId = CustomItem.getItemId(itemStack, plugin);
                if (customItemId != null && customItemId.equals(this.identifier)) {
                    // If matchNBT is true, further NBT comparison would be needed here.
                    // For now, just matching by custom item ID is sufficient.
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return "CustomRecipeIngredient{" +
               "type=" + type +
               ", identifier='" + identifier + '\'' +
               ", quantity=" + quantity +
               (matchNBT ? ", matchNBT=true" : "") +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomRecipeIngredient that = (CustomRecipeIngredient) o;
        return quantity == that.quantity &&
               matchNBT == that.matchNBT &&
               type == that.type &&
               identifier.equals(that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, identifier, quantity, matchNBT);
    }
}
