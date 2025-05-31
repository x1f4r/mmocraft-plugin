package com.x1f4r.mmocraft.combat.service;

import com.x1f4r.mmocraft.combat.model.DamageInstance;
import com.x1f4r.mmocraft.combat.model.DamageType;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Random;
import java.util.UUID;

public class BasicDamageCalculationService implements DamageCalculationService {

    private final PlayerDataService playerDataService;
    private final LoggingUtil logger;
    private final Random random = new Random();

    // Example scaling factors (could be moved to config or PlayerProfile constants)
    private static final double STRENGTH_DAMAGE_SCALING_PHYSICAL = 0.5; // Extra damage per point of STR
    private static final double INTELLIGENCE_DAMAGE_SCALING_MAGICAL = 0.7; // Extra damage per point of INT

    public BasicDamageCalculationService(PlayerDataService playerDataService, LoggingUtil logger) {
        this.playerDataService = playerDataService;
        this.logger = logger;
        logger.debug("BasicDamageCalculationService initialized.");
    }

    @Override
    public DamageInstance calculateDamage(Entity attacker, Entity victim, double baseWeaponDamage, DamageType damageType) {
        Entity actualAttacker = attacker;
        // Resolve actual attacker if it's a projectile
        if (attacker instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Entity) {
                actualAttacker = (Entity) shooter;
            }
        }

        UUID attackerId = (actualAttacker != null) ? actualAttacker.getUniqueId() : null;
        UUID victimId = victim.getUniqueId();

        PlayerProfile attackerProfile = null;
        if (actualAttacker instanceof Player) {
            attackerProfile = playerDataService.getPlayerProfile(attackerId);
        }

        PlayerProfile victimProfile = null;
        if (victim instanceof Player) {
            victimProfile = playerDataService.getPlayerProfile(victimId);
        }

        // If profiles are null (e.g. mob, or player data not loaded), they won't contribute stats.
        // This is handled by getStatValue defaulting to 0 if profile is null or stat not present.

        double currentDamage = baseWeaponDamage;
        boolean isCriticalHit = false;
        boolean isEvaded = false;
        String mitigationDetailsLog = "";


        // 1. Apply Attacker's Offensive Bonuses (if attacker is Player with profile)
        if (attackerProfile != null) {
            if (damageType == DamageType.PHYSICAL) {
                currentDamage += attackerProfile.getStatValue(Stat.STRENGTH) * STRENGTH_DAMAGE_SCALING_PHYSICAL;
            } else if (damageType == DamageType.MAGICAL) {
                currentDamage += attackerProfile.getStatValue(Stat.INTELLIGENCE) * INTELLIGENCE_DAMAGE_SCALING_MAGICAL;
            }
            // Could add more complex scaling here based on other stats or buffs

            // Critical Hit Check
            if (random.nextDouble() < attackerProfile.getCriticalHitChance()) {
                isCriticalHit = true;
                currentDamage *= attackerProfile.getCriticalDamageBonus();
            }
        } else if (actualAttacker instanceof LivingEntity && !(actualAttacker instanceof Player)) {
            // Basic Mob Damage (baseWeaponDamage is already their configured damage)
            // Mobs could have implicit crit chance/damage later
        }


        double damageBeforeMitigation = currentDamage;

        // 2. Apply Victim's Defensive Capabilities
        if (victimProfile != null) {
            // Evasion Check (should ideally happen before resource-intensive calculations)
            if (random.nextDouble() < victimProfile.getEvasionChance()) {
                isEvaded = true;
                currentDamage = 0;
                mitigationDetailsLog += "Evaded. ";
            }

            if (!isEvaded && damageType != DamageType.TRUE) { // True damage bypasses reductions
                double reductionPercent = 0;
                if (damageType == DamageType.PHYSICAL) {
                    reductionPercent = victimProfile.getPhysicalDamageReduction();
                    mitigationDetailsLog += String.format("PhysReduc:%.1f%%. ", reductionPercent * 100);
                } else if (damageType == DamageType.MAGICAL) {
                    reductionPercent = victimProfile.getMagicDamageReduction();
                    mitigationDetailsLog += String.format("MagReduc:%.1f%%. ", reductionPercent * 100);
                }
                currentDamage *= (1.0 - reductionPercent);
            }
        } else if (victim instanceof LivingEntity && !(victim instanceof Player) && !isEvaded && damageType != DamageType.TRUE) {
            // Basic Mob Defense (e.g. some mobs might have default armor values)
            // For now, assume mobs take full damage after attacker bonuses unless specific logic is added.
        }

        // Ensure damage is not negative
        double finalDamage = Math.max(0, currentDamage);
        if (isEvaded) finalDamage = 0; // Ensure final damage is 0 if evaded

        return new DamageInstance(
                actualAttacker, victim,
                attackerId, victimId,
                attackerProfile, victimProfile,
                damageBeforeMitigation, // Base damage for DamageInstance means after offensive scaling but before mitigation
                damageType,
                isCriticalHit,
                isEvaded,
                mitigationDetailsLog.trim(),
                finalDamage
        );
    }
}
