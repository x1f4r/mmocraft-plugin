package com.x1f4r.mmocraft.config;

import org.bukkit.configuration.file.FileConfiguration;
import com.x1f4r.mmocraft.util.LoggingUtil; // Added
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
// import java.io.IOException; // No longer used directly
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

public class BasicConfigService implements ConfigService {

    private final JavaPlugin plugin; // Needed for getDataFolder, saveResource, getResource
    private final LoggingUtil logger; // Added
    private FileConfiguration config;
    private File configFile;

    public BasicConfigService(JavaPlugin plugin, LoggingUtil logger) {
        this.plugin = plugin;
        this.logger = logger; // Store logger
        this.configFile = new File(plugin.getDataFolder(), "mmocraft.conf");

        logger.debug("Initializing BasicConfigService..."); // Example usage
        saveDefaultConfig();
        loadConfig();
        logger.info("Configuration loaded successfully.");
    }

    private void saveDefaultConfig() {
        if (!configFile.exists()) {
            try {
                plugin.saveResource("mmocraft.conf", false);
                logger.info("Default 'mmocraft.conf' copied to data folder.");
            } catch (Exception e) {
                logger.severe("Could not save default 'mmocraft.conf': " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void loadConfig() {
        try {
            config = YamlConfiguration.loadConfiguration(configFile);
            InputStream defaultConfigStream = plugin.getResource("mmocraft.conf");
            if (defaultConfigStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(defaultConfigStream));
                config.setDefaults(defaultConfig);
                logger.debug("Default config values applied from JAR's mmocraft.conf.");
            } else {
                logger.warning("Default 'mmocraft.conf' not found in JAR resources.");
            }
        } catch (Exception e) {
            logger.severe("Could not load 'mmocraft.conf': " + e.getMessage(), e);
            // Fallback to an empty configuration to prevent NPEs, or rethrow
            config = new YamlConfiguration();
        }
    }

    @Override
    public String getString(String path) {
        return config.getString(path);
    }

    @Override
    public int getInt(String path) {
        return config.getInt(path);
    }

    @Override
    public double getDouble(String path) {
        return config.getDouble(path);
    }

    @Override
    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }

    @Override
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    @Override
    public void reloadConfig() {
        // logger.debug("Attempting to reload configuration..."); // Already logged by MMOCraftPlugin
        if (configFile == null) {
            // This case should ideally not happen if constructor ran correctly
            this.configFile = new File(plugin.getDataFolder(), "mmocraft.conf");
            logger.warning("Config file was null during reload, re-initialized path. This might indicate an issue.");
        }
        loadConfig(); // Reuse loadConfig logic, which includes logging
        // logger.info("Configuration reloaded."); // Already logged by MMOCraftPlugin
    }
}
