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
 * Farming rally ability for the Harvester's Scythe.
 */
public class HarvestRallySkill extends Skill {

    public static final String SKILL_ID = "harvest_rally";
    public static final String DISPLAY_NAME = "Harvest Rally";
    public static final String DESCRIPTION = "Bolster your farming speed and nourishment.";
    public static final double MANA_COST = 45.0;
    public static final double COOLDOWN_SECONDS = 20.0;
    private static final int DURATION_SECONDS = 10;

    public HarvestRallySkill(MMOCraftPlugin plugin) {
        super(plugin, SKILL_ID, DISPLAY_NAME, DESCRIPTION, MANA_COST, COOLDOWN_SECONDS, 0.0, SkillType.ACTIVE_SELF);
    }

    @Override
    public void execute(PlayerProfile casterProfile, Entity targetEntity, Location targetLocation) {
        Player player = Bukkit.getPlayer(casterProfile.getPlayerUUID());
        if (player == null) {
            return;
        }

        int ticks = DURATION_SECONDS * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, ticks, 0, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticks, 1, false, false, true));

        Location location = player.getLocation();
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, location, 45, 0.6, 0.5, 0.6, 0.05);
        player.getWorld().playSound(location, Sound.BLOCK_CROP_BREAK, 1.0f, 1.1f);

        player.sendMessage(StringUtil.colorize("&aYou feel invigorated to reap every crop."));
        applyManaCost(casterProfile);
    }
}
