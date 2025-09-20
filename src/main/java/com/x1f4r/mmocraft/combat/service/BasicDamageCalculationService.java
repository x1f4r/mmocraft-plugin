package com.x1f4r.mmocraft.combat.service;

import com.x1f4r.mmocraft.combat.model.DamageInstance;
import com.x1f4r.mmocraft.combat.model.DamageType;
import com.x1f4r.mmocraft.config.gameplay.GameplayConfigService;
import com.x1f4r.mmocraft.config.gameplay.RuntimeStatConfig;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.world.spawning.service.BasicCustomSpawningService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public class BasicDamageCalculationService implements DamageCalculationService {

    private final PlayerDataService playerDataService;
    private final LoggingUtil logger;
    private final MobStatProvider mobStatProvider;
    private final GameplayConfigService gameplayConfigService;
    private final Random random = new Random();

    public BasicDamageCalculationService(PlayerDataService playerDataService,
                                         LoggingUtil logger,
                                         MobStatProvider mobStatProvider,
                                         GameplayConfigService gameplayConfigService) {
        this.playerDataService = Objects.requireNonNull(playerDataService, "playerDataService");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.mobStatProvider = Objects.requireNonNull(mobStatProvider, "mobStatProvider");
        this.gameplayConfigService = Objects.requireNonNull(gameplayConfigService, "gameplayConfigService");
        logger.debug("BasicDamageCalculationService initialized with MobStatProvider.");
    }

    @Override
    public DamageInstance calculateDamage(Entity attacker, Entity victim, double initialBaseDamage, DamageType damageType) {
        Entity actualAttacker = resolveActualAttacker(attacker);
        UUID attackerId = actualAttacker != null ? actualAttacker.getUniqueId() : null;
        UUID victimId = victim.getUniqueId();

        PlayerProfile attackerProfile = attackerId != null ? playerDataService.getPlayerProfile(attackerId) : null;
        PlayerProfile victimProfile = playerDataService.getPlayerProfile(victimId);

        if (actualAttacker instanceof Player && attackerProfile == null) {
            logger.warning("Attacker Player " + actualAttacker.getName() + " has no PlayerProfile in cache. Using raw damage.");
        }
        if (victim instanceof Player && victimProfile == null) {
            logger.warning("Victim Player " + victim.getName() + " has no PlayerProfile in cache. Will take damage without profile-based mitigation.");
        }

        RuntimeStatConfig runtimeConfig = gameplayConfigService.getRuntimeStatConfig();
        RuntimeStatConfig.CombatSettings combatConfig = runtimeConfig.getCombatSettings();
        RuntimeStatConfig.MobScalingSettings mobScaling = runtimeConfig.getMobScalingSettings();

        double currentDamage = initialBaseDamage;
        boolean isCriticalHit = false;
        boolean isEvaded = false;
        StringBuilder mitigationDetails = new StringBuilder();

        if (attackerProfile != null) {
            if (damageType == DamageType.PHYSICAL) {
                currentDamage += attackerProfile.getStatValue(Stat.STRENGTH) * combatConfig.getStrengthPhysicalScaling();
            } else if (damageType == DamageType.MAGICAL) {
                double abilityPowerMultiplier = 1.0 + (attackerProfile.getStatValue(Stat.ABILITY_POWER)
                        * combatConfig.getAbilityPowerPercentPerPoint() / 100.0);
                currentDamage += attackerProfile.getStatValue(Stat.INTELLIGENCE)
                        * combatConfig.getIntelligenceMagicalScaling() * abilityPowerMultiplier;
            }
            if (random.nextDouble() < attackerProfile.getCriticalHitChance()) {
                isCriticalHit = true;
                currentDamage *= attackerProfile.getCriticalDamageBonus();
            }
        }

        if (victimProfile != null && actualAttacker instanceof LivingEntity && !(actualAttacker instanceof Player)) {
            double levelFactor = Math.max(0, victimProfile.getLevel() - 1);
            double scaling = 1.0 + (levelFactor * mobScaling.getDamagePerLevelPercent());
            scaling = Math.min(scaling, mobScaling.getMaxDamageMultiplier());
            if (scaling > 1.0) {
                mitigationDetails.append(String.format("MobScale:+%.0f%%. ", (scaling - 1) * 100));
            }
            currentDamage *= scaling;
        }

        double damageAfterOffensiveBonuses = currentDamage;

        if (victimProfile != null && random.nextDouble() < victimProfile.getEvasionChance()) {
            isEvaded = true;
            currentDamage = 0;
            mitigationDetails.append("Evaded. ");
        }

        if (!isEvaded && damageType != DamageType.TRUE) {
            if (victimProfile != null) {
                double reductionPercent = damageType == DamageType.PHYSICAL
                        ? victimProfile.getPhysicalDamageReduction()
                        : victimProfile.getMagicDamageReduction();
                if (reductionPercent > 0) {
                    mitigationDetails.append(String.format("%sReduc:%.1f%%. ",
                            damageType == DamageType.PHYSICAL ? "P." : "M.", reductionPercent * 100));
                }
                currentDamage *= (1.0 - reductionPercent);
            } else if (victim instanceof LivingEntity livingVictim) {
                double mobDefense = getMetadataDouble(livingVictim, BasicCustomSpawningService.METADATA_KEY_SCALED_DEFENSE);
                if (Double.isNaN(mobDefense)) {
                    mobDefense = mobStatProvider.getBaseDefense(livingVictim.getType());
                    if (attackerProfile != null) {
                        double levelFactor = Math.max(0, attackerProfile.getLevel() - 1);
                        mobDefense = Math.min(mobDefense + (levelFactor * mobScaling.getDefensePerLevel()),
                                mobScaling.getMaxDefenseBonus());
                    }
                }
                double mobReduction = Math.min(0.95, Math.max(0.0, mobDefense) * combatConfig.getMobDefenseReductionFactor());
                if (mobReduction > 0) {
                    mitigationDetails.append(String.format("MobDefReduc:%.1f%% (Def:%.0f). ", mobReduction * 100, mobDefense));
                }
                currentDamage *= (1.0 - mobReduction);
            }
        }

        double mitigatedDamage = Math.max(0, currentDamage);
        double ferocityBonus = 0.0;
        if (!isEvaded && attackerProfile != null) {
            double ferocity = attackerProfile.getStatValue(Stat.FEROCITY);
            double perHit = combatConfig.getFerocityPerExtraHit();
            if (perHit > 0 && ferocity > 0) {
                int guaranteedHits = (int) Math.floor(ferocity / perHit);
                double remainderChance = (ferocity % perHit) / perHit;
                int extraHits = Math.min(guaranteedHits, combatConfig.getFerocityMaxExtraHits());
                if (random.nextDouble() < remainderChance && extraHits < combatConfig.getFerocityMaxExtraHits()) {
                    extraHits++;
                }
                if (extraHits > 0) {
                    ferocityBonus = mitigatedDamage * extraHits;
                    mitigationDetails.append("Ferocity:").append(extraHits).append("x. ");
                }
            }
        }

        double finalDamage = isEvaded ? 0 : Math.max(0, mitigatedDamage + ferocityBonus);

        return new DamageInstance(
                actualAttacker, victim,
                attackerId, victimId,
                attackerProfile, victimProfile,
                damageAfterOffensiveBonuses,
                damageType,
                isCriticalHit,
                isEvaded,
                mitigationDetails.toString().trim(),
                finalDamage
        );
    }

    private Entity resolveActualAttacker(Entity attacker) {
        Entity actualAttacker = attacker;
        if (attacker instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Entity shooterEntity) {
                actualAttacker = shooterEntity;
            }
        }
        return actualAttacker;
    }

    private double getMetadataDouble(Entity entity, String key) {
        if (entity == null || !entity.hasMetadata(key)) {
            return Double.NaN;
        }
        for (MetadataValue metadataValue : entity.getMetadata(key)) {
            Object value = metadataValue.value();
            if (value instanceof Number number) {
                return number.doubleValue();
            }
        }
        return Double.NaN;
    }
}
