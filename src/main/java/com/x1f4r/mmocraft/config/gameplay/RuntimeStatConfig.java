package com.x1f4r.mmocraft.config.gameplay;

import com.x1f4r.mmocraft.playerdata.model.Stat;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Runtime gameplay configuration that controls how calculated stats are
 * translated into tangible in-game effects such as movement speed, attack
 * cadence, ability resource consumption, gathering throughput and mob scaling.
 */
public final class RuntimeStatConfig {

    private final MovementSettings movementSettings;
    private final CombatSettings combatSettings;
    private final AbilitySettings abilitySettings;
    private final GatheringSettings gatheringSettings;
    private final MobScalingSettings mobScalingSettings;

    private RuntimeStatConfig(Builder builder) {
        this.movementSettings = builder.movementSettingsBuilder.build();
        this.combatSettings = builder.combatSettingsBuilder.build();
        this.abilitySettings = builder.abilitySettingsBuilder.build();
        this.gatheringSettings = builder.gatheringSettingsBuilder.build();
        this.mobScalingSettings = builder.mobScalingSettingsBuilder.build();
    }

    public MovementSettings getMovementSettings() {
        return movementSettings;
    }

    public CombatSettings getCombatSettings() {
        return combatSettings;
    }

    public AbilitySettings getAbilitySettings() {
        return abilitySettings;
    }

    public GatheringSettings getGatheringSettings() {
        return gatheringSettings;
    }

    public MobScalingSettings getMobScalingSettings() {
        return mobScalingSettings;
    }

    public static RuntimeStatConfig defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.movementSettingsBuilder = movementSettings.toBuilder();
        builder.combatSettingsBuilder = combatSettings.toBuilder();
        builder.abilitySettingsBuilder = abilitySettings.toBuilder();
        builder.gatheringSettingsBuilder = gatheringSettings.toBuilder();
        builder.mobScalingSettingsBuilder = mobScalingSettings.toBuilder();
        return builder;
    }

    public static final class Builder {
        private MovementSettings.Builder movementSettingsBuilder = MovementSettings.builder();
        private CombatSettings.Builder combatSettingsBuilder = CombatSettings.builder();
        private AbilitySettings.Builder abilitySettingsBuilder = AbilitySettings.builder();
        private GatheringSettings.Builder gatheringSettingsBuilder = GatheringSettings.builder();
        private MobScalingSettings.Builder mobScalingSettingsBuilder = MobScalingSettings.builder();

        private Builder() {
        }

        public Builder movementSettings(MovementSettings.Builder builder) {
            this.movementSettingsBuilder = Objects.requireNonNull(builder, "movementSettingsBuilder");
            return this;
        }

        public Builder combatSettings(CombatSettings.Builder builder) {
            this.combatSettingsBuilder = Objects.requireNonNull(builder, "combatSettingsBuilder");
            return this;
        }

        public Builder abilitySettings(AbilitySettings.Builder builder) {
            this.abilitySettingsBuilder = Objects.requireNonNull(builder, "abilitySettingsBuilder");
            return this;
        }

        public Builder gatheringSettings(GatheringSettings.Builder builder) {
            this.gatheringSettingsBuilder = Objects.requireNonNull(builder, "gatheringSettingsBuilder");
            return this;
        }

        public Builder mobScalingSettings(MobScalingSettings.Builder builder) {
            this.mobScalingSettingsBuilder = Objects.requireNonNull(builder, "mobScalingSettingsBuilder");
            return this;
        }

        public RuntimeStatConfig build() {
            return new RuntimeStatConfig(this);
        }
    }

    public static final class MovementSettings {
        private final double baseWalkSpeed;
        private final double maxWalkSpeed;
        private final double minWalkSpeed;
        private final double speedBaseline;

        private MovementSettings(Builder builder) {
            this.baseWalkSpeed = builder.baseWalkSpeed;
            this.maxWalkSpeed = builder.maxWalkSpeed;
            this.minWalkSpeed = builder.minWalkSpeed;
            this.speedBaseline = builder.speedBaseline;
        }

        public double getBaseWalkSpeed() {
            return baseWalkSpeed;
        }

        public double getMaxWalkSpeed() {
            return maxWalkSpeed;
        }

        public double getMinWalkSpeed() {
            return minWalkSpeed;
        }

        public double getSpeedBaseline() {
            return speedBaseline;
        }

        public Builder toBuilder() {
            return builder()
                    .baseWalkSpeed(baseWalkSpeed)
                    .maxWalkSpeed(maxWalkSpeed)
                    .minWalkSpeed(minWalkSpeed)
                    .speedBaseline(speedBaseline);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private double baseWalkSpeed = 0.2;
            private double maxWalkSpeed = 0.7;
            private double minWalkSpeed = 0.05;
            private double speedBaseline = 100.0;

            private Builder() {
            }

            public Builder baseWalkSpeed(double value) {
                this.baseWalkSpeed = value;
                return this;
            }

            public Builder maxWalkSpeed(double value) {
                this.maxWalkSpeed = value;
                return this;
            }

            public Builder minWalkSpeed(double value) {
                this.minWalkSpeed = value;
                return this;
            }

            public Builder speedBaseline(double value) {
                this.speedBaseline = value;
                return this;
            }

            public MovementSettings build() {
                return new MovementSettings(this);
            }
        }
    }

    public static final class CombatSettings {
        private final double baseAttackSpeed;
        private final double attackSpeedPerPoint;
        private final double maxAttackSpeed;
        private final double strengthPhysicalScaling;
        private final double intelligenceMagicalScaling;
        private final double abilityPowerPercentPerPoint;
        private final double ferocityPerExtraHit;
        private final int ferocityMaxExtraHits;
        private final double mobDefenseReductionFactor;

        private CombatSettings(Builder builder) {
            this.baseAttackSpeed = builder.baseAttackSpeed;
            this.attackSpeedPerPoint = builder.attackSpeedPerPoint;
            this.maxAttackSpeed = builder.maxAttackSpeed;
            this.strengthPhysicalScaling = builder.strengthPhysicalScaling;
            this.intelligenceMagicalScaling = builder.intelligenceMagicalScaling;
            this.abilityPowerPercentPerPoint = builder.abilityPowerPercentPerPoint;
            this.ferocityPerExtraHit = builder.ferocityPerExtraHit;
            this.ferocityMaxExtraHits = builder.ferocityMaxExtraHits;
            this.mobDefenseReductionFactor = builder.mobDefenseReductionFactor;
        }

        public double getBaseAttackSpeed() {
            return baseAttackSpeed;
        }

        public double getAttackSpeedPerPoint() {
            return attackSpeedPerPoint;
        }

        public double getMaxAttackSpeed() {
            return maxAttackSpeed;
        }

        public double getStrengthPhysicalScaling() {
            return strengthPhysicalScaling;
        }

        public double getIntelligenceMagicalScaling() {
            return intelligenceMagicalScaling;
        }

        public double getAbilityPowerPercentPerPoint() {
            return abilityPowerPercentPerPoint;
        }

        public double getFerocityPerExtraHit() {
            return ferocityPerExtraHit;
        }

        public int getFerocityMaxExtraHits() {
            return ferocityMaxExtraHits;
        }

        public double getMobDefenseReductionFactor() {
            return mobDefenseReductionFactor;
        }

        public Builder toBuilder() {
            return builder()
                    .baseAttackSpeed(baseAttackSpeed)
                    .attackSpeedPerPoint(attackSpeedPerPoint)
                    .maxAttackSpeed(maxAttackSpeed)
                    .strengthPhysicalScaling(strengthPhysicalScaling)
                    .intelligenceMagicalScaling(intelligenceMagicalScaling)
                    .abilityPowerPercentPerPoint(abilityPowerPercentPerPoint)
                    .ferocityPerExtraHit(ferocityPerExtraHit)
                    .ferocityMaxExtraHits(ferocityMaxExtraHits)
                    .mobDefenseReductionFactor(mobDefenseReductionFactor);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private double baseAttackSpeed = 4.0;
            private double attackSpeedPerPoint = 0.02;
            private double maxAttackSpeed = 8.0;
            private double strengthPhysicalScaling = 0.5;
            private double intelligenceMagicalScaling = 0.7;
            private double abilityPowerPercentPerPoint = 1.0;
            private double ferocityPerExtraHit = 100.0;
            private int ferocityMaxExtraHits = 4;
            private double mobDefenseReductionFactor = 0.04;

            private Builder() {
            }

            public Builder baseAttackSpeed(double value) {
                this.baseAttackSpeed = value;
                return this;
            }

            public Builder attackSpeedPerPoint(double value) {
                this.attackSpeedPerPoint = value;
                return this;
            }

            public Builder maxAttackSpeed(double value) {
                this.maxAttackSpeed = value;
                return this;
            }

            public Builder strengthPhysicalScaling(double value) {
                this.strengthPhysicalScaling = value;
                return this;
            }

            public Builder intelligenceMagicalScaling(double value) {
                this.intelligenceMagicalScaling = value;
                return this;
            }

            public Builder abilityPowerPercentPerPoint(double value) {
                this.abilityPowerPercentPerPoint = value;
                return this;
            }

            public Builder ferocityPerExtraHit(double value) {
                this.ferocityPerExtraHit = value;
                return this;
            }

            public Builder ferocityMaxExtraHits(int value) {
                this.ferocityMaxExtraHits = value;
                return this;
            }

            public Builder mobDefenseReductionFactor(double value) {
                this.mobDefenseReductionFactor = value;
                return this;
            }

            public CombatSettings build() {
                return new CombatSettings(this);
            }
        }
    }

    public static final class AbilitySettings {
        private final double cooldownReductionPerAttackSpeedPoint;
        private final double cooldownReductionPerIntelligencePoint;
        private final double minimumCooldownSeconds;
        private final double manaCostReductionPerIntelligencePoint;
        private final double manaCostReductionPerAbilityPowerPoint;
        private final double minimumManaCostMultiplier;
        private final double minimumManaCost;

        private AbilitySettings(Builder builder) {
            this.cooldownReductionPerAttackSpeedPoint = builder.cooldownReductionPerAttackSpeedPoint;
            this.cooldownReductionPerIntelligencePoint = builder.cooldownReductionPerIntelligencePoint;
            this.minimumCooldownSeconds = builder.minimumCooldownSeconds;
            this.manaCostReductionPerIntelligencePoint = builder.manaCostReductionPerIntelligencePoint;
            this.manaCostReductionPerAbilityPowerPoint = builder.manaCostReductionPerAbilityPowerPoint;
            this.minimumManaCostMultiplier = builder.minimumManaCostMultiplier;
            this.minimumManaCost = builder.minimumManaCost;
        }

        public double getCooldownReductionPerAttackSpeedPoint() {
            return cooldownReductionPerAttackSpeedPoint;
        }

        public double getCooldownReductionPerIntelligencePoint() {
            return cooldownReductionPerIntelligencePoint;
        }

        public double getMinimumCooldownSeconds() {
            return minimumCooldownSeconds;
        }

        public double getManaCostReductionPerIntelligencePoint() {
            return manaCostReductionPerIntelligencePoint;
        }

        public double getManaCostReductionPerAbilityPowerPoint() {
            return manaCostReductionPerAbilityPowerPoint;
        }

        public double getMinimumManaCostMultiplier() {
            return minimumManaCostMultiplier;
        }

        public double getMinimumManaCost() {
            return minimumManaCost;
        }

        public Builder toBuilder() {
            return builder()
                    .cooldownReductionPerAttackSpeedPoint(cooldownReductionPerAttackSpeedPoint)
                    .cooldownReductionPerIntelligencePoint(cooldownReductionPerIntelligencePoint)
                    .minimumCooldownSeconds(minimumCooldownSeconds)
                    .manaCostReductionPerIntelligencePoint(manaCostReductionPerIntelligencePoint)
                    .manaCostReductionPerAbilityPowerPoint(manaCostReductionPerAbilityPowerPoint)
                    .minimumManaCostMultiplier(minimumManaCostMultiplier)
                    .minimumManaCost(minimumManaCost);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private double cooldownReductionPerAttackSpeedPoint = 0.002;
            private double cooldownReductionPerIntelligencePoint = 0.0;
            private double minimumCooldownSeconds = 0.2;
            private double manaCostReductionPerIntelligencePoint = 0.001;
            private double manaCostReductionPerAbilityPowerPoint = 0.001;
            private double minimumManaCostMultiplier = 0.2;
            private double minimumManaCost = 1.0;

            private Builder() {
            }

            public Builder cooldownReductionPerAttackSpeedPoint(double value) {
                this.cooldownReductionPerAttackSpeedPoint = value;
                return this;
            }

            public Builder cooldownReductionPerIntelligencePoint(double value) {
                this.cooldownReductionPerIntelligencePoint = value;
                return this;
            }

            public Builder minimumCooldownSeconds(double value) {
                this.minimumCooldownSeconds = value;
                return this;
            }

            public Builder manaCostReductionPerIntelligencePoint(double value) {
                this.manaCostReductionPerIntelligencePoint = value;
                return this;
            }

            public Builder manaCostReductionPerAbilityPowerPoint(double value) {
                this.manaCostReductionPerAbilityPowerPoint = value;
                return this;
            }

            public Builder minimumManaCostMultiplier(double value) {
                this.minimumManaCostMultiplier = value;
                return this;
            }

            public Builder minimumManaCost(double value) {
                this.minimumManaCost = value;
                return this;
            }

            public AbilitySettings build() {
                return new AbilitySettings(this);
            }
        }
    }

    public static final class GatheringSettings {
        private final double baseGatherDelaySeconds;
        private final double minimumGatherDelaySeconds;
        private final double miningSpeedDelayDivisor;
        private final double miningSpeedHastePerTier;
        private final int miningSpeedMaxHasteTier;
        private final Map<Stat, Double> fortunePerPoint;

        private GatheringSettings(Builder builder) {
            this.baseGatherDelaySeconds = builder.baseGatherDelaySeconds;
            this.minimumGatherDelaySeconds = builder.minimumGatherDelaySeconds;
            this.miningSpeedDelayDivisor = builder.miningSpeedDelayDivisor;
            this.miningSpeedHastePerTier = builder.miningSpeedHastePerTier;
            this.miningSpeedMaxHasteTier = builder.miningSpeedMaxHasteTier;
            this.fortunePerPoint = Map.copyOf(builder.fortunePerPoint);
        }

        public double getBaseGatherDelaySeconds() {
            return baseGatherDelaySeconds;
        }

        public double getMinimumGatherDelaySeconds() {
            return minimumGatherDelaySeconds;
        }

        public double getMiningSpeedDelayDivisor() {
            return miningSpeedDelayDivisor;
        }

        public double getMiningSpeedHastePerTier() {
            return miningSpeedHastePerTier;
        }

        public int getMiningSpeedMaxHasteTier() {
            return miningSpeedMaxHasteTier;
        }

        public double getFortunePerPoint(Stat stat) {
            return fortunePerPoint.getOrDefault(stat, 0.0);
        }

        public Builder toBuilder() {
            Builder builder = builder()
                    .baseGatherDelaySeconds(baseGatherDelaySeconds)
                    .minimumGatherDelaySeconds(minimumGatherDelaySeconds)
                    .miningSpeedDelayDivisor(miningSpeedDelayDivisor)
                    .miningSpeedHastePerTier(miningSpeedHastePerTier)
                    .miningSpeedMaxHasteTier(miningSpeedMaxHasteTier);
            builder.fortunePerPoint.putAll(this.fortunePerPoint);
            return builder;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private double baseGatherDelaySeconds = 1.0;
            private double minimumGatherDelaySeconds = 0.25;
            private double miningSpeedDelayDivisor = 100.0;
            private double miningSpeedHastePerTier = 80.0;
            private int miningSpeedMaxHasteTier = 4;
            private final Map<Stat, Double> fortunePerPoint = new EnumMap<>(Stat.class);

            private Builder() {
                fortunePerPoint.put(Stat.MINING_FORTUNE, 0.002);
                fortunePerPoint.put(Stat.FARMING_FORTUNE, 0.002);
                fortunePerPoint.put(Stat.FORAGING_FORTUNE, 0.002);
                fortunePerPoint.put(Stat.FISHING_FORTUNE, 0.0015);
            }

            public Builder baseGatherDelaySeconds(double value) {
                this.baseGatherDelaySeconds = value;
                return this;
            }

            public Builder minimumGatherDelaySeconds(double value) {
                this.minimumGatherDelaySeconds = value;
                return this;
            }

            public Builder miningSpeedDelayDivisor(double value) {
                this.miningSpeedDelayDivisor = value;
                return this;
            }

            public Builder miningSpeedHastePerTier(double value) {
                this.miningSpeedHastePerTier = value;
                return this;
            }

            public Builder miningSpeedMaxHasteTier(int value) {
                this.miningSpeedMaxHasteTier = value;
                return this;
            }

            public Builder fortunePerPoint(Stat stat, double value) {
                this.fortunePerPoint.put(Objects.requireNonNull(stat), value);
                return this;
            }

            public GatheringSettings build() {
                return new GatheringSettings(this);
            }
        }
    }

    public static final class MobScalingSettings {
        private final double healthPerLevelPercent;
        private final double damagePerLevelPercent;
        private final double defensePerLevel;
        private final double maxHealthMultiplier;
        private final double maxDamageMultiplier;
        private final double maxDefenseBonus;

        private MobScalingSettings(Builder builder) {
            this.healthPerLevelPercent = builder.healthPerLevelPercent;
            this.damagePerLevelPercent = builder.damagePerLevelPercent;
            this.defensePerLevel = builder.defensePerLevel;
            this.maxHealthMultiplier = builder.maxHealthMultiplier;
            this.maxDamageMultiplier = builder.maxDamageMultiplier;
            this.maxDefenseBonus = builder.maxDefenseBonus;
        }

        public double getHealthPerLevelPercent() {
            return healthPerLevelPercent;
        }

        public double getDamagePerLevelPercent() {
            return damagePerLevelPercent;
        }

        public double getDefensePerLevel() {
            return defensePerLevel;
        }

        public double getMaxHealthMultiplier() {
            return maxHealthMultiplier;
        }

        public double getMaxDamageMultiplier() {
            return maxDamageMultiplier;
        }

        public double getMaxDefenseBonus() {
            return maxDefenseBonus;
        }

        public Builder toBuilder() {
            return builder()
                    .healthPerLevelPercent(healthPerLevelPercent)
                    .damagePerLevelPercent(damagePerLevelPercent)
                    .defensePerLevel(defensePerLevel)
                    .maxHealthMultiplier(maxHealthMultiplier)
                    .maxDamageMultiplier(maxDamageMultiplier)
                    .maxDefenseBonus(maxDefenseBonus);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private double healthPerLevelPercent = 0.05;
            private double damagePerLevelPercent = 0.03;
            private double defensePerLevel = 0.5;
            private double maxHealthMultiplier = 5.0;
            private double maxDamageMultiplier = 5.0;
            private double maxDefenseBonus = 200.0;

            private Builder() {
            }

            public Builder healthPerLevelPercent(double value) {
                this.healthPerLevelPercent = value;
                return this;
            }

            public Builder damagePerLevelPercent(double value) {
                this.damagePerLevelPercent = value;
                return this;
            }

            public Builder defensePerLevel(double value) {
                this.defensePerLevel = value;
                return this;
            }

            public Builder maxHealthMultiplier(double value) {
                this.maxHealthMultiplier = value;
                return this;
            }

            public Builder maxDamageMultiplier(double value) {
                this.maxDamageMultiplier = value;
                return this;
            }

            public Builder maxDefenseBonus(double value) {
                this.maxDefenseBonus = value;
                return this;
            }

            public MobScalingSettings build() {
                return new MobScalingSettings(this);
            }
        }
    }
}
