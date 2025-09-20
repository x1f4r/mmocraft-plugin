package com.x1f4r.mmocraft.playerdata.hud;

import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.util.LoggingUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles lightweight HUD updates such as health/mana action bars and passive mana regeneration.
 */
public class PlayerHudService {

    private final PlayerDataService playerDataService;
    private final LoggingUtil logger;
    private final Map<UUID, Double> manaRemainder = new ConcurrentHashMap<>();

    public PlayerHudService(PlayerDataService playerDataService, LoggingUtil logger) {
        this.playerDataService = playerDataService;
        this.logger = logger;
    }

    /**
     * Updates all online players, regenerating mana and refreshing their action bar HUD.
     *
     * @param deltaSeconds time elapsed since the previous tick invocation.
     */
    public void tick(double deltaSeconds) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline()) {
                continue;
            }
            PlayerProfile profile = playerDataService.getPlayerProfile(player.getUniqueId());
            if (profile == null) {
                continue;
            }
            try {
                regenerateMana(profile, deltaSeconds);
                sendActionBar(player, profile);
            } catch (Exception ex) {
                logger.severe("Failed to update HUD for " + player.getName() + ": " + ex.getMessage(), ex);
            }
        }
    }

    private void regenerateMana(PlayerProfile profile, double deltaSeconds) {
        long maxMana = Math.max(0L, profile.getMaxMana());
        if (maxMana <= 0) {
            manaRemainder.remove(profile.getPlayerUUID());
            profile.setCurrentMana(0);
            return;
        }
        if (profile.getCurrentMana() >= maxMana) {
            manaRemainder.remove(profile.getPlayerUUID());
            profile.setCurrentMana(Math.min(profile.getCurrentMana(), maxMana));
            return;
        }
        double regenRate = Math.max(0.0, profile.getStatValue(Stat.MANA_REGEN));
        double accumulated = manaRemainder.getOrDefault(profile.getPlayerUUID(), 0.0);
        accumulated += regenRate * deltaSeconds;
        long wholePoints = (long) Math.floor(accumulated);
        if (wholePoints > 0) {
            long newMana = Math.min(maxMana, profile.getCurrentMana() + wholePoints);
            profile.setCurrentMana(newMana);
            accumulated -= wholePoints;
        }
        manaRemainder.put(profile.getPlayerUUID(), accumulated);
    }

    private void sendActionBar(Player player, PlayerProfile profile) {
        AttributeInstance healthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = healthAttribute != null ? healthAttribute.getValue() : profile.getMaxHealth();
        double currentHealth = Math.min(player.getHealth(), maxHealth);

        long currentMana = profile.getCurrentMana();
        long maxMana = Math.max(1L, profile.getMaxMana());
        double regenRate = Math.max(0.0, profile.getStatValue(Stat.MANA_REGEN));

        String message = String.format(
                "&c❤ %.0f/%.0f  &b✦ %d/%d  &3⇑ %.1f/s  &6Lvl %d",
                currentHealth,
                maxHealth,
                currentMana,
                maxMana,
                regenRate,
                profile.getLevel());
        player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
    }

    public void clearCache(UUID playerId) {
        if (playerId != null) {
            manaRemainder.remove(playerId);
        }
    }

    public void clearAll() {
        manaRemainder.clear();
    }
}
