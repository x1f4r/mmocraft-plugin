package com.x1f4r.mmocraft.demo.skill;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.combat.model.DamageInstance;
import com.x1f4r.mmocraft.combat.model.DamageType;
import com.x1f4r.mmocraft.combat.service.DamageCalculationService;
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
import org.bukkit.util.Vector;

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
    private static final double EFFECT_RADIUS = 6.5;
    private static final int FIRE_TICKS = 120;
    private static final double CONE_DOT_THRESHOLD = 0.55; // ~56 degree cone

    public InfernoBurstSkill(MMOCraftPlugin plugin) {
        super(plugin, SKILL_ID, DISPLAY_NAME, DESCRIPTION, MANA_COST, COOLDOWN_SECONDS, 0.0, SkillType.ACTIVE_AOE_POINT);
    }

    @Override
    public void execute(PlayerProfile casterProfile, Entity targetEntity, Location targetLocation) {
        Player caster = Bukkit.getPlayer(casterProfile.getPlayerUUID());
        if (caster == null) {
            return;
        }

        Location origin = caster.getLocation();
        Vector facing = origin.getDirection().normalize();
        Location castCenter = origin.clone().add(facing.clone().multiply(2));
        Location center = targetLocation != null ? targetLocation.clone() : castCenter;
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        DamageCalculationService damageService = plugin.getDamageCalculationService();
        double baseDamage = BASE_DAMAGE
                + casterProfile.getStatValue(Stat.INTELLIGENCE) * INT_SCALING
                + casterProfile.getStatValue(Stat.ABILITY_POWER) * ABILITY_POWER_SCALING;
        baseDamage = Math.max(0.0, baseDamage);

        for (Entity entity : world.getNearbyEntities(center, EFFECT_RADIUS, EFFECT_RADIUS, EFFECT_RADIUS)) {
            if (!(entity instanceof LivingEntity living) || entity.equals(caster)) {
                continue;
            }
            Vector toEntity = living.getLocation().toVector().subtract(origin.toVector());
            double distance = toEntity.length();
            if (distance == 0) {
                distance = 0.001;
            }
            Vector direction = toEntity.clone().normalize();
            if (direction.dot(facing) < CONE_DOT_THRESHOLD) {
                continue; // Outside cone
            }

            double distanceMultiplier = 1.0 - Math.min(1.0, distance / EFFECT_RADIUS) * 0.5;
            double finalBaseDamage = baseDamage * (0.75 + distanceMultiplier);

            if (damageService != null) {
                DamageInstance instance = damageService.calculateDamage(caster, living, finalBaseDamage, DamageType.MAGICAL);
                if (instance.finalDamage() > 0) {
                    living.damage(instance.finalDamage(), caster);
                }
            } else {
                living.damage(finalBaseDamage, caster);
            }
            living.setFireTicks(FIRE_TICKS);
        }

        world.spawnParticle(Particle.FLAME, origin, 40, 0.3, 0.1, 0.3, 0.04);
        world.spawnParticle(Particle.FLAME, center, 90, 1.6, 0.6, 1.6, 0.05);
        world.spawnParticle(Particle.SMALL_FLAME, center, 40, 1.4, 0.4, 1.4, 0.03);
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, 25, 1.2, 0.3, 1.2, 0.02);
        world.playSound(center, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.7f);
        world.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 1.4f);

        caster.sendMessage(StringUtil.colorize("&6You unleash an inferno of flames!"));
        applyManaCost(casterProfile);
    }
}
