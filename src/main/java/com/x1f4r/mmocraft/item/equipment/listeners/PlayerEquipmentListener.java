package com.x1f4r.mmocraft.item.equipment.listeners;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.item.equipment.service.PlayerEquipmentManager;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
// Consider adding more specific events like PlayerDropItemEvent, EntityPickupItemEvent, InventoryClickEvent (ARMOR slots)
// but InventoryCloseEvent and PlayerItemHeldEvent cover many cases more simply for now.

public class PlayerEquipmentListener implements Listener {

    private final PlayerEquipmentManager equipmentManager;
    private final LoggingUtil logger;
    private final MMOCraftPlugin plugin; // For scheduler

    public PlayerEquipmentListener(MMOCraftPlugin plugin, PlayerEquipmentManager equipmentManager, LoggingUtil logger) {
        this.plugin = plugin;
        this.equipmentManager = equipmentManager;
        this.logger = logger;
        logger.debug("PlayerEquipmentListener initialized.");
    }

    @EventHandler(priority = EventPriority.MONITOR) // Process after player data is loaded
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        logger.fine("Player " + player.getName() + " joined. Updating equipment stats.");
        // Schedule for next tick to ensure PlayerProfile is fully loaded and cached, especially if async load
        Bukkit.getScheduler().runTaskLater(plugin, () -> equipmentManager.updateEquipmentStats(player), 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        logger.fine("Player " + player.getName() + " respawned. Updating equipment stats.");
        // Equipment might change on respawn (e.g., keepInventory rules, or specific respawn loadouts)
        Bukkit.getScheduler().runTaskLater(plugin, () -> equipmentManager.updateEquipmentStats(player), 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            logger.finer("Player " + player.getName() + " closed inventory. Potentially updating equipment stats.");
            // This is a broad catch-all. More specific events (InventoryClickEvent on armor slots)
            // could be more performant but are more complex to implement correctly for all cases.
            equipmentManager.updateEquipmentStats(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        logger.finer("Player " + player.getName() + " changed held item slot. Updating equipment stats.");
        // Item in hand might change stats.
        // Bukkit documentation suggests the event fires before the inventory is actually updated.
        // A 1-tick delay is often recommended.
        Bukkit.getScheduler().runTaskLater(plugin, () -> equipmentManager.updateEquipmentStats(player), 1L);
    }

    // TODO: Consider more granular events for equip/unequip for better accuracy and performance:
    // - InventoryClickEvent (specifically for ARMOR slots, or player inventory <-> armor slots)
    // - PlayerDropItemEvent / PlayerPickupItemEvent if it involves equipped items (less common for armor/hands directly)
    // - BlockDispenseArmorEvent (if dispensers can equip armor onto players)
}
