package com.x1f4r.mmocraft.content;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Describes a single content pack loaded from the filesystem.
 */
public record ContentPack(
        String id,
        String displayName,
        String version,
        int priority,
        Path rootDirectory,
        List<String> requires,
        String description
) {

    public ContentPack {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(rootDirectory, "rootDirectory");
        requires = List.copyOf(requires == null ? List.of() : requires);
        description = description == null ? "" : description;
    }

    public String normalizedId() {
        return id.toLowerCase(Locale.ROOT);
    }

    public List<String> normalizedRequires() {
        return requires.stream()
                .map(require -> require.toLowerCase(Locale.ROOT))
                .toList();
    }
}
