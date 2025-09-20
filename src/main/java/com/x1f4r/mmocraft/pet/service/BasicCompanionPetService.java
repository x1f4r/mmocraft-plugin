package com.x1f4r.mmocraft.pet.service;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.pet.model.ActiveCompanionPet;
import com.x1f4r.mmocraft.pet.model.CompanionPetDefinition;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.runtime.PlayerRuntimeAttributeService;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.util.StringUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory implementation of {@link CompanionPetService}.
 */
public class BasicCompanionPetService implements CompanionPetService {

    private static final String STAT_SOURCE_PREFIX = "pet:";
    private static final String METADATA_KEY = "MMOCRAFT_COMPANION";

    private final MMOCraftPlugin plugin;
    private final PlayerDataService playerDataService;
    private final PlayerRuntimeAttributeService runtimeAttributeService;
    private final LoggingUtil logger;
    private final Map<UUID, ActiveCompanionPet> activePets = new ConcurrentHashMap<>();

    public BasicCompanionPetService(MMOCraftPlugin plugin,
                                    PlayerDataService playerDataService,
                                    PlayerRuntimeAttributeService runtimeAttributeService,
                                    LoggingUtil logger) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
        this.runtimeAttributeService = runtimeAttributeService;
        this.logger = logger;
    }

    @Override
    public void summonPet(Player player, CompanionPetDefinition definition) {
        if (player == null || definition == null) {
            return;
        }
        dismissPet(player);
        PlayerProfile profile = playerDataService.getPlayerProfile(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(StringUtil.colorize("&cUnable to summon a pet while your profile is loading."));
            return;
        }
        LivingEntity entity = spawnPetEntity(player, definition);
        if (entity == null) {
            player.sendMessage(StringUtil.colorize("&cFailed to summon your companion."));
            return;
        }
        String statSourceKey = STAT_SOURCE_PREFIX + definition.id();
        applyStatBonuses(profile, definition.statBonuses(), statSourceKey);
        runtimeAttributeService.syncPlayer(player);
        activePets.put(player.getUniqueId(), new ActiveCompanionPet(player.getUniqueId(), entity, definition, statSourceKey));
        player.sendMessage(StringUtil.colorize("&a" + definition.displayName() + " answers your call."));
    }

    @Override
    public void dismissPet(Player player) {
        if (player == null) {
            return;
        }
        dismissPet(player.getUniqueId());
    }

    @Override
    public void dismissPet(UUID playerId) {
        if (playerId == null) {
            return;
        }
        ActiveCompanionPet active = activePets.remove(playerId);
        if (active == null) {
            return;
        }
        Entity entity = active.entity();
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
        PlayerProfile profile = playerDataService.getPlayerProfile(playerId);
        if (profile != null) {
            profile.clearTemporaryStatModifiers(active.statSourceKey());
            Player owner = Bukkit.getPlayer(playerId);
            if (owner != null && owner.isOnline()) {
                runtimeAttributeService.syncPlayer(owner);
            }
        }
    }

    @Override
    public Optional<ActiveCompanionPet> getActivePet(UUID playerId) {
        return Optional.ofNullable(activePets.get(playerId));
    }

    @Override
    public void tick() {
        for (ActiveCompanionPet active : activePets.values()) {
            Player owner = Bukkit.getPlayer(active.ownerId());
            if (owner == null || !owner.isOnline()) {
                dismissPet(active.ownerId());
                continue;
            }
            LivingEntity pet = active.entity();
            if (pet == null || pet.isDead()) {
                dismissPet(active.ownerId());
                continue;
            }
            keepPetNearOwner(owner, pet);
        }
    }

    private void keepPetNearOwner(Player owner, LivingEntity pet) {
        Location ownerLocation = owner.getLocation();
        double distanceSquared = pet.getLocation().distanceSquared(ownerLocation);
        if (distanceSquared > 100) {
            pet.teleport(ownerLocation.add(0.5, 0, 0.5));
            return;
        }
        if (distanceSquared > 16) {
            Vector direction = ownerLocation.toVector().subtract(pet.getLocation().toVector()).normalize().multiply(0.35);
            pet.setVelocity(direction);
        }
    }

    private LivingEntity spawnPetEntity(Player owner, CompanionPetDefinition definition) {
        Location spawnLocation = owner.getLocation().clone().add(owner.getLocation().getDirection().normalize().multiply(0.8));
        LivingEntity entity;
        try {
            entity = (LivingEntity) owner.getWorld().spawnEntity(spawnLocation, definition.entityType());
        } catch (Exception ex) {
            logger.severe("Failed to spawn companion entity of type " + definition.entityType() + ": " + ex.getMessage(), ex);
            return null;
        }
        entity.setCustomNameVisible(true);
        entity.customName(LegacyComponentSerializer.legacyAmpersand().deserialize(definition.displayName()));
        entity.setPersistent(false);
        entity.setRemoveWhenFarAway(false);
        entity.setAI(true);
        entity.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));
        if (definition.invulnerable()) {
            entity.setInvulnerable(true);
            AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(Math.max(maxHealth.getBaseValue(), 40.0));
                entity.setHealth(maxHealth.getValue());
            }
        }
        if (entity instanceof Tameable tameable) {
            tameable.setTamed(true);
            tameable.setOwner(owner);
        }
        return entity;
    }

    private void applyStatBonuses(PlayerProfile profile, Map<Stat, Double> bonuses, String sourceKey) {
        if (bonuses == null || bonuses.isEmpty()) {
            profile.clearTemporaryStatModifiers(sourceKey);
            return;
        }
        Map<Stat, Double> copy = new EnumMap<>(Stat.class);
        bonuses.forEach((stat, value) -> {
            if (stat != null && value != null && value != 0.0) {
                copy.put(stat, value);
            }
        });
        profile.setTemporaryStatModifiers(sourceKey, copy);
    }

    @Override
    public void handlePlayerQuit(UUID playerId) {
        dismissPet(playerId);
    }

    @Override
    public void handlePlayerDeath(UUID playerId) {
        dismissPet(playerId);
    }

    @Override
    public void shutdown() {
        activePets.keySet().forEach(this::dismissPet);
        activePets.clear();
    }
}
