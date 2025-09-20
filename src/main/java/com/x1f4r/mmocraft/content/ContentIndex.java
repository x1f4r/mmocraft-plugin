package com.x1f4r.mmocraft.content;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregates the directories that hold content for the loaded packs and
 * exposes helper lookup methods for gameplay services.
 */
public final class ContentIndex {

    private static final ContentIndex EMPTY = new ContentIndex(List.of(), Map.of());

    private final List<ContentPack> packs;
    private final Map<ContentCategory, List<Path>> categoryRoots;

    private ContentIndex(List<ContentPack> packs, Map<ContentCategory, List<Path>> categoryRoots) {
        this.packs = packs;
        this.categoryRoots = categoryRoots;
    }

    public static ContentIndex empty() {
        return EMPTY;
    }

    public static ContentIndex fromPacks(List<ContentPack> packs) {
        Objects.requireNonNull(packs, "packs");
        Map<ContentCategory, List<Path>> roots = new EnumMap<>(ContentCategory.class);
        for (ContentCategory category : ContentCategory.values()) {
            roots.put(category, new ArrayList<>());
        }

        for (ContentPack pack : packs) {
            for (ContentCategory category : ContentCategory.values()) {
                Path root = category.resolve(pack.rootDirectory());
                if (Files.isDirectory(root)) {
                    roots.get(category).add(root);
                }
            }
        }

        Map<ContentCategory, List<Path>> immutableRoots = new EnumMap<>(ContentCategory.class);
        for (Map.Entry<ContentCategory, List<Path>> entry : roots.entrySet()) {
            immutableRoots.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return new ContentIndex(List.copyOf(packs), Collections.unmodifiableMap(immutableRoots));
    }

    public List<ContentPack> packs() {
        return packs;
    }

    public List<Path> getCategoryRoots(ContentCategory category) {
        return categoryRoots.getOrDefault(category, List.of());
    }

    public Optional<Path> resolveFirst(ContentCategory category, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return Optional.empty();
        }
        for (Path root : getCategoryRoots(category)) {
            Path candidate = root.resolve(relativePath);
            if (Files.exists(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    public List<Path> listFiles(ContentCategory category, String glob) {
        List<Path> results = new ArrayList<>();
        String pattern = (glob == null || glob.isBlank()) ? "*" : glob;
        for (Path root : getCategoryRoots(category)) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, pattern)) {
                for (Path entry : stream) {
                    results.add(entry);
                }
            } catch (IOException ignored) {
                // Intentionally ignored; diagnostics will report unreadable directories separately.
            }
        }
        return results;
    }
}
