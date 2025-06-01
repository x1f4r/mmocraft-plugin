package com.x1f4r.mmocraft.command;

// import org.bukkit.plugin.java.JavaPlugin; // Was unused

public interface CommandRegistryService {
    void registerCommand(String commandName, AbstractPluginCommand commandExecutor);
    // Potentially add an unregister method in the future
    // void unregisterCommand(String commandName);
}
