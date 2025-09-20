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
 * Temporary mining buff tied to the Prospector's Drill.
 */
public class ProspectorPulseSkill extends Skill {

    public static final String SKILL_ID = "prospector_pulse";
    public static final String DISPLAY_NAME = "Prospector Pulse";
    public static final String DESCRIPTION = "Charges your drill with haste and fortune.";
    public static final double MANA_COST = 50.0;
    public static final double COOLDOWN_SECONDS = 24.0;
    private static final int DURATION_SECONDS = 10;

    public ProspectorPulseSkill(MMOCraftPlugin plugin) {
        super(plugin, SKILL_ID, DISPLAY_NAME, DESCRIPTION, MANA_COST, COOLDOWN_SECONDS, 0.0, SkillType.ACTIVE_SELF);
    }

    @Override
    public void execute(PlayerProfile casterProfile, Entity targetEntity, Location targetLocation) {
        Player player = Bukkit.getPlayer(casterProfile.getPlayerUUID());
        if (player == null) {
            return;
        }

        int ticks = DURATION_SECONDS * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, ticks, 2, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, ticks, 1, false, false, true));

        Location location = player.getLocation();
        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, location, 60, 0.6, 0.4, 0.6, 0.1);
        player.getWorld().playSound(location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);

        player.sendMessage(StringUtil.colorize("&eYour drill hums with prospecting energy."));
        applyManaCost(casterProfile);
    }
}
