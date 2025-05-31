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
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicConfigServiceTest {

    @Mock
    private JavaPlugin mockPlugin;
    @Mock
    private LoggingUtil mockLogger; // Using our LoggingUtil for consistency
    @Mock
    private FileConfiguration mockFileConfiguration;

    @TempDir
    Path tempDir; // For simulating plugin data folder

    private BasicConfigService configService;
    private File configFile;

    @BeforeEach
    void setUp() {
        // Mock JavaPlugin behavior
        when(mockPlugin.getName()).thenReturn("MMOCraftTestPlugin");
        // Simulate data folder using @TempDir
        when(mockPlugin.getDataFolder()).thenReturn(tempDir.toFile());

        // The BasicConfigService constructor will try to create the configFile object.
        // It then calls saveDefaultConfig() and loadConfig().
        configFile = new File(tempDir.toFile(), "mmocraft.conf");

        // We need to ensure that when loadConfiguration is called on YamlConfiguration,
        // it returns our mockFileConfiguration for the specific configFile.
        // This is tricky because loadConfiguration is static.
        // A common approach is to have a real file or use PowerMockito for static methods,
        // but for unit tests, we try to avoid PowerMock if possible.

        // Alternative: We can't easily mock YamlConfiguration.loadConfiguration(file).
        // So, we will let it create a real (empty) FileConfiguration for the temp file.
        // Then, in tests, we'll interact with this real (but initially empty) config.
        // For verifying defaults, we can mock getResource.

        // For most getter tests, we'll directly use the `config` field in BasicConfigService
        // and replace it with our mockFileConfiguration *after* the constructor has run.
        // This means saveDefaultConfig and initial loadConfig might run with a real empty config.

        configService = new BasicConfigService(mockPlugin, mockLogger);
        // Now, replace the internally created FileConfiguration with our mock for getter tests
        // This is a bit of a workaround due to the static method.
        // A better way would be to refactor BasicConfigService to allow injecting FileConfiguration,
        // but for now, we'll use this approach.
        // Let's assume BasicConfigService has a field `this.config` of type FileConfiguration.
        // We can't directly access it.

        // Let's stick to testing behavior:
        // 1. saveDefaultConfig: verify plugin.saveResource is called if file doesn't exist.
        // 2. loadConfig: verify defaults are set if resource exists.
        // 3. getters: for this, we need to control what FileConfiguration returns.
        //    Since we can't easily swap out the FileConfiguration instance after constructor,
        //    we'll make the configFile actually contain some data for some tests.
    }

    @Test
    void saveDefaultConfig_shouldSaveResource_whenFileDoesNotExist() {
        // Ensure configFile does not exist (setUp in @TempDir should ensure this initially)
        if (configFile.exists()) configFile.delete(); // Make sure

        // Re-run constructor part that calls saveDefaultConfig
        // For this specific test, we want to control the existence of the file *before* constructor.
        // So, this test is slightly different.
        new BasicConfigService(mockPlugin, mockLogger); // This will call saveDefaultConfig

        verify(mockPlugin).saveResource("mmocraft.conf", false);
        // verify(mockLogger).info(contains("Default 'mmocraft.conf' copied")); // If logging is successful
    }

    @Test
    void saveDefaultConfig_shouldNotSaveResource_whenFileExists() throws Exception {
        // Create the file to simulate it existing
        assertTrue(configFile.createNewFile(), "Test config file should be created.");

        new BasicConfigService(mockPlugin, mockLogger);

        verify(mockPlugin, never()).saveResource("mmocraft.conf", false);
    }

    @Test
    void loadConfig_shouldLoadValuesFromFileAndDefaults() throws Exception {
        // Prepare a dummy mmocraft.conf in the tempDir
        Path testConfPath = tempDir.resolve("mmocraft.conf");
        String content = "test.value: 123\n" +
                         "list.value:\n" +
                         "  - item1\n" +
                         "  - item2";
        Files.writeString(testConfPath, content);

        // Simulate default values from JAR resource
        // Create a dummy InputStream for the default config
        String defaultConfigContent = "test.value: 456\n" + // This will be overridden by file
                                      "default.only: true\n" +
                                      "list.value:\n" + // This will also be overridden
                                      "  - defaultItem";
        InputStream mockDefaultStream = new java.io.ByteArrayInputStream(defaultConfigContent.getBytes());
        when(mockPlugin.getResource("mmocraft.conf")).thenReturn(mockDefaultStream);

        // Re-initialize service to pick up the file and defaults
        configService = new BasicConfigService(mockPlugin, mockLogger);

        assertEquals(123, configService.getInt("test.value"), "Should get value from file.");
        assertTrue(configService.getBoolean("default.only"), "Should get value from defaults.");
        assertEquals(Arrays.asList("item1", "item2"), configService.getStringList("list.value"), "Should get list from file.");

        // Verify logging for defaults
        verify(mockLogger).debug(contains("Default config values applied"));
    }


    // For the following getter tests, we need a way to mock what the FileConfiguration returns.
    // The current BasicConfigService directly uses YamlConfiguration.loadConfiguration(file).
    // To properly unit test getters without relying on an actual file, BasicConfigService
    // would ideally allow injection of a FileConfiguration object, or a factory for it.
    // Since that's a larger refactor, these tests will be more integration-like for now,
    // relying on a real (temporary) config file created with specific content.

    private void writeToConfigFile(String content) throws Exception {
         Files.writeString(configFile.toPath(), content);
         // Force a reload to pick up changes (or re-initialize for test isolation)
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
        // Ensure config file is empty or doesn't contain the key
        writeToConfigFile("# Empty config");
        // Setup defaults through mocked resource stream
        String defaultConfigContent = "default.string: default_test_value";
        InputStream mockDefaultStream = new java.io.ByteArrayInputStream(defaultConfigContent.getBytes());
        when(mockPlugin.getResource("mmocraft.conf")).thenReturn(mockDefaultStream);

        // Re-initialize to load defaults
        configService = new BasicConfigService(mockPlugin, mockLogger);

        assertEquals("default_test_value", configService.getString("default.string"));
    }

    @Test
    void reloadConfig_shouldReloadValues() throws Exception {
        writeToConfigFile("reload.test: initial_value");
        assertEquals("initial_value", configService.getString("reload.test"));

        writeToConfigFile("reload.test: reloaded_value");
        // configService.reloadConfig(); // This calls loadConfig which includes logging
        // No, the above line is wrong. writeToConfigFile already re-initializes.
        // To test reloadConfig() itself:

        // 1. Initial state
        Path tempConf = configFile.toPath();
        Files.writeString(tempConf, "key: value1");
        configService = new BasicConfigService(mockPlugin, mockLogger);
        assertEquals("value1", configService.getString("key"));

        // 2. Change file content externally
        Files.writeString(tempConf, "key: value2");

        // 3. Call reload
        configService.reloadConfig();

        // 4. Assert new value
        assertEquals("value2", configService.getString("key"));
        verify(mockLogger, atLeastOnce()).debug(contains("Default config values applied")); // from loadConfig
    }
}
