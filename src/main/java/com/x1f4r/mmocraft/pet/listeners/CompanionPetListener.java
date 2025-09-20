package com.x1f4r.mmocraft.pet.listeners;

import com.x1f4r.mmocraft.pet.service.CompanionPetService;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;

/**
 * Keeps companion pets in sync with player lifecycle events.
 */
public class CompanionPetListener implements Listener {

    private final CompanionPetService companionPetService;
    private final LoggingUtil logger;

    public CompanionPetListener(CompanionPetService companionPetService, LoggingUtil logger) {
        this.companionPetService = Objects.requireNonNull(companionPetService, "companionPetService");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        companionPetService.handlePlayerQuit(event.getPlayer().getUniqueId());
        logger.finer("Dismissed companion pet for quitting player " + event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        companionPetService.handlePlayerDeath(event.getEntity().getUniqueId());
    }
}
