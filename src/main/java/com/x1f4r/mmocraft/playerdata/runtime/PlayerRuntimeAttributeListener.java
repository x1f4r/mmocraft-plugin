package com.x1f4r.mmocraft.playerdata.runtime;

import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Objects;

/**
 * Triggers immediate synchronisation of player runtime attributes on key lifecycle events.
 */
public class PlayerRuntimeAttributeListener implements Listener {

    private final PlayerRuntimeAttributeService runtimeAttributeService;
    private final LoggingUtil logger;

    public PlayerRuntimeAttributeListener(PlayerRuntimeAttributeService runtimeAttributeService, LoggingUtil logger) {
        this.runtimeAttributeService = Objects.requireNonNull(runtimeAttributeService, "runtimeAttributeService");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        runtimeAttributeService.syncPlayer(event.getPlayer());
        logger.finer("Applied runtime attributes on join for " + event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        runtimeAttributeService.syncPlayer(event.getPlayer());
        logger.finer("Applied runtime attributes on respawn for " + event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        runtimeAttributeService.clearCache(event.getPlayer().getUniqueId());
    }
}
