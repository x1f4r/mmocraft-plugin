package com.x1f4r.mmocraft.pet.model;

import com.x1f4r.mmocraft.playerdata.model.Stat;
import org.bukkit.entity.EntityType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Configuration describing a summonable companion pet.
 */
public record CompanionPetDefinition(String id,
                                     String displayName,
                                     EntityType entityType,
                                     Map<Stat, Double> statBonuses,
                                     boolean invulnerable) {

    public CompanionPetDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Pet id cannot be null or blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Pet display name cannot be null or blank");
        }
        if (entityType == null) {
            throw new IllegalArgumentException("Pet entity type cannot be null");
        }
    }

    public Map<Stat, Double> statBonuses() {
        if (statBonuses == null || statBonuses.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Stat, Double> copy = new EnumMap<>(Stat.class);
        statBonuses.forEach((stat, value) -> {
            if (stat != null && value != null && value != 0.0) {
                copy.put(stat, value);
            }
        });
        return Collections.unmodifiableMap(copy);
    }
}
