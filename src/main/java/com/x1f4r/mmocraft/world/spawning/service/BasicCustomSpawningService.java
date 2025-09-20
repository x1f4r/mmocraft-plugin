package com.x1f4r.mmocraft.world.spawning.service;

import com.x1f4r.mmocraft.config.gameplay.GameplayConfigService;
import com.x1f4r.mmocraft.config.gameplay.RuntimeStatConfig;
import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.combat.service.MobStatProvider;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.loot.service.LootService;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.world.spawning.model.CustomSpawnRule;
import com.x1f4r.mmocraft.world.spawning.model.MobSpawnDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class BasicCustomSpawningService implements CustomSpawningService {

    private final MMOCraftPlugin plugin;
    private final LoggingUtil logger;
    private final MobStatProvider mobStatProvider;
    private final LootService lootService;
    private final CustomItemRegistry customItemRegistry;
    private final PlayerDataService playerDataService;
    private final GameplayConfigService gameplayConfigService;

    private final List<CustomSpawnRule> spawnRules = new CopyOnWriteArrayList<>();

    public static final String METADATA_KEY_CUSTOM_MOB_ID = "MMOCRAFT_CUSTOM_MOB_ID";
    public static final String METADATA_KEY_LOOT_TABLE_ID = "MMOCRAFT_LOOT_TABLE_ID";
    public static final String METADATA_KEY_SCALED_ATTACK = "MMOCRAFT_SCALED_ATTACK";
    public static final String METADATA_KEY_SCALED_DEFENSE = "MMOCRAFT_SCALED_DEFENSE";
    public static final String METADATA_KEY_SCALED_MAX_HEALTH = "MMOCRAFT_SCALED_MAX_HEALTH";

    public BasicCustomSpawningService(MMOCraftPlugin plugin,
                                      LoggingUtil logger,
                                      MobStatProvider mobStatProvider,
                                      LootService lootService,
                                      CustomItemRegistry customItemRegistry,
                                      PlayerDataService playerDataService,
                                      GameplayConfigService gameplayConfigService) {
        this.plugin = plugin;
        this.logger = logger;
        this.mobStatProvider = mobStatProvider;
        this.lootService = lootService;
        this.customItemRegistry = customItemRegistry;
        this.playerDataService = playerDataService;
        this.gameplayConfigService = gameplayConfigService;
        logger.debug("BasicCustomSpawningService initialized.");
    }

    @Override
    public void registerRule(CustomSpawnRule rule) {
        if (rule == null || rule.getRuleId() == null || rule.getRuleId().trim().isEmpty()) {
            logger.warning("Attempted to register null rule or rule with invalid ID.");
            return;
        }
        if (spawnRules.stream().anyMatch(r -> r.getRuleId().equals(rule.getRuleId()))) {
            logger.warning("Spawn rule with ID '" + rule.getRuleId() + "' already exists. Skipping registration.");
            return;
        }
        spawnRules.add(rule);
        logger.info("Registered custom spawn rule: " + rule.getRuleId() + " for mob type " + rule.getMobSpawnDefinition().getEntityType());
    }

    @Override
    public boolean unregisterRule(String ruleId) {
        if (ruleId == null) {
            return false;
        }
        boolean removed = spawnRules.removeIf(rule -> rule.getRuleId().equals(ruleId));
        if (removed) {
            logger.info("Unregistered custom spawn rule: " + ruleId);
        }
        return removed;
    }

    @Override
    public List<CustomSpawnRule> getAllRules() {
        return Collections.unmodifiableList(spawnRules);
    }

    @Override
    public void attemptSpawns() {
        if (Bukkit.getServer().getWorlds().isEmpty()) {
            return;
        }
        long currentTick = Bukkit.getServer().getWorlds().get(0).getFullTime();
        for (World world : Bukkit.getServer().getWorlds()) {
            for (Player player : world.getPlayers()) {
                if (player.isDead() || !player.isValid()) {
                    continue;
                }
                attemptSpawnsAroundPlayer(player, currentTick);
            }
        }
    }

    private void attemptSpawnsAroundPlayer(Player player, long currentTick) {
        Location playerLocation = player.getLocation();
        int spawnAttemptRadius = 64;
        int attemptsPerPlayer = 3;
        World world = playerLocation.getWorld();
        if (world == null) {
            return;
        }
        for (int i = 0; i < attemptsPerPlayer; i++) {
            double angle = Math.random() * Math.PI * 2;
            double distance = 24 + (Math.random() * (spawnAttemptRadius - 24));
            int x = (int) (playerLocation.getX() + Math.cos(angle) * distance);
            int z = (int) (playerLocation.getZ() + Math.sin(angle) * distance);
            Location potentialLocation = new Location(world, x, world.getHighestBlockYAt(x, z) + 1, z);
            if (!world.isChunkLoaded(potentialLocation.getBlockX() >> 4, potentialLocation.getBlockZ() >> 4)) {
                continue;
            }
            processRulesForLocation(potentialLocation, world, player, currentTick);
        }
    }

    private void processRulesForLocation(Location loc, World world, Player nearestPlayer, long currentTick) {
        for (CustomSpawnRule rule : spawnRules) {
            if (!rule.isReadyToAttemptSpawn(currentTick)) {
                continue;
            }
            if (!rule.conditionsMet(loc, world, nearestPlayer)) {
                continue;
            }
            long nearbyCount = world.getNearbyEntities(loc, rule.getSpawnRadiusCheck(), rule.getSpawnRadiusCheck(), rule.getSpawnRadiusCheck(),
                    entity -> entity.getType() == rule.getMobSpawnDefinition().getEntityType() && !entity.isDead()).size();
            if (nearbyCount >= rule.getMaxNearbyEntities()) {
                continue;
            }
            if (rule.rollForSpawn()) {
                spawnMob(loc, rule.getMobSpawnDefinition(), nearestPlayer);
                rule.setLastSpawnAttemptTickGlobal(currentTick);
                logger.fine("Successfully spawned " + rule.getMobSpawnDefinition().getDefinitionId() + " via rule " + rule.getRuleId() + " at " + loc.toVector());
                break;
            }
        }
    }

    private void spawnMob(Location location, MobSpawnDefinition definition, Player nearestPlayer) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Entity spawnedEntity = world.spawnEntity(location, definition.getEntityType());
        if (!(spawnedEntity instanceof LivingEntity livingEntity)) {
            logger.warning("Spawned entity for " + definition.getDefinitionId() + " is not a LivingEntity. Removing.");
            spawnedEntity.remove();
            return;
        }

        definition.getDisplayName().ifPresent(name -> {
            Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(name);
            livingEntity.customName(component);
            livingEntity.setCustomNameVisible(true);
        });

        PlayerProfile referenceProfile = nearestPlayer != null
                ? playerDataService.getPlayerProfile(nearestPlayer.getUniqueId())
                : null;
        RuntimeStatConfig.MobScalingSettings mobScaling = gameplayConfigService.getRuntimeStatConfig().getMobScalingSettings();
        double levelFactor = referenceProfile != null ? Math.max(0, referenceProfile.getLevel() - 1) : 0;

        double baseHealth = mobStatProvider.getBaseHealth(definition.getEntityType());
        double baseAttack = mobStatProvider.getBaseAttackDamage(definition.getEntityType());
        double baseDefense = mobStatProvider.getBaseDefense(definition.getEntityType());

        double scaledHealth = Math.min(baseHealth * (1.0 + levelFactor * mobScaling.getHealthPerLevelPercent()),
                baseHealth * mobScaling.getMaxHealthMultiplier());
        if (scaledHealth <= 0) {
            scaledHealth = baseHealth;
        }
        Attribute maxHealthAttribute = Attribute.MAX_HEALTH;
        if (livingEntity.getAttribute(maxHealthAttribute) != null) {
            livingEntity.getAttribute(maxHealthAttribute).setBaseValue(Math.max(1.0, scaledHealth));
        }
        livingEntity.setHealth(Math.max(1.0, scaledHealth));

        double scaledAttack = Math.min(baseAttack * (1.0 + levelFactor * mobScaling.getDamagePerLevelPercent()),
                baseAttack * mobScaling.getMaxDamageMultiplier());
        if (scaledAttack <= 0) {
            scaledAttack = baseAttack;
        }

        double scaledDefense = Math.min(baseDefense + levelFactor * mobScaling.getDefensePerLevel(),
                mobScaling.getMaxDefenseBonus());
        if (scaledDefense < 0) {
            scaledDefense = baseDefense;
        }

        EntityEquipment equipment = livingEntity.getEquipment();
        if (equipment != null && definition.getEquipment() != null) {
            definition.getEquipment().forEach((slot, itemStack) -> {
                if (itemStack != null && !itemStack.getType().isAir()) {
                    equipment.setItem(slot, itemStack.clone());
                    equipment.setDropChance(slot, 0.0f);
                }
            });
        }

        livingEntity.setMetadata(METADATA_KEY_CUSTOM_MOB_ID, new FixedMetadataValue(plugin, definition.getDefinitionId()));
        definition.getLootTableId().ifPresent(lootId ->
                livingEntity.setMetadata(METADATA_KEY_LOOT_TABLE_ID, new FixedMetadataValue(plugin, lootId)));
        livingEntity.setMetadata(METADATA_KEY_SCALED_ATTACK, new FixedMetadataValue(plugin, scaledAttack));
        livingEntity.setMetadata(METADATA_KEY_SCALED_DEFENSE, new FixedMetadataValue(plugin, scaledDefense));
        livingEntity.setMetadata(METADATA_KEY_SCALED_MAX_HEALTH, new FixedMetadataValue(plugin, scaledHealth));

        logger.fine("Custom mob '" + definition.getDefinitionId() + "' spawned at " + location.toVector());
    }

    @Override
    public void shutdown() {
        logger.info("BasicCustomSpawningService shutting down. (No specific cleanup actions implemented yet)");
    }
}
