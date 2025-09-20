package com.x1f4r.mmocraft.config.gameplay;

import com.x1f4r.mmocraft.playerdata.model.Stat;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable view of the player stat scaling configuration.
 */
public final class StatScalingConfig {

    private final long baseHealth;
    private final double healthPerVitality;
    private final double healthPerLevel;
    private final long baseMana;
    private final double manaPerWisdom;
    private final double manaPerLevel;
    private final double baseCriticalHitChance;
    private final double critChancePerAgility;
    private final double critChancePerLuck;
    private final double baseCriticalDamageBonus;
    private final double critDamageBonusPerStrength;
    private final double baseEvasionChance;
    private final double evasionPerAgility;
    private final double evasionPerLuck;
    private final double physReductionPerDefense;
    private final double maxPhysReduction;
    private final double magicReductionPerWisdom;
    private final double maxMagicReduction;
    private final double defaultStatBaseValue;
    private final Map<Stat, Double> defaultStatOverrides;

    private StatScalingConfig(Builder builder) {
        this.baseHealth = builder.baseHealth;
        this.healthPerVitality = builder.healthPerVitality;
        this.healthPerLevel = builder.healthPerLevel;
        this.baseMana = builder.baseMana;
        this.manaPerWisdom = builder.manaPerWisdom;
        this.manaPerLevel = builder.manaPerLevel;
        this.baseCriticalHitChance = builder.baseCriticalHitChance;
        this.critChancePerAgility = builder.critChancePerAgility;
        this.critChancePerLuck = builder.critChancePerLuck;
        this.baseCriticalDamageBonus = builder.baseCriticalDamageBonus;
        this.critDamageBonusPerStrength = builder.critDamageBonusPerStrength;
        this.baseEvasionChance = builder.baseEvasionChance;
        this.evasionPerAgility = builder.evasionPerAgility;
        this.evasionPerLuck = builder.evasionPerLuck;
        this.physReductionPerDefense = builder.physReductionPerDefense;
        this.maxPhysReduction = builder.maxPhysReduction;
        this.magicReductionPerWisdom = builder.magicReductionPerWisdom;
        this.maxMagicReduction = builder.maxMagicReduction;
        this.defaultStatBaseValue = builder.defaultStatBaseValue;
        this.defaultStatOverrides = Collections.unmodifiableMap(new EnumMap<>(builder.defaultStatOverrides));
    }

    public long getBaseHealth() { return baseHealth; }
    public double getHealthPerVitality() { return healthPerVitality; }
    public double getHealthPerLevel() { return healthPerLevel; }
    public long getBaseMana() { return baseMana; }
    public double getManaPerWisdom() { return manaPerWisdom; }
    public double getManaPerLevel() { return manaPerLevel; }
    public double getBaseCriticalHitChance() { return baseCriticalHitChance; }
    public double getCritChancePerAgility() { return critChancePerAgility; }
    public double getCritChancePerLuck() { return critChancePerLuck; }
    public double getBaseCriticalDamageBonus() { return baseCriticalDamageBonus; }
    public double getCritDamageBonusPerStrength() { return critDamageBonusPerStrength; }
    public double getBaseEvasionChance() { return baseEvasionChance; }
    public double getEvasionPerAgility() { return evasionPerAgility; }
    public double getEvasionPerLuck() { return evasionPerLuck; }
    public double getPhysReductionPerDefense() { return physReductionPerDefense; }
    public double getMaxPhysReduction() { return maxPhysReduction; }
    public double getMagicReductionPerWisdom() { return magicReductionPerWisdom; }
    public double getMaxMagicReduction() { return maxMagicReduction; }
    public double getDefaultStatBaseValue() { return defaultStatBaseValue; }

    public Map<Stat, Double> getDefaultStatOverrides() {
        return defaultStatOverrides;
    }

    public double getDefaultStatValue(Stat stat) {
        return defaultStatOverrides.getOrDefault(Objects.requireNonNull(stat), defaultStatBaseValue);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder(defaults());
    }

    public static Builder builder(StatScalingConfig seed) {
        return new Builder(seed);
    }

    public static StatScalingConfig defaults() {
        Builder builder = new Builder();
        builder.baseHealth = 50L;
        builder.healthPerVitality = 5.0;
        builder.healthPerLevel = 2.0;
        builder.baseMana = 20L;
        builder.manaPerWisdom = 3.0;
        builder.manaPerLevel = 1.0;
        builder.baseCriticalHitChance = 0.05;
        builder.critChancePerAgility = 0.005;
        builder.critChancePerLuck = 0.002;
        builder.baseCriticalDamageBonus = 1.5;
        builder.critDamageBonusPerStrength = 0.01;
        builder.baseEvasionChance = 0.02;
        builder.evasionPerAgility = 0.004;
        builder.evasionPerLuck = 0.001;
        builder.physReductionPerDefense = 0.005;
        builder.maxPhysReduction = 0.80;
        builder.magicReductionPerWisdom = 0.003;
        builder.maxMagicReduction = 0.80;
        builder.defaultStatBaseValue = 10.0;
        builder.defaultStatOverrides = new EnumMap<>(Stat.class);
        builder.defaultStatOverrides.put(Stat.VITALITY, 12.0);
        builder.defaultStatOverrides.put(Stat.WISDOM, 11.0);
        return builder.build();
    }

    public static final class Builder {
        private long baseHealth;
        private double healthPerVitality;
        private double healthPerLevel;
        private long baseMana;
        private double manaPerWisdom;
        private double manaPerLevel;
        private double baseCriticalHitChance;
        private double critChancePerAgility;
        private double critChancePerLuck;
        private double baseCriticalDamageBonus;
        private double critDamageBonusPerStrength;
        private double baseEvasionChance;
        private double evasionPerAgility;
        private double evasionPerLuck;
        private double physReductionPerDefense;
        private double maxPhysReduction;
        private double magicReductionPerWisdom;
        private double maxMagicReduction;
        private double defaultStatBaseValue;
        private Map<Stat, Double> defaultStatOverrides;

        private Builder() {
            this.defaultStatOverrides = new EnumMap<>(Stat.class);
        }

        private Builder(StatScalingConfig seed) {
            this();
            Objects.requireNonNull(seed, "seed");
            this.baseHealth = seed.baseHealth;
            this.healthPerVitality = seed.healthPerVitality;
            this.healthPerLevel = seed.healthPerLevel;
            this.baseMana = seed.baseMana;
            this.manaPerWisdom = seed.manaPerWisdom;
            this.manaPerLevel = seed.manaPerLevel;
            this.baseCriticalHitChance = seed.baseCriticalHitChance;
            this.critChancePerAgility = seed.critChancePerAgility;
            this.critChancePerLuck = seed.critChancePerLuck;
            this.baseCriticalDamageBonus = seed.baseCriticalDamageBonus;
            this.critDamageBonusPerStrength = seed.critDamageBonusPerStrength;
            this.baseEvasionChance = seed.baseEvasionChance;
            this.evasionPerAgility = seed.evasionPerAgility;
            this.evasionPerLuck = seed.evasionPerLuck;
            this.physReductionPerDefense = seed.physReductionPerDefense;
            this.maxPhysReduction = seed.maxPhysReduction;
            this.magicReductionPerWisdom = seed.magicReductionPerWisdom;
            this.maxMagicReduction = seed.maxMagicReduction;
            this.defaultStatBaseValue = seed.defaultStatBaseValue;
            this.defaultStatOverrides.putAll(seed.defaultStatOverrides);
        }

        public Builder baseHealth(long value) { this.baseHealth = value; return this; }
        public Builder healthPerVitality(double value) { this.healthPerVitality = value; return this; }
        public Builder healthPerLevel(double value) { this.healthPerLevel = value; return this; }
        public Builder baseMana(long value) { this.baseMana = value; return this; }
        public Builder manaPerWisdom(double value) { this.manaPerWisdom = value; return this; }
        public Builder manaPerLevel(double value) { this.manaPerLevel = value; return this; }
        public Builder baseCriticalHitChance(double value) { this.baseCriticalHitChance = value; return this; }
        public Builder critChancePerAgility(double value) { this.critChancePerAgility = value; return this; }
        public Builder critChancePerLuck(double value) { this.critChancePerLuck = value; return this; }
        public Builder baseCriticalDamageBonus(double value) { this.baseCriticalDamageBonus = value; return this; }
        public Builder critDamageBonusPerStrength(double value) { this.critDamageBonusPerStrength = value; return this; }
        public Builder baseEvasionChance(double value) { this.baseEvasionChance = value; return this; }
        public Builder evasionPerAgility(double value) { this.evasionPerAgility = value; return this; }
        public Builder evasionPerLuck(double value) { this.evasionPerLuck = value; return this; }
        public Builder physReductionPerDefense(double value) { this.physReductionPerDefense = value; return this; }
        public Builder maxPhysReduction(double value) { this.maxPhysReduction = value; return this; }
        public Builder magicReductionPerWisdom(double value) { this.magicReductionPerWisdom = value; return this; }
        public Builder maxMagicReduction(double value) { this.maxMagicReduction = value; return this; }
        public Builder defaultStatBaseValue(double value) { this.defaultStatBaseValue = value; return this; }

        public Builder setOverride(Stat stat, double value) {
            this.defaultStatOverrides.put(Objects.requireNonNull(stat), value);
            return this;
        }

        public Builder overrides(Map<Stat, Double> overrides) {
            this.defaultStatOverrides.clear();
            if (overrides != null) {
                overrides.forEach(this::setOverride);
            }
            return this;
        }

        public StatScalingConfig build() {
            return new StatScalingConfig(this);
        }
    }
}
