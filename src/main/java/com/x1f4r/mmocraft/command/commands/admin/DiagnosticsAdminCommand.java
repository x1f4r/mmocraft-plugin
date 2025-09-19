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

public class DiagnosticsAdminCommand extends AbstractPluginCommand {

    private static final String PERMISSION = "mmocraft.admin.diagnostics";

    private final PluginDiagnosticsService diagnosticsService;
    private final LoggingUtil logger;

    public DiagnosticsAdminCommand(MMOCraftPlugin plugin) {
        super("diagnostics", PERMISSION, "Run MMOCraft health checks and log potential issues.");
        this.diagnosticsService = plugin.getDiagnosticsService();
        this.logger = plugin.getLoggingUtil();
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (diagnosticsService == null) {
            sender.sendMessage(StringUtil.colorize("&cDiagnostics service is not available."));
            logger.severe("Diagnostics command invoked but diagnosticsService was null.");
            return true;
        }

        List<DiagnosticEntry> entries = diagnosticsService.runDiagnostics();
        long warningCount = entries.stream().filter(e -> e.getSeverity() == Severity.WARNING).count();
        long errorCount = entries.stream().filter(e -> e.getSeverity() == Severity.ERROR).count();

        sender.sendMessage(StringUtil.colorize("&6--- MMOCraft Diagnostics Report ---"));
        entries.forEach(entry -> sender.sendMessage(formatEntry(entry)));
        sender.sendMessage(StringUtil.colorize("&7Warnings: &e" + warningCount + " &7| Errors: &c" + errorCount));

        logger.info("Diagnostics report requested by " + sender.getName() + ". Issues: warnings=" + warningCount + ", errors=" + errorCount);
        entries.forEach(this::logEntry);
        return true;
    }

    private String formatEntry(DiagnosticEntry entry) {
        String color;
        switch (entry.getSeverity()) {
            case ERROR -> color = "&c";
            case WARNING -> color = "&e";
            default -> color = "&a";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(color).append("[").append(entry.getSeverity().name()).append("] ").append(entry.getMessage());
        entry.getDetail().ifPresent(detail -> builder.append(" &7- ").append(detail));
        return StringUtil.colorize(builder.toString());
    }

    private void logEntry(DiagnosticEntry entry) {
        String logMessage = "Diagnostics: [" + entry.getSeverity() + "] " + entry.getMessage() + entry.getDetail().map(d -> " - " + d).orElse("");
        switch (entry.getSeverity()) {
            case ERROR -> logger.severe(logMessage);
            case WARNING -> logger.warning(logMessage);
            default -> logger.info(logMessage);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
