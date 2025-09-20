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
 * Movement burst used by the Windrunner Boots.
 */
public class GaleForceDashSkill extends Skill {

    public static final String SKILL_ID = "gale_force_dash";
    public static final String DISPLAY_NAME = "Gale Force Dash";
    public static final String DESCRIPTION = "Dash forward on a gust of wind gaining extreme speed.";
    public static final double MANA_COST = 60.0;
    public static final double COOLDOWN_SECONDS = 15.0;
    private static final int DURATION_SECONDS = 6;

    public GaleForceDashSkill(MMOCraftPlugin plugin) {
        super(plugin, SKILL_ID, DISPLAY_NAME, DESCRIPTION, MANA_COST, COOLDOWN_SECONDS, 0.0, SkillType.ACTIVE_SELF);
    }

    @Override
    public void execute(PlayerProfile casterProfile, Entity targetEntity, Location targetLocation) {
        Player player = Bukkit.getPlayer(casterProfile.getPlayerUUID());
        if (player == null) {
            return;
        }

        int ticks = DURATION_SECONDS * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticks, 2, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, ticks, 1, false, false, true));

        Location location = player.getLocation();
        player.getWorld().spawnParticle(Particle.CLOUD, location, 40, 0.6, 0.2, 0.6, 0.02);
        player.getWorld().playSound(location, Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.2f);

        player.sendMessage(StringUtil.colorize("&bWind surges around you, accelerating your stride."));
        applyManaCost(casterProfile);
    }
}
