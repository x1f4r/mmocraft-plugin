# Custom Mob Spawning System

## Overview

The Custom Mob Spawning System in MMOCraft allows for fine-grained control over how, when, and where custom and vanilla mobs appear in the game world. It replaces or augments Minecraft's default spawning mechanics for specific mob types, enabling designers to create unique ecological zones, dynamic spawning events, and carefully balanced mob populations.

The system is rule-based, where each rule defines a specific mob type, the conditions under which it can spawn, and various parameters controlling its appearance rate and location.

## Core Components

### 1. Models

#### `MobSpawnDefinition.java`

This class defines the blueprint for a custom mob that can be spawned by the system.

*   **`definitionId` (String)**: A unique identifier for this mob definition (e.g., "skeletal_warrior", "goblin_scout").
*   **`entityType` (EntityType)**: The base Bukkit `EntityType` of the mob (e.g., `EntityType.SKELETON`, `EntityType.ZOMBIE`).
*   **`displayName` (String, optional)**: A custom display name for the mob. If null, the default entity name is used. Supports color codes.
*   **`mobStatKey` (String, optional)**: A key to look up this mob's base stats (health, attack, defense) in the `MobStatProvider`. If null, default stats for the `EntityType` might be used or stats might be defined directly by equipment.
*   **`lootTableId` (String, optional)**: The ID of the loot table to be associated with this mob when it's killed. This ID is stored as NBT on the spawned mob.
*   **`equipment` (Map<EquipmentSlot, ItemStack>, optional)**: A map defining the equipment the mob should spawn with (e.g., sword in main hand, helmet).

#### `CustomSpawnRule.java`

This class encapsulates a single rule that dictates the spawning of a mob defined by a `MobSpawnDefinition`.

*   **`ruleId` (String)**: A unique identifier for this spawn rule (e.g., "graveyard_skeletons_night", "forest_goblin_ambush").
*   **`mobSpawnDefinition` (MobSpawnDefinition)**: The definition of the mob to be spawned by this rule.
*   **`conditions` (List<SpawnCondition>)**: A list of `SpawnCondition` objects that must *all* be met for this rule to be considered active.
*   **`spawnChance` (double)**: The probability (0.0 to 1.0) that a mob will spawn if all conditions are met and a spawn attempt is made for this rule.
*   **`minSpawnHeight` (int)**: The minimum Y-coordinate at which the mob can spawn.
*   **`maxSpawnHeight` (int)**: The maximum Y-coordinate at which the mob can spawn.
*   **`maxNearbyEntities` (int)**: The maximum number of entities of the *same `EntityType`* allowed within the `spawnRadiusCheck` for a new mob to spawn. This helps prevent overcrowding.
*   **`spawnRadiusCheck` (double)**: The radius (in blocks) around a potential spawn location to check for nearby entities.
*   **`spawnIntervalTicks` (long)**: The minimum number of server ticks that must pass globally for this specific rule before another spawn attempt can be made using this rule. This acts as a global cooldown for the rule.
*   **`lastSpawnAttemptTickGlobal` (long, internal)**: Tracks the server tick when this rule last successfully initiated a spawn or was seriously considered.

Key methods in `CustomSpawnRule`:
*   `conditionsMet(Player player, Location spawnLocation)`: Checks all associated `SpawnCondition`s.
*   `isReadyToAttemptSpawn(long currentTick)`: Checks if the `spawnIntervalTicks` cooldown has passed.
*   `rollForSpawn()`: Returns true if a random roll succeeds based on `spawnChance`.

### 2. Spawn Conditions

Spawn conditions are criteria that must be satisfied for a `CustomSpawnRule` to be active. They are evaluated around a potential spawn location and often a reference player.

#### `SpawnCondition.java` (Interface)

A functional interface defining the contract for a spawn condition.
*   `canSpawn(Player player, Location location, World world)`: Returns `true` if the condition is met, `false` otherwise.

#### Implemented Conditions:

*   **`BiomeCondition.java`**:
    *   Checks if the biome at the potential `location` is one of a predefined list of allowed biomes.
    *   Constructor: `BiomeCondition(List<Biome> allowedBiomes)`.
*   **`TimeCondition.java`**:
    *   Checks if the current world time at the `location`'s world is within a specified range (inclusive).
    *   Constructor: `TimeCondition(long minTime, long maxTime)` (uses Minecraft ticks, 0-24000).

#### Extensibility

The system is designed to be extensible. Developers can create their own classes implementing the `SpawnCondition` interface to introduce custom logic (e.g., weather conditions, player inventory checks, region-specific flags).

### 3. Service Layer

#### `CustomSpawningService.java` (Interface)

Defines the contract for the service responsible for managing and executing custom mob spawns.
*   `addRule(CustomSpawnRule rule)`
*   `removeRule(String ruleId)`
*   `getRule(String ruleId)`
*   `attemptSpawns()`: The core method called periodically to try and spawn mobs.
*   `spawnMob(Player anchorPlayer, Location spawnLocation, MobSpawnDefinition definition)`: Handles the actual creation and setup of the mob.
*   `shutdown()`: For cleanup, like stopping any internal tasks.

#### `BasicCustomSpawningService.java` (Implementation)

The default implementation of `CustomSpawningService`.

*   **Rule Management**: Stores `CustomSpawnRule`s in a list.
*   **`attemptSpawns()` Logic**:
    1.  Iterates through all online players. Each player acts as a potential "anchor" for spawning.
    2.  For each player, it attempts to find a suitable random spawn location within a configurable radius around the player (respecting Y-coordinates).
    3.  It then iterates through all registered `CustomSpawnRule`s.
    4.  For each rule, it checks:
        *   If the rule is ready based on its global `spawnIntervalTicks` (`isReadyToAttemptSpawn()`).
        *   If all `conditionsMet()` for the player and the chosen location.
        *   If the `maxNearbyEntities` limit is not exceeded at the location.
        *   If `rollForSpawn()` is successful.
    5.  If all checks pass, it calls `spawnMob()`.
*   **`spawnMob()` Logic**:
    1.  Spawns the Bukkit entity of the specified `EntityType`.
    2.  **Custom Name**: If `definition.getDisplayName()` is set, it's applied to the mob.
    3.  **Stats**: Uses `MobStatProvider` (referenced by `definition.getMobStatKey()`) to fetch base stats (health, attack, etc.) and applies them to the mob using Bukkit's `AttributeInstance` API. Max health is set, and current health is set to max.
    4.  **Equipment**: Sets the mob's equipment as defined in `definition.getEquipment()`.
    5.  **NBT Data**:
        *   Sets a persistent NBT tag "MMOCRAFT_CUSTOM_MOB_ID" with the `definition.getDefinitionId()`.
        *   Sets a persistent NBT tag "MMOCRAFT_LOOT_TABLE_ID" with `definition.getLootTableId()` if present. This allows the loot system to identify the correct loot table on death.
*   **Scheduler**: The `MMOCraftPlugin` class is responsible for scheduling `attemptSpawns()` to run periodically (e.g., every 10 seconds).

## Configuration & Usage Example

Currently, `MobSpawnDefinition`s and `CustomSpawnRule`s are defined and registered programmatically, typically within the `onEnable` method of `MMOCraftPlugin` or a dedicated setup class.

**Example (Conceptual - in `MMOCraftPlugin.java`):**

```java
// In onEnable() or a helper method

// 1. Get the CustomSpawningService instance
CustomSpawningService spawningService = getCustomSpawningService();

// 2. Define MobSpawnDefinitions
MobSpawnDefinition skeletalWarriorDef = new MobSpawnDefinition(
    "skeletal_warrior",
    EntityType.SKELETON,
    ChatColor.RED + "Skeletal Warrior",
    "skeletal_warrior_stats", // Key for MobStatProvider
    "warrior_common_loot",    // Key for LootService
    new EnumMap<>(Map.of(
        EquipmentSlot.HAND, new ItemStack(Material.IRON_SWORD),
        EquipmentSlot.CHEST, new ItemStack(Material.IRON_CHESTPLATE)
    ))
);
// plugin.getCustomItemRegistry().getCustomItem("custom_material_id").createItemStack(1) can be used for equipment.

// 3. Define SpawnConditions
List<Biome> graveyardBiomes = List.of(Biome.PLAINS, Biome.DESERT); // Example
SpawnCondition biomeCondition = new BiomeCondition(graveyardBiomes);
SpawnCondition nightTimeCondition = new TimeCondition(13000, 23000); // Night time

// 4. Create CustomSpawnRules
CustomSpawnRule warriorRule = new CustomSpawnRule(
    "graveyard_skeletal_warriors_night",
    skeletalWarriorDef,
    List.of(biomeCondition, nightTimeCondition),
    0.1,  // 10% chance per attempt if conditions met
    60,   // minSpawnHeight
    200,  // maxSpawnHeight
    5,    // maxNearbyEntities of same type (SKELETON)
    16.0, // spawnRadiusCheck for nearby entities
    200   // spawnIntervalTicks (10 seconds global cooldown for this rule)
);

// 5. Register the rule
spawningService.addRule(warriorRule);

// The scheduler set up in MMOCraftPlugin will then periodically call
// spawningService.attemptSpawns(), which will evaluate this rule.
```

## Integration with Other Systems

*   **`MobStatProvider`**: The `mobStatKey` in `MobSpawnDefinition` links to entries in the `MobStatProvider` to determine the base health, damage, and other attributes of the spawned mob.
*   **Loot System**: The `lootTableId` in `MobSpawnDefinition` is stored as an NBT tag on the spawned mob. When the mob is killed, the `MobDeathLootListener` (from the Loot System) reads this tag to determine which `LootTable` to use for dropping items.
*   **`CustomItemRegistry`**: When defining equipment for mobs, `ItemStack`s created from `CustomItem` definitions can be used, allowing mobs to spawn with custom-textured and stat-enhanced gear.

## Admin Commands
*(Placeholder for future admin commands related to testing or managing custom spawns, e.g., forcing a spawn attempt for a rule, listing active rules, etc.)*
Currently, there are no direct admin commands to manage this system, but it can be tested by meeting the conditions in-game.
