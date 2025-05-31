package com.x1f4r.mmocraft.playerdata.model;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents a player's persistent data within MMOCraft.
 * This includes core attributes, stats, progression, and metadata.
 */
public class PlayerProfile {

    private final UUID playerUUID;
    private String playerName; // Last known player name

    private long currentHealth;
    private long maxHealth;
    private long currentMana;
    private long maxMana;

    private int level;
    private long experience; // Experience towards next level
    private long currency;

    private Map<Stat, Double> coreStats;

    private LocalDateTime firstLogin;
    private LocalDateTime lastLogin;

    // Consider a field for total playtime, calculated on logout.
    // private long totalPlaytimeSeconds;

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

        // Default base stats
        this.maxHealth = 100; // Example base value
        this.currentHealth = this.maxHealth;
        this.maxMana = 50;   // Example base value
        this.currentMana = this.maxMana;

        this.coreStats = new EnumMap<>(Stat.class);
        // Initialize with base values for each stat
        for (Stat stat : Stat.values()) {
            this.coreStats.put(stat, 10.0); // Default starting value for all stats
        }
        // Adjust specific stats if needed, e.g., Vitality might start higher
        this.coreStats.put(Stat.VITALITY, 15.0);


        LocalDateTime now = LocalDateTime.now();
        this.firstLogin = now;
        this.lastLogin = now;
    }

    /**
     * Full constructor for loading a player's profile from persistence.
     */
    public PlayerProfile(UUID playerUUID, String playerName, long currentHealth, long maxHealth,
                         long currentMana, long maxMana, int level, long experience, long currency,
                         Map<Stat, Double> coreStats, LocalDateTime firstLogin, LocalDateTime lastLogin) {
        this.playerUUID = Objects.requireNonNull(playerUUID, "Player UUID cannot be null.");
        this.playerName = Objects.requireNonNull(playerName, "Player name cannot be null.");
        this.currentHealth = currentHealth;
        this.maxHealth = maxHealth;
        this.currentMana = currentMana;
        this.maxMana = maxMana;
        this.level = level;
        this.experience = experience;
        this.currency = currency;
        this.coreStats = new EnumMap<>(Objects.requireNonNull(coreStats, "Core stats map cannot be null."));
        this.firstLogin = Objects.requireNonNull(firstLogin, "First login time cannot be null.");
        this.lastLogin = Objects.requireNonNull(lastLogin, "Last login time cannot be null.");
    }

    // Getters
    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public long getCurrentHealth() { return currentHealth; }
    public long getMaxHealth() { return maxHealth; }
    public long getCurrentMana() { return currentMana; }
    public long getMaxMana() { return maxMana; }
    public int getLevel() { return level; }
    public long getExperience() { return experience; }
    public long getCurrency() { return currency; }
    public Map<Stat, Double> getCoreStats() { return coreStats; } // Consider returning an unmodifiable map
    public Double getStatValue(Stat stat) { return coreStats.getOrDefault(stat, 0.0); }
    public LocalDateTime getFirstLogin() { return firstLogin; }
    public LocalDateTime getLastLogin() { return lastLogin; }

    // Setters
    public void setPlayerName(String playerName) { this.playerName = Objects.requireNonNull(playerName, "Player name cannot be null."); }
    public void setCurrentHealth(long currentHealth) { this.currentHealth = Math.max(0, Math.min(currentHealth, this.maxHealth)); }
    public void setMaxHealth(long maxHealth) { this.maxHealth = Math.max(1, maxHealth); if(this.currentHealth > this.maxHealth) this.currentHealth = this.maxHealth; }
    public void setCurrentMana(long currentMana) { this.currentMana = Math.max(0, Math.min(currentMana, this.maxMana)); }
    public void setMaxMana(long maxMana) { this.maxMana = Math.max(0, maxMana); if(this.currentMana > this.maxMana) this.currentMana = this.maxMana; }
    public void setLevel(int level) { this.level = Math.max(1, level); }
    public void setExperience(long experience) { this.experience = Math.max(0, experience); }
    public void setCurrency(long currency) { this.currency = Math.max(0, currency); }
    public void setCoreStats(Map<Stat, Double> coreStats) { this.coreStats = new EnumMap<>(Objects.requireNonNull(coreStats)); }
    public void setStatValue(Stat stat, double value) { this.coreStats.put(Objects.requireNonNull(stat), Math.max(0, value)); }
    // No setter for firstLogin as it should be immutable after creation
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = Objects.requireNonNull(lastLogin); }


    // Business logic examples (can be expanded into separate services/managers)
    public void addExperience(long amount) {
        if (amount <= 0) return;
        this.experience += amount;
        // TODO: Add logic for leveling up based on experience thresholds
        // e.g., while (this.experience >= getExperienceToNextLevel(this.level)) { levelUp(); }
    }

    public void takeDamage(long amount) {
        if (amount <= 0) return;
        setCurrentHealth(this.currentHealth - amount);
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
