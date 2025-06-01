package com.x1f4r.mmocraft.item.service;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class BasicCustomItemRegistry implements CustomItemRegistry {

    private final MMOCraftPlugin plugin; // Needed for NBTUtil context when getting ID from ItemStack
    private final LoggingUtil logger;
    private final Map<String, CustomItem> registeredItems = new ConcurrentHashMap<>();

    public BasicCustomItemRegistry(MMOCraftPlugin plugin, LoggingUtil logger) {
        this.plugin = plugin;
        this.logger = logger;
        logger.debug("BasicCustomItemRegistry initialized.");
    }

    @Override
    public void registerItem(CustomItem item) {
        if (item == null || item.getItemId() == null || item.getItemId().trim().isEmpty()) {
            logger.warning("Attempted to register a null item or an item with an invalid ID.");
            return;
        }
        CustomItem existingItem = registeredItems.put(item.getItemId().toLowerCase(), item); // Store IDs in lowercase for case-insensitive retrieval
        if (existingItem != null) {
            logger.warning("CustomItem ID '" + item.getItemId() + "' was already registered. Overwriting '" +
                           existingItem.getDisplayName() + "' with '" + item.getDisplayName() + "'.");
        } else {
            logger.info("Registered custom item: " + item.getDisplayName() + " (ID: " + item.getItemId() + ")");
        }
    }

    @Override
    public Optional<CustomItem> getCustomItem(String itemId) {
        if (itemId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(registeredItems.get(itemId.toLowerCase()));
    }

    @Override
    public Optional<CustomItem> getCustomItem(ItemStack itemStack) {
        String itemId = CustomItem.getItemId(itemStack, plugin);
        if (itemId != null) {
            return getCustomItem(itemId);
        }
        return Optional.empty();
    }

    @Override
    public ItemStack createItemStack(String itemId, int amount) {
        Optional<CustomItem> customItemOptional = getCustomItem(itemId);
        if (customItemOptional.isPresent()) {
            return customItemOptional.get().createItemStack(amount);
        } else {
            logger.warning("Attempted to create ItemStack for unknown custom item ID: " + itemId);
            // Return AIR or throw exception based on desired strictness
            // throw new IllegalArgumentException("No custom item registered with ID: " + itemId);
            return new ItemStack(Material.AIR);
        }
    }

    @Override
    public Collection<CustomItem> getAllItems() {
        return Collections.unmodifiableCollection(registeredItems.values());
    }

    @Override
    public boolean unregisterItem(String itemId) {
        if (itemId == null) {
            return false;
        }
        CustomItem removedItem = registeredItems.remove(itemId.toLowerCase());
        if (removedItem != null) {
            logger.info("Unregistered custom item: " + removedItem.getDisplayName() + " (ID: " + itemId + ")");
            return true;
        }
        return false;
    }
}
