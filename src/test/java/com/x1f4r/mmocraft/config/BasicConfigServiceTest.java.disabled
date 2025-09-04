package com.x1f4r.mmocraft.config;

import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
// import java.util.Collections; // Unused
import java.util.List;
// import java.util.logging.Logger; // Unused

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicConfigServiceTest {

    @Mock
    private JavaPlugin mockPlugin;
    @Mock
    private LoggingUtil mockLogger;
    @Mock
    private FileConfiguration mockFileConfiguration;

    @TempDir
    Path tempDir;

    private BasicConfigService configService;
    private File configFile;

    @BeforeEach
    void setUp() {
        when(mockPlugin.getName()).thenReturn("MMOCraftTestPlugin");
        when(mockPlugin.getDataFolder()).thenReturn(tempDir.toFile());
        configFile = new File(tempDir.toFile(), "mmocraft.conf");
        configService = new BasicConfigService(mockPlugin, mockLogger);
    }

    @Test
    void saveDefaultConfig_shouldSaveResource_whenFileDoesNotExist() {
        if (configFile.exists()) configFile.delete();
        new BasicConfigService(mockPlugin, mockLogger);
        verify(mockPlugin).saveResource("mmocraft.conf", false);
    }

    @Test
    void saveDefaultConfig_shouldNotSaveResource_whenFileExists() throws Exception {
        assertTrue(configFile.createNewFile(), "Test config file should be created.");
        new BasicConfigService(mockPlugin, mockLogger);
        verify(mockPlugin, never()).saveResource("mmocraft.conf", false);
    }

    @Test
    void loadConfig_shouldLoadValuesFromFileAndDefaults() throws Exception {
        Path testConfPath = tempDir.resolve("mmocraft.conf");
        String content = "test.value: 123\n" +
                         "list.value:\n" +
                         "  - item1\n" +
                         "  - item2";
        Files.writeString(testConfPath, content);

        String defaultConfigContent = "test.value: 456\n" +
                                      "default.only: true\n" +
                                      "list.value:\n" +
                                      "  - defaultItem";
        InputStream mockDefaultStream = new java.io.ByteArrayInputStream(defaultConfigContent.getBytes());
        when(mockPlugin.getResource("mmocraft.conf")).thenReturn(mockDefaultStream);

        configService = new BasicConfigService(mockPlugin, mockLogger);

        assertEquals(123, configService.getInt("test.value"), "Should get value from file.");
        assertTrue(configService.getBoolean("default.only"), "Should get value from defaults.");
        assertEquals(Arrays.asList("item1", "item2"), configService.getStringList("list.value"), "Should get list from file.");
        verify(mockLogger).debug(contains("Default config values applied"));
    }

    private void writeToConfigFile(String content) throws Exception {
         Files.writeString(configFile.toPath(), content);
         configService = new BasicConfigService(mockPlugin, mockLogger);
    }

    @Test
    void getString_shouldReturnValueFromFile() throws Exception {
        writeToConfigFile("example.string: test_value");
        assertEquals("test_value", configService.getString("example.string"));
    }

    @Test
    void getInt_shouldReturnValueFromFile() throws Exception {
        writeToConfigFile("example.int: 12345");
        assertEquals(12345, configService.getInt("example.int"));
    }

    @Test
    void getDouble_shouldReturnValueFromFile() throws Exception {
        writeToConfigFile("example.double: 123.45");
        assertEquals(123.45, configService.getDouble("example.double"), 0.001);
    }

    @Test
    void getBoolean_shouldReturnValueFromFile() throws Exception {
        writeToConfigFile("example.boolean: true");
        assertTrue(configService.getBoolean("example.boolean"));
    }

    @Test
    void getStringList_shouldReturnValueFromFile() throws Exception {
        writeToConfigFile("example.list:\n  - one\n  - two\n  - three");
        List<String> expected = Arrays.asList("one", "two", "three");
        assertEquals(expected, configService.getStringList("example.list"));
    }

    @Test
    void getString_shouldReturnDefaultValue_whenKeyNotPresent() throws Exception {
        writeToConfigFile("# Empty config");
        String defaultConfigContent = "default.string: default_test_value";
        InputStream mockDefaultStream = new java.io.ByteArrayInputStream(defaultConfigContent.getBytes());
        when(mockPlugin.getResource("mmocraft.conf")).thenReturn(mockDefaultStream);
        configService = new BasicConfigService(mockPlugin, mockLogger);
        assertEquals("default_test_value", configService.getString("default.string"));
    }

    @Test
    void reloadConfig_shouldReloadValues() throws Exception {
        Path tempConf = configFile.toPath();
        Files.writeString(tempConf, "key: value1");
        configService = new BasicConfigService(mockPlugin, mockLogger);
        assertEquals("value1", configService.getString("key"));
        Files.writeString(tempConf, "key: value2");
        configService.reloadConfig();
        assertEquals("value2", configService.getString("key"));
        verify(mockLogger, atLeastOnce()).debug(contains("Default config values applied"));
    }
}
