package com.x1f4r.mmocraft.skill.model;

import com.x1f4r.mmocraft.core.MMOCraftPlugin; // Added for service access
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Objects;

/**
 * Abstract base class for all skills in MMOCraft.
 * Defines common properties and behaviors for skills.
 */
public abstract class Skill {

    protected final String skillId;
    protected final String skillName;
    protected final String description;
    protected final double manaCost;
    protected final double cooldownSeconds;
    protected final double castTimeSeconds;
    protected final SkillType skillType;
    protected final transient MMOCraftPlugin plugin; // Added for service access, transient to avoid serialization issues if skill itself is serialized

    /**
     * Constructs a new Skill.
     *
     * @param plugin The main plugin instance, used to access core services.
     * @param skillId Unique identifier for the skill (e.g., "fireball").
     * @param skillName Display name of the skill (e.g., "Fireball").
     * @param description A brief description of what the skill does.
     * @param manaCost The amount of mana required to use this skill.
     * @param cooldownSeconds The cooldown period in seconds after using the skill.
     * @param castTimeSeconds The time in seconds it takes to cast the skill (0 for instant).
     * @param skillType The {@link SkillType} categorizing this skill.
     */
    public Skill(MMOCraftPlugin plugin, String skillId, String skillName, String description,
                 double manaCost, double cooldownSeconds, double castTimeSeconds, SkillType skillType) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.skillId = Objects.requireNonNull(skillId, "skillId cannot be null");
        this.skillName = Objects.requireNonNull(skillName, "skillName cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");

        if (manaCost < 0) throw new IllegalArgumentException("Mana cost cannot be negative.");
        this.manaCost = manaCost;

        if (cooldownSeconds < 0) throw new IllegalArgumentException("Cooldown seconds cannot be negative.");
        this.cooldownSeconds = cooldownSeconds;

        if (castTimeSeconds < 0) throw new IllegalArgumentException("Cast time seconds cannot be negative.");
        this.castTimeSeconds = castTimeSeconds;

        this.skillType = Objects.requireNonNull(skillType, "skillType cannot be null");
    }

    // Getters
    public String getSkillId() { return skillId; }
    public String getSkillName() { return skillName; }
    public String getDescription() { return description; }
    public double getManaCost() { return manaCost; }
    public double getCooldownSeconds() { return cooldownSeconds; }
    public double getCastTimeSeconds() { return castTimeSeconds; }
    public SkillType getSkillType() { return skillType; }

    /**
     * Checks if the caster can currently use this skill.
     * This base implementation checks for mana cost and cooldown.
     * Concrete skills can override to add more specific conditions (e.g., target requirements, caster state).
     *
     * @param casterProfile The profile of the player attempting to use the skill.
     * @return True if the skill can be used, false otherwise.
     */
    public boolean canUse(PlayerProfile casterProfile) {
        if (casterProfile.getCurrentMana() < this.manaCost) {
            // Optionally send message to player: casterProfile.getPlayer().sendMessage("Not enough mana!");
            return false;
        }
        if (casterProfile.isSkillOnCooldown(this.skillId)) {
            // Optionally send message: casterProfile.getPlayer().sendMessage(this.skillName + " is on cooldown!");
            return false;
        }
        return true;
    }

    /**
     * Executes the core logic of the skill.
     * This method is called when the skill is successfully cast.
     *
     * @param casterProfile The profile of the player casting the skill.
     * @param targetEntity The primary entity target, if applicable (for {@link SkillType#ACTIVE_TARGETED_ENTITY}). Can be null.
     * @param targetLocation The target location, if applicable (for {@link SkillType#ACTIVE_AOE_POINT}). Can be null.
     *                       (Note: Parameter list might need to be more flexible, e.g., using a SkillContext object)
     */
    public abstract void execute(PlayerProfile casterProfile, Entity targetEntity, Location targetLocation);

    /**
     * Puts the skill on cooldown for the specified caster.
     * This should be called after the skill is successfully used or its casting is confirmed.
     *
     * @param casterProfile The profile of the player who used the skill.
     */
    public void onCooldown(PlayerProfile casterProfile) {
        if (this.cooldownSeconds > 0) {
            casterProfile.setSkillCooldown(this.skillId, this.cooldownSeconds);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false; // Or !(o instanceof Skill) if interface
        Skill skill = (Skill) o;
        return skillId.equals(skill.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skillId);
    }

    @Override
    public String toString() {
        return "Skill{" +
               "skillId='" + skillId + '\'' +
               ", skillName='" + skillName + '\'' +
               ", type=" + skillType +
               ", manaCost=" + manaCost +
               ", cooldown=" + cooldownSeconds +
               ", castTime=" + castTimeSeconds +
               '}';
    }
}
