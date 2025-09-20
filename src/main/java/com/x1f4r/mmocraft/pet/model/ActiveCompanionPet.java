package com.x1f4r.mmocraft.pet.model;

import org.bukkit.entity.LivingEntity;

import java.util.UUID;

/**
 * Runtime association between a player and their active companion pet.
 */
public record ActiveCompanionPet(UUID ownerId, LivingEntity entity, CompanionPetDefinition definition, String statSourceKey) {
}
