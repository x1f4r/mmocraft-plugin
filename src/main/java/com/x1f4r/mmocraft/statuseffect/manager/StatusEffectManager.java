package com.x1f4r.mmocraft.statuseffect.manager;

import com.x1f4r.mmocraft.statuseffect.model.ActiveStatusEffect;
import com.x1f4r.mmocraft.statuseffect.model.StatusEffect;
import com.x1f4r.mmocraft.statuseffect.model.StatusEffectType;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.UUID;

/**
 * Manages the application, removal, and ticking of status effects on entities.
 */
public interface StatusEffectManager {

    /**
     * Applies a status effect to the target entity.
     * If the target already has an effect of the same type, behavior depends on
     * the effect's stacking/application rules (e.g., refresh duration, add stack, or ignore).
     *
     * @param target The {@link LivingEntity} to apply the effect to.
     * @param effect The {@link StatusEffect} to apply.
     */
    void applyEffect(LivingEntity target, StatusEffect effect);

    /**
     * Removes all instances of a specific status effect type from the target entity.
     *
     * @param target The {@link LivingEntity} to remove the effect from.
     * @param effectType The {@link StatusEffectType} to remove.
     */
    void removeEffect(LivingEntity target, StatusEffectType effectType);

    /**
     * Removes a specific active status effect instance from a target.
     * This is useful if multiple instances of the same effect type from different sources can exist.
     *
     * @param target The {@link LivingEntity} from which to remove the effect.
     * @param activeEffect The specific {@link ActiveStatusEffect} instance to remove.
     */
    void removeEffectInstance(LivingEntity target, ActiveStatusEffect activeEffect);


    /**
     * Removes all status effects currently active on the target entity.
     *
     * @param target The {@link LivingEntity} to clear all effects from.
     */
    void removeAllEffects(LivingEntity target);

    /**
     * Checks if the target entity currently has any active status effect of the given type.
     *
     * @param target The {@link LivingEntity} to check.
     * @param effectType The {@link StatusEffectType} to look for.
     * @return True if at least one instance of the effect type is active, false otherwise.
     */
    boolean hasEffect(LivingEntity target, StatusEffectType effectType);

    /**
     * Retrieves all active status effects currently applied to the target entity.
     *
     * @param target The {@link LivingEntity} whose effects to retrieve.
     * @return A list of {@link ActiveStatusEffect} instances. The list is empty if no effects are active.
     *         The returned list should be a copy to prevent direct modification of internal state.
     */
    List<ActiveStatusEffect> getActiveEffectsOnEntity(LivingEntity target);

    /**
     * Retrieves all active status effects of a specific type on the target entity.
     *
     * @param target The {@link LivingEntity} to check.
     * @param effectType The {@link StatusEffectType} to filter by.
     * @return A list of {@link ActiveStatusEffect} of the specified type. Empty if none.
     */
    List<ActiveStatusEffect> getActiveEffectsByType(LivingEntity target, StatusEffectType effectType);


    /**
     * Central update tick method for the status effect system.
     * This method should be called periodically by a scheduler (e.g., every server tick or every second).
     * It iterates through all entities with active effects, processes ticks for effects
     * that are ready, and handles the expiration of effects.
     */
    void tickAllActiveEffects();

    /**
     * Called when the plugin is shutting down to clean up resources or active effects.
     */
    void shutdown();
}
