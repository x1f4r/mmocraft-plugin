package com.x1f4r.mmocraft.statuseffect.model;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import org.bukkit.entity.LivingEntity;

import java.util.Objects;
import java.util.UUID;

/**
 * Abstract base class for all status effects.
 * A status effect represents a temporary (or permanent) condition applied to an entity,
 * potentially altering its stats, behavior, or applying periodic effects.
 */
public abstract class StatusEffect {

    protected final MMOCraftPlugin plugin; // For accessing services
    protected final StatusEffectType effectType;
    protected final double durationSeconds; // -1 for permanent or until explicitly removed
    protected final double potency; // Magnitude of the effect (e.g., damage amount, stat bonus)
    protected final double tickIntervalSeconds; // How often onTick is called; 0 if it doesn't tick
    protected final UUID sourceEntityId; // Optional: Who applied this effect

    /**
     * Constructs a new StatusEffect.
     *
     * @param plugin The MMOCraftPlugin instance for service access.
     * @param effectType The type of this status effect.
     * @param durationSeconds The total duration of the effect in seconds. Use -1 for permanent effects.
     * @param potency The magnitude or strength of the effect.
     * @param tickIntervalSeconds The interval in seconds at which {@link #onTick(LivingEntity, PlayerProfile)} should be called.
     *                            Set to 0 or less if the effect does not have periodic ticks.
     * @param sourceEntityId The UUID of the entity that applied this effect, can be null.
     */
    public StatusEffect(MMOCraftPlugin plugin, StatusEffectType effectType, double durationSeconds,
                        double potency, double tickIntervalSeconds, UUID sourceEntityId) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin instance cannot be null for StatusEffect.");
        this.effectType = Objects.requireNonNull(effectType, "StatusEffectType cannot be null.");
        this.durationSeconds = durationSeconds;
        this.potency = potency;
        this.tickIntervalSeconds = tickIntervalSeconds;
        this.sourceEntityId = sourceEntityId; // Nullable
    }

    // Getters
    public StatusEffectType getEffectType() { return effectType; }
    public double getDurationSeconds() { return durationSeconds; }
    public double getPotency() { return potency; }
    public double getTickIntervalSeconds() { return tickIntervalSeconds; }
    public UUID getSourceEntityId() { return sourceEntityId; }
    public boolean isPermanent() { return durationSeconds < 0; }
    public boolean doesTick() { return tickIntervalSeconds > 0; }


    /**
     * Called when this status effect is first applied to the target entity.
     * Use this to apply initial changes, like immediate stat modifications or messages.
     *
     * @param target The {@link LivingEntity} the effect is being applied to.
     * @param targetProfileIfPlayer The {@link PlayerProfile} of the target, if the target is a Player. Null otherwise.
     */
    public abstract void onApply(LivingEntity target, PlayerProfile targetProfileIfPlayer);

    /**
     * Called periodically if {@code tickIntervalSeconds > 0}.
     * Use this for effects like damage over time (DoT), healing over time (HoT), or periodic checks.
     *
     * @param target The {@link LivingEntity} affected.
     * @param targetProfileIfPlayer The {@link PlayerProfile} of the target, if the target is a Player. Null otherwise.
     */
    public abstract void onTick(LivingEntity target, PlayerProfile targetProfileIfPlayer);

    /**
     * Called when the status effect expires naturally (its duration runs out).
     * Use this to clean up, such as reverting stat modifications.
     *
     * @param target The {@link LivingEntity} the effect is expiring from.
     * @param targetProfileIfPlayer The {@link PlayerProfile} of the target, if the target is a Player. Null otherwise.
     */
    public abstract void onExpire(LivingEntity target, PlayerProfile targetProfileIfPlayer);

    /**
     * Called if the status effect is explicitly removed before its natural expiration.
     * Often, this can delegate to {@link #onExpire(LivingEntity, PlayerProfile)}.
     *
     * @param target The {@link LivingEntity} the effect is being removed from.
     * @param targetProfileIfPlayer The {@link PlayerProfile} of the target, if the target is a Player. Null otherwise.
     */
    public void onRemove(LivingEntity target, PlayerProfile targetProfileIfPlayer) {
        onExpire(target, targetProfileIfPlayer); // Default behavior is same as expiry
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StatusEffect that)) return false;
        // Two status effects are the same if they are of the same type and from the same source (for stackable/replaceable effects)
        // Potency and duration might differ for different instances of the "same" effect.
        // For management purposes, type and source are often key.
        return effectType == that.effectType && Objects.equals(sourceEntityId, that.sourceEntityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(effectType, sourceEntityId);
    }

    @Override
    public String toString() {
        return "StatusEffect{" +
               "type=" + effectType +
               ", duration=" + durationSeconds + "s" +
               ", potency=" + potency +
               (doesTick() ? ", interval=" + tickIntervalSeconds + "s" : "") +
               (sourceEntityId != null ? ", source=" + sourceEntityId.toString().substring(0,8) : "") +
               '}';
    }
}
