package com.x1f4r.mmocraft.command;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class BasicCommandRegistryService implements CommandRegistryService {

    private final JavaPlugin plugin; // For Bukkit API calls like getCommand()
    private final LoggingUtil log;    // For logging

    public BasicCommandRegistryService(JavaPlugin plugin) {
        this.plugin = plugin; // Store the plugin instance

        // Obtain LoggingUtil from the plugin instance
        if (plugin instanceof MMOCraftPlugin) {
            LoggingUtil mmocraftLogger = ((MMOCraftPlugin) plugin).getLoggingUtil();
            if (mmocraftLogger == null) {
                // This is a safeguard. LoggingUtil should be initialized in MMOCraftPlugin before this service.
                this.log = new LoggingUtil(plugin);
                this.log.warning("BasicCommandRegistryService: MMOCraftPlugin's LoggingUtil was not yet initialized. Using a fallback logger. Ensure LoggingUtil is created before CommandRegistryService in MMOCraftPlugin.onEnable().");
            } else {
                this.log = mmocraftLogger;
            }
        } else {
            // If the plugin is not an instance of MMOCraftPlugin (e.g., during tests or if used by another plugin),
            // create a new LoggingUtil as a fallback.
            this.log = new LoggingUtil(plugin);
            this.log.warning("BasicCommandRegistryService: Initialized with a generic Plugin instance. Logging will use a standalone LoggingUtil.");
        }
    }

    @Override
    public void registerCommand(String commandName, AbstractPluginCommand commandExecutor) {
        PluginCommand pluginCommand = this.plugin.getCommand(commandName); // Use the stored plugin instance

        if (pluginCommand != null) {
            pluginCommand.setExecutor(commandExecutor);
            pluginCommand.setTabCompleter(commandExecutor);
            this.log.info("Command '" + commandName + "' registered successfully.");
        } else {
            this.log.warning("Failed to register command '" + commandName + "'. Make sure it's defined in plugin.yml.");
        }
    }
}
