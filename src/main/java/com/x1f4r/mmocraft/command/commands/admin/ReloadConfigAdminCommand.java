package com.x1f4r.mmocraft.command.commands.admin;

import com.x1f4r.mmocraft.command.AbstractPluginCommand;
import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.util.StringUtil;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class ReloadConfigAdminCommand extends AbstractPluginCommand {

    private static final String PERMISSION = "mmocraft.admin.reload";
    private final MMOCraftPlugin plugin;

    public ReloadConfigAdminCommand(MMOCraftPlugin plugin) {
        super("reloadconfig", PERMISSION, "Reload all MMOCraft configuration files and reapply settings.");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        plugin.reloadPluginConfig();
        sender.sendMessage(StringUtil.colorize("&aMMOCraft configuration reloaded."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
