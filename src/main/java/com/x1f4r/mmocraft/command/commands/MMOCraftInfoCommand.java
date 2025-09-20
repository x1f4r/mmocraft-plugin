package com.x1f4r.mmocraft.command.commands;

import com.x1f4r.mmocraft.command.AbstractPluginCommand;
import com.x1f4r.mmocraft.command.CommandExecutable;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.util.LoggingUtil; // Added
import com.x1f4r.mmocraft.util.StringUtil; // Added
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

// import java.util.Arrays; // Keep if used, for now it's not. // Now confirmed unused
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MMOCraftInfoCommand extends AbstractPluginCommand {

    private final JavaPlugin plugin;
    private final LoggingUtil logger; // Added
    private final PlayerDataService playerDataService;

    public MMOCraftInfoCommand(JavaPlugin plugin, String commandName, String permission, String description,
                               LoggingUtil logger, PlayerDataService playerDataService) {
        super(commandName, permission, description);
        this.plugin = plugin;
        this.logger = logger; // Initialize logger
        this.playerDataService = playerDataService;

        // Register a simple subcommand: /mmoc version
        registerSubCommand("version", new CommandExecutable() {
            @Override
            public boolean onCommand(CommandSender sender, String[] args) {
                // Example of using logger if needed: logger.debug("Version subcommand executed by " + sender.getName());
                sender.sendMessage(StringUtil.colorize("&6[MMOCraft] &bVersion: &f" + plugin.getPluginMeta().getVersion()));
                return true;
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String[] args) {
                return Collections.emptyList(); // No further arguments for "version"
            }
        });

        // Register another simple subcommand: /mmoc help
        registerSubCommand("help", new CommandExecutable() {
            @Override
            public boolean onCommand(CommandSender sender, String[] args) {
                sender.sendMessage(StringUtil.colorize("&6--- MMOCraft Help ---"));
                sender.sendMessage(StringUtil.colorize("&b/" + commandName + " version &7- Shows plugin version."));
                sender.sendMessage(StringUtil.colorize("&b/" + commandName + " help &7- Shows this help message."));
                return true;
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String[] args) {
                return Collections.emptyList();
            }
        });

        registerSubCommand("profile", new CommandExecutable() {
            @Override
            public boolean onCommand(CommandSender sender, String[] args) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(StringUtil.colorize("&cOnly players can view their profile."));
                    return true;
                }
                PlayerProfile profile = playerDataService.getPlayerProfile(player.getUniqueId());
                if (profile == null) {
                    sender.sendMessage(StringUtil.colorize("&cYour profile is still loading. Please try again shortly."));
                    return true;
                }
                sendProfileSheet(player, profile);
                return true;
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String[] args) {
                return Collections.emptyList();
            }
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        // This is the default action if no subcommand (or a non-matching one) is provided.
        // Or if /mmoc is typed with no arguments.
        sender.sendMessage(StringUtil.colorize("&6[MMOCraft] &aWelcome to MMOCraft! Your adventure begins."));
        sender.sendMessage(StringUtil.colorize("&eType '/" + commandName + " help' for a list of commands."));
        // Example: logger.info("Base MMOCraftInfoCommand executed by " + sender.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        // This method is called by AbstractPluginCommand if no subcommand matches the first argument.
        // For the base /mmoc command, if we expect further arguments beyond subcommands,
        // we can provide them here. Otherwise, return empty list or null.
        // Since subcommands handle their own tab completion, this is mostly for the base command's direct args.
        if (args.length == 1) { // If they are typing the first argument (potential subcommand)
            // AbstractPluginCommand's onTabComplete will already suggest "version" and "help"
            // We can add more suggestions here if /mmoc itself had direct arguments not part of subcommands
            return null; // Let AbstractPluginCommand handle subcommand suggestions
        }
        return Collections.emptyList(); // No further specific suggestions for the base command itself
    }

    private void sendProfileSheet(Player player, PlayerProfile profile) {
        UUID uuid = profile.getPlayerUUID();
        player.sendMessage(StringUtil.colorize("&6--- &e" + profile.getPlayerName() + "'s Stats &6---"));
        player.sendMessage(StringUtil.colorize(String.format("&eLevel: &f%d &7(%d/%d XP)",
                profile.getLevel(), profile.getExperience(), profile.getExperienceToNextLevel())));
        player.sendMessage(StringUtil.colorize(String.format("&cHealth: &f%d/%d &7| &bMana: &f%d/%d",
                profile.getCurrentHealth(), profile.getMaxHealth(), profile.getCurrentMana(), profile.getMaxMana())));
        player.sendMessage(StringUtil.colorize(String.format("&aDefense: &f%.1f &7(%.1f%% DR)",
                profile.getStatValue(Stat.DEFENSE), profile.getPhysicalDamageReduction() * 100.0)));
        player.sendMessage(StringUtil.colorize(String.format("&aTrue Defense: &f%.1f", profile.getStatValue(Stat.TRUE_DEFENSE))));
        player.sendMessage(StringUtil.colorize(String.format("&dCrit Chance: &f%.1f%% &7| &dCrit Damage: &f%.1f%%",
                profile.getCriticalHitChance() * 100.0,
                (profile.getCriticalDamageBonus() - 1.0) * 100.0)));
        player.sendMessage(StringUtil.colorize(String.format("&dAbility Power: &f%.1f%% &7| &dFerocity: &f%.1f%%",
                profile.getStatValue(Stat.ABILITY_POWER), profile.getStatValue(Stat.FEROCITY))));
        player.sendMessage(StringUtil.colorize(String.format("&bSpeed: &f%.1f &7| &bEvasion: &f%.1f%%",
                profile.getStatValue(Stat.SPEED), profile.getEvasionChance() * 100.0)));

        player.sendMessage(StringUtil.colorize("&6Offense:"));
        sendStatLines(player, profile,
                Stat.STRENGTH,
                Stat.INTELLIGENCE,
                Stat.ATTACK_SPEED,
                Stat.MANA_REGEN);

        player.sendMessage(StringUtil.colorize("&6Utility:"));
        sendStatLines(player, profile,
                Stat.MAGIC_FIND,
                Stat.PET_LUCK);

        player.sendMessage(StringUtil.colorize("&6Gathering:"));
        sendStatLines(player, profile,
                Stat.MINING_SPEED,
                Stat.MINING_FORTUNE,
                Stat.FARMING_FORTUNE,
                Stat.FORAGING_FORTUNE,
                Stat.FISHING_FORTUNE);

        logger.fine("Displayed profile sheet for " + uuid);
    }

    private void sendStatLines(Player player, PlayerProfile profile, Stat... stats) {
        Map<Stat, Double> effective = profile.getEffectiveStats();
        for (Stat stat : stats) {
            double total = effective.getOrDefault(stat, 0.0);
            double raw = profile.getTotalInvestedStatValue(stat);
            player.sendMessage(StringUtil.colorize(String.format("  &b%s: &f%s &7(raw %.1f)",
                    stat.getDisplayName(), formatStatValue(stat, total), raw)));
        }
    }

    private String formatStatValue(Stat stat, double value) {
        switch (stat) {
            case CRITICAL_CHANCE, CRITICAL_DAMAGE, ABILITY_POWER, EVASION, FEROCITY,
                    MAGIC_FIND, PET_LUCK, MINING_FORTUNE, FARMING_FORTUNE,
                    FORAGING_FORTUNE, FISHING_FORTUNE -> {
                return String.format("%.1f%%", value);
            }
            default -> {
                return String.format("%.1f", value);
            }
        }
    }
}
