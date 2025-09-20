package com.x1f4r.mmocraft.demo.skill;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.skill.model.Skill;
import com.x1f4r.mmocraft.skill.model.SkillType;
import com.x1f4r.mmocraft.util.StringUtil;
import com.x1f4r.mmocraft.world.resourcegathering.model.ActiveResourceNode;
import com.x1f4r.mmocraft.world.resourcegathering.service.ActiveNodeManager;
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
    private static final double NODE_REVEAL_RADIUS = 18.0;

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
        player.getWorld().spawnParticle(Particle.GLOW, location, 30, 0.4, 0.2, 0.4, 0.02);
        player.getWorld().playSound(location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);

        highlightNearbyNodes(player, location);

        player.sendMessage(StringUtil.colorize("&eYour drill hums with prospecting energy."));
        applyManaCost(casterProfile);
    }

    private void highlightNearbyNodes(Player player, Location origin) {
        ActiveNodeManager nodeManager = plugin.getActiveNodeManager();
        if (nodeManager == null) {
            return;
        }

        int highlighted = 0;
        for (ActiveResourceNode node : nodeManager.getAllActiveNodesView().values()) {
            if (node.isDepleted() || node.getLocation().getWorld() == null) {
                continue;
            }
            if (!node.getLocation().getWorld().equals(origin.getWorld())) {
                continue;
            }
            if (node.getLocation().distanceSquared(origin) > NODE_REVEAL_RADIUS * NODE_REVEAL_RADIUS) {
                continue;
            }

            Location nodeLocation = node.getLocation().clone().add(0.5, 0.5, 0.5);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, nodeLocation, 12, 0.25, 0.35, 0.25, 0.05);
            player.getWorld().spawnParticle(Particle.CRIT, nodeLocation, 8, 0.2, 0.25, 0.2, 0.02);
            player.getWorld().playSound(nodeLocation, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.6f, 1.5f);
            highlighted++;
        }

        if (highlighted > 0) {
            player.sendMessage(StringUtil.colorize("&aProspector pulse reveals &e" + highlighted + " &aminable nodes nearby."));
        } else {
            player.sendMessage(StringUtil.colorize("&7No rich veins resonate with your pulse."));
        }
    }
}
