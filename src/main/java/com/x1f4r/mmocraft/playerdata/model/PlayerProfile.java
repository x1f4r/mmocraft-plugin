package com.x1f4r.mmocraft.playerdata.model;

import com.x1f4r.mmocraft.config.gameplay.StatScalingConfig;
import com.x1f4r.mmocraft.playerdata.util.ExperienceUtil;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Represents a player's persistent data within MMOCraft.
 * This includes core attributes, stats, progression, and metadata.
 */
public class PlayerProfile {

    private final UUID playerUUID;
    private String playerName; // Last known player name

    // Primary Attributes (managed by this class, influenced by stats)
    private long currentHealth;
    private long maxHealth;
    private long currentMana;
    private long maxMana;

    // Progression
    private int level;
    private long experience; // Experience towards next level
    private long currency;

    private Map<Stat, Double> coreStats;

    private LocalDateTime firstLogin;
    private LocalDateTime lastLogin;

    // Consider a field for total playtime, calculated on logout.
    // private long totalPlaytimeSeconds;

    // --- Skill Cooldowns ---
    private final Map<String, Long> skillCooldowns = new ConcurrentHashMap<>();

    // --- Equipment Stat Modifiers ---
    private final Map<Stat, Double> equipmentStatModifiers = new EnumMap<>(Stat.class); // Added
    private final Map<String, Map<Stat, Double>> temporaryStatModifiers = new ConcurrentHashMap<>();
    private final Map<Stat, Double> effectiveStats = new EnumMap<>(Stat.class);

    // --- Derived Secondary Stats ---
    private double criticalHitChance;
    private double criticalDamageBonus; // Multiplier, e.g., 1.5 for +50% damage
    private double evasionChance;
    private double physicalDamageReduction; // Percentage, e.g., 0.1 for 10%
    private double magicDamageReduction;    // Percentage

    private static volatile StatScalingConfig statScalingConfig = StatScalingConfig.defaults();

    public static void setStatScalingConfig(StatScalingConfig config) {
        statScalingConfig = config == null ? StatScalingConfig.defaults() : config;
    }

    private static StatScalingConfig getStatScalingConfig() {
        return statScalingConfig;
    }

    private void ensureAllStatsInitialized() {
        StatScalingConfig config = getStatScalingConfig();
        for (Stat stat : Stat.values()) {
            coreStats.putIfAbsent(stat, config.getDefaultStatValue(stat));
        }
    }


    // For future expansion, e.g., storing skill levels, quest progress, etc.
    // private Map<String, Object> customData = new HashMap<>();

    /**
     * Minimal constructor for creating a new player's profile.
     * Initializes with default values for a level 1 player.
     *
     * @param playerUUID The unique ID of the player.
     * @param playerName The current name of the player.
     */
    public PlayerProfile(UUID playerUUID, String playerName) {
        this.playerUUID = Objects.requireNonNull(playerUUID, "Player UUID cannot be null.");
        this.playerName = Objects.requireNonNull(playerName, "Player name cannot be null.");

        this.level = 1;
        this.experience = 0;
        this.currency = 0;

        this.coreStats = new EnumMap<>(Stat.class);
        StatScalingConfig config = getStatScalingConfig();
        for (Stat stat : Stat.values()) {
            this.coreStats.put(stat, config.getDefaultStatValue(stat));
        }
        ensureAllStatsInitialized();

        LocalDateTime now = LocalDateTime.now();
        this.firstLogin = now;
        this.lastLogin = now;

        recalculateDerivedAttributes(); // Initial calculation
        // After maxHealth/maxMana are calculated, set current to max for new profile
        this.currentHealth = this.maxHealth;
        this.currentMana = this.maxMana;
    }

    /**
     * Full constructor for loading a player's profile from persistence.
     * Note: currentHealth and currentMana are passed directly as they were at save time.
     */
    public PlayerProfile(UUID playerUUID, String playerName, long currentHealth, long maxHealth, // maxHealth from DB is pre-calculated
                         long currentMana, long maxMana, // maxMana from DB is pre-calculated
                         int level, long experience, long currency,
                         Map<Stat, Double> coreStats, LocalDateTime firstLogin, LocalDateTime lastLogin) {
        this.playerUUID = Objects.requireNonNull(playerUUID, "Player UUID cannot be null.");
        this.playerName = Objects.requireNonNull(playerName, "Player name cannot be null.");

        this.level = level;
        this.experience = experience;
        this.currency = currency;
        this.coreStats = new EnumMap<>(Objects.requireNonNull(coreStats, "Core stats map cannot be null."));
        ensureAllStatsInitialized();
        this.firstLogin = Objects.requireNonNull(firstLogin, "First login time cannot be null.");
        this.lastLogin = Objects.requireNonNull(lastLogin, "Last login time cannot be null.");

        // Recalculate derived attributes based on loaded stats and level
        recalculateDerivedAttributes();

        // Set current health/mana, ensuring they don't exceed the (potentially newly calculated) max values
        this.currentHealth = Math.min(currentHealth, this.maxHealth);
        this.currentMana = Math.min(currentMana, this.maxMana);
    }

    // --- Getters for Primary Attributes & Progression ---
    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public long getCurrentHealth() { return currentHealth; }
    public long getMaxHealth() { return maxHealth; } // Now calculated
    public long getCurrentMana() { return currentMana; }
    public long getMaxMana() { return maxMana; } // Now calculated
    public int getLevel() { return level; }
    public long getExperience() { return experience; }
    public long getCurrency() { return currency; }
    public Map<Stat, Double> getCoreStats() { return new EnumMap<>(coreStats); } // Returns base stats

    public Map<Stat, Double> getEffectiveStats() { return new EnumMap<>(effectiveStats); }

    /**
     * Gets the base value of a core stat (before equipment or other temporary modifiers).
     * @param stat The stat to retrieve.
     * @return The base value of the stat, or 0.0 if not set (though usually all stats are initialized).
     */
    public Double getBaseStatValue(Stat stat) {
        return coreStats.getOrDefault(stat, 0.0); // Assuming 0.0 as a hard default if a stat was somehow missed
    }

    /**
     * Gets the total value of a core stat, including base value and equipment modifiers.
     * This is the value that should be used for most game calculations (e.g., damage, derived attributes).
     * @param stat The stat to retrieve.
     * @return The effective value of the stat.
     */
    public Double getStatValue(Stat stat) {
        return effectiveStats.getOrDefault(Objects.requireNonNull(stat), 0.0);
    }

    /**
     * Gets the invested value for a stat before scaling rules are applied.
     * @param stat The stat to retrieve.
     * @return Base plus equipment contributions.
     */
    public double getTotalInvestedStatValue(Stat stat) {
        return getBaseStatValue(stat) + getEquipmentStatModifier(stat) + getTemporaryStatModifierTotal(stat);
    }

    public LocalDateTime getFirstLogin() { return firstLogin; }
    public LocalDateTime getLastLogin() { return lastLogin; }

    // --- Getters for Derived Secondary Stats ---
    public double getCriticalHitChance() { return criticalHitChance; }
    public double getCriticalDamageBonus() { return criticalDamageBonus; }
    public double getEvasionChance() { return evasionChance; }
    public double getPhysicalDamageReduction() { return physicalDamageReduction; }
    public double getMagicDamageReduction() { return magicDamageReduction; }

    // --- Setters ---
    public void setPlayerName(String playerName) { this.playerName = Objects.requireNonNull(playerName, "Player name cannot be null."); }

    public void setCurrentHealth(long currentHealth) {
        this.currentHealth = Math.max(0L, Math.min(currentHealth, this.maxHealth));
    }
    // setMaxHealth is now implicitly handled by recalculateDerivedAttributes via stats/level

    public void setCurrentMana(long currentMana) {
        this.currentMana = Math.max(0L, Math.min(currentMana, this.maxMana));
    }
    // setMaxMana is now implicitly handled by recalculateDerivedAttributes via stats/level

    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(level, ExperienceUtil.getMaxLevel()));
        recalculateDerivedAttributes(); // Level change can affect max health/mana
    }
    public void setExperience(long experience) { this.experience = Math.max(0, experience); }
    public void setCurrency(long currency) { this.currency = Math.max(0, currency); }

    public void setCoreStats(Map<Stat, Double> coreStats) {
        this.coreStats = new EnumMap<>(Objects.requireNonNull(coreStats));
        ensureAllStatsInitialized();
        recalculateDerivedAttributes();
    }
    public void setStatValue(Stat stat, double value) {
        this.coreStats.put(Objects.requireNonNull(stat), value); // Modifies BASE stat
        recalculateDerivedAttributes();
    }

    // No setter for firstLogin as it should be immutable after creation
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = Objects.requireNonNull(lastLogin); }

    // --- Equipment Stat Modifier Methods ---

    /** Clears all temporary stat modifiers from equipment. Does NOT recalculate derived attributes. */
    public void clearEquipmentStatModifiers() {
        // boolean changed = !equipmentStatModifiers.isEmpty(); // Recalculate will be called by manager
        equipmentStatModifiers.clear();
        // if (changed) {
        //     recalculateDerivedAttributes(); // Manager will call this
        // }
    }

    /**
     * Adds a value to an equipment-based stat modifier.
     * @param stat The stat to modify.
     * @param value The value to add (can be negative).
     */
    public void addEquipmentStatModifier(Stat stat, double value) {
        equipmentStatModifiers.merge(Objects.requireNonNull(stat), value, Double::sum);
        // recalculateDerivedAttributes(); // Manager will call this
    }

    /**
     * Adds all modifiers from the given map to the equipment stat modifiers.
     * Does NOT recalculate derived attributes; caller (PlayerEquipmentManager) is responsible.
     * @param modifiers A map of stats and their values to add.
     */
    public void addAllEquipmentStatModifiers(Map<Stat, Double> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return;
        }
        modifiers.forEach((stat, value) -> equipmentStatModifiers.merge(stat, value, Double::sum));
        // recalculateDerivedAttributes(); // Manager will call this once at the end
    }

    private double getTemporaryStatModifierTotal(Stat stat) {
        double total = 0.0;
        for (Map<Stat, Double> map : temporaryStatModifiers.values()) {
            total += map.getOrDefault(stat, 0.0);
        }
        return total;
    }

    /**
     * Applies or replaces a set of temporary stat modifiers associated with a specific source.
     * Temporary modifiers include contributions from status effects, zone bonuses and other
     * ephemeral gameplay systems.
     *
     * @param sourceKey A unique key representing the source of the modifier bundle.
     * @param modifiers The stat values to apply. Passing {@code null} or an empty map clears the source.
     */
    public void setTemporaryStatModifiers(String sourceKey, Map<Stat, Double> modifiers) {
        Objects.requireNonNull(sourceKey, "sourceKey");
        if (modifiers == null || modifiers.isEmpty()) {
            clearTemporaryStatModifiers(sourceKey);
            return;
        }
        EnumMap<Stat, Double> copy = new EnumMap<>(Stat.class);
        for (Map.Entry<Stat, Double> entry : modifiers.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue() != 0.0) {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        if (copy.isEmpty()) {
            clearTemporaryStatModifiers(sourceKey);
            return;
        }
        temporaryStatModifiers.put(sourceKey, copy);
        recalculateDerivedAttributes();
    }

    /**
     * Adds a single stat modifier contribution for a source, stacking with existing values.
     *
     * @param sourceKey Identifier of the modifier bundle.
     * @param stat      The stat being modified.
     * @param value     The amount to add (can be negative).
     */
    public void addTemporaryStatModifier(String sourceKey, Stat stat, double value) {
        Objects.requireNonNull(sourceKey, "sourceKey");
        Objects.requireNonNull(stat, "stat");
        temporaryStatModifiers
                .computeIfAbsent(sourceKey, k -> new EnumMap<>(Stat.class))
                .merge(stat, value, Double::sum);
        recalculateDerivedAttributes();
    }

    /**
     * Clears all modifiers contributed by a specific source.
     *
     * @param sourceKey Identifier of the modifier bundle.
     */
    public void clearTemporaryStatModifiers(String sourceKey) {
        if (sourceKey == null) {
            return;
        }
        if (temporaryStatModifiers.remove(sourceKey) != null) {
            recalculateDerivedAttributes();
        }
    }

    /**
     * Clears all temporary modifiers currently applied to the profile.
     */
    public void clearAllTemporaryStatModifiers() {
        if (!temporaryStatModifiers.isEmpty()) {
            temporaryStatModifiers.clear();
            recalculateDerivedAttributes();
        }
    }

    /**
     * Gets the total modifier value for a specific stat from all equipment.
     * @param stat The stat.
     * @return The total modifier value, or 0.0 if no modifier exists for that stat.
     */
    public double getEquipmentStatModifier(Stat stat) {
        return equipmentStatModifiers.getOrDefault(Objects.requireNonNull(stat), 0.0);
    }


    /**
     * Recalculates derived attributes like max health, max mana, critical hit chance, etc.,
     * based on current core stats (including equipment modifiers) and level. This should be called whenever a core stat
     * or the player's level changes, or when the profile is loaded.
     */
    public void recalculateDerivedAttributes() {
        StatScalingConfig config = getStatScalingConfig();

        effectiveStats.clear();
        for (Stat stat : Stat.values()) {
            StatScalingConfig.StatRule rule = config.getStatRule(stat);
            double invested = getTotalInvestedStatValue(stat);
            double computed = rule.compute(invested, this.level);
            effectiveStats.put(stat, computed);
        }

        double healthStat = effectiveStats.getOrDefault(Stat.HEALTH,
                config.getStatRule(Stat.HEALTH).compute(0.0, this.level));
        this.maxHealth = Math.max(1L, Math.round(healthStat));
        this.currentHealth = Math.max(0L, Math.min(this.currentHealth, this.maxHealth));

        double manaStat = effectiveStats.getOrDefault(Stat.INTELLIGENCE,
                config.getStatRule(Stat.INTELLIGENCE).compute(0.0, this.level));
        this.maxMana = Math.max(0L, Math.round(manaStat));
        this.currentMana = Math.max(0L, Math.min(this.currentMana, this.maxMana));

        double critChancePercent = effectiveStats.getOrDefault(Stat.CRITICAL_CHANCE, 0.0);
        this.criticalHitChance = clamp(0.0, 1.0, critChancePercent / 100.0);

        double critDamagePercent = effectiveStats.getOrDefault(Stat.CRITICAL_DAMAGE, 0.0);
        this.criticalDamageBonus = Math.max(1.0, 1.0 + (critDamagePercent / 100.0));

        double evasionPercent = effectiveStats.getOrDefault(Stat.EVASION, 0.0);
        this.evasionChance = clamp(0.0, config.getMaxEvasionChance(), evasionPercent / 100.0);

        double defenseValue = Math.max(0.0, effectiveStats.getOrDefault(Stat.DEFENSE, 0.0));
        double trueDefenseValue = Math.max(0.0, effectiveStats.getOrDefault(Stat.TRUE_DEFENSE, 0.0));
        double defenseReduction = defenseValue <= 0.0
                ? 0.0
                : defenseValue / (defenseValue + config.getDefenseReductionBase());
        double trueDefenseReduction = trueDefenseValue <= 0.0
                ? 0.0
                : trueDefenseValue / (trueDefenseValue + config.getTrueDefenseReductionBase());
        double combinedReduction = 1.0 - ((1.0 - defenseReduction) * (1.0 - trueDefenseReduction));
        double clampedReduction = clamp(0.0, config.getMaxDamageReduction(), combinedReduction);
        this.physicalDamageReduction = clampedReduction;
        this.magicDamageReduction = clampedReduction;
    }


    // --- Business Logic Examples ---
    // addExperience method was moved to PlayerDataService as per subtask instructions.

    /**
     * Gets the total experience points needed to advance from the current level to the next.
     * If the player is at the maximum level, this will return a very large value (effectively infinite).
     * @return Experience points needed for the next level.
     */
    public long getExperienceToNextLevel() {
        return ExperienceUtil.getXPForNextLevel(this.level);
    }

    // --- Skill Cooldown Management ---

    /**
     * Sets a cooldown for a specific skill.
     * @param skillId The unique ID of the skill.
     * @param cooldownSeconds The duration of the cooldown in seconds.
     */
    public void setSkillCooldown(String skillId, double cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            this.skillCooldowns.remove(skillId);
        } else {
            this.skillCooldowns.put(skillId, System.currentTimeMillis() + (long) (cooldownSeconds * 1000));
        }
    }

    /**
     * Checks if a specific skill is currently on cooldown.
     * @param skillId The unique ID of the skill.
     * @return True if the skill is on cooldown, false otherwise.
     */
    public boolean isSkillOnCooldown(String skillId) {
        Long expiryTime = this.skillCooldowns.get(skillId);
        return expiryTime != null && System.currentTimeMillis() < expiryTime;
    }

    /**
     * Gets the remaining cooldown time for a specific skill in milliseconds.
     * @param skillId The unique ID of the skill.
     * @return Remaining cooldown in milliseconds, or 0 if not on cooldown.
     */
    public long getSkillRemainingCooldown(String skillId) {
        Long expiryTime = this.skillCooldowns.get(skillId);
        if (expiryTime == null) {
            return 0;
        }
        long remaining = expiryTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Reduces the player's current health by the given amount.
     * Ensures health does not drop below 0.
     * This method is for internal record-keeping; actual damage application to Bukkit entity
     * is handled by systems interacting with Bukkit's damage events.
     * @param amount The amount of damage to take.
     */
    public void takeDamage(double amount) { // Changed to double to match DamageInstance.finalDamage
        if (amount <= 0) return;
        setCurrentHealth(this.currentHealth - (long)Math.ceil(amount)); // Apply damage, ensuring it's at least 1 if amount > 0
    }

    public void heal(long amount) {
        if (amount <= 0) return;
        setCurrentHealth(this.currentHealth + amount);
    }

    public void consumeMana(long amount) {
        if (amount <= 0) return;
        setCurrentMana(this.currentMana - amount);
    }

    private static double clamp(double min, double max, double value) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerProfile that = (PlayerProfile) o;
        return playerUUID.equals(that.playerUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerUUID);
    }

    @Override
    public String toString() {
        return "PlayerProfile{" +
               "playerUUID=" + playerUUID +
               ", playerName='" + playerName + '\'' +
               ", level=" + level +
               ", currentHealth=" + currentHealth + "/" + maxHealth +
               ", currentMana=" + currentMana + "/" + maxMana +
               ", experience=" + experience +
               ", currency=" + currency +
               ", coreStats=" + coreStats.entrySet().stream()
                                        .map(e -> e.getKey().name() + ":" + String.format("%.1f", e.getValue()))
                                        .collect(Collectors.joining(", ", "{", "}")) +
               ", firstLogin=" + firstLogin +
               ", lastLogin=" + lastLogin +
               '}';
    }
}
