package com.x1f4r.mmocraft.demo.skill;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.skill.model.Skill;
import com.x1f4r.mmocraft.skill.model.SkillType;
import com.x1f4r.mmocraft.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Frenzy window used by the Berserker Gauntlet.
 */
public class BerserkerRageSkill extends Skill {

    public static final String SKILL_ID = "berserker_rage";
    public static final String DISPLAY_NAME = "Berserker Rage";
    public static final String DESCRIPTION = "Enter a frenzied state increasing damage and swing speed.";
    public static final double MANA_COST = 80.0;
    public static final double COOLDOWN_SECONDS = 25.0;
    private static final int DURATION_SECONDS = 8;

    public BerserkerRageSkill(MMOCraftPlugin plugin) {
        super(plugin, SKILL_ID, DISPLAY_NAME, DESCRIPTION, MANA_COST, COOLDOWN_SECONDS, 0.0, SkillType.ACTIVE_SELF);
    }

    @Override
    public void execute(PlayerProfile casterProfile, Entity targetEntity, Location targetLocation) {
        Player player = Bukkit.getPlayer(casterProfile.getPlayerUUID());
        if (player == null) {
            return;
        }

        int ticks = DURATION_SECONDS * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ticks, 1, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, ticks, 1, false, false, true));

        Location location = player.getLocation();
        player.getWorld().spawnParticle(Particle.CRIT, location, 50, 0.8, 0.4, 0.8, 0.05);
        player.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);

        player.sendMessage(StringUtil.colorize("&cBloodlust surges through your veins!"));
        applyManaCost(casterProfile);
    }
}
