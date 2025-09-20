package com.x1f4r.mmocraft.util;

import com.x1f4r.mmocraft.config.ConfigService;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingUtilTest {

    @Mock
    private Plugin mockPlugin;
    @Mock
    private Logger mockBukkitLogger;
    @Mock
    private ConfigService mockConfigService;

    private LoggingUtil loggingUtil;
    private LoggingUtil loggingUtilWithConfig;

    private final String pluginName = "TestPlugin";
    private final String expectedPrefix = "[" + pluginName + "] ";

    @BeforeEach
    void setUp() {
        when(mockPlugin.getName()).thenReturn(pluginName);
        when(mockPlugin.getLogger()).thenReturn(mockBukkitLogger);

        loggingUtil = new LoggingUtil(mockPlugin); // ConfigService is null here
        loggingUtilWithConfig = new LoggingUtil(mockPlugin, mockConfigService);
    }

    @Test
    void info_shouldLogWithInfoLevelAndPrefix() {
        String message = "This is an info message.";
        loggingUtil.info(message);
        verify(mockBukkitLogger).info(expectedPrefix + message);
    }

    @Test
    void warning_shouldLogWithWarningLevelAndPrefix() {
        String message = "This is a warning message.";
        loggingUtil.warning(message);
        verify(mockBukkitLogger).warning(expectedPrefix + message);
    }

    @Test
    void warning_withThrowable_shouldLogWithWarningLevelAndThrowable() {
        String message = "This is a warning message with throwable.";
        Throwable throwable = new RuntimeException("Test Exception");
        loggingUtil.warning(message, throwable);
        verify(mockBukkitLogger).log(Level.WARNING, expectedPrefix + message, throwable);
    }

    @Test
    void severe_shouldLogWithSevereLevelAndPrefix() {
        String message = "This is a severe message.";
        loggingUtil.severe(message);
        verify(mockBukkitLogger).severe(expectedPrefix + message);
    }

    @Test
    void severe_withThrowable_shouldLogWithSevereLevelPrefixAndThrowable() {
        String message = "This is a severe message with throwable.";
        Throwable throwable = new RuntimeException("Test Exception");
        loggingUtil.severe(message, throwable);
        verify(mockBukkitLogger).log(Level.SEVERE, expectedPrefix + message, throwable);
    }

    @Test
    void debug_shouldLogWhenConfigServiceIsNull() {
        // Uses loggingUtil (where configService is null)
        String message = "Debug message (no config).";
        loggingUtil.debug(message);
        verify(mockBukkitLogger).info(expectedPrefix + "[DEBUG] " + message);
    }

    @Test
    void debug_shouldLogWhenDebugIsEnabledInConfig() {
        when(mockConfigService.getBoolean("core.debug-logging")).thenReturn(true);
        String message = "Debug message (enabled).";
        loggingUtilWithConfig.debug(message);
        verify(mockBukkitLogger).info(expectedPrefix + "[DEBUG] " + message);
    }

    @Test
    void debug_shouldNotLogWhenDebugIsDisabledInConfig() {
        when(mockConfigService.getBoolean("core.debug-logging")).thenReturn(false);
        String message = "Debug message (disabled).";
        loggingUtilWithConfig.debug(message);
        verify(mockBukkitLogger, never()).info(anyString());
    }

    @Test
    void fine_shouldLogWithFineLevelAndPrefix() {
        String message = "This is a fine message.";
        loggingUtil.fine(message);
        verify(mockBukkitLogger).fine(expectedPrefix + message);
    }

    @Test
    void finer_shouldLogWithFinerLevelAndPrefix() {
        String message = "This is a finer message.";
        loggingUtil.finer(message);
        verify(mockBukkitLogger).finer(expectedPrefix + message);
    }

    @Test
    void finest_shouldLogWithFinestLevelAndPrefix() {
        String message = "This is a finest message.";
        loggingUtil.finest(message);
        verify(mockBukkitLogger).finest(expectedPrefix + message);
    }

    @Test
    void structuredWarning_shouldEmitJsonPayload() {
        loggingUtil.structuredWarning("crafting", "Unable to craft", Map.of("recipeId", "demo", "player", "Alex"));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockBukkitLogger).log(eq(Level.WARNING), payloadCaptor.capture());
        String payload = payloadCaptor.getValue();
        assertAll(
                () -> org.junit.jupiter.api.Assertions.assertTrue(payload.contains("\"severity\":\"WARNING\"")),
                () -> org.junit.jupiter.api.Assertions.assertTrue(payload.contains("\"event\":\"crafting\"")),
                () -> org.junit.jupiter.api.Assertions.assertTrue(payload.contains("\"recipeId\":\"demo\"")),
                () -> org.junit.jupiter.api.Assertions.assertTrue(payload.contains("\"player\":\"Alex\""))
        );
    }

    @Test
    void structuredError_withThrowable_shouldIncludeContext() {
        RuntimeException throwable = new RuntimeException("boom");
        loggingUtil.structuredError("stats", "Failed to compute", Map.of("stat", "strength"), throwable);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockBukkitLogger).log(eq(Level.SEVERE), payloadCaptor.capture(), eq(throwable));
        String payload = payloadCaptor.getValue();
        assertAll(
                () -> org.junit.jupiter.api.Assertions.assertTrue(payload.contains("\"severity\":\"ERROR\"")),
                () -> org.junit.jupiter.api.Assertions.assertTrue(payload.contains("\"stat\":\"strength\"")),
                () -> org.junit.jupiter.api.Assertions.assertTrue(payload.contains("\"message\":\"Failed to compute\""))
        );
    }
}
