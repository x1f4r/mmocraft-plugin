package com.x1f4r.mmocraft.item.equipment.service;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class PlayerEquipmentManager {

    private final MMOCraftPlugin plugin; // For CustomItem.getItemId
    private final PlayerDataService playerDataService;
    private final CustomItemRegistry customItemRegistry;
    private final LoggingUtil logger;

    public PlayerEquipmentManager(MMOCraftPlugin plugin, PlayerDataService playerDataService,
                                  CustomItemRegistry customItemRegistry, LoggingUtil logger) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
        this.customItemRegistry = customItemRegistry;
        this.logger = logger;
        logger.debug("PlayerEquipmentManager initialized.");
    }

    /**
     * Updates a player's stats based on their currently equipped custom items.
     * This method clears existing equipment stat modifiers, iterates through relevant
     * inventory slots, sums up stat modifiers from any custom items found,
     * and then applies these new totals to the player's profile, triggering a
     * recalculation of derived attributes once at the end.
     *
     * @param player The player whose equipment stats need to be updated.
     */
    public void updateEquipmentStats(Player player) {
        if (player == null) {
            logger.warning("Attempted to update equipment stats for null player.");
            return;
        }

        PlayerProfile profile = playerDataService.getPlayerProfile(player.getUniqueId());
        if (profile == null) {
            logger.fine("PlayerProfile not found for " + player.getName() + " during equipment update. Likely still loading or not an MMOCraft managed player.");
            return;
        }

        profile.clearEquipmentStatModifiers(); // Clear previous equipment bonuses (does not recalc yet)

        Map<Stat, Double> collectiveModifiers = new EnumMap<>(Stat.class);
        List<ItemStack> itemsToCheck = new ArrayList<>();

        PlayerInventory inventory = player.getInventory();
        itemsToCheck.add(inventory.getItemInMainHand());
        itemsToCheck.add(inventory.getItemInOffHand());
        if (inventory.getHelmet() != null) itemsToCheck.add(inventory.getHelmet());
        if (inventory.getChestplate() != null) itemsToCheck.add(inventory.getChestplate());
        if (inventory.getLeggings() != null) itemsToCheck.add(inventory.getLeggings());
        if (inventory.getBoots() != null) itemsToCheck.add(inventory.getBoots());
        // Consider PlayerInventory.getExtraContents() for other slots if custom items can go there

        for (ItemStack itemStack : itemsToCheck) {
            if (itemStack != null && !itemStack.getType().isAir()) {
                customItemRegistry.getCustomItem(itemStack).ifPresent(customItem -> {
                    Map<Stat, Double> itemModifiers = customItem.getStatModifiers();
                    if (itemModifiers != null && !itemModifiers.isEmpty()) {
                        logger.finer("Item " + customItem.getItemId() + " provides stats: " + itemModifiers);
                        itemModifiers.forEach((stat, value) -> collectiveModifiers.merge(stat, value, Double::sum));
                    }
                });
            }
        }

        if (!collectiveModifiers.isEmpty()) {
            logger.fine("Applying collective equipment stats for " + player.getName() + ": " + collectiveModifiers);
            profile.addAllEquipmentStatModifiers(collectiveModifiers); // Applies all at once (does not recalc yet)
        }

        // Crucial: Recalculate all derived attributes once after all modifiers have been updated.
        profile.recalculateDerivedAttributes();
        logger.fine("Recalculated derived attributes for " + player.getName() + " after equipment update. MaxHP: " + profile.getMaxHealth());
    }
}
