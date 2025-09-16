package com.x1f4r.mmocraft.config;

import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A utility class to manage individual YAML configuration files.
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private final String fileName;
    private final LoggingUtil logger;
    private final boolean applyDefaultsFromJar;
    private FileConfiguration config;
    private final File configFile;

    public ConfigManager(JavaPlugin plugin, String fileName, LoggingUtil logger) {
        this(plugin, fileName, logger, true);
    }

    public ConfigManager(JavaPlugin plugin, String fileName, LoggingUtil logger, boolean copyDefaultsFromJar) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.logger = logger;
        this.applyDefaultsFromJar = copyDefaultsFromJar;
        this.configFile = new File(plugin.getDataFolder(), fileName);

        if (copyDefaultsFromJar) {
            saveDefaultConfig();
        } else if (!configFile.exists()) {
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            try {
                if (configFile.createNewFile()) {
                    logger.info("Created empty '" + fileName + "' because demo content is disabled.");
                }
            } catch (IOException e) {
                logger.severe("Could not create empty '" + fileName + "': " + e.getMessage(), e);
            }
        }

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
            if (applyDefaultsFromJar) {
                InputStream defaultConfigStream = plugin.getResource(fileName);
                if (defaultConfigStream != null) {
                    YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream));
                    config.setDefaults(defaultConfig);
                    logger.debug("Default values for '" + fileName + "' applied from JAR.");
                }
            } else {
                logger.debug("Jar defaults intentionally not applied for '" + fileName + "'.");
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
