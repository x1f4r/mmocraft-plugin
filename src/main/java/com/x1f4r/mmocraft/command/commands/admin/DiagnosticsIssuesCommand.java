package com.x1f4r.mmocraft.command.commands.admin;

import com.x1f4r.mmocraft.command.AbstractPluginCommand;
import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.diagnostics.PluginDiagnosticsService;
import com.x1f4r.mmocraft.diagnostics.PluginDiagnosticsService.DiagnosticEntry;
import com.x1f4r.mmocraft.diagnostics.PluginDiagnosticsService.Severity;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.util.StringUtil;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Admin command that only surfaces outstanding diagnostics issues (warnings and errors).
 */
public class DiagnosticsIssuesCommand extends AbstractPluginCommand {

    private static final String PERMISSION = "mmocraft.admin.diagnostics";

    private final PluginDiagnosticsService diagnosticsService;
    private final LoggingUtil logger;

    public DiagnosticsIssuesCommand(MMOCraftPlugin plugin) {
        super("issues", PERMISSION, "Show unresolved MMOCraft warnings and errors.");
        this.diagnosticsService = plugin.getDiagnosticsService();
        this.logger = plugin.getLoggingUtil();
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (diagnosticsService == null) {
            sender.sendMessage(StringUtil.colorize("&cDiagnostics service is not available."));
            logger.structuredError(
                    "command-diagnostics",
                    "Issues command invoked without diagnostics service.",
                    java.util.Map.of(
                            "sender", sender.getName(),
                            "command", "/mmocadm issues"
                    )
            );
            return true;
        }

        List<DiagnosticEntry> issues = diagnosticsService.findIssues();
        sender.sendMessage(StringUtil.colorize("&6--- Outstanding MMOCraft Issues ---"));

        if (issues.isEmpty()) {
            sender.sendMessage(StringUtil.colorize("&aNo warnings or errors are currently active."));
            logger.structuredInfo(
                    "command-diagnostics",
                    "Outstanding issues requested with none found.",
                    java.util.Map.of(
                            "sender", sender.getName(),
                            "issueCount", 0
                    )
            );
            return true;
        }

        issues.forEach(entry -> sender.sendMessage(formatEntry(entry)));
        logger.structuredInfo(
                "command-diagnostics",
                "Outstanding issues reported.",
                java.util.Map.of(
                        "sender", sender.getName(),
                        "issueCount", issues.size()
                )
        );
        issues.forEach(this::logEntry);
        return true;
    }

    private String formatEntry(DiagnosticEntry entry) {
        String color = entry.getSeverity() == Severity.ERROR ? "&c" : "&e";
        StringBuilder builder = new StringBuilder();
        builder.append(color)
                .append("[")
                .append(entry.getSeverity().name())
                .append("] ")
                .append(entry.getMessage());
        entry.getDetail().ifPresent(detail -> builder.append(" &7- ").append(detail));
        return StringUtil.colorize(builder.toString());
    }

    private void logEntry(DiagnosticEntry entry) {
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        context.put("severity", entry.getSeverity().name());
        entry.getDetail().ifPresent(detail -> context.put("detail", detail));
        switch (entry.getSeverity()) {
            case ERROR -> logger.structuredError("command-diagnostics", entry.getMessage(), context);
            case WARNING -> logger.structuredWarning("command-diagnostics", entry.getMessage(), context);
            default -> logger.structuredInfo("command-diagnostics", entry.getMessage(), context);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
