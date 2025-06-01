package com.x1f4r.mmocraft.item.service;

import com.x1f4r.mmocraft.item.model.CustomItem;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Optional;

/**
 * Service responsible for managing and providing access to all defined {@link CustomItem}s.
 */
public interface CustomItemRegistry {

    /**
     * Registers a custom item definition.
     * If an item with the same ID is already registered, it might be overwritten.
     *
     * @param item The {@link CustomItem} to register.
     */
    void registerItem(CustomItem item);

    /**
     * Retrieves a custom item definition by its unique ID.
     *
     * @param itemId The unique ID of the custom item.
     * @return An {@link Optional} containing the {@link CustomItem} if found, otherwise empty.
     */
    Optional<CustomItem> getCustomItem(String itemId);

    /**
     * Retrieves a custom item definition from a given {@link ItemStack}
     * by reading its "MMOCRAFT_ITEM_ID" NBT tag.
     *
     * @param itemStack The ItemStack to inspect.
     * @return An {@link Optional} containing the {@link CustomItem} if it's a recognized custom item, otherwise empty.
     */
    Optional<CustomItem> getCustomItem(ItemStack itemStack);

    /**
     * Creates an {@link ItemStack} instance of a registered custom item.
     *
     * @param itemId The unique ID of the custom item to create.
     * @param amount The desired amount for the ItemStack.
     * @return The created {@link ItemStack}.
     * @throws IllegalArgumentException if the itemId is not found in the registry or amount is invalid.
     *         Alternatively, could return {@link org.bukkit.Material#AIR} or null.
     */
    ItemStack createItemStack(String itemId, int amount);

    /**
     * Retrieves a collection of all registered custom item definitions.
     *
     * @return An unmodifiable collection of all {@link CustomItem}s.
     */
    Collection<CustomItem> getAllItems();

    /**
     * Unregisters a custom item definition by its ID.
     *
     * @param itemId The ID of the item to unregister.
     * @return True if an item was removed, false otherwise.
     */
    boolean unregisterItem(String itemId);
}
