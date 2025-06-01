package com.x1f4r.mmocraft.world.resourcegathering.model;

import org.bukkit.Material;
import java.util.Collections;
import java.util.Set;
import java.util.Objects;

public class ResourceNodeType {

    private final String typeId;
    private final Material displayMaterial;
    private final double breakTimeSeconds;
    private final Set<Material> requiredToolTypes;
    private final String lootTableId;
    private final int respawnTimeSeconds;
    private final String customName; // Optional

    public ResourceNodeType(String typeId, Material displayMaterial, double breakTimeSeconds,
                            Set<Material> requiredToolTypes, String lootTableId, int respawnTimeSeconds,
                            String customName) {
        this.typeId = Objects.requireNonNull(typeId, "typeId cannot be null");
        if (typeId.trim().isEmpty()) {
            throw new IllegalArgumentException("typeId cannot be empty");
        }
        this.displayMaterial = Objects.requireNonNull(displayMaterial, "displayMaterial cannot be null");
        if (breakTimeSeconds <= 0) {
            throw new IllegalArgumentException("breakTimeSeconds must be positive");
        }
        this.breakTimeSeconds = breakTimeSeconds;
        this.requiredToolTypes = requiredToolTypes != null ? Collections.unmodifiableSet(Set.copyOf(requiredToolTypes)) : Collections.emptySet();
        this.lootTableId = Objects.requireNonNull(lootTableId, "lootTableId cannot be null");
        if (respawnTimeSeconds <= 0) {
            throw new IllegalArgumentException("respawnTimeSeconds must be positive");
        }
        this.respawnTimeSeconds = respawnTimeSeconds;
        this.customName = customName; // Can be null
    }

    // Constructor without customName
    public ResourceNodeType(String typeId, Material displayMaterial, double breakTimeSeconds,
                            Set<Material> requiredToolTypes, String lootTableId, int respawnTimeSeconds) {
        this(typeId, displayMaterial, breakTimeSeconds, requiredToolTypes, lootTableId, respawnTimeSeconds, null);
    }

    public String getTypeId() {
        return typeId;
    }

    public Material getDisplayMaterial() {
        return displayMaterial;
    }

    public double getBreakTimeSeconds() {
        return breakTimeSeconds;
    }

    public Set<Material> getRequiredToolTypes() {
        return requiredToolTypes;
    }

    public String getLootTableId() {
        return lootTableId;
    }

    public int getRespawnTimeSeconds() {
        return respawnTimeSeconds;
    }

    public String getCustomName() {
        return customName;
    }

    public String getEffectiveName() {
        return customName != null ? customName : typeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceNodeType that = (ResourceNodeType) o;
        return typeId.equals(that.typeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeId);
    }

    @Override
    public String toString() {
        return "ResourceNodeType{" +
               "typeId='" + typeId + '\'' +
               ", displayMaterial=" + displayMaterial +
               ", breakTimeSeconds=" + breakTimeSeconds +
               ", requiredToolTypes=" + requiredToolTypes +
               ", lootTableId='" + lootTableId + '\'' +
               ", respawnTimeSeconds=" + respawnTimeSeconds +
               ", customName='" + customName + '\'' +
               '}';
    }
}
