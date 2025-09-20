package com.x1f4r.mmocraft.pet.service;

import com.x1f4r.mmocraft.pet.model.ActiveCompanionPet;
import com.x1f4r.mmocraft.pet.model.CompanionPetDefinition;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Manages lifecycle of companion pets bound to players.
 */
public interface CompanionPetService {

    void summonPet(Player player, CompanionPetDefinition definition);

    void dismissPet(Player player);

    void dismissPet(UUID playerId);

    Optional<ActiveCompanionPet> getActivePet(UUID playerId);

    void tick();

    void handlePlayerQuit(UUID playerId);

    void handlePlayerDeath(UUID playerId);

    void shutdown();
}
