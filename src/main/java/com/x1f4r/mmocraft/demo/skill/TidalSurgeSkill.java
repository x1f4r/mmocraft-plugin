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
 * Fishing-focused ability for the Angler's Tidal Rod.
 */
public class TidalSurgeSkill extends Skill {

    public static final String SKILL_ID = "tidal_surge";
    public static final String DISPLAY_NAME = "Tidal Surge";
    public static final String DESCRIPTION = "Ride a watery current, improving aquatic treasure chances.";
    public static final double MANA_COST = 65.0;
    public static final double COOLDOWN_SECONDS = 22.0;
    private static final int DURATION_SECONDS = 8;

    public TidalSurgeSkill(MMOCraftPlugin plugin) {
        super(plugin, SKILL_ID, DISPLAY_NAME, DESCRIPTION, MANA_COST, COOLDOWN_SECONDS, 0.0, SkillType.ACTIVE_SELF);
    }

    @Override
    public void execute(PlayerProfile casterProfile, Entity targetEntity, Location targetLocation) {
        Player player = Bukkit.getPlayer(casterProfile.getPlayerUUID());
        if (player == null) {
            return;
        }

        int ticks = DURATION_SECONDS * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, ticks, 0, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, ticks, 1, false, false, true));

        Location location = player.getLocation();
        player.getWorld().spawnParticle(Particle.SPLASH, location, 60, 0.8, 0.5, 0.8, 0.1);
        player.getWorld().playSound(location, Sound.ENTITY_DOLPHIN_PLAY, 1.0f, 1.0f);

        player.sendMessage(StringUtil.colorize("&3Currents propel you toward bountiful catches."));
        applyManaCost(casterProfile);
    }
}
