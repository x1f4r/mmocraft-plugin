package com.x1f4r.mmocraft.combat.model;

import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import org.bukkit.entity.Entity;

import java.util.UUID;

/**
 * Represents a single instance of damage dealt (or attempted) in combat.
 * This record encapsulates all relevant details about the damage interaction.
 *
 * @param attacker The entity that initiated the damage. Can be null if source is not an entity (e.g. environment).
 * @param victim The entity that received the damage.
 * @param attackerId UUID of the attacker, if applicable.
 * @param victimId UUID of the victim.
 * @param attackerProfile The {@link PlayerProfile} of the attacker, if they are a player and data is available. Null otherwise.
 * @param victimProfile The {@link PlayerProfile} of the victim, if they are a player and data is available. Null otherwise.
 * @param baseDamage The initial damage value before critical hits and reductions, but potentially after initial stat scaling.
 * @param type The {@link DamageType} of the damage (e.g., PHYSICAL, MAGICAL).
 * @param criticalHit True if the damage instance was a critical hit, false otherwise.
 * @param mitigationDetails Details about how damage was reduced (e.g., "Reduced by 20% (Armor)"). For future use.
 * @param finalDamage The actual damage dealt after all calculations (critical hits, reductions, etc.).
 * @param evaded True if the attack was evaded by the victim. If true, finalDamage should be 0.
 */
public record DamageInstance(
    Entity attacker, // Can be null
    Entity victim,   // Should not be null for EntityDamageByEntityEvent context
    UUID attackerId, // Can be null
    UUID victimId,
    PlayerProfile attackerProfile, // Nullable
    PlayerProfile victimProfile,   // Nullable
    double baseDamage,          // Damage after initial scaling (e.g. weapon + strength) but before crit/defense
    DamageType type,
    boolean criticalHit,
    boolean evaded,
    String mitigationDetails, // For future detailed combat logging or display
    double finalDamage          // Damage after crits, defense, evasion
) {
    /**
     * Constructor with minimal required fields, assuming further details are set or calculated.
     */
    public DamageInstance(Entity attacker, Entity victim, double baseDamage, DamageType type) {
        this(attacker, victim,
             attacker != null ? attacker.getUniqueId() : null,
             victim.getUniqueId(),
             null, null, // Profiles usually resolved by calculation service
             baseDamage, type, false, false, "", baseDamage);
    }

    // You might add more constructors or builder pattern if construction becomes complex.

    @Override
    public String toString() {
        return "DamageInstance{" +
               "attacker=" + (attacker != null ? attacker.getType() +":"+ (attackerId != null ? attackerId.toString().substring(0,8) : "N/A") : "N/A") +
               ", victim=" + victim.getType() +":"+victimId.toString().substring(0,8) +
               (attackerProfile != null ? ", attackerName=" + attackerProfile.getPlayerName() : "") +
               (victimProfile != null ? ", victimName=" + victimProfile.getPlayerName() : "") +
               ", baseDamage=" + String.format("%.2f", baseDamage) +
               ", type=" + type +
               ", criticalHit=" + criticalHit +
               ", evaded=" + evaded +
               ", finalDamage=" + String.format("%.2f", finalDamage) +
               (mitigationDetails != null && !mitigationDetails.isEmpty() ? ", mitigation='" + mitigationDetails + '\'' : "") +
               '}';
    }
}
