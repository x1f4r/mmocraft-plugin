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
    private final MobStatProvider mobStatProvider; // Added
    private final Random random = new Random();

    // Example scaling factors (could be moved to config or PlayerProfile constants)
    private static final double STRENGTH_DAMAGE_SCALING_PHYSICAL = 0.5;
    private static final double INTELLIGENCE_DAMAGE_SCALING_MAGICAL = 0.7;
    private static final double MOB_DEFENSE_REDUCTION_FACTOR = 0.04; // 4% damage reduction per defense point

    public BasicDamageCalculationService(PlayerDataService playerDataService, LoggingUtil logger, MobStatProvider mobStatProvider) { // Added mobStatProvider
        this.playerDataService = playerDataService;
        this.logger = logger;
        this.mobStatProvider = mobStatProvider; // Added
        logger.debug("BasicDamageCalculationService initialized with MobStatProvider.");
    }

    @Override
    public DamageInstance calculateDamage(Entity attacker, Entity victim, double initialBaseDamage, DamageType damageType) {
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
        PlayerProfile victimProfile = null;
        double currentDamage = initialBaseDamage; // Start with the raw damage passed in

        if (actualAttacker instanceof Player pAttacker) {
            attackerProfile = playerDataService.getPlayerProfile(attackerId);
            if (attackerProfile == null) {
                 logger.warning("Attacker Player " + pAttacker.getName() + " has no PlayerProfile in cache. Using raw damage.");
            }
        } else if (actualAttacker instanceof LivingEntity) {
            // Attacker is a Mob. Use MobStatProvider for its base attack if initialBaseDamage wasn't already set by listener from it.
            // The listener now sets initialBaseDamage from MobStatProvider, so we don't need to re-fetch here.
            // currentDamage is already mob's base attack.
        }


        if (victim instanceof Player pVictim) {
            victimProfile = playerDataService.getPlayerProfile(victimId);
            if (victimProfile == null) {
                logger.warning("Victim Player " + pVictim.getName() + " has no PlayerProfile in cache. Will take damage without profile-based mitigation.");
            }
        }

        boolean isCriticalHit = false;
        boolean isEvaded = false;
        String mitigationDetailsLog = "";


        // 1. Apply Attacker's Offensive Bonuses
        if (attackerProfile != null) { // Attacker is Player with profile
            if (damageType == DamageType.PHYSICAL) {
                currentDamage += attackerProfile.getStatValue(Stat.STRENGTH) * STRENGTH_DAMAGE_SCALING_PHYSICAL;
            } else if (damageType == DamageType.MAGICAL) {
                double abilityPowerMultiplier = 1.0 + (attackerProfile.getStatValue(Stat.ABILITY_POWER) / 100.0);
                currentDamage += attackerProfile.getStatValue(Stat.INTELLIGENCE) * INTELLIGENCE_DAMAGE_SCALING_MAGICAL * abilityPowerMultiplier;
            }
            // Critical Hit Check for players
            if (random.nextDouble() < attackerProfile.getCriticalHitChance()) {
                isCriticalHit = true;
                currentDamage *= attackerProfile.getCriticalDamageBonus();
            }
        }
        // Note: Mobs currently don't have offensive stat scaling or crits in this basic system.
        // Their `initialBaseDamage` (from MobStatProvider via listener) is their full base hit.

        double damageAfterOffensiveBonuses = currentDamage; // This is what DamageInstance will store as 'baseDamage'

        // 2. Apply Victim's Defensive Capabilities
        // Evasion Check (Players only for now)
        if (victimProfile != null) {
            if (random.nextDouble() < victimProfile.getEvasionChance()) {
                isEvaded = true;
                currentDamage = 0;
                mitigationDetailsLog += "Evaded. ";
            }
        }
        // No evasion for mobs yet.

        // Damage Reduction
        if (!isEvaded && damageType != DamageType.TRUE) {
            if (victimProfile != null) { // Victim is Player with profile
                double reductionPercent = 0;
                if (damageType == DamageType.PHYSICAL) {
                    reductionPercent = victimProfile.getPhysicalDamageReduction();
                    mitigationDetailsLog += String.format("P.Reduc:%.1f%%. ", reductionPercent * 100);
                } else if (damageType == DamageType.MAGICAL) {
                    reductionPercent = victimProfile.getMagicDamageReduction();
                    mitigationDetailsLog += String.format("M.Reduc:%.1f%%. ", reductionPercent * 100);
                }
                currentDamage *= (1.0 - reductionPercent);
            } else if (victim instanceof LivingEntity && !(victim instanceof Player)) { // Victim is a Mob
                double mobDefense = mobStatProvider.getBaseDefense(victim.getType());
                double mobReduction = Math.min(0.95, mobDefense * MOB_DEFENSE_REDUCTION_FACTOR); // Cap reduction at 95%
                mitigationDetailsLog += String.format("MobDefReduc:%.1f%% (Def:%.0f). ", mobReduction * 100, mobDefense);
                currentDamage *= (1.0 - mobReduction);
            }
        }

        double finalDamage = Math.max(0, currentDamage); // Ensure damage is not negative
        if (isEvaded) finalDamage = 0;

        return new DamageInstance(
                actualAttacker, victim,
                attackerId, victimId,
                attackerProfile, victimProfile,
                damageAfterOffensiveBonuses, // Store damage after attacker's bonuses as 'base' for the instance
                damageType,
                isCriticalHit,
                isEvaded,
                mitigationDetailsLog.trim(),
                finalDamage
        );
    }
}
