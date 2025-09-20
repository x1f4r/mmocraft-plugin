package com.x1f4r.mmocraft.content;

import java.nio.file.Path;

/**
 * Enumerates the root directories that compose an MMOCraft content pack.
 */
public enum ContentCategory {

    ITEMS("items"),
    ABILITIES("abilities"),
    CRAFTING("crafting"),
    COMBAT("combat"),
    GATHERING("gathering"),
    ECONOMY("economy"),
    PROGRESSION("progression"),
    WORLD("world"),
    UI("ui");

    private final String directoryName;

    ContentCategory(String directoryName) {
        this.directoryName = directoryName;
    }

    public String directoryName() {
        return directoryName;
    }

    public Path resolve(Path packRoot) {
        return packRoot.resolve(directoryName);
    }
}
