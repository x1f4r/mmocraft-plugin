package com.x1f4r.mmocraft.demo;

import com.x1f4r.mmocraft.config.gameplay.DemoContentConfig;
import com.x1f4r.mmocraft.util.LoggingUtil;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable configuration snapshot describing which demo features should be active.
 */
public record DemoContentSettings(
        boolean masterEnabled,
        boolean itemsEnabled,
        boolean skillsEnabled,
        boolean lootTablesEnabled,
        boolean customSpawnsEnabled,
        boolean resourceNodesEnabled,
        boolean zonesEnabled
) {

    public static DemoContentSettings disabled() {
        return new DemoContentSettings(false, false, false, false, false, false, false);
    }

    public static DemoContentSettings fromDemoConfig(DemoContentConfig demoConfig, LoggingUtil logger) {
        if (demoConfig == null) {
            if (logger != null) {
                logger.warning("DemoContentConfig missing while loading demo settings. Falling back to disabled state.");
            }
            return disabled();
        }
        DemoContentConfig.DemoToggles toggles = demoConfig.getToggles();
        boolean master = toggles.master();
        DemoContentSettings settings = new DemoContentSettings(
                master,
                master && toggles.items(),
                master && toggles.skills(),
                master && toggles.lootTables(),
                master && toggles.customSpawns(),
                master && toggles.resourceNodes(),
                master && toggles.zones()
        );
        if (logger != null) {
            if (!settings.masterEnabled()) {
                logger.info("Demo content disabled via configuration.");
            } else {
                logger.info("Demo content enabled for: " + settings.describeEnabledFeatures());
            }
        }
        return settings;
    }

    public DemoContentSettings withFeature(DemoFeature feature, boolean enabled) {
        Objects.requireNonNull(feature, "feature");
        return switch (feature) {
            case MASTER -> withMasterEnabled(enabled);
            case ITEMS -> new DemoContentSettings(masterEnabled, enabled, skillsEnabled, lootTablesEnabled,
                    customSpawnsEnabled, resourceNodesEnabled, zonesEnabled);
            case SKILLS -> new DemoContentSettings(masterEnabled, itemsEnabled, enabled, lootTablesEnabled,
                    customSpawnsEnabled, resourceNodesEnabled, zonesEnabled);
            case LOOT_TABLES -> new DemoContentSettings(masterEnabled, itemsEnabled, skillsEnabled, enabled,
                    customSpawnsEnabled, resourceNodesEnabled, zonesEnabled);
            case CUSTOM_SPAWNS -> new DemoContentSettings(masterEnabled, itemsEnabled, skillsEnabled, lootTablesEnabled,
                    enabled, resourceNodesEnabled, zonesEnabled);
            case RESOURCE_NODES -> new DemoContentSettings(masterEnabled, itemsEnabled, skillsEnabled, lootTablesEnabled,
                    customSpawnsEnabled, enabled, zonesEnabled);
            case ZONES -> new DemoContentSettings(masterEnabled, itemsEnabled, skillsEnabled, lootTablesEnabled,
                    customSpawnsEnabled, resourceNodesEnabled, enabled);
        };
    }

    public DemoContentSettings withMasterEnabled(boolean enabled) {
        return new DemoContentSettings(enabled, itemsEnabled, skillsEnabled, lootTablesEnabled,
                customSpawnsEnabled, resourceNodesEnabled, zonesEnabled);
    }

    public DemoContentSettings withAllFeatures(boolean enabled) {
        return new DemoContentSettings(enabled,
                enabled,
                enabled,
                enabled,
                enabled,
                enabled,
                enabled);
    }

    public boolean featureEnabled(DemoFeature feature) {
        Objects.requireNonNull(feature, "feature");
        return switch (feature) {
            case MASTER -> masterEnabled;
            case ITEMS -> itemsEnabled;
            case SKILLS -> skillsEnabled;
            case LOOT_TABLES -> lootTablesEnabled;
            case CUSTOM_SPAWNS -> customSpawnsEnabled;
            case RESOURCE_NODES -> resourceNodesEnabled;
            case ZONES -> zonesEnabled;
        };
    }

    public boolean hasAnyFeatureEnabled() {
        return itemsEnabled || skillsEnabled || lootTablesEnabled || customSpawnsEnabled || resourceNodesEnabled || zonesEnabled;
    }

    public String describeEnabledFeatures() {
        if (!masterEnabled) {
            return "none";
        }
        List<String> enabledFeatures = new ArrayList<>();
        if (itemsEnabled) enabledFeatures.add("items");
        if (skillsEnabled) enabledFeatures.add("skills");
        if (lootTablesEnabled) enabledFeatures.add("loot tables");
        if (customSpawnsEnabled) enabledFeatures.add("custom spawns");
        if (resourceNodesEnabled) enabledFeatures.add("resource nodes");
        if (zonesEnabled) enabledFeatures.add("zones");
        return enabledFeatures.isEmpty() ? "none" : String.join(", ", enabledFeatures);
    }

    public Map<DemoFeature, Boolean> asFeatureMap() {
        Map<DemoFeature, Boolean> map = new EnumMap<>(DemoFeature.class);
        for (DemoFeature feature : DemoFeature.values()) {
            map.put(feature, featureEnabled(feature));
        }
        return map;
    }
}
