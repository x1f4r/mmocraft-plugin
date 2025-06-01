package com.x1f4r.mmocraft.loot.listeners;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.loot.model.LootTable;
import com.x1f4r.mmocraft.loot.service.LootService;
import com.x1f4r.mmocraft.util.LoggingUtil;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public class MobDeathLootListener implements Listener {

    private final LootService lootService;
    private final CustomItemRegistry itemRegistry;
    private final MMOCraftPlugin plugin; // For passing to LootTable.generateLoot
    private final LoggingUtil logger;

    public MobDeathLootListener(LootService lootService, CustomItemRegistry itemRegistry,
                                MMOCraftPlugin plugin, LoggingUtil logger) {
        this.lootService = lootService;
        this.itemRegistry = itemRegistry;
        this.plugin = plugin;
        this.logger = logger;
        logger.debug("MobDeathLootListener initialized.");
    }

    @EventHandler(priority = EventPriority.NORMAL) // Default priority, can be adjusted
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        Player killer = deadEntity.getKiller(); // Returns null if not killed by a player

        if (killer == null) {
            // Mob not killed by a player, or killed by non-player entity (e.g. another mob, environment)
            // Depending on game design, may or may not want to drop custom loot.
            // For now, require a player killer for custom loot.
            logger.finer("Mob " + deadEntity.getType() + " died without a player killer. No custom loot processed.");
            return;
        }

        EntityType mobType = deadEntity.getType();
        Optional<LootTable> optionalLootTable = lootService.getLootTable(mobType);

        if (optionalLootTable.isPresent()) {
            LootTable lootTable = optionalLootTable.get();
            logger.fine("Processing loot table " + lootTable.getLootTableId() + " for " + mobType + " killed by " + killer.getName());

            List<ItemStack> customDrops = lootTable.generateLoot(itemRegistry, plugin);

            if (!customDrops.isEmpty()) {
                // Decision: Clear vanilla drops if we have custom drops?
                // This is a common behavior for full loot control.
                event.getDrops().clear();
                logger.fine("Cleared vanilla drops for " + mobType + ". Spawning custom loot.");

                lootService.spawnLoot(deadEntity.getLocation(), customDrops);
                for (ItemStack drop : customDrops) {
                    logger.info("Custom drop for " + killer.getName() + ": " + drop.getType() + "x" + drop.getAmount() + " from " + mobType);
                }
            } else {
                logger.fine("Loot table " + lootTable.getLootTableId() + " for " + mobType + " generated no items this time.");
            }
        } else {
            logger.finer("No custom loot table registered for mob type: " + mobType + ". Vanilla drops will apply.");
        }
    }
}
