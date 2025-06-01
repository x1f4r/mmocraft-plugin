package com.x1f4r.mmocraft.item.api;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class NBTUtil {

    private static NamespacedKey getKey(MMOCraftPlugin plugin, String key) {
        return new NamespacedKey(plugin, key);
    }

    // --- String NBT ---
    public static ItemStack setString(ItemStack item, String key, String value, MMOCraftPlugin plugin) {
        if (item == null || item.getType().isAir()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item; // Should not happen for valid items

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(getKey(plugin, key), PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return item;
    }

    public static String getString(ItemStack item, String key, MMOCraftPlugin plugin) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(getKey(plugin, key), PersistentDataType.STRING);
    }

    // --- Integer NBT ---
    public static ItemStack setInt(ItemStack item, String key, int value, MMOCraftPlugin plugin) {
        if (item == null || item.getType().isAir()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(getKey(plugin, key), PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
        return item;
    }

    public static int getInt(ItemStack item, String key, int defaultValue, MMOCraftPlugin plugin) {
        if (item == null || item.getType().isAir()) return defaultValue;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return defaultValue;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        Integer value = container.get(getKey(plugin, key), PersistentDataType.INTEGER);
        return value != null ? value : defaultValue;
    }

    // --- Double NBT ---
    public static ItemStack setDouble(ItemStack item, String key, double value, MMOCraftPlugin plugin) {
        if (item == null || item.getType().isAir()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(getKey(plugin, key), PersistentDataType.DOUBLE, value);
        item.setItemMeta(meta);
        return item;
    }

    public static double getDouble(ItemStack item, String key, double defaultValue, MMOCraftPlugin plugin) {
        if (item == null || item.getType().isAir()) return defaultValue;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return defaultValue;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        Double value = container.get(getKey(plugin, key), PersistentDataType.DOUBLE);
        return value != null ? value : defaultValue;
    }

    // --- Boolean NBT (stored as byte) ---
    public static ItemStack setBoolean(ItemStack item, String key, boolean value, MMOCraftPlugin plugin) {
        if (item == null || item.getType().isAir()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(getKey(plugin, key), PersistentDataType.BYTE, (byte)(value ? 1 : 0));
        item.setItemMeta(meta);
        return item;
    }

    public static boolean getBoolean(ItemStack item, String key, MMOCraftPlugin plugin) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        Byte value = container.get(getKey(plugin, key), PersistentDataType.BYTE);
        return value != null && value == 1;
    }


    // --- Tag Check ---
    public static boolean hasTag(ItemStack item, String key, MMOCraftPlugin plugin) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(getKey(plugin, key), PersistentDataType.STRING) ||
               container.has(getKey(plugin, key), PersistentDataType.INTEGER) ||
               container.has(getKey(plugin, key), PersistentDataType.DOUBLE) ||
               container.has(getKey(plugin, key), PersistentDataType.BYTE); // Add other types if used
    }

    public static ItemStack removeTag(ItemStack item, String key, MMOCraftPlugin plugin) {
        if (item == null || item.getType().isAir()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.remove(getKey(plugin, key));
        item.setItemMeta(meta);
        return item;
    }
}
