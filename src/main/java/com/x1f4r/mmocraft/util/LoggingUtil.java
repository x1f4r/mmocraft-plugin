package com.x1f4r.mmocraft.util;

import com.x1f4r.mmocraft.config.ConfigService;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
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

    public void structuredInfo(String event, String message, Map<String, ?> context) {
        logStructured(Level.INFO, "INFO", event, message, context, null);
    }

    public void structuredWarning(String event, String message, Map<String, ?> context) {
        logStructured(Level.WARNING, "WARNING", event, message, context, null);
    }

    public void structuredError(String event, String message, Map<String, ?> context) {
        logStructured(Level.SEVERE, "ERROR", event, message, context, null);
    }

    public void structuredError(String event, String message, Map<String, ?> context, Throwable throwable) {
        logStructured(Level.SEVERE, "ERROR", event, message, context, throwable);
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

    private void logStructured(Level level,
                               String severity,
                               String event,
                               String message,
                               Map<String, ?> context,
                               Throwable throwable) {
        Objects.requireNonNull(message, "message");
        String payload = formatStructuredPayload(severity, event, message, context);
        if (throwable != null) {
            logger.log(level, payload, throwable);
        } else {
            logger.log(level, payload);
        }
    }

    private String formatStructuredPayload(String severity, String event, String message, Map<String, ?> context) {
        StringBuilder builder = new StringBuilder(prefix).append('{');
        builder.append("\"severity\":\"").append(escapeJson(severity != null ? severity : "UNKNOWN"))
                .append('\"');
        if (event != null && !event.isBlank()) {
            builder.append(',').append("\"event\":\"").append(escapeJson(event)).append('\"');
        }
        builder.append(',').append("\"message\":\"").append(escapeJson(message)).append('\"');

        Map<String, ?> safeContext = context == null ? Collections.emptyMap() : new LinkedHashMap<>(context);
        if (!safeContext.isEmpty()) {
            builder.append(',').append("\"context\":{");
            boolean first = true;
            for (Map.Entry<String, ?> entry : safeContext.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                if (!first) {
                    builder.append(',');
                }
                builder.append("\"").append(escapeJson(entry.getKey())).append("\":");
                Object value = entry.getValue();
                if (value == null) {
                    builder.append("null");
                } else {
                    builder.append("\"").append(escapeJson(String.valueOf(value))).append('\"');
                }
                first = false;
            }
            builder.append('}');
        }

        builder.append('}');
        return builder.toString();
    }

    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"', '\\' -> escaped.append('\\').append(c);
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
