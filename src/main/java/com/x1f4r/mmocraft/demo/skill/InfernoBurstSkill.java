package com.x1f4r.mmocraft.demo.skill;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.skill.model.Skill;
import com.x1f4r.mmocraft.skill.model.SkillType;
import com.x1f4r.mmocraft.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * High impact AOE ability used by the Blazing Ember Rod.
 */
public class InfernoBurstSkill extends Skill {

    public static final String SKILL_ID = "inferno_burst";
    public static final String DISPLAY_NAME = "Inferno Burst";
    public static final String DESCRIPTION = "Ignites enemies in front of you with ability-scaled flame damage.";
    public static final double MANA_COST = 120.0;
    public static final double COOLDOWN_SECONDS = 20.0;
    private static final double BASE_DAMAGE = 60.0;
    private static final double INT_SCALING = 0.6;
    private static final double ABILITY_POWER_SCALING = 0.9;
    private static final double EFFECT_RADIUS = 5.0;
    private static final int FIRE_TICKS = 100;

    public InfernoBurstSkill(MMOCraftPlugin plugin) {
        super(plugin, SKILL_ID, DISPLAY_NAME, DESCRIPTION, MANA_COST, COOLDOWN_SECONDS, 0.0, SkillType.ACTIVE_AOE_POINT);
    }

    @Override
    public void execute(PlayerProfile casterProfile, Entity targetEntity, Location targetLocation) {
        Player caster = Bukkit.getPlayer(casterProfile.getPlayerUUID());
        if (caster == null) {
            return;
        }

        Location center = targetLocation != null ? targetLocation.clone() : caster.getLocation();
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        double damage = BASE_DAMAGE
                + casterProfile.getStatValue(Stat.INTELLIGENCE) * INT_SCALING
                + casterProfile.getStatValue(Stat.ABILITY_POWER) * ABILITY_POWER_SCALING;
        damage = Math.max(0.0, damage);

        for (Entity entity : world.getNearbyEntities(center, EFFECT_RADIUS, EFFECT_RADIUS, EFFECT_RADIUS)) {
            if (!(entity instanceof LivingEntity living) || entity.equals(caster)) {
                continue;
            }
            living.damage(damage, caster);
            living.setFireTicks(FIRE_TICKS);
        }

        world.spawnParticle(Particle.FLAME, center, 80, 1.25, 0.5, 1.25, 0.05);
        world.spawnParticle(Particle.LARGE_SMOKE, center, 40, 1.25, 0.5, 1.25, 0.01);
        world.playSound(center, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.75f);

        caster.sendMessage(StringUtil.colorize("&6You unleash an inferno of flames!"));
        applyManaCost(casterProfile);
    }
}
