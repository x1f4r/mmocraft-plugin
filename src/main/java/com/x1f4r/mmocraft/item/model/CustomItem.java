package com.x1f4r.mmocraft.item.model;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.item.api.NBTUtil;
import com.x1f4r.mmocraft.util.StringUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.x1f4r.mmocraft.playerdata.model.Stat; // Added for StatModifiers

import java.util.ArrayList; // Added for lore modification
import java.util.Collections; // Added for default StatModifiers
import java.util.List;
import java.util.Map; // Added for StatModifiers
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Abstract base class for all custom items in MMOCraft.
 * Provides a structured way to define items with custom properties and NBT tags.
 */
public abstract class CustomItem {

    protected final MMOCraftPlugin plugin;
    public static final String CUSTOM_ITEM_ID_NBT_KEY = "MMOCRAFT_ITEM_ID";

    /**
     * Constructs a new CustomItem.
     * @param plugin The MMOCraftPlugin instance, required for NBT operations.
     */
    protected CustomItem(MMOCraftPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin instance cannot be null for CustomItem.");
    }

    // --- Abstract Properties (to be defined by concrete subclasses) ---

    /**
     * @return The unique internal identifier for this custom item (e.g., "ruby_sword", "health_potion_tier1").
     */
    public abstract String getItemId();

    /**
     * @return The base Bukkit {@link Material} for this custom item.
     */
    public abstract Material getMaterial();

    /**
     * @return The display name for this custom item (supports color codes).
     */
    public abstract String getDisplayName();

    /**
     * @return The lore for this custom item (list of strings, supports color codes).
     *         Each string in the list will be a new line in the lore.
     */
    public abstract List<String> getLore();

    // --- Optional Concrete Properties (with defaults, can be overridden) ---

    /**
     * @return The custom model data value for this item's appearance. Defaults to 0 (no custom model).
     */
    public int getCustomModelData() {
        return 0;
    }

    /**
     * @return True if this item should be unbreakable, false otherwise. Defaults to false.
     */
    public boolean isUnbreakable() {
        return false;
    }

    /**
     * @return True if this item should have default enchant glint. Defaults to false.
     * Can be overridden for items that should glow without explicit enchantments.
     */
    public boolean hasEnchantGlint() {
        return false;
    }

    /**
     * @return A map of core stats this item modifies and their values (e.g., Stat.STRENGTH, 5.0).
     *         Defaults to an empty map. Subclasses should override this.
     */
    public Map<Stat, Double> getStatModifiers() {
        return Collections.emptyMap();
    }

    /**
     * @return The {@link ItemRarity} of this item. Defaults to {@link ItemRarity#COMMON}.
     *         Subclasses should override this to specify a different rarity.
     */
    public ItemRarity getRarity() {
        return ItemRarity.COMMON; // Default rarity
    }


    // --- Core Method: ItemStack Creation ---

    /**
     * Creates an {@link ItemStack} representation of this custom item.
     * The item stack will have its display name, lore, custom model data, and unbreakability set.
     * Crucially, it will also have an NBT tag "MMOCRAFT_ITEM_ID" storing this item's unique ID.
     *
     * @param amount The number of items in the stack.
     * @return The newly created ItemStack.
     */
    public ItemStack createItemStack(int amount) {
        if (amount <= 0) amount = 1;
        ItemStack itemStack = new ItemStack(getMaterial(), amount);
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            ItemRarity rarity = getRarity();
            String finalDisplayName = rarity.getChatColor() + StringUtil.stripColor(getDisplayName()); // Prepend rarity color, strip existing just in case
            meta.setDisplayName(StringUtil.colorize(finalDisplayName));


            // Set lore
            List<String> baseLore = getLore();
            List<String> finalLore = new ArrayList<>();
            if (baseLore != null) {
                for (String line : baseLore) {
                    finalLore.add(StringUtil.colorize(line));
                }
            }

            // Add stat modifiers to lore automatically
            Map<Stat, Double> statModifiers = getStatModifiers();
            if (statModifiers != null && !statModifiers.isEmpty()) {
                if (!finalLore.isEmpty() && !statModifiers.isEmpty()) { // Add a separator if base lore exists and stats exist
                    finalLore.add(StringUtil.colorize("&7----------"));
                }
                for (Map.Entry<Stat, Double> entry : statModifiers.entrySet()) {
                    Stat stat = entry.getKey();
                    double value = entry.getValue();
                    // Format positive and negative values
                    String sign = value >= 0 ? "+" : "";
                    // Choose color based on buff (green) or debuff (red), or neutral (gray)
                    String color = value > 0 ? "&a" : (value < 0 ? "&c" : "&7");

                    finalLore.add(StringUtil.colorize(color + sign + String.format("%.1f", value) + " " + stat.getDisplayName()));
                }
            }

            // Add Rarity to lore
            if (!finalLore.isEmpty() && (!statModifiers.isEmpty() || rarity != ItemRarity.COMMON)) { // Add separator if there's base lore AND stats/non-common rarity
                 // finalLore.add(""); // Add an empty line for spacing before rarity, if desired
            }
            if (rarity != ItemRarity.COMMON) { // Only add rarity line if not common, or choose to always show
                finalLore.add(StringUtil.colorize(rarity.getDisplayName()));
            }

            if (!finalLore.isEmpty()) {
                meta.setLore(finalLore);
            }

            // Set custom model data
            if (getCustomModelData() > 0) {
                meta.setCustomModelData(getCustomModelData());
            }

            // Set unbreakability
            meta.setUnbreakable(isUnbreakable());

            // Add enchant glint if specified (and no other enchantments are present that would cause it)
            // This usually requires adding a dummy enchantment and hiding it with ItemFlags,
            // or using specific NBT tags if the server platform supports it.
            // For simplicity, if hasEnchantGlint() is true, we can add a dummy enchantment later.
            // For now, this is a placeholder for that logic.
            if (hasEnchantGlint()) {
                // TODO: Add logic for custom enchant glint (e.g., dummy enchant + ItemFlag)
            }

            itemStack.setItemMeta(meta); // Apply initial meta before NBT

            // Set the custom item ID NBT tag
            NBTUtil.setString(itemStack, CUSTOM_ITEM_ID_NBT_KEY, getItemId(), plugin);

        } else {
            // This should ideally not happen for valid materials
            plugin.getLoggingUtil().warning("Failed to get ItemMeta for material: " + getMaterial() + " while creating custom item: " + getItemId());
        }
        return itemStack;
    }

    // --- Static Helper Method ---

    /**
     * Retrieves the custom item ID ("MMOCRAFT_ITEM_ID") from an ItemStack's NBT data.
     *
     * @param itemStack The ItemStack to check.
     * @param plugin The MMOCraftPlugin instance, needed for {@link NamespacedKey}.
     * @return The custom item ID as a String, or null if the tag is not present or the item is invalid.
     */
    public static String getItemId(ItemStack itemStack, MMOCraftPlugin plugin) {
        if (itemStack == null || itemStack.getType().isAir() || !itemStack.hasItemMeta()) {
            return null;
        }
        return NBTUtil.getString(itemStack, CUSTOM_ITEM_ID_NBT_KEY, plugin);
    }

    // --- Standard Overrides ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomItem that)) return false;
        return getItemId().equals(that.getItemId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getItemId());
    }

    @Override
    public String toString() {
        return "CustomItem{" +
               "itemId='" + getItemId() + '\'' +
               ", material=" + getMaterial() +
               ", displayName='" + getDisplayName() + '\'' +
               '}';
    }
}
