package com.x1f4r.mmocraft.item.listeners;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.item.api.NBTUtil;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.skill.model.Skill;
import com.x1f4r.mmocraft.skill.model.SkillType;
import com.x1f4r.mmocraft.skill.service.SkillRegistryService;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.util.StringUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Optional;

/**
 * Allows custom items to trigger their associated skills when used.
 */
public class CustomItemAbilityListener implements Listener {

    private final MMOCraftPlugin plugin;
    private final SkillRegistryService skillRegistryService;
    private final PlayerDataService playerDataService;
    private final LoggingUtil logger;

    public CustomItemAbilityListener(MMOCraftPlugin plugin) {
        this.plugin = plugin;
        this.skillRegistryService = plugin.getSkillRegistryService();
        this.playerDataService = plugin.getPlayerDataService();
        this.logger = plugin.getLoggingUtil();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        Location clickedLocation = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : null;
        if (!triggerAbility(event.getPlayer(), item, null, clickedLocation)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            return;
        }
        if (triggerAbility(event.getPlayer(), item, event.getRightClicked(), event.getRightClicked().getLocation())) {
            event.setCancelled(true);
        }
    }

    private boolean triggerAbility(Player player, ItemStack item, Entity explicitTarget, Location explicitLocation) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        String abilityIds = NBTUtil.getString(item, CustomItem.CUSTOM_ITEM_ABILITY_IDS_NBT_KEY, plugin);
        if (abilityIds == null || abilityIds.isBlank()) {
            return false;
        }
        PlayerProfile profile = playerDataService.getPlayerProfile(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(StringUtil.colorize("&cYour profile data is still loading."));
            return false;
        }
        return Arrays.stream(abilityIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .anyMatch(id -> castAbility(player, profile, id, explicitTarget, explicitLocation));
    }

    private boolean castAbility(Player player, PlayerProfile profile, String abilityId, Entity explicitTarget, Location explicitLocation) {
        Optional<Skill> optionalSkill = skillRegistryService.getSkill(abilityId);
        if (optionalSkill.isEmpty()) {
            return false;
        }
        Skill skill = optionalSkill.get();
        if (!skill.canUse(profile)) {
            if (profile.isSkillOnCooldown(skill.getSkillId())) {
                double seconds = profile.getSkillRemainingCooldown(skill.getSkillId()) / 1000.0;
                player.sendMessage(StringUtil.colorize("&c" + skill.getSkillName() + " is on cooldown for " + String.format("%.1f", seconds) + "s."));
            } else if (profile.getCurrentMana() < Math.ceil(skill.getEffectiveManaCost(profile))) {
                player.sendMessage(StringUtil.colorize("&cNot enough mana for " + skill.getSkillName() + "."));
            }
            return false;
        }

        Entity targetEntity = resolveTargetEntity(player, skill, explicitTarget);
        if (skill.getSkillType() == SkillType.ACTIVE_TARGETED_ENTITY) {
            if (!(targetEntity instanceof LivingEntity)) {
                player.sendMessage(StringUtil.colorize("&cNo valid target in sight for " + skill.getSkillName() + "."));
                return false;
            }
        }

        Location targetLocation = resolveLocation(player, skill, explicitLocation);

        try {
            skill.execute(profile, targetEntity, targetLocation);
            skill.onCooldown(profile);
            player.sendActionBar(StringUtil.colorize("&b» " + skill.getSkillName()));
            return true;
        } catch (Exception ex) {
            logger.severe("Error executing item ability '" + abilityId + "' for " + player.getName() + ": " + ex.getMessage(), ex);
            player.sendMessage(StringUtil.colorize("&cSomething went wrong while using that ability."));
            return false;
        }
    }

    private Entity resolveTargetEntity(Player player, Skill skill, Entity explicitTarget) {
        if (skill.getSkillType() != SkillType.ACTIVE_TARGETED_ENTITY) {
            return null;
        }
        if (explicitTarget != null) {
            return explicitTarget;
        }
        try {
            return player.getTargetEntity(12);
        } catch (NoSuchMethodError ignored) {
            // API fallback: use ray trace if running on an older server version
            return player.getNearbyEntities(12, 12, 12).stream()
                    .filter(LivingEntity.class::isInstance)
                    .findFirst()
                    .orElse(null);
        }
    }

    private Location resolveLocation(Player player, Skill skill, Location explicitLocation) {
        if (skill.getSkillType() == SkillType.ACTIVE_AOE_POINT) {
            if (explicitLocation != null) {
                return explicitLocation.clone();
            }
            Block targetBlock = player.getTargetBlockExact(20);
            if (targetBlock != null) {
                return targetBlock.getLocation();
            }
            return player.getLocation();
        }
        return explicitLocation;
    }
}
