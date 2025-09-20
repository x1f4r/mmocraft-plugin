package com.x1f4r.mmocraft.config.gameplay;

import com.x1f4r.mmocraft.playerdata.model.Stat;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable view of stat scaling and derived combat configuration.
 */
public final class StatScalingConfig {

    private final double defaultStatInvestment;
    private final Map<Stat, Double> defaultStatOverrides;
    private final Map<Stat, StatRule> statRules;
    private final double defenseReductionBase;
    private final double trueDefenseReductionBase;
    private final double maxDamageReduction;
    private final double maxEvasionChance;

    private StatScalingConfig(Builder builder) {
        this.defaultStatInvestment = builder.defaultStatInvestment;
        this.defaultStatOverrides = Collections.unmodifiableMap(new EnumMap<>(builder.defaultStatOverrides));

        Map<Stat, StatRule> rules = new EnumMap<>(Stat.class);
        for (Stat stat : Stat.values()) {
            StatRule rule = builder.statRules.get(stat);
            if (rule == null) {
                rule = StatRule.builder().build();
            }
            rules.put(stat, rule);
        }
        this.statRules = Collections.unmodifiableMap(rules);

        this.defenseReductionBase = builder.defenseReductionBase;
        this.trueDefenseReductionBase = builder.trueDefenseReductionBase;
        this.maxDamageReduction = builder.maxDamageReduction;
        this.maxEvasionChance = builder.maxEvasionChance;
    }

    public double getDefaultStatInvestment() {
        return defaultStatInvestment;
    }

    public Map<Stat, Double> getDefaultStatOverrides() {
        return defaultStatOverrides;
    }

    public double getDefaultStatValue(Stat stat) {
        return defaultStatOverrides.getOrDefault(Objects.requireNonNull(stat), defaultStatInvestment);
    }

    public StatRule getStatRule(Stat stat) {
        StatRule rule = statRules.get(Objects.requireNonNull(stat));
        return rule == null ? StatRule.builder().build() : rule;
    }

    public Map<Stat, StatRule> getStatRules() {
        return statRules;
    }

    public double getDefenseReductionBase() {
        return defenseReductionBase;
    }

    public double getTrueDefenseReductionBase() {
        return trueDefenseReductionBase;
    }

    public double getMaxDamageReduction() {
        return maxDamageReduction;
    }

    public double getMaxEvasionChance() {
        return maxEvasionChance;
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
        builder.defaultStatInvestment = 0.0;

        builder.statRule(Stat.HEALTH, StatRule.builder()
                .baseValue(100.0)
                .perPoint(1.0)
                .perLevel(5.0)
                .minValue(1.0)
                .build());

        builder.statRule(Stat.DEFENSE, StatRule.builder()
                .baseValue(0.0)
                .perPoint(1.0)
                .perLevel(0.5)
                .diminishingReturns(new DiminishingReturns(1000.0, 0.5))
                .build());

        builder.statRule(Stat.TRUE_DEFENSE, StatRule.builder()
                .baseValue(0.0)
                .perPoint(1.0)
                .perLevel(0.25)
                .build());

        builder.statRule(Stat.STRENGTH, StatRule.builder()
                .baseValue(0.0)
                .perPoint(1.0)
                .perLevel(0.75)
                .build());

        builder.statRule(Stat.CRITICAL_CHANCE, StatRule.builder()
                .baseValue(30.0)
                .perPoint(0.5)
                .perLevel(0.25)
                .minValue(0.0)
                .maxValue(100.0)
                .build());

        builder.statRule(Stat.CRITICAL_DAMAGE, StatRule.builder()
                .baseValue(50.0)
                .perPoint(1.0)
                .perLevel(0.5)
                .minValue(0.0)
                .build());

        builder.statRule(Stat.INTELLIGENCE, StatRule.builder()
                .baseValue(100.0)
                .perPoint(1.0)
                .perLevel(3.0)
                .minValue(0.0)
                .build());

        builder.statRule(Stat.MANA_REGEN, StatRule.builder()
                .baseValue(5.0)
                .perPoint(0.25)
                .perLevel(0.1)
                .minValue(0.0)
                .build());

        builder.statRule(Stat.ABILITY_POWER, StatRule.builder()
                .baseValue(0.0)
                .perPoint(1.0)
                .perLevel(0.5)
                .minValue(0.0)
                .build());

        builder.statRule(Stat.ATTACK_SPEED, StatRule.builder()
                .baseValue(0.0)
                .perPoint(0.5)
                .perLevel(0.2)
                .minValue(0.0)
                .maxValue(100.0)
                .build());

        builder.statRule(Stat.FEROCITY, StatRule.builder()
                .baseValue(0.0)
                .perPoint(0.5)
                .perLevel(0.2)
                .minValue(0.0)
                .build());

        builder.statRule(Stat.EVASION, StatRule.builder()
                .baseValue(0.0)
                .perPoint(0.5)
                .perLevel(0.2)
                .minValue(0.0)
                .maxValue(60.0)
                .build());

        builder.statRule(Stat.SPEED, StatRule.builder()
                .baseValue(100.0)
                .perPoint(0.25)
                .perLevel(0.15)
                .minValue(0.0)
                .maxValue(400.0)
                .build());

        builder.statRule(Stat.MAGIC_FIND, StatRule.builder()
                .baseValue(0.0)
                .perPoint(0.25)
                .perLevel(0.05)
                .minValue(0.0)
                .build());

        builder.statRule(Stat.PET_LUCK, StatRule.builder()
                .baseValue(0.0)
                .perPoint(0.1)
                .perLevel(0.05)
                .build());

        builder.statRule(Stat.MINING_SPEED, StatRule.builder()
                .baseValue(0.0)
                .perPoint(2.0)
                .perLevel(1.0)
                .minValue(0.0)
                .build());

        builder.statRule(Stat.MINING_FORTUNE, StatRule.builder()
                .baseValue(0.0)
                .perPoint(1.0)
                .perLevel(0.5)
                .minValue(0.0)
                .build());

        builder.statRule(Stat.FARMING_FORTUNE, StatRule.builder()
                .baseValue(0.0)
                .perPoint(1.0)
                .perLevel(0.5)
                .minValue(0.0)
                .build());

        builder.statRule(Stat.FORAGING_FORTUNE, StatRule.builder()
                .baseValue(0.0)
                .perPoint(1.0)
                .perLevel(0.5)
                .minValue(0.0)
                .build());

        builder.statRule(Stat.FISHING_FORTUNE, StatRule.builder()
                .baseValue(0.0)
                .perPoint(1.0)
                .perLevel(0.5)
                .minValue(0.0)
                .build());

        builder.defenseReductionBase = 100.0;
        builder.trueDefenseReductionBase = 100.0;
        builder.maxDamageReduction = 0.90;
        builder.maxEvasionChance = 0.60;

        return builder.build();
    }

    public static final class Builder {
        private double defaultStatInvestment;
        private final Map<Stat, Double> defaultStatOverrides;
        private final Map<Stat, StatRule> statRules;
        private double defenseReductionBase;
        private double trueDefenseReductionBase;
        private double maxDamageReduction;
        private double maxEvasionChance;

        private Builder() {
            this.defaultStatOverrides = new EnumMap<>(Stat.class);
            this.statRules = new EnumMap<>(Stat.class);
            this.defenseReductionBase = 100.0;
            this.trueDefenseReductionBase = 100.0;
            this.maxDamageReduction = 0.90;
            this.maxEvasionChance = 0.60;
        }

        private Builder(StatScalingConfig seed) {
            this();
            Objects.requireNonNull(seed, "seed");
            this.defaultStatInvestment = seed.defaultStatInvestment;
            this.defaultStatOverrides.putAll(seed.defaultStatOverrides);
            this.statRules.putAll(seed.statRules);
            this.defenseReductionBase = seed.defenseReductionBase;
            this.trueDefenseReductionBase = seed.trueDefenseReductionBase;
            this.maxDamageReduction = seed.maxDamageReduction;
            this.maxEvasionChance = seed.maxEvasionChance;
        }

        public Builder defaultStatInvestment(double value) {
            this.defaultStatInvestment = value;
            return this;
        }

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

        public Builder statRule(Stat stat, StatRule rule) {
            this.statRules.put(Objects.requireNonNull(stat), Objects.requireNonNull(rule));
            return this;
        }

        public Builder defenseReductionBase(double value) {
            this.defenseReductionBase = value;
            return this;
        }

        public Builder trueDefenseReductionBase(double value) {
            this.trueDefenseReductionBase = value;
            return this;
        }

        public Builder maxDamageReduction(double value) {
            this.maxDamageReduction = value;
            return this;
        }

        public Builder maxEvasionChance(double value) {
            this.maxEvasionChance = value;
            return this;
        }

        public StatScalingConfig build() {
            return new StatScalingConfig(this);
        }
    }

    public static final class DiminishingReturns {
        private final double threshold;
        private final double multiplier;

        public DiminishingReturns(double threshold, double multiplier) {
            this.threshold = threshold;
            this.multiplier = multiplier;
        }

        public double threshold() {
            return threshold;
        }

        public double multiplier() {
            return multiplier;
        }

        public double apply(double value) {
            if (value <= threshold) {
                return value;
            }
            double extra = value - threshold;
            double scaledExtra = extra * multiplier;
            return threshold + scaledExtra;
        }
    }

    public static final class StatRule {
        private final double baseValue;
        private final double perPoint;
        private final double perLevel;
        private final double minValue;
        private final Double maxValue;
        private final DiminishingReturns diminishingReturns;

        private StatRule(Builder builder) {
            this.baseValue = builder.baseValue;
            this.perPoint = builder.perPoint;
            this.perLevel = builder.perLevel;
            this.minValue = builder.minValue;
            this.maxValue = builder.maxValue;
            this.diminishingReturns = builder.diminishingReturns;
        }

        public double getBaseValue() {
            return baseValue;
        }

        public double getPerPoint() {
            return perPoint;
        }

        public double getPerLevel() {
            return perLevel;
        }

        public double getMinValue() {
            return minValue;
        }

        public Double getMaxValue() {
            return maxValue;
        }

        public DiminishingReturns getDiminishingReturns() {
            return diminishingReturns;
        }

        public double compute(double invested, int level) {
            int effectiveLevel = Math.max(level, 1);
            double value = baseValue + (invested * perPoint) + ((effectiveLevel - 1) * perLevel);
            if (diminishingReturns != null) {
                value = diminishingReturns.apply(value);
            }
            if (value < minValue) {
                value = minValue;
            }
            if (maxValue != null) {
                value = Math.min(value, maxValue);
            }
            return value;
        }

        public Builder toBuilder() {
            return new Builder(this);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private double baseValue = 0.0;
            private double perPoint = 1.0;
            private double perLevel = 0.0;
            private double minValue = 0.0;
            private Double maxValue = null;
            private DiminishingReturns diminishingReturns = null;

            private Builder() {
            }

            private Builder(StatRule seed) {
                this.baseValue = seed.baseValue;
                this.perPoint = seed.perPoint;
                this.perLevel = seed.perLevel;
                this.minValue = seed.minValue;
                this.maxValue = seed.maxValue;
                this.diminishingReturns = seed.diminishingReturns;
            }

            public Builder baseValue(double value) {
                this.baseValue = value;
                return this;
            }

            public Builder perPoint(double value) {
                this.perPoint = value;
                return this;
            }

            public Builder perLevel(double value) {
                this.perLevel = value;
                return this;
            }

            public Builder minValue(double value) {
                this.minValue = value;
                return this;
            }

            public Builder maxValue(Double value) {
                this.maxValue = value;
                return this;
            }

            public Builder diminishingReturns(DiminishingReturns diminishingReturns) {
                this.diminishingReturns = diminishingReturns;
                return this;
            }

            public StatRule build() {
                return new StatRule(this);
            }
        }
    }
}

