package com.x1f4r.mmocraft.content;

import com.x1f4r.mmocraft.util.LoggingUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class BasicContentPackServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private LoggingUtil loggingUtil;

    private Function<String, InputStream> resourceSupplier;

    @BeforeEach
    void setUpSupplier() {
        ClassLoader classLoader = getClass().getClassLoader();
        resourceSupplier = resource -> classLoader.getResourceAsStream("content/" + resource);
    }

    @Test
    void reloadPacks_whenFirstRunCreatesDefaults_loadsBundledPack() {
        Path contentRoot = tempDir.resolve("content");
        BasicContentPackService service = new BasicContentPackService(contentRoot, resourceSupplier, loggingUtil);

        ContentIndex index = service.reloadPacks();

        assertFalse(service.getLoadedPacks().isEmpty(), "Default pack should load on first run");
        assertTrue(Files.exists(contentRoot.resolve("packs.yml")), "packs.yml should be created");
        assertFalse(index.getCategoryRoots(ContentCategory.ITEMS).isEmpty(), "Item directories should be indexed");
        assertTrue(service.getIssues().isEmpty(), "Default load should be issue free");
    }

    @Test
    void reloadPacks_whenPackDirectoryMissing_createsDirectoryAndWarns() throws IOException {
        Path contentRoot = tempDir.resolve("content");
        BasicContentPackService service = new BasicContentPackService(contentRoot, resourceSupplier, loggingUtil);
        service.reloadPacks();

        deleteRecursively(contentRoot.resolve("default_pack"));
        Files.writeString(contentRoot.resolve("packs.yml"), "packs:\n  - id: custom\n    name: Custom\n    path: custom_pack\n    enabled: true\n");

        service.reloadPacks();

        assertEquals(1, service.getLoadedPacks().size(), "Custom pack should be registered");
        assertTrue(Files.isDirectory(contentRoot.resolve("custom_pack")), "Missing pack directory should be created");
        assertTrue(service.getIssues().stream().anyMatch(issue -> issue.severity() == ContentPackIssue.Severity.WARNING),
                "Missing directory should produce a warning");
    }

    @Test
    void reloadPacks_whenDependencyMissing_reportsError() throws IOException {
        Path contentRoot = tempDir.resolve("content");
        BasicContentPackService service = new BasicContentPackService(contentRoot, resourceSupplier, loggingUtil);
        service.reloadPacks();

        Files.writeString(contentRoot.resolve("packs.yml"), """
                packs:
                  - id: alpha
                    name: Alpha
                    path: alpha
                    enabled: true
                    requires:
                      - beta
                  - id: beta
                    name: Beta
                    path: beta
                    enabled: false
                """.stripIndent());

        service.reloadPacks();

        assertEquals(1, service.getLoadedPacks().size(), "Only enabled packs should load");
        assertTrue(service.getIssues().stream().anyMatch(ContentPackIssue::isError),
                "Missing dependency should be reported as an error");
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.notExists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}
