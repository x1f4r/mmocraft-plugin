package com.x1f4r.mmocraft.util;

import com.x1f4r.mmocraft.config.ConfigService;
import org.bukkit.plugin.Plugin;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingUtil {

    private final Logger logger;
    private final String prefix;
    private final ConfigService configService;

    public LoggingUtil(Plugin plugin, ConfigService configService) {
        this.logger = plugin.getLogger();
        this.prefix = "[" + plugin.getName() + "] ";
        this.configService = configService;
    }

    public LoggingUtil(Plugin plugin) {
        this(plugin, null);
    }

    public void info(String message) {
        logger.info(prefix + message);
    }

    public void warning(String message) {
        logger.warning(prefix + message);
    }

    public void warning(String message, Throwable throwable) {
        logger.log(Level.WARNING, prefix + message, throwable);
    }

    public void severe(String message) {
        logger.severe(prefix + message);
    }

    public void severe(String message, Throwable throwable) {
        logger.log(Level.SEVERE, prefix + message, throwable);
    }

    public void debug(String message) {
        boolean debugEnabled = (configService == null) || configService.getBoolean("core.debug-logging");
        if (debugEnabled) {
            logger.info(prefix + "[DEBUG] " + message);
        }
    }

    public void fine(String message) {
        logger.fine(prefix + message);
    }

    public void finer(String message) {
        logger.finer(prefix + message);
    }

    public void finest(String message) {
        logger.finest(prefix + message);
    }
}
