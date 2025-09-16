package com.x1f4r.mmocraft.demo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Enumerates the togglable demo features that can be enabled or disabled at runtime.
 */
public enum DemoFeature {
    MASTER("master", List.of("master", "main")),
    ITEMS("items", List.of("items", "item")),
    SKILLS("skills", List.of("skills", "skill")),
    LOOT_TABLES("loot tables", List.of("loot", "loot-table", "loot-tables")),
    CUSTOM_SPAWNS("custom spawns", List.of("spawns", "spawn", "custom-spawns")),
    RESOURCE_NODES("resource nodes", List.of("resource-nodes", "nodes", "resources", "resource")),
    ZONES("zones", List.of("zones", "zone"));

    private final String displayName;
    private final List<String> aliases;

    DemoFeature(String displayName, List<String> aliases) {
        this.displayName = displayName;
        this.aliases = Collections.unmodifiableList(aliases);
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public static Optional<DemoFeature> fromToken(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }
        String normalized = token.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(feature -> feature.aliases.stream().anyMatch(alias -> alias.equalsIgnoreCase(normalized)))
                .findFirst();
    }
}
