package com.x1f4r.mmocraft.command.commands.admin.demo;

import com.x1f4r.mmocraft.command.AbstractPluginCommand;
import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.demo.DemoContentSettings;
import com.x1f4r.mmocraft.demo.DemoFeature;
import com.x1f4r.mmocraft.util.StringUtil;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class DemoAdminCommand extends AbstractPluginCommand {

    private final MMOCraftPlugin plugin;

    public DemoAdminCommand(MMOCraftPlugin plugin) {
        super("demo", "mmocraft.admin.demo", "Controls bundled demo content toggles.");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "status" -> {
                showStatus(sender);
                yield true;
            }
            case "enable" -> {
                handleToggle(sender, args, true);
                yield true;
            }
            case "disable" -> {
                handleToggle(sender, args, false);
                yield true;
            }
            case "reload" -> {
                plugin.reloadPluginConfig();
                sender.sendMessage(StringUtil.colorize("&aReloaded configuration and demo content settings."));
                yield true;
            }
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("status", "enable", "disable", "reload"), args[0]);
        }
        if (args.length == 2 && ("enable".equalsIgnoreCase(args[0]) || "disable".equalsIgnoreCase(args[0]))) {
            List<String> options = new ArrayList<>();
            options.add("all");
            for (DemoFeature feature : DemoFeature.values()) {
                options.add(feature.getAliases().get(0));
            }
            return filter(options, args[1]);
        }
        return Collections.emptyList();
    }

    private void showStatus(CommandSender sender) {
        DemoContentSettings settings = plugin.getDemoSettings();
        sender.sendMessage(StringUtil.colorize("&6--- Demo Content Status ---"));
        sender.sendMessage(StringUtil.colorize("&eMaster: " + (settings.masterEnabled() ? "&aENABLED" : "&cDISABLED")));
        Map<DemoFeature, Boolean> features = settings.asFeatureMap();
        for (DemoFeature feature : DemoFeature.values()) {
            if (feature == DemoFeature.MASTER) {
                continue;
            }
            boolean enabled = features.getOrDefault(feature, false);
            sender.sendMessage(StringUtil.colorize("&7- &f" + feature.getDisplayName() + ": " + (enabled ? "&aON" : "&cOFF")));
        }
        sender.sendMessage(StringUtil.colorize("&8(Note: command changes are not persisted to mmocraft.conf.)"));
    }

    private void handleToggle(CommandSender sender, String[] args, boolean enable) {
        if (args.length < 2) {
            sender.sendMessage(StringUtil.colorize("&cUsage: /mmocadm demo " + (enable ? "enable" : "disable") + " <feature|all>"));
            return;
        }
        DemoContentSettings current = plugin.getDemoSettings();
        String token = args[1];
        DemoContentSettings updated;
        if ("all".equalsIgnoreCase(token)) {
            updated = current.withAllFeatures(enable);
        } else {
            DemoFeature feature = DemoFeature.fromToken(token).orElse(null);
            if (feature == null) {
                sender.sendMessage(StringUtil.colorize("&cUnknown feature: " + token));
                return;
            }
            updated = current.withFeature(feature, enable);
            if (feature != DemoFeature.MASTER && enable && !updated.masterEnabled()) {
                updated = updated.withMasterEnabled(true);
            }
            if (feature != DemoFeature.MASTER && !enable && !updated.hasAnyFeatureEnabled()) {
                updated = updated.withMasterEnabled(false);
            }
            if (feature == DemoFeature.MASTER && !enable) {
                updated = updated.withAllFeatures(false);
            }
        }
        plugin.applyDemoSettings(updated);
        sender.sendMessage(StringUtil.colorize("&aDemo settings updated. Current: &f" + updated.describeEnabledFeatures()));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(StringUtil.colorize("&6--- /mmocadm demo ---"));
        sender.sendMessage(StringUtil.colorize("&e/mmocadm demo status &7- Show current demo toggle status."));
        sender.sendMessage(StringUtil.colorize("&e/mmocadm demo enable <feature|all> &7- Enable demo content."));
        sender.sendMessage(StringUtil.colorize("&e/mmocadm demo disable <feature|all> &7- Disable demo content."));
        sender.sendMessage(StringUtil.colorize("&e/mmocadm demo reload &7- Reload mmocraft.conf and reapply demo content."));
    }

    private List<String> filter(List<String> options, String prefix) {
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }
}
