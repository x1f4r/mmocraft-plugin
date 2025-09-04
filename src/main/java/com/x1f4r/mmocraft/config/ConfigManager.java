package com.x1f4r.mmocraft.config;

import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A utility class to manage individual YAML configuration files.
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private final String fileName;
    private final LoggingUtil logger;
    private FileConfiguration config;
    private final File configFile;

    public ConfigManager(JavaPlugin plugin, String fileName, LoggingUtil logger) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.logger = logger;
        this.configFile = new File(plugin.getDataFolder(), fileName);
        saveDefaultConfig();
        loadConfig();
    }

    public FileConfiguration getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    public void loadConfig() {
        try {
            config = YamlConfiguration.loadConfiguration(configFile);
            InputStream defaultConfigStream = plugin.getResource(fileName);
            if (defaultConfigStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream));
                config.setDefaults(defaultConfig);
                logger.debug("Default values for '" + fileName + "' applied from JAR.");
            }
        } catch (Exception e) {
            logger.severe("Could not load '" + fileName + "': " + e.getMessage(), e);
            config = new YamlConfiguration(); // Fallback to an empty config
        }
    }

    public void saveDefaultConfig() {
        if (!configFile.exists()) {
            try {
                plugin.saveResource(fileName, false);
                logger.info("Default '" + fileName + "' copied to data folder.");
            } catch (Exception e) {
                logger.severe("Could not save default '" + fileName + "': " + e.getMessage(), e);
            }
        }
    }

    public void reloadConfig() {
        loadConfig();
    }
}
