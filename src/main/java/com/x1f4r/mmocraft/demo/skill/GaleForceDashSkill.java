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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Movement burst used by the Windrunner Boots.
 */
public class GaleForceDashSkill extends Skill {

    public static final String SKILL_ID = "gale_force_dash";
    public static final String DISPLAY_NAME = "Gale Force Dash";
    public static final String DESCRIPTION = "Dash forward on a gust of wind gaining extreme speed.";
    public static final double MANA_COST = 60.0;
    public static final double COOLDOWN_SECONDS = 15.0;
    private static final int DURATION_SECONDS = 6;
    private static final double DASH_DISTANCE = 7.0;
    private static final double COLLISION_RADIUS = 1.5;

    public GaleForceDashSkill(MMOCraftPlugin plugin) {
        super(plugin, SKILL_ID, DISPLAY_NAME, DESCRIPTION, MANA_COST, COOLDOWN_SECONDS, 0.0, SkillType.ACTIVE_SELF);
    }

    @Override
    public void execute(PlayerProfile casterProfile, Entity targetEntity, Location targetLocation) {
        Player player = Bukkit.getPlayer(casterProfile.getPlayerUUID());
        if (player == null) {
            return;
        }

        Location start = player.getLocation();
        Vector direction = start.getDirection().setY(0).normalize();
        if (direction.lengthSquared() < 0.0001) {
            direction = new Vector(0, 0, 1);
        }

        Vector dashVelocity = direction.clone().multiply(1.6).setY(0.25);
        player.setVelocity(dashVelocity);

        int ticks = DURATION_SECONDS * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticks, 2, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, ticks, 1, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0, false, false, true));

        spawnDashParticles(player, start, direction);
        handleDashCollisions(player, casterProfile, start, direction);

        player.getWorld().playSound(start, Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.2f);
        player.getWorld().playSound(start, Sound.ITEM_TRIDENT_RIPTIDE_2, 0.8f, 1.3f);

        player.sendMessage(StringUtil.colorize("&bWind surges around you, accelerating your stride."));
        applyManaCost(casterProfile);
    }

    private void spawnDashParticles(Player player, Location start, Vector direction) {
        Location step = start.clone();
        for (int i = 0; i < 12; i++) {
            player.getWorld().spawnParticle(Particle.CLOUD, step, 6, 0.2, 0.05, 0.2, 0.01);
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, step, 1, 0.1, 0.05, 0.1, 0.0);
            step.add(direction.clone().multiply(DASH_DISTANCE / 12.0));
        }
    }

    private void handleDashCollisions(Player player, PlayerProfile profile, Location start, Vector direction) {
        DamageCalculationService damageService = plugin.getDamageCalculationService();
        Location end = start.clone().add(direction.clone().multiply(DASH_DISTANCE));
        double baseDamage = 28.0
                + profile.getStatValue(Stat.SPEED) * 0.35
                + profile.getStatValue(Stat.STRENGTH) * 0.25;

        for (Entity entity : player.getWorld().getNearbyEntities(start, DASH_DISTANCE, 2.5, DASH_DISTANCE)) {
            if (!(entity instanceof LivingEntity living) || living.equals(player)) {
                continue;
            }
            if (!isWithinDashPath(living.getLocation(), start, direction, DASH_DISTANCE)) {
                continue;
            }

            double travel = living.getLocation().toVector().subtract(start.toVector()).dot(direction);
            double damageMultiplier = 0.8 + Math.min(1.0, travel / DASH_DISTANCE) * 0.4;
            double attackDamage = Math.max(0.0, baseDamage * damageMultiplier);

            if (damageService != null) {
                DamageInstance instance = damageService.calculateDamage(player, living, attackDamage, DamageType.PHYSICAL);
                if (instance.finalDamage() > 0) {
                    living.damage(instance.finalDamage(), player);
                }
            } else {
                living.damage(attackDamage, player);
            }

            Vector knockback = direction.clone().multiply(0.9).setY(0.4);
            living.setVelocity(living.getVelocity().add(knockback));
            living.getWorld().spawnParticle(Particle.CLOUD, living.getLocation(), 12, 0.3, 0.2, 0.3, 0.02);
        }

        player.getWorld().spawnParticle(Particle.END_ROD, end, 12, 0.4, 0.2, 0.4, 0.01);
    }

    private boolean isWithinDashPath(Location entityLocation, Location start, Vector direction, double dashLength) {
        Vector relative = entityLocation.toVector().subtract(start.toVector());
        double projection = relative.dot(direction);
        if (projection < 0 || projection > dashLength) {
            return false;
        }
        Vector closestPoint = start.toVector().add(direction.clone().multiply(projection));
        double lateralDistance = entityLocation.toVector().distance(closestPoint);
        return lateralDistance <= COLLISION_RADIUS;
    }
}
