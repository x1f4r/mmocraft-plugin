package com.x1f4r.mmocraft.playerdata.runtime;

import com.x1f4r.mmocraft.config.gameplay.RuntimeStatConfig;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Synchronises calculated player stats with live Bukkit entity attributes.
 * This service is responsible for translating profile values such as SPEED,
 * ATTACK_SPEED and MINING_SPEED into the corresponding runtime effects.
 */
public class PlayerRuntimeAttributeService {

    private final PlayerDataService playerDataService;
    private volatile RuntimeStatConfig runtimeStatConfig;
    private final LoggingUtil logger;
    private final Map<UUID, PlayerAttributeSnapshot> lastAppliedSnapshots = new ConcurrentHashMap<>();
    private final HasteEffectApplier hasteEffectApplier;
    private final AttributeResolver attributeResolver;

    public PlayerRuntimeAttributeService(PlayerDataService playerDataService,
                                         RuntimeStatConfig runtimeStatConfig,
                                         LoggingUtil logger) {
        this(playerDataService, runtimeStatConfig, logger, new BukkitHasteEffectApplier(logger), new BukkitAttributeResolver());
    }

    public PlayerRuntimeAttributeService(PlayerDataService playerDataService,
                                         RuntimeStatConfig runtimeStatConfig,
                                         LoggingUtil logger,
                                         HasteEffectApplier hasteEffectApplier) {
        this(playerDataService, runtimeStatConfig, logger, hasteEffectApplier, new BukkitAttributeResolver());
    }

    public PlayerRuntimeAttributeService(PlayerDataService playerDataService,
                                         RuntimeStatConfig runtimeStatConfig,
                                         LoggingUtil logger,
                                         HasteEffectApplier hasteEffectApplier,
                                         AttributeResolver attributeResolver) {
        this.playerDataService = Objects.requireNonNull(playerDataService, "playerDataService");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.hasteEffectApplier = Objects.requireNonNull(hasteEffectApplier, "hasteEffectApplier");
        this.attributeResolver = Objects.requireNonNull(attributeResolver, "attributeResolver");
        updateRuntimeConfig(runtimeStatConfig);
    }

    /**
     * Updates the runtime stat configuration used for subsequent synchronisation calls.
     *
     * @param runtimeStatConfig the new runtime configuration to apply.
     */
    public void updateRuntimeConfig(RuntimeStatConfig runtimeStatConfig) {
        this.runtimeStatConfig = Objects.requireNonNull(runtimeStatConfig, "runtimeStatConfig");
    }

    /**
     * Applies attribute updates to all currently online players.
     */
    public void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                syncPlayer(player);
            } catch (Exception ex) {
                logger.severe("Failed to apply runtime attributes for " + player.getName() + ": " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Ensures the provided player's live attributes match their {@link PlayerProfile} values.
     *
     * @param player Bukkit player to update.
     */
    public void syncPlayer(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        PlayerProfile profile = playerDataService.getPlayerProfile(player.getUniqueId());
        if (profile == null) {
            return;
        }
        PlayerAttributeSnapshot desired = computeSnapshot(player, profile);
        PlayerAttributeSnapshot previous = lastAppliedSnapshots.get(player.getUniqueId());
        applySnapshot(player, profile, desired, previous);
        lastAppliedSnapshots.put(player.getUniqueId(), desired);
    }

    /**
     * Clears cached attribute information for a player, e.g. on quit.
     */
    public void clearCache(UUID playerId) {
        if (playerId != null) {
            lastAppliedSnapshots.remove(playerId);
        }
    }

    private PlayerAttributeSnapshot computeSnapshot(Player player, PlayerProfile profile) {
        RuntimeStatConfig runtimeConfig = Objects.requireNonNull(this.runtimeStatConfig, "runtimeStatConfig");
        RuntimeStatConfig.MovementSettings movement = runtimeConfig.getMovementSettings();
        RuntimeStatConfig.CombatSettings combat = runtimeConfig.getCombatSettings();
        RuntimeStatConfig.GatheringSettings gathering = runtimeConfig.getGatheringSettings();

        double maxHealth = profile.getMaxHealth();
        double walkSpeedStat = profile.getStatValue(Stat.SPEED);
        double walkSpeedBaseline = movement.getSpeedBaseline() <= 0 ? 100.0 : movement.getSpeedBaseline();
        double scaledWalkSpeed = movement.getBaseWalkSpeed() * (walkSpeedStat / walkSpeedBaseline);
        double walkSpeed = clamp(movement.getMinWalkSpeed(), movement.getMaxWalkSpeed(), scaledWalkSpeed);

        double attackSpeedStat = profile.getStatValue(Stat.ATTACK_SPEED);
        double attackSpeed = combat.getBaseAttackSpeed() + (attackSpeedStat * combat.getAttackSpeedPerPoint());
        attackSpeed = Math.min(attackSpeed, combat.getMaxAttackSpeed());

        double miningSpeed = profile.getStatValue(Stat.MINING_SPEED);
        double hastePerTier = gathering.getMiningSpeedHastePerTier();
        int hasteTier = hastePerTier <= 0 ? 0 : (int) Math.floor(miningSpeed / hastePerTier);
        hasteTier = Math.min(hasteTier, gathering.getMiningSpeedMaxHasteTier());
        int hasteAmplifier = hasteTier > 0 ? hasteTier - 1 : -1;

        double targetHealth = Math.min(profile.getCurrentHealth(), maxHealth);

        return new PlayerAttributeSnapshot(maxHealth, walkSpeed, attackSpeed, hasteAmplifier, targetHealth);
    }

    private void applySnapshot(Player player, PlayerProfile profile,
                               PlayerAttributeSnapshot desired, PlayerAttributeSnapshot previous) {
        if (previous == null || desired.maxHealth != previous.maxHealth) {
            AttributeInstance maxHealthAttribute = attributeResolver.getMaxHealth(player);
            if (maxHealthAttribute != null) {
                maxHealthAttribute.setBaseValue(Math.max(1.0, desired.maxHealth));
            }
        }

        if (previous == null || desired.walkSpeed != previous.walkSpeed) {
            player.setWalkSpeed((float) desired.walkSpeed);
        }

        if (previous == null || desired.attackSpeed != previous.attackSpeed) {
            AttributeInstance attackSpeedAttribute = attributeResolver.getAttackSpeed(player);
            if (attackSpeedAttribute != null) {
                attackSpeedAttribute.setBaseValue(Math.max(0.1, desired.attackSpeed));
            }
        }

        int previousHaste = previous != null ? previous.hasteAmplifier : -1;
        if (desired.hasteAmplifier >= 0 || previousHaste >= 0) {
            hasteEffectApplier.apply(player, desired.hasteAmplifier, previousHaste);
        }

        double clampedHealth = clamp(0.0, desired.maxHealth, desired.targetHealth);
        if (player.getHealth() != clampedHealth) {
            player.setHealth(clampedHealth);
        }
        profile.setCurrentHealth((long) Math.round(clampedHealth));
    }

    private static double clamp(double min, double max, double value) {
        return Math.max(min, Math.min(max, value));
    }

    @FunctionalInterface
    public interface HasteEffectApplier {
        void apply(Player player, int desiredAmplifier, int previousAmplifier);
    }

    private static final class BukkitHasteEffectApplier implements HasteEffectApplier {
        private final LoggingUtil logger;
        private final AtomicBoolean hasteWarningLogged = new AtomicBoolean(false);

        private BukkitHasteEffectApplier(LoggingUtil logger) {
            this.logger = logger;
        }

        @Override
        public void apply(Player player, int desiredAmplifier, int previousAmplifier) {
            PotionEffectType hasteType = resolveHasteEffectType();
            if (hasteType == null || desiredAmplifier < 0) {
                return;
            }
            PotionEffect current = player.getPotionEffect(hasteType);
            boolean needsUpdate = current == null
                    || current.getAmplifier() != desiredAmplifier
                    || current.getDuration() <= 20;
            if (needsUpdate) {
                player.addPotionEffect(new PotionEffect(hasteType, 60, desiredAmplifier, false, false, false));
            }
        }

        private PotionEffectType resolveHasteEffectType() {
            try {
                return PotionEffectType.HASTE;
            } catch (ExceptionInInitializerError | NoClassDefFoundError | IllegalStateException | IllegalArgumentException ex) {
                if (!hasteWarningLogged.getAndSet(true) && logger != null) {
                    logger.warning("Unable to resolve HASTE potion effect; mining haste bonuses will be skipped: " + ex.getMessage());
                }
                return null;
            }
        }
    }

    @FunctionalInterface
    public interface AttributeResolver {
        AttributeInstance getMaxHealth(Player player);

        default AttributeInstance getAttackSpeed(Player player) {
            return null;
        }
    }

    private static final class BukkitAttributeResolver implements AttributeResolver {
        @Override
        public AttributeInstance getMaxHealth(Player player) {
            return player.getAttribute(Attribute.MAX_HEALTH);
        }

        @Override
        public AttributeInstance getAttackSpeed(Player player) {
            return player.getAttribute(Attribute.ATTACK_SPEED);
        }
    }

    private record PlayerAttributeSnapshot(double maxHealth,
                                           double walkSpeed,
                                           double attackSpeed,
                                           int hasteAmplifier,
                                           double targetHealth) {
    }
}
