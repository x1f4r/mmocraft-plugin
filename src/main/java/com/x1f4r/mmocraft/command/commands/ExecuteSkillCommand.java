package com.x1f4r.mmocraft.command.commands;

import com.x1f4r.mmocraft.command.AbstractPluginCommand;
import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.skill.model.Skill;
import com.x1f4r.mmocraft.skill.model.SkillType;
import com.x1f4r.mmocraft.skill.service.SkillRegistryService;
import com.x1f4r.mmocraft.util.StringUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
// import org.bukkit.ChatColor; // No longer needed
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ExecuteSkillCommand extends AbstractPluginCommand {

    private final MMOCraftPlugin plugin;
    private final SkillRegistryService skillRegistryService;
    private final PlayerDataService playerDataService;

    public ExecuteSkillCommand(MMOCraftPlugin plugin) {
        super("useskill", "mmocraft.command.useskill", "Executes a specified skill.");
        this.plugin = plugin;
        this.skillRegistryService = plugin.getSkillRegistryService();
        this.playerDataService = plugin.getPlayerDataService();
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by a player.", NamedTextColor.RED));
            return true;
        }
        Player casterPlayer = (Player) sender;
        PlayerProfile casterProfile = playerDataService.getPlayerProfile(casterPlayer.getUniqueId());

        if (casterProfile == null) {
            casterPlayer.sendMessage(Component.text("Your player data could not be found. Please try re-logging.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            casterPlayer.sendMessage(Component.text("Usage: /useskill <skillId> [targetName]", NamedTextColor.RED));
            // Optionally list available skills if desired (could be spammy)
            // skillRegistryService.getAllSkills().forEach(skill -> casterPlayer.sendMessage(StringUtil.colorize("&e - " + skill.getSkillId() + " (&7" + skill.getSkillName() + "&7)")));
            return true;
        }

        String skillId = args[0];
        Optional<Skill> optionalSkill = skillRegistryService.getSkill(skillId);

        if (optionalSkill.isEmpty()) {
            casterPlayer.sendMessage(Component.text("Skill '" + skillId + "' not found.", NamedTextColor.RED));
            return true;
        }

        Skill skill = optionalSkill.get();

        // --- Pre-Execution Checks ---
        double effectiveManaCost = skill.getEffectiveManaCost(casterProfile);

        if (!skill.canUse(casterProfile)) {
            if (casterProfile.isSkillOnCooldown(skillId)) {
                long remainingMillis = casterProfile.getSkillRemainingCooldown(skillId);
                double remainingSeconds = remainingMillis / 1000.0;
                casterPlayer.sendMessage(StringUtil.colorize("&c" + skill.getSkillName() + " is on cooldown for " + String.format("%.1f", remainingSeconds) + "s."));
            } else if (casterProfile.getCurrentMana() < Math.ceil(effectiveManaCost)) {
                casterPlayer.sendMessage(StringUtil.colorize("&cNot enough mana for " + skill.getSkillName() + ". (Need " + String.format("%.1f", effectiveManaCost) + ")"));
            } else {
                 casterPlayer.sendMessage(StringUtil.colorize("&cYou cannot use " + skill.getSkillName() + " right now."));
            }
            return true;
        }

        // --- Target Resolution (if applicable) ---
        Entity targetEntity = null;
        Location targetLocation = null; // For AOE_POINT

        if (skill.getSkillType() == SkillType.ACTIVE_TARGETED_ENTITY) {
            if (args.length < 2) {
                casterPlayer.sendMessage(Component.text("Usage: /useskill " + skillId + " <targetName>", NamedTextColor.RED));
                return true;
            }
            String targetName = args[1];
            targetEntity = Bukkit.getPlayerExact(targetName);
            if (targetEntity == null) {
                // Try to find other living entities if not player, for PvE
                for (Entity entity : casterPlayer.getNearbyEntities(15, 15, 15)) { // 15 block radius
                    if (entity instanceof LivingEntity && entity.getName().equalsIgnoreCase(targetName)) {
                        targetEntity = entity;
                        break;
                    }
                }
                if (targetEntity == null) {
                    casterPlayer.sendMessage(Component.text("Target '" + targetName + "' not found or not online/nearby.", NamedTextColor.RED));
                    return true;
                }
            }
            if (!(targetEntity instanceof LivingEntity)) {
                 casterPlayer.sendMessage(Component.text("Target must be a living entity.", NamedTextColor.RED));
                 return true;
            }
            if (targetEntity.equals(casterPlayer) && skill.getSkillType() != SkillType.ACTIVE_SELF) {
                 // Some targeted skills might allow self-targeting, others not.
                 // casterPlayer.sendMessage(Component.text("You cannot target yourself with " + skill.getSkillName(), NamedTextColor.RED));
                 // return true;
            }
        } else if (skill.getSkillType() == SkillType.ACTIVE_AOE_POINT) {
            // For simplicity, target player's looking at block, or self location if no block
            targetLocation = casterPlayer.getTargetBlock(null, 20).getLocation(); // Max 20 blocks
            if (targetLocation.getBlock().getType().isAir()) { // If looking at air, target feet
                targetLocation = casterPlayer.getLocation();
            }
        }
        // ACTIVE_SELF and ACTIVE_NO_TARGET don't require specific target parsing here.

        // TODO: Implement Cast Time (delay execution)
        if (skill.getCastTimeSeconds() > 0) {
            casterPlayer.sendMessage(StringUtil.colorize("&eCasting " + skill.getSkillName() + "... ("+skill.getCastTimeSeconds()+"s)"));
            // Schedule a task to execute the skill after castTimeSeconds
            // For now, direct execution for simplicity
        }

        // --- Execute Skill ---
        try {
            skill.execute(casterProfile, targetEntity, targetLocation);
            skill.onCooldown(casterProfile); // Put skill on cooldown after successful execution
            // PlayerDataService save is not called here; assumed to be periodic or on quit.
            // Skills that change profile data (like MinorHeal changing health) modify the cached PlayerProfile.
        } catch (Exception e) {
            casterPlayer.sendMessage(Component.text("An error occurred while using the skill: " + e.getMessage(), NamedTextColor.RED));
            plugin.getLoggingUtil().structuredError(
                    "command-execution",
                    "Skill execution failed.",
                    Map.of(
                            "skillId", skill.getSkillId(),
                            "player", casterPlayer.getName(),
                            "skillType", skill.getSkillType().name()
                    ),
                    e
            );
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return skillRegistryService.getAllSkills().stream()
                    .map(Skill::getSkillId)
                    .filter(id -> id.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String skillId = args[0];
            Optional<Skill> optionalSkill = skillRegistryService.getSkill(skillId);
            if (optionalSkill.isPresent() && optionalSkill.get().getSkillType() == SkillType.ACTIVE_TARGETED_ENTITY) {
                // Suggest online player names
                return null; // Bukkit default player name completion
            }
        }
        return Collections.emptyList();
    }
}
