package com.x1f4r.mmocraft.command.commands.admin;

import com.x1f4r.mmocraft.command.AbstractPluginCommand;
import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.playerdata.util.ExperienceUtil;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerDataAdminCommand extends AbstractPluginCommand {

    private final MMOCraftPlugin plugin;
    private final PlayerDataService playerDataService;
    private final LoggingUtil logger;

    private static final String PERM_BASE = "mmocraft.admin.playerdata";
    private static final String PERM_VIEW = PERM_BASE + ".view";
    private static final String PERM_SETSTAT = PERM_BASE + ".setstat";
    private static final String PERM_SETLEVEL = PERM_BASE + ".setlevel";
    private static final String PERM_ADDXP = PERM_BASE + ".addxp";
    private static final String PERM_ADDCURRENCY = PERM_BASE + ".addcurrency";

    public PlayerDataAdminCommand(MMOCraftPlugin plugin) {
        super("playerdata", PERM_BASE, "Admin command for managing player data.");
        this.plugin = plugin;
        this.playerDataService = plugin.getPlayerDataService();
        this.logger = plugin.getLoggingUtil();

        // Register subcommands
        registerSubCommand("view", this::executeView);
        registerSubCommand("setstat", this::executeSetStat);
        registerSubCommand("setlevel", this::executeSetLevel);
        registerSubCommand("addxp", this::executeAddXp);
        registerSubCommand("addcurrency", this::executeAddCurrency);
    }

    // Base /playerdata command execution (shows help)
    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(StringUtil.colorize("&6--- PlayerData Admin Help ---"));
        if (sender.hasPermission(PERM_VIEW)) sender.sendMessage(StringUtil.colorize("&e/pd view <playerName> &7- View player data."));
        if (sender.hasPermission(PERM_SETSTAT)) sender.sendMessage(StringUtil.colorize("&e/pd setstat <playerName> <statName> <value> &7- Set player stat."));
        if (sender.hasPermission(PERM_SETLEVEL)) sender.sendMessage(StringUtil.colorize("&e/pd setlevel <playerName> <level> &7- Set player level."));
        if (sender.hasPermission(PERM_ADDXP)) sender.sendMessage(StringUtil.colorize("&e/pd addxp <playerName> <amount> &7- Add XP to player."));
        if (sender.hasPermission(PERM_ADDCURRENCY)) sender.sendMessage(StringUtil.colorize("&e/pd addcurrency <playerName> <amount> &7- Add currency."));
    }

    // --- Subcommand Implementations ---

    private boolean executeView(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_VIEW)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission for this command.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /pd view <playerName>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or not online.");
            return true;
        }
        PlayerProfile profile = playerDataService.getPlayerProfile(target.getUniqueId());
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "PlayerProfile for '" + args[0] + "' not found in cache (should be if online).");
            return true;
        }

        sender.sendMessage(StringUtil.colorize("&6--- Player Data: " + profile.getPlayerName() + " ---"));
        sender.sendMessage(StringUtil.colorize("&eUUID: &f" + profile.getPlayerUUID()));
        sender.sendMessage(StringUtil.colorize("&eLevel: &f" + profile.getLevel()));
        sender.sendMessage(StringUtil.colorize("&eExperience: &f" + profile.getExperience() + " / " + profile.getExperienceToNextLevel()));
        sender.sendMessage(StringUtil.colorize("&eHealth: &f" + profile.getCurrentHealth() + " / " + profile.getMaxHealth()));
        sender.sendMessage(StringUtil.colorize("&eMana: &f" + profile.getCurrentMana() + " / " + profile.getMaxMana()));
        sender.sendMessage(StringUtil.colorize("&eCurrency: &f" + profile.getCurrency()));
        sender.sendMessage(StringUtil.colorize("&eCore Stats:"));
        for (Map.Entry<Stat, Double> entry : profile.getCoreStats().entrySet()) {
            sender.sendMessage(StringUtil.colorize("  &b" + entry.getKey().getDisplayName() + ": &f" + String.format("%.1f", entry.getValue())));
        }
        sender.sendMessage(StringUtil.colorize("&eDerived Stats:"));
        sender.sendMessage(StringUtil.colorize("  &bCrit Chance: &f" + String.format("%.2f%%", profile.getCriticalHitChance() * 100)));
        sender.sendMessage(StringUtil.colorize("  &bCrit Damage Bonus: &f" + String.format("%.0f%%", profile.getCriticalDamageBonus() * 100)));
        sender.sendMessage(StringUtil.colorize("  &bEvasion Chance: &f" + String.format("%.2f%%", profile.getEvasionChance() * 100)));
        sender.sendMessage(StringUtil.colorize("  &bPhys Reduction: &f" + String.format("%.2f%%", profile.getPhysicalDamageReduction() * 100)));
        sender.sendMessage(StringUtil.colorize("  &bMagic Reduction: &f" + String.format("%.2f%%", profile.getMagicDamageReduction() * 100)));
        sender.sendMessage(StringUtil.colorize("&eFirst Login: &f" + profile.getFirstLogin().toString()));
        sender.sendMessage(StringUtil.colorize("&eLast Login: &f" + profile.getLastLogin().toString()));
        return true;
    }

    private boolean executeSetStat(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_SETSTAT)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission for this command.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /pd setstat <playerName> <statName> <value>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or not online.");
            return true;
        }
        PlayerProfile profile = playerDataService.getPlayerProfile(target.getUniqueId());
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "PlayerProfile for '" + args[0] + "' not found.");
            return true;
        }

        Stat statToSet;
        try {
            statToSet = Stat.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid stat name: " + args[1]);
            sender.sendMessage(ChatColor.YELLOW + "Available stats: " + Arrays.stream(Stat.values()).map(Enum::name).collect(Collectors.joining(", ")));
            return true;
        }

        double value;
        try {
            value = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid value: " + args[2] + ". Must be a number.");
            return true;
        }

        profile.setStatValue(statToSet, value);
        // PlayerProfile.setStatValue calls recalculateDerivedAttributes
        playerDataService.savePlayerProfile(target.getUniqueId()); // Persist change
        sender.sendMessage(StringUtil.colorize("&aSet " + statToSet.getDisplayName() + " for " + target.getName() + " to " + String.format("%.1f", value) + ". Derived attributes recalculated."));
        logger.info(sender.getName() + " set " + statToSet.name() + " for " + target.getName() + " to " + value);
        return true;
    }

    private boolean executeSetLevel(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_SETLEVEL)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission for this command.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /pd setlevel <playerName> <level>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or not online.");
            return true;
        }
        PlayerProfile profile = playerDataService.getPlayerProfile(target.getUniqueId());
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "PlayerProfile for '" + args[0] + "' not found.");
            return true;
        }

        int newLevel;
        try {
            newLevel = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid level: " + args[1] + ". Must be an integer.");
            return true;
        }

        if (newLevel < ExperienceUtil.getMinLevel() || newLevel > ExperienceUtil.getMaxLevel()) {
            sender.sendMessage(ChatColor.RED + "Level must be between " + ExperienceUtil.getMinLevel() + " and " + ExperienceUtil.getMaxLevel() + ".");
            return true;
        }

        profile.setLevel(newLevel);
        profile.setExperience(0); // Reset XP for the new level
        // PlayerProfile.setLevel calls recalculateDerivedAttributes
        playerDataService.savePlayerProfile(target.getUniqueId());
        sender.sendMessage(StringUtil.colorize("&aSet level for " + target.getName() + " to " + newLevel + ". XP reset. Derived attributes recalculated."));
        logger.info(sender.getName() + " set level for " + target.getName() + " to " + newLevel);
        return true;
    }

    private boolean executeAddXp(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADDXP)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission for this command.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /pd addxp <playerName> <amount>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or not online.");
            return true;
        }
        // PlayerDataService handles profile null check for addExperience

        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[1] + ". Must be an integer.");
            return true;
        }
        if (amount <= 0) {
            sender.sendMessage(ChatColor.RED + "XP amount must be positive.");
            return true;
        }

        playerDataService.addExperience(target.getUniqueId(), amount);
        // addExperience in service handles level ups, events, and recalculations
        playerDataService.savePlayerProfile(target.getUniqueId());
        sender.sendMessage(StringUtil.colorize("&aAdded " + amount + " XP to " + target.getName() + "."));
        logger.info(sender.getName() + " added " + amount + " XP to " + target.getName());
        return true;
    }

    private boolean executeAddCurrency(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADDCURRENCY)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission for this command.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /pd addcurrency <playerName> <amount>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or not online.");
            return true;
        }
        PlayerProfile profile = playerDataService.getPlayerProfile(target.getUniqueId());
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "PlayerProfile for '" + args[0] + "' not found.");
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[1] + ". Must be an integer.");
            return true;
        }
        // Allow negative amounts to subtract currency, or add check if only positive
        // if (amount <= 0 && action_is_add_only) { ... }

        profile.setCurrency(profile.getCurrency() + amount);
        playerDataService.savePlayerProfile(target.getUniqueId());
        sender.sendMessage(StringUtil.colorize("&a" + (amount >= 0 ? "Added " : "Removed ") + Math.abs(amount) + " currency " + (amount >=0 ? "to" : "from") + " " + target.getName() + ". New balance: " + profile.getCurrency()));
        logger.info(sender.getName() + " changed currency for " + target.getName() + " by " + amount);
        return true;
    }

    // --- Tab Completion ---
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        // Base AbstractPluginCommand already handles suggesting top-level subcommand names ("view", "setstat", etc.) if args.length == 1
        // We need to provide further completion for specific subcommands.

        if (args.length > 1) {
            String subCmd = args[0].toLowerCase();
            // Player Name completion for relevant commands (second argument)
            if (args.length == 2 && Arrays.asList("view", "setstat", "setlevel", "addxp", "addcurrency").contains(subCmd)) {
                return null; // Bukkit default player name completion
            }

            // Stat Name completion for setstat (third argument)
            if (subCmd.equals("setstat") && args.length == 3) {
                if (!sender.hasPermission(PERM_SETSTAT)) return Collections.emptyList();
                return Arrays.stream(Stat.values())
                        .map(Enum::name)
                        .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList(); // Default: no other suggestions
    }
}
