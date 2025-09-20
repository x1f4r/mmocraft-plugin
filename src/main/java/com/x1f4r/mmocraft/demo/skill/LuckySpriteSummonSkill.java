package com.x1f4r.mmocraft.demo.skill;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.demo.pet.DemoCompanionPets;
import com.x1f4r.mmocraft.pet.model.ActiveCompanionPet;
import com.x1f4r.mmocraft.pet.model.CompanionPetDefinition;
import com.x1f4r.mmocraft.pet.service.CompanionPetService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.skill.model.Skill;
import com.x1f4r.mmocraft.skill.model.SkillType;
import com.x1f4r.mmocraft.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Toggle skill that summons or dismisses the Lucky Sprite companion.
 */
public class LuckySpriteSummonSkill extends Skill {

    public static final String SKILL_ID = "lucky_sprite_summon";
    public static final String DISPLAY_NAME = "Lucky Sprite";
    public static final String DESCRIPTION = "Summon a sprite companion that grants pet luck and looting bonuses.";
    public static final double MANA_COST = 45.0;
    public static final double COOLDOWN_SECONDS = 12.0;

    private final CompanionPetDefinition companionDefinition = DemoCompanionPets.luckySprite();

    public LuckySpriteSummonSkill(MMOCraftPlugin plugin) {
        super(plugin, SKILL_ID, DISPLAY_NAME, DESCRIPTION, MANA_COST, COOLDOWN_SECONDS, 0.0, SkillType.ACTIVE_SELF);
    }

    @Override
    public boolean canUse(PlayerProfile casterProfile) {
        CompanionPetService service = plugin.getCompanionPetService();
        if (service != null) {
            Optional<ActiveCompanionPet> active = service.getActivePet(casterProfile.getPlayerUUID());
            if (active.isPresent() && DemoCompanionPets.LUCKY_SPRITE_ID.equals(active.get().definition().id())) {
                return true; // Always allow dismissal even if on cooldown or out of mana
            }
        }
        return super.canUse(casterProfile);
    }

    @Override
    public void execute(PlayerProfile casterProfile, Entity targetEntity, Location targetLocation) {
        Player player = Bukkit.getPlayer(casterProfile.getPlayerUUID());
        if (player == null) {
            return;
        }
        CompanionPetService service = plugin.getCompanionPetService();
        if (service == null) {
            player.sendMessage(StringUtil.colorize("&cCompanion service unavailable."));
            return;
        }

        UUID playerId = player.getUniqueId();
        Optional<ActiveCompanionPet> active = service.getActivePet(playerId);
        if (active.isPresent() && DemoCompanionPets.LUCKY_SPRITE_ID.equals(active.get().definition().id())) {
            service.dismissPet(player);
            try {
                playSoundSafely(player.getWorld(), player.getLocation(), Sound.ENTITY_ALLAY_ITEM_TAKEN, 0.7f, 1.4f);
            } catch (ExceptionInInitializerError | NoClassDefFoundError ignored) {
            }
            player.sendMessage(StringUtil.colorize("&dYour Lucky Sprite twirls away."));
            // Clear cooldown on next tick so players can resummon once ready.
            plugin.getServer().getScheduler().runTask(plugin, () -> casterProfile.setSkillCooldown(getSkillId(), 0));
            return;
        }

        long spentMana = applyManaCost(casterProfile);
        if (spentMana <= 0 && MANA_COST > 0) {
            // Mana was not consumed; likely due to configuration quirks. Abort to avoid free casts.
            player.sendMessage(StringUtil.colorize("&cYou failed to channel the sprite's essence."));
            return;
        }

        service.summonPet(player, companionDefinition);
        Location location = player.getLocation().add(0, 1.1, 0);
        World world = player.getWorld();
        world.spawnParticle(Particle.END_ROD, location, 30, 0.4, 0.4, 0.4, 0.01);
        try {
            playSoundSafely(world, location, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 0.9f, 1.5f);
        } catch (ExceptionInInitializerError | NoClassDefFoundError ignored) {
        }
        player.sendMessage(StringUtil.colorize("&dA Lucky Sprite pledges to follow you."));
    }

    private void playSoundSafely(World world, Location location, Sound sound, float volume, float pitch) {
        try {
            world.playSound(location, sound, volume, pitch);
        } catch (ExceptionInInitializerError | NoClassDefFoundError ignored) {
            // Environment without a fully initialised registry (e.g. unit tests).
        }
    }
}

