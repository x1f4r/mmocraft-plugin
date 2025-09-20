package com.x1f4r.mmocraft.item.model;

import java.util.Objects;

/**
 * Describes an active or passive ability that can be attached to a custom item.
 * These descriptors drive tooltip generation, admin summaries, and NBT metadata
 * so that gameplay systems can discover which skills/items are linked together.
 */
public record ItemAbilityDescriptor(String abilityId,
                                    String displayName,
                                    String activationHint,
                                    String summary,
                                    double manaCost,
                                    double cooldownSeconds) {

    public ItemAbilityDescriptor {
        Objects.requireNonNull(abilityId, "abilityId");
        Objects.requireNonNull(displayName, "displayName");
        activationHint = activationHint == null ? "" : activationHint;
        summary = summary == null ? "" : summary;
    }

    /**
     * @return {@code true} if the descriptor includes descriptive text.
     */
    public boolean hasSummary() {
        return summary != null && !summary.isBlank();
    }

    /**
     * @return {@code true} if the ability consumes mana when activated.
     */
    public boolean consumesMana() {
        return manaCost > 0.0;
    }

    /**
     * @return {@code true} if the ability applies a cooldown on use.
     */
    public boolean hasCooldown() {
        return cooldownSeconds > 0.0;
    }
}
