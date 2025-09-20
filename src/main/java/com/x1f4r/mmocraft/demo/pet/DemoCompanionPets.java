package com.x1f4r.mmocraft.demo.pet;

import com.x1f4r.mmocraft.pet.model.CompanionPetDefinition;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import org.bukkit.entity.EntityType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Static helper for demo companion pet definitions.
 */
public final class DemoCompanionPets {

    public static final String LUCKY_SPRITE_ID = "lucky_sprite";

    private DemoCompanionPets() {
    }

    public static CompanionPetDefinition luckySprite() {
        Map<Stat, Double> bonuses = new EnumMap<>(Stat.class);
        bonuses.put(Stat.MAGIC_FIND, 30.0);
        bonuses.put(Stat.PET_LUCK, 60.0);
        bonuses.put(Stat.MANA_REGEN, 5.0);
        return new CompanionPetDefinition(
                LUCKY_SPRITE_ID,
                "&dLucky Sprite",
                EntityType.ALLAY,
                bonuses,
                true
        );
    }
}

