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
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * A self-cast heal used to demonstrate supportive abilities.
 */
public class MinorHealSkill extends Skill {

    private static final double BASE_HEAL_AMOUNT = 10.0;
    private static final double INTELLIGENCE_SCALING_FACTOR = 0.8;

    public MinorHealSkill(MMOCraftPlugin plugin) {
        super(plugin,
                "minor_heal",
                "Minor Heal",
                "Heals yourself for a small amount based on Intelligence and Ability Power.",
                15.0,
                8.0,
                0.5,
                SkillType.ACTIVE_SELF);
    }

    @Override
    public boolean canUse(PlayerProfile casterProfile) {
        return super.canUse(casterProfile);
    }

    @Override
    public void execute(PlayerProfile casterProfile, Entity target, Location targetLocation) {
        LoggingUtil logger = plugin.getLoggingUtil();
        Player casterPlayer = Bukkit.getPlayer(casterProfile.getPlayerUUID());
        if (casterPlayer == null) {
            logger.warning("MinorHealSkill: caster player not found for UUID " + casterProfile.getPlayerUUID());
            return;
        }

        double abilityPowerMultiplier = 1.0 + (casterProfile.getStatValue(Stat.ABILITY_POWER) / 100.0);
        double healAmount = Math.max(0, (BASE_HEAL_AMOUNT +
                (casterProfile.getStatValue(Stat.INTELLIGENCE) * INTELLIGENCE_SCALING_FACTOR)) * abilityPowerMultiplier);
        long previousHealth = casterProfile.getCurrentHealth();
        long newHealth = Math.min(casterProfile.getMaxHealth(), previousHealth + Math.round(healAmount));
        casterProfile.setCurrentHealth(newHealth);
        long actualHeal = newHealth - previousHealth;

        AttributeInstance maxHealthAttribute = casterPlayer.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            double maxBukkitHealth = maxHealthAttribute.getValue();
            double appliedHealth = Math.min(maxBukkitHealth, casterPlayer.getHealth() + actualHeal);
            casterPlayer.setHealth(appliedHealth);
        }

        casterPlayer.sendMessage(StringUtil.colorize("&aYou cast Minor Heal and restored &f" + actualHeal + "&a health."));
        logger.info(casterPlayer.getName() + " used Minor Heal, restoring " + actualHeal + " health.");

        casterPlayer.getWorld().playSound(casterPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
        applyManaCost(casterProfile);
    }
}
