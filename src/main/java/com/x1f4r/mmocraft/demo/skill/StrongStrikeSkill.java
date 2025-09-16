package com.x1f4r.mmocraft.demo.skill;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.skill.model.Skill;
import com.x1f4r.mmocraft.skill.model.SkillType;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * A targeted melee skill used to demonstrate custom combat calculations.
 */
public class StrongStrikeSkill extends Skill {

    private static final double BASE_SKILL_DAMAGE = 5.0;
    private static final double STRENGTH_SCALING_FACTOR = 1.2;
    private static final double DAMAGE_MULTIPLIER = 1.5;

    public StrongStrikeSkill(MMOCraftPlugin plugin) {
        super(plugin,
                "strong_strike",
                "Strong Strike",
                "A powerful blow that deals " + DAMAGE_MULTIPLIER + "x damage based on Strength.",
                10.0,
                5.0,
                0.0,
                SkillType.ACTIVE_TARGETED_ENTITY);
    }

    @Override
    public boolean canUse(PlayerProfile casterProfile) {
        if (!super.canUse(casterProfile)) {
            return false;
        }
        Player player = Bukkit.getPlayer(casterProfile.getPlayerUUID());
        if (player == null) {
            return false;
        }
        if (player.getInventory().getItemInMainHand().getType().isAir()) {
            player.sendMessage(StringUtil.colorize("&eYou strike with empty hands. Damage will be reduced."));
        }
        return true;
    }

    @Override
    public void execute(PlayerProfile casterProfile, Entity target, Location targetLocation) {
        LoggingUtil logger = plugin.getLoggingUtil();
        Player casterPlayer = Bukkit.getPlayer(casterProfile.getPlayerUUID());
        if (casterPlayer == null) {
            logger.warning("StrongStrikeSkill: caster player not found for UUID " + casterProfile.getPlayerUUID());
            return;
        }
        if (!(target instanceof LivingEntity livingTarget)) {
            casterPlayer.sendMessage(StringUtil.colorize("&cStrong Strike requires a living target."));
            return;
        }
        double skillDamageContribution = BASE_SKILL_DAMAGE + (casterProfile.getStatValue(Stat.STRENGTH) * STRENGTH_SCALING_FACTOR);
        double totalDamagePotential = Math.max(0, skillDamageContribution * DAMAGE_MULTIPLIER);

        boolean isCrit = Math.random() < casterProfile.getCriticalHitChance();
        if (isCrit) {
            totalDamagePotential *= casterProfile.getCriticalDamageBonus();
        }

        PlayerProfile victimProfile = livingTarget instanceof Player playerVictim
                ? plugin.getPlayerDataService().getPlayerProfile(playerVictim.getUniqueId())
                : null;

        if (victimProfile != null && Math.random() < victimProfile.getEvasionChance()) {
            casterPlayer.sendMessage(StringUtil.colorize("&eYour Strong Strike was evaded by " + livingTarget.getName() + "!"));
            if (livingTarget instanceof Player) {
                ((Player) livingTarget).sendMessage(StringUtil.colorize("&aYou evaded a Strong Strike from " + casterPlayer.getName() + "!"));
            }
            livingTarget.getWorld().playSound(livingTarget.getLocation(), Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.0f, 1.0f);
            return;
        }

        double finalDamage = totalDamagePotential;
        if (victimProfile != null) {
            finalDamage *= Math.max(0, 1 - victimProfile.getPhysicalDamageReduction());
        }

        if (finalDamage <= 0) {
            casterPlayer.sendMessage(StringUtil.colorize("&eYour target shrugs off the blow."));
            return;
        }

        livingTarget.damage(finalDamage, casterPlayer);
        String critTag = isCrit ? " &c(Critical!)" : "";
        casterPlayer.sendMessage(StringUtil.colorize("&aYou hit " + livingTarget.getName() + " for &f" + String.format("%.2f", finalDamage) + "&a damage!" + critTag));
        logger.info(casterPlayer.getName() + " used Strong Strike on " + livingTarget.getName() + " for " + String.format("%.2f", finalDamage) + " damage." + (isCrit ? " (CRIT)" : ""));

        casterPlayer.getWorld().playSound(casterPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
        livingTarget.getWorld().playSound(livingTarget.getLocation(), Sound.ENTITY_GENERIC_HURT, 1.0f, 1.0f);

        casterProfile.setCurrentMana(casterProfile.getCurrentMana() - (long) getManaCost());
    }
}
