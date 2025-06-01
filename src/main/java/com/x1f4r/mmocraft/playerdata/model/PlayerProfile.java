package com.x1f4r.mmocraft.playerdata.model;

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

    // --- Derived Secondary Stats ---
    private double criticalHitChance;
    private double criticalDamageBonus; // Multiplier, e.g., 1.5 for +50% damage
    private double evasionChance;
    private double physicalDamageReduction; // Percentage, e.g., 0.1 for 10%
    private double magicDamageReduction;    // Percentage

    // --- Constants for Derived Stat Calculation ---
    // Max Health
    private static final long BASE_HEALTH = 50;
    private static final double HEALTH_PER_VITALITY = 5.0;
    private static final double HEALTH_PER_LEVEL = 2.0;
    // Max Mana
    private static final long BASE_MANA = 20;
    private static final double MANA_PER_WISDOM = 3.0;
    private static final double MANA_PER_LEVEL = 1.0;
    // Critical Hit Chance
    private static final double BASE_CRITICAL_HIT_CHANCE = 0.05; // 5%
    private static final double CRIT_CHANCE_PER_AGILITY = 0.005; // 0.5% per Agility point
    private static final double CRIT_CHANCE_PER_LUCK = 0.002;    // 0.2% per Luck point
    // Critical Damage Bonus
    private static final double BASE_CRITICAL_DAMAGE_BONUS = 1.5; // 150% total damage (i.e., +50% bonus)
    private static final double CRIT_DAMAGE_BONUS_PER_STRENGTH = 0.01; // +1% bonus damage per Strength
    // Evasion Chance
    private static final double BASE_EVASION_CHANCE = 0.02; // 2%
    private static final double EVASION_PER_AGILITY = 0.004; // 0.4% per Agility
    private static final double EVASION_PER_LUCK = 0.001;    // 0.1% per Luck
    // Physical Damage Reduction (example: 0.5% per point of Defense, capped for safety)
    private static final double PHYS_REDUCTION_PER_DEFENSE = 0.005; // 0.5%
    private static final double MAX_PHYS_REDUCTION = 0.80; // 80% cap
    // Magic Damage Reduction (example: 0.3% per point of Wisdom, capped)
    private static final double MAGIC_REDUCTION_PER_WISDOM = 0.003; // 0.3%
    private static final double MAX_MAGIC_REDUCTION = 0.80; // 80% cap


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
        // Initialize with base values for each stat (these can also be constants or configurable)
        for (Stat stat : Stat.values()) {
            this.coreStats.put(stat, 10.0); // Default starting value for all core stats
        }
        // Example: Make Vitality and Wisdom slightly higher by default if desired
        this.coreStats.put(Stat.VITALITY, 12.0);
        this.coreStats.put(Stat.WISDOM, 11.0);

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
        return getBaseStatValue(stat) + getEquipmentStatModifier(stat);
        // Future: + getTemporaryStatusEffectModifier(stat);
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
        this.currentHealth = Math.max(0, Math.min(currentHealth, this.maxHealth));
    }
    // setMaxHealth is now implicitly handled by recalculateDerivedAttributes via stats/level

    public void setCurrentMana(long currentMana) {
        this.currentMana = Math.max(0, Math.min(currentMana, this.maxMana));
    }
    // setMaxMana is now implicitly handled by recalculateDerivedAttributes via stats/level

    public void setLevel(int level) {
        this.level = Math.max(1, level);
        recalculateDerivedAttributes(); // Level change can affect max health/mana
    }
    public void setExperience(long experience) { this.experience = Math.max(0, experience); }
    public void setCurrency(long currency) { this.currency = Math.max(0, currency); }

    public void setCoreStats(Map<Stat, Double> coreStats) {
        this.coreStats = new EnumMap<>(Objects.requireNonNull(coreStats));
        recalculateDerivedAttributes();
    }
    public void setStatValue(Stat stat, double value) {
        this.coreStats.put(Objects.requireNonNull(stat), Math.max(0, value)); // Modifies BASE stat
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
        // Max Health Calculation
        this.maxHealth = (long) (BASE_HEALTH +
                                (getStatValue(Stat.VITALITY) * HEALTH_PER_VITALITY) +
                                (this.level * HEALTH_PER_LEVEL));
        this.maxHealth = Math.max(1, this.maxHealth); // Ensure maxHealth is at least 1
        this.currentHealth = Math.min(this.currentHealth, this.maxHealth); // Clamp current health

        // Max Mana Calculation
        this.maxMana = (long) (BASE_MANA +
                               (getStatValue(Stat.WISDOM) * MANA_PER_WISDOM) +
                               (this.level * MANA_PER_LEVEL));
        this.maxMana = Math.max(0, this.maxMana); // Max mana can be 0
        this.currentMana = Math.min(this.currentMana, this.maxMana); // Clamp current mana

        // Critical Hit Chance Calculation
        double critChance = BASE_CRITICAL_HIT_CHANCE +
                            (getStatValue(Stat.AGILITY) * CRIT_CHANCE_PER_AGILITY) +
                            (getStatValue(Stat.LUCK) * CRIT_CHANCE_PER_LUCK);
        this.criticalHitChance = Math.max(0.0, Math.min(critChance, 1.0)); // Clamp between 0.0 (0%) and 1.0 (100%)

        // Critical Damage Bonus Calculation
        this.criticalDamageBonus = BASE_CRITICAL_DAMAGE_BONUS +
                                   (getStatValue(Stat.STRENGTH) * CRIT_DAMAGE_BONUS_PER_STRENGTH);
        this.criticalDamageBonus = Math.max(1.0, this.criticalDamageBonus); // Min 100% (no negative bonus)


        // Evasion Chance Calculation
        double evasion = BASE_EVASION_CHANCE +
                         (getStatValue(Stat.AGILITY) * EVASION_PER_AGILITY) +
                         (getStatValue(Stat.LUCK) * EVASION_PER_LUCK);
        this.evasionChance = Math.max(0.0, Math.min(evasion, 0.95)); // Clamp, max 95% evasion

        // Physical Damage Reduction (simple linear, capped)
        double physReduction = getStatValue(Stat.DEFENSE) * PHYS_REDUCTION_PER_DEFENSE;
        this.physicalDamageReduction = Math.max(0.0, Math.min(physReduction, MAX_PHYS_REDUCTION));

        // Magic Damage Reduction (simple linear, capped)
        double magicRed = getStatValue(Stat.WISDOM) * MAGIC_REDUCTION_PER_WISDOM; // Example: Wisdom for magic def
        this.magicDamageReduction = Math.max(0.0, Math.min(magicRed, MAX_MAGIC_REDUCTION));
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
