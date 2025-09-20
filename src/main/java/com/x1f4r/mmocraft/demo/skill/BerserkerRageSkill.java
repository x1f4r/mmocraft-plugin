package com.x1f4r.mmocraft.demo.skill;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.demo.statuseffect.BerserkerRageStatusEffect;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.skill.model.Skill;
import com.x1f4r.mmocraft.skill.model.SkillType;
import com.x1f4r.mmocraft.statuseffect.manager.StatusEffectManager;
import com.x1f4r.mmocraft.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Frenzy window used by the Berserker Gauntlet.
 */
public class BerserkerRageSkill extends Skill {

    public static final String SKILL_ID = "berserker_rage";
    public static final String DISPLAY_NAME = "Berserker Rage";
    public static final String DESCRIPTION = "Enter a blood frenzy gaining massive melee bonuses, cleave, and lifesteal.";
    public static final double MANA_COST = 80.0;
    public static final double COOLDOWN_SECONDS = 25.0;

    public BerserkerRageSkill(MMOCraftPlugin plugin) {
        super(plugin, SKILL_ID, DISPLAY_NAME, DESCRIPTION, MANA_COST, COOLDOWN_SECONDS, 0.0, SkillType.ACTIVE_SELF);
    }

    @Override
    public void execute(PlayerProfile casterProfile, Entity targetEntity, Location targetLocation) {
        Player player = Bukkit.getPlayer(casterProfile.getPlayerUUID());
        if (player == null) {
            return;
        }

        StatusEffectManager manager = plugin.getStatusEffectManager();
        if (manager == null) {
            player.sendMessage(StringUtil.colorize("&cStatus effects are unavailable right now."));
            return;
        }

        manager.applyEffect(player, new BerserkerRageStatusEffect(plugin, player.getUniqueId()));

        Location location = player.getLocation();
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, location, 1);
        player.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.6f);

        player.sendMessage(StringUtil.colorize("&4<< &cBloodlust surges through your veins! &4>>"));
        applyManaCost(casterProfile);
    }
}
