package com.x1f4r.mmocraft.playerdata.listeners;

import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinQuitListener implements Listener {

    private final PlayerDataService playerDataService;
    private final LoggingUtil logger;

    public PlayerJoinQuitListener(PlayerDataService playerDataService, LoggingUtil logger) {
        this.playerDataService = playerDataService;
        this.logger = logger;
        logger.debug("PlayerJoinQuitListener initialized.");
    }

    // Using AsyncPlayerPreLoginEvent to load data before player fully joins.
    // This is good for performance as it doesn't block the main thread for DB ops.
    @EventHandler(priority = EventPriority.MONITOR) // Use MONITOR if just observing, or NORMAL/HIGH if you might deny login based on data
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            // Don't load data if login is already denied by another plugin or server conditions
            return;
        }
        logger.fine("AsyncPlayerPreLoginEvent for " + event.getName() + " (UUID: " + event.getUniqueId() + "). Loading profile...");
        try {
            // loadPlayerProfile returns a CompletableFuture.
            // We need to ensure this completes before the player is fully in the game if other systems depend on it immediately.
            // For AsyncPlayerPreLoginEvent, we can wait for it.
            playerDataService.loadPlayerProfile(event.getUniqueId(), event.getName())
                .thenAccept(profile -> {
                    if (profile != null) {
                        logger.info("Profile loaded successfully for " + event.getName() + " during pre-login.");
                        // Profile is now in cache via loadPlayerProfile's implementation
                    } else {
                        // This case should ideally be handled within loadPlayerProfile (e.g., creating a temporary one)
                        // or by denying login if a profile is absolutely critical and couldn't be loaded/created.
                        logger.severe("Profile could not be loaded or created for " + event.getName() + ". This might affect player session.");
                        // event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Failed to load your player data. Please try again.");
                    }
                })
                .exceptionally(ex -> {
                    logger.severe("Exception during profile load for " + event.getName() + ": " + ex.getMessage(), ex);
                    // Depending on policy, you might disallow login if data load fails critically
                    // event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Error loading your player data. Please contact an administrator.");
                    return null;
                }).join(); // .join() will block this async thread until the CF completes. This is acceptable here.
        } catch (Exception e) {
            // Catch synchronous exceptions from .join() if the CF completed exceptionally (already logged by exceptionally block)
            logger.severe("Synchronous exception caught from CompletableFuture.join() for " + event.getName() + ": " + e.getMessage(), e);
            // event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Critical error loading your player data.");
        }
    }

    // Fallback or alternative: PlayerJoinEvent (synchronous, but data is available when player is in world)
    // If AsyncPlayerPreLoginEvent is problematic or if later access is fine.
    /*
    @EventHandler(priority = EventPriority.LOWEST) // Load data as early as possible on join
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        logger.fine("PlayerJoinEvent for " + player.getName() + ". Ensuring profile is loaded/cached.");
        // If AsyncPlayerPreLoginEvent succeeded, this will likely just fetch from cache.
        // If it failed, or if not using async pre-login, this will trigger the load.
        playerDataService.loadPlayerProfile(player.getUniqueId(), player.getName())
            .thenAccept(profile -> {
                 if (profile != null) {
                     logger.info("Profile available for " + player.getName() + " on join.");
                 } else {
                     logger.severe("Profile still not available for " + player.getName() + " on join. Problems likely.");
                 }
            })
            .exceptionally(ex -> {
                logger.severe("Failed to ensure profile for " + player.getName() + " on join: " + ex.getMessage(), ex);
                return null;
            });
    }
    */


    @EventHandler(priority = EventPriority.MONITOR) // Use MONITOR as we are just reacting to player leaving
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        logger.fine("PlayerQuitEvent for " + player.getName() + ". Saving and uncaching profile...");

        playerDataService.savePlayerProfile(player.getUniqueId())
            .thenRun(() -> {
                logger.info("Profile saved successfully for " + player.getName() + " on quit.");
                playerDataService.uncachePlayerProfile(player.getUniqueId());
            })
            .exceptionally(ex -> {
                logger.severe("Failed to save profile for " + player.getName() + " on quit: " + ex.getMessage(), ex);
                // Data might be stale on next login if this fails.
                // Still uncache to prevent issues with stale data in memory if player rejoins quickly? Or keep to retry save?
                // For now, let's still uncache.
                playerDataService.uncachePlayerProfile(player.getUniqueId());
                return null;
            });
    }
}
