package com.x1f4r.mmocraft.content;

import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * File-system backed content pack loader that materialises the bundled reference
 * content on first run and aggregates all enabled packs into a {@link ContentIndex}.
 */
public class BasicContentPackService implements ContentPackService {

    private static final List<String> BUNDLED_RESOURCES = List.of(
            "packs.yml",
            "default_pack/pack.yml",
            "default_pack/items/weapons.yml",
            "default_pack/items/armor.yml",
            "default_pack/items/tools.yml",
            "default_pack/items/consumables.yml",
            "default_pack/abilities/active.yml",
            "default_pack/abilities/passive.yml",
            "default_pack/crafting/recipes.toml",
            "default_pack/gathering/mining.yml",
            "default_pack/gathering/farming.yml",
            "default_pack/gathering/foraging.yml",
            "default_pack/combat/mob_families.yml",
            "default_pack/combat/encounters.yml",
            "default_pack/economy/vendors.yml",
            "default_pack/economy/auction.yml",
            "default_pack/economy/currency.yml",
            "default_pack/progression/skills.yml",
            "default_pack/progression/quests.yml",
            "default_pack/progression/achievements.yml",
            "default_pack/world/islands.yml",
            "default_pack/world/instanced.yml",
            "default_pack/ui/crafting_book.yml",
            "default_pack/ui/localization.yml"
    );

    private final Path contentRoot;
    private final Function<String, InputStream> resourceSupplier;
    private final LoggingUtil logger;

    private final List<ContentPackIssue> issues = new ArrayList<>();
    private List<ContentPack> loadedPacks = List.of();
    private ContentIndex contentIndex = ContentIndex.empty();

    public BasicContentPackService(Path contentRoot,
                                   Function<String, InputStream> resourceSupplier,
                                   LoggingUtil logger) {
        this.contentRoot = Objects.requireNonNull(contentRoot, "contentRoot").toAbsolutePath().normalize();
        this.resourceSupplier = Objects.requireNonNull(resourceSupplier, "resourceSupplier");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public synchronized ContentIndex reloadPacks() {
        issues.clear();
        ensureBaseStructure();

        Path packsFile = contentRoot.resolve("packs.yml");
        if (!Files.exists(packsFile)) {
            recordIssue(ContentPackIssue.error("Missing packs.yml", packsFile.toString()));
            loadedPacks = List.of();
            contentIndex = ContentIndex.empty();
            return contentIndex;
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(packsFile.toFile());
        List<Map<?, ?>> packDefinitions = configuration.getMapList("packs");
        if (packDefinitions == null || packDefinitions.isEmpty()) {
            recordIssue(ContentPackIssue.error("packs.yml does not define any content packs", packsFile.toString()));
            loadedPacks = List.of();
            contentIndex = ContentIndex.empty();
            return contentIndex;
        }

        List<ContentPack> packs = new ArrayList<>();
        Set<String> identifiers = new HashSet<>();

        for (Map<?, ?> rawDefinition : packDefinitions) {
            if (rawDefinition == null) {
                continue;
            }
            String id = asString(rawDefinition.get("id"));
            if (id == null || id.isBlank()) {
                recordIssue(ContentPackIssue.error("Encountered content pack with missing id", null));
                continue;
            }
            if (!identifiers.add(id.toLowerCase(Locale.ROOT))) {
                recordIssue(ContentPackIssue.error("Duplicate content pack id detected", id));
                continue;
            }

            boolean enabled = asBoolean(rawDefinition.get("enabled"), true);
            if (!enabled) {
                recordIssue(ContentPackIssue.info("Content pack disabled", id));
                continue;
            }

            String name = asString(rawDefinition.get("name"));
            if (name == null || name.isBlank()) {
                name = id;
            }
            String version = asString(rawDefinition.get("version"));
            if (version == null || version.isBlank()) {
                version = "0.0.0";
            }
            String pathValue = asString(rawDefinition.get("path"));
            if (pathValue == null || pathValue.isBlank()) {
                pathValue = id;
            }
            Path packRoot = contentRoot.resolve(pathValue).normalize();
            if (!packRoot.startsWith(contentRoot)) {
                recordIssue(ContentPackIssue.error("Content pack path escapes content root", packRoot.toString()));
                continue;
            }
            if (Files.notExists(packRoot)) {
                try {
                    Files.createDirectories(packRoot);
                    recordIssue(ContentPackIssue.warning("Created missing directory for content pack", packRoot.toString()));
                } catch (IOException ioException) {
                    recordIssue(ContentPackIssue.error("Failed to create directory for content pack", packRoot + " :: " + ioException.getMessage()));
                    continue;
                }
            }
            if (!Files.isDirectory(packRoot)) {
                recordIssue(ContentPackIssue.error("Content pack path is not a directory", packRoot.toString()));
                continue;
            }
            int priority = asInt(rawDefinition.get("priority"), 0);
            List<String> requires = asStringList(rawDefinition.get("requires"));
            String description = asString(rawDefinition.get("description"));

            ContentPack pack = new ContentPack(id, name, version, priority, packRoot, requires, description);
            packs.add(pack);
            logger.structuredInfo("content_pack.loaded", "Loaded content pack", Map.of(
                    "id", pack.id(),
                    "name", pack.displayName(),
                    "version", pack.version(),
                    "priority", Integer.toString(pack.priority()),
                    "path", pack.rootDirectory()
            ));
        }

        packs.sort((a, b) -> {
            int priorityCompare = Integer.compare(b.priority(), a.priority());
            return priorityCompare != 0 ? priorityCompare : a.id().compareToIgnoreCase(b.id());
        });

        Set<String> loadedIdentifiers = packs.stream()
                .map(ContentPack::normalizedId)
                .collect(Collectors.toCollection(HashSet::new));
        for (ContentPack pack : packs) {
            List<String> normalizedRequires = pack.normalizedRequires();
            List<String> originalRequires = pack.requires();
            for (int i = 0; i < normalizedRequires.size(); i++) {
                String requirement = normalizedRequires.get(i);
                if (!loadedIdentifiers.contains(requirement)) {
                    String original = originalRequires.get(i);
                    recordIssue(ContentPackIssue.error(
                            "Content pack '" + pack.id() + "' requires missing pack '" + original + "'",
                            "Enable the dependency or correct the requires entry."));
                }
            }
        }

        loadedPacks = List.copyOf(packs);
        contentIndex = ContentIndex.fromPacks(loadedPacks);

        logger.structuredInfo("content_pack.summary", "Content pack reload complete", Map.of(
                "loadedPacks", Integer.toString(loadedPacks.size()),
                "issues", Integer.toString(issues.size()),
                "root", contentRoot
        ));

        return contentIndex;
    }

    @Override
    public ContentIndex getContentIndex() {
        return contentIndex;
    }

    @Override
    public List<ContentPack> getLoadedPacks() {
        return loadedPacks;
    }

    @Override
    public List<ContentPackIssue> getIssues() {
        return List.copyOf(issues);
    }

    @Override
    public Path getContentRoot() {
        return contentRoot;
    }

    private void ensureBaseStructure() {
        try {
            Files.createDirectories(contentRoot);
        } catch (IOException e) {
            recordIssue(ContentPackIssue.error("Failed to create content root directory", contentRoot + " :: " + e.getMessage()));
            return;
        }

        for (String resource : BUNDLED_RESOURCES) {
            Path target = contentRoot.resolve(resource);
            if (Files.exists(target)) {
                continue;
            }
            try (InputStream in = resourceSupplier.apply(resource)) {
                if (in == null) {
                    recordIssue(ContentPackIssue.warning("Missing bundled content resource", resource));
                    continue;
                }
                Files.createDirectories(target.getParent());
                Files.copy(in, target);
                logger.debug("Deployed default content resource: " + resource);
            } catch (IOException ioException) {
                recordIssue(ContentPackIssue.error("Failed to write default content resource", resource + " :: " + ioException.getMessage()));
            }
        }
    }

    private void recordIssue(ContentPackIssue issue) {
        issues.add(issue);
        String detail = issue.detail();
        String message = issue.message() + (detail == null || detail.isBlank() ? "" : " (" + detail + ")");
        switch (issue.severity()) {
            case ERROR -> logger.severe("Content pack issue: " + message);
            case WARNING -> logger.warning("Content pack issue: " + message);
            default -> logger.info("Content pack note: " + message);
        }
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value).trim();
    }

    private boolean asBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }
        return defaultValue;
    }

    private int asInt(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private List<String> asStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object element : list) {
                String elementString = asString(element);
                if (elementString != null && !elementString.isBlank()) {
                    result.add(elementString);
                }
            }
            return result;
        }
        String single = asString(value);
        return single == null ? List.of() : List.of(single);
    }
}
