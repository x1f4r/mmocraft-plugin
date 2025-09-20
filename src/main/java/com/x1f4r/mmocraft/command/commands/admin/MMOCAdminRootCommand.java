package com.x1f4r.mmocraft.command.commands.admin;

import com.x1f4r.mmocraft.command.AbstractPluginCommand;
import com.x1f4r.mmocraft.command.commands.admin.demo.DemoAdminCommand;
import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.util.StringUtil;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MMOCAdminRootCommand extends AbstractPluginCommand {

    private final MMOCraftPlugin plugin;

    public MMOCAdminRootCommand(MMOCraftPlugin plugin) {
        super("mmocadm", "mmocraft.admin", "Base command for MMOCraft administration.");
        this.plugin = plugin;

        // Register other admin command modules as subcommands here
        registerSubCommand("combat", new CombatAdminCommand(plugin));
        registerSubCommand("item", new ItemAdminCommand(plugin));
        registerSubCommand("resource", new ResourceAdminCommand(plugin, "mmocadm resource", "mmocraft.admin.resource"));
        registerSubCommand("demo", new DemoAdminCommand(plugin));
        registerSubCommand("diagnostics", new DiagnosticsAdminCommand(plugin));
        registerSubCommand("issues", new DiagnosticsIssuesCommand(plugin));
        registerSubCommand("reloadconfig", new ReloadConfigAdminCommand(plugin));
        // Example: registerSubCommand("config", new ConfigAdminCommand(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        // Base /mmocadm command - show help for its subcommands
        sender.sendMessage(StringUtil.colorize("&6--- MMOCraft Admin Commands ---"));
        if (sender.hasPermission("mmocraft.admin.combat")) {
            sender.sendMessage(StringUtil.colorize("&e/mmocadm combat &7- Access combat admin commands."));
        }
        if (sender.hasPermission("mmocraft.admin.item")) {
            sender.sendMessage(StringUtil.colorize("&e/mmocadm item &7- Access item admin commands."));
        }
        if (sender.hasPermission("mmocraft.admin.resource")) { // General permission for the resource module
            sender.sendMessage(StringUtil.colorize("&e/mmocadm resource &7- Access resource gathering admin commands."));
        }
        if (sender.hasPermission("mmocraft.admin.diagnostics")) {
            sender.sendMessage(StringUtil.colorize("&e/mmocadm diagnostics &7- Run plugin health checks and log issues."));
            sender.sendMessage(StringUtil.colorize("&e/mmocadm issues &7- List outstanding warnings or errors."));
        }
        if (sender.hasPermission("mmocraft.admin.reload")) {
            sender.sendMessage(StringUtil.colorize("&e/mmocadm reloadconfig &7- Reload gameplay configuration files."));
        }

        // Check if the sender has permission for any registered subcommand to avoid "No admin modules available"
        // if they have permission for a dynamically registered one but not the hardcoded ones above.
        boolean hasAtLeastOneSubCommand = !subCommands.isEmpty();
        if (!hasAtLeastOneSubCommand) {
            sender.sendMessage(StringUtil.colorize("&7No admin modules available to you or none registered."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        // This method in AbstractPluginCommand already handles suggesting registered subcommand names
        // like "combat" when args.length == 1.
        // If further arguments were directly for /mmocadm, they would be handled here.
        if (args.length == 1) {
            return subCommands.keySet().stream()
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
