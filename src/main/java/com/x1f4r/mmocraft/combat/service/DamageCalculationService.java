package com.x1f4r.mmocraft.combat.service;

import com.x1f4r.mmocraft.combat.model.DamageInstance;
import com.x1f4r.mmocraft.combat.model.DamageType; // If type is decided early
import org.bukkit.entity.Entity;

/**
 * Service responsible for calculating the outcome of a damage event
 * between an attacker and a victim.
 */
public interface DamageCalculationService {

    /**
     * Calculates a {@link DamageInstance} based on the attacker, victim, and base parameters.
     * This method considers attacker's offensive stats, victim's defensive stats,
     * critical hits, evasion, and damage reductions.
     *
     * @param attacker The entity performing the attack.
     * @param victim The entity receiving the damage.
     * @param baseWeaponDamage The initial base damage of the attack (e.g., from a weapon or mob's default).
     * @param damageType The primary type of the damage (e.g., PHYSICAL, MAGICAL).
     * @return A {@link DamageInstance} object detailing the entire damage interaction.
     */
    DamageInstance calculateDamage(Entity attacker, Entity victim, double baseWeaponDamage, DamageType damageType);

    // Potentially add other methods for more specific calculation steps if needed,
    // or for calculating healing, DoTs, etc.
    // Example:
    // double applyOffensiveBonuses(PlayerProfile attackerProfile, double currentDamage, DamageType type);
    // double applyDefensiveReductions(PlayerProfile victimProfile, double currentDamage, DamageType type);
}
