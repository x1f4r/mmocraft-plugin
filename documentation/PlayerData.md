# Player Data & Statistics System

This document details the Player Data system in MMOCraft, covering how player information is structured, persisted, and managed. It also explains the core statistics, derived attributes, and the experience/leveling system.

---
## Contents

1.  [PlayerProfile Model & Stat Enum](#playerprofile-model--stat-enum)
    *   [PlayerProfile Class](#playerprofile-class)
    *   [Stat Enum](#stat-enum)
2.  [Derived Attributes and Statistics System](#derived-attributes-and-statistics-system)
3.  [PlayerDataService](#playerdataservice)
4.  [Experience and Leveling System](#experience-and-leveling-system)
5.  [Admin Commands (`/playerdata`)](#admin-commands-playerdata)

---

## 1. PlayerProfile Model & Stat Enum

The foundation of player data management is the `PlayerProfile` class, which is supported by the `Stat` enum for defining core player attributes.

### PlayerProfile Class

(`com.x1f4r.mmocraft.playerdata.model.PlayerProfile.java`)

The `PlayerProfile` class is a Plain Old Java Object (POJO) that encapsulates all persistent and some dynamic data associated with a player.

**Key Fields:**

*   `UUID playerUUID`: The unique identifier for the player. This is the primary key.
*   `String playerName`: The last known in-game name of the player. Used for convenience but UUID is authoritative.
*   `long currentHealth`: The player's current health points.
*   `long maxHealth`: The player's maximum health points. This is a derived attribute.
*   `long currentMana`: The player's current mana (or energy/resource) points.
*   `long maxMana`: The player's maximum mana points. This is a derived attribute.
*   `int level`: The player's current character level.
*   `long experience`: The player's current experience points accumulated towards the next level.
*   `long currency`: The player's balance of the primary in-game currency.
*   `Map<Stat, Double> coreStats`: A map holding the values for the player's core statistics (defined by the `Stat` enum).
*   `LocalDateTime firstLogin`: Timestamp recorded when the player first joined the server (or when their profile was created).
*   `LocalDateTime lastLogin`: Timestamp recorded when the player last logged in. Updated on each login.

The class provides constructors for creating new profiles (with default starting values) and for loading existing profiles from the database. It includes getters and setters for its fields, with setters for stats and level triggering recalculations of derived attributes.

### Stat Enum

(`com.x1f4r.mmocraft.playerdata.model.Stat.java`)

The `Stat` enum defines the set of core statistics that every player profile possesses. Using an enum provides type safety and makes it easy to manage and reference these stats throughout the codebase.

**Defined Core Stats:**

*   **`HEALTH`**: Drives the player's maximum health pool.
*   **`DEFENSE`**: Reduces incoming non-true damage using a configurable diminishing returns curve.
*   **`TRUE_DEFENSE`**: Mitigates true damage sources that bypass regular defense.
*   **`STRENGTH`**: Increases physical/weapon damage and contributes to critical damage scaling.
*   **`CRITICAL_CHANCE`**: Percentage chance for attacks to crit (capped by configuration).
*   **`CRITICAL_DAMAGE`**: Increases the bonus damage dealt by critical strikes.
*   **`INTELLIGENCE`**: Governs mana capacity and spell potency.
*   **`MANA_REGEN`**: Boosts passive mana regeneration.
*   **`ABILITY_POWER`**: Adds percentage-based modifiers to ability and spell damage/healing.
*   **`ATTACK_SPEED`**: Influences weapon swing speed.
*   **`FEROCITY`**: Provides chances to deliver additional hits.
*   **`EVASION`**: Grants a chance to dodge incoming attacks (capped by configuration).
*   **`SPEED`**: Affects base movement speed up to a configurable cap.
*   **`MAGIC_FIND`** and **`PET_LUCK`**: Modify loot rolls for rare items or pets.
*   **Gathering stats** such as **`MINING_SPEED`**, **`MINING_FORTUNE`**, **`FARMING_FORTUNE`**, **`FORAGING_FORTUNE`**, and **`FISHING_FORTUNE`** increase efficiency and yields in their respective professions.

Each `Stat` enum constant includes a display name and description to help surface its intent in-game.

---

## 2. Derived Attributes and Statistics System

Several key player attributes are not stored directly but are *derived* from their core stats (from the `Stat` enum) and their current level. This allows for dynamic character progression where investing in core stats meaningfully impacts combat and survival capabilities.

The calculation logic resides within the `PlayerProfile` class, specifically in the `recalculateDerivedAttributes()` method. This method is automatically called when:
*   A `PlayerProfile` is first created (either new or loaded from database).
*   A player's level changes (`setLevel()`).
*   A player's core stat value changes (`setStatValue()` or `setCoreStats()`).

**Key Derived Attributes:**

*   **`maxHealth`**: Directly sourced from the scaled `HEALTH` stat.
*   **`maxMana`**: Equal to the scaled `INTELLIGENCE` stat.
*   **`criticalHitChance`**: Converts the `CRITICAL_CHANCE` stat into a 0–1 fraction (capped at 100%).
*   **`criticalDamageBonus`**: Converts `CRITICAL_DAMAGE` into a multiplicative bonus (`1 + critDamage%/100`).
*   **`evasionChance`**: Based on the `EVASION` stat and capped by configuration.
*   **`physicalDamageReduction` / `magicDamageReduction`**: Calculated via configurable diminishing returns using both `DEFENSE` and `TRUE_DEFENSE`.

The specific scaling factors, caps, and diminishing returns behaviour come from `StatScalingConfig` (loaded via `stats.yml`). After derived attributes are recalculated, `currentHealth` and `currentMana` are clamped so they never exceed the newly computed maximums.

This system ensures that player stats are always up-to-date and reflect changes in core stats or level immediately.

---

## 3. PlayerDataService

(`com.x1f4r.mmocraft.playerdata.BasicPlayerDataService.java`)

The `PlayerDataService` is the central component for managing the lifecycle of `PlayerProfile` objects. Its implementation, `BasicPlayerDataService`, orchestrates loading, saving, and caching player data.

**Key Responsibilities & Features:**

*   **Loading Player Data:**
    *   Triggered by the `PlayerJoinQuitListener` when a player joins (specifically during `AsyncPlayerPreLoginEvent`).
    *   The `loadPlayerProfile(UUID playerUUID, String playerName)` method attempts to fetch data from the SQLite database.
    *   If a player record exists, it's deserialized into a `PlayerProfile` object. The player's name and last login time are updated.
    *   If no record exists, a new `PlayerProfile` is created with default values (level 1, default stats, etc.). This new profile is then immediately saved to the database.
    *   All database operations are performed asynchronously on a dedicated thread pool to avoid blocking the server's main thread.
*   **Saving Player Data:**
    *   Triggered by `PlayerJoinQuitListener` when a player quits (`PlayerQuitEvent`).
    *   The `savePlayerProfile(UUID playerUUID)` method retrieves the `PlayerProfile` from the cache.
    *   It updates the `lastLogin` timestamp (though this is more accurately a "last seen" or "save time" in this context).
    *   The profile (including `coreStats` serialized to a JSON-like string) is saved to the database using an "UPSERT" (insert or replace) operation to handle both new and existing records cleanly.
    *   Saving is also performed asynchronously.
*   **Caching:**
    *   An in-memory cache (`Map<UUID, PlayerProfile>`) holds the `PlayerProfile` objects for all currently online players.
    *   `getPlayerProfile(UUID playerUUID)`: Retrieves a profile from this cache.
    *   `cachePlayerProfile(PlayerProfile profile)`: Adds a profile to the cache (done after loading/creation).
    *   `uncachePlayerProfile(UUID playerUUID)`: Removes a profile from the cache (done after saving on player quit).
*   **Database Schema:**
    *   The service includes an `initDatabaseSchema()` method, called during plugin startup (`MMOCraftPlugin#onEnable`).
    *   This method creates the `player_profiles` table in the SQLite database (`plugins/MMOCraft/mmocraft_data.db`) if it doesn't already exist.
    *   **Table Structure (`player_profiles`):**
        *   `player_uuid` (TEXT PRIMARY KEY): Player's unique ID.
        *   `player_name` (TEXT): Player's last known name.
        *   `current_health`, `max_health` (BIGINT)
        *   `current_mana`, `max_mana` (BIGINT)
        *   `level` (INTEGER)
        *   `experience` (BIGINT)
        *   `currency` (BIGINT)
        *   `core_stats` (TEXT): Stores the `Map<Stat, Double>` serialized as a JSON string. A custom `JsonUtil` is currently used for this; a dedicated library (Gson/Jackson) would be more robust.
        *   `first_login` (TEXT): Timestamp of first login (ISO_LOCAL_DATE_TIME format).
        *   `last_login` (TEXT): Timestamp of last login/save (ISO_LOCAL_DATE_TIME format).

The `BasicPlayerDataService` relies on `PersistenceService` for executing database queries and `LoggingUtil` for logging its operations. It also has access to `EventBusService` to dispatch player data-related events (like `PlayerLevelUpEvent`).

---

## 4. Experience and Leveling System

Player progression in MMOCraft is primarily managed through an experience (XP) and leveling system.

### `ExperienceUtil.java`

(`com.x1f4r.mmocraft.playerdata.util.ExperienceUtil.java`)

This utility class defines the core mechanics of the leveling curve.

*   **`static long getXPForNextLevel(int currentLevel)`**: This is the key method used by the system. It calculates the amount of XP a player needs to accumulate *while they are at `currentLevel`* to advance to `currentLevel + 1`.
    *   Formula example: `BaseXP * Math.pow(currentLevel, ExponentFactor)` (e.g., `100 * Math.pow(currentLevel, 1.5)`).
*   **`static int getMaxLevel()`**: Defines the maximum achievable player level (e.g., 100).
*   **`static int getMinLevel()`**: Defines the starting player level (typically 1).
*   **`static long getTotalXPForLevel(int level)`**: Calculates the XP required to complete `level - 1` and reach `level`. For example, `getTotalXPForLevel(2)` is the same as `getXPForNextLevel(1)`. This might be more clearly named in the future to represent "XP needed to reach this level's threshold from the previous".

The `PlayerProfile` class uses `ExperienceUtil.getXPForNextLevel(this.level)` in its `getExperienceToNextLevel()` getter to determine the current XP cap for leveling.

### `PlayerDataService.addExperience(UUID playerUUID, long amount)`

This method, located in `BasicPlayerDataService`, handles the logic of adding experience to a player and processing level-ups.

1.  **Retrieves Profile**: Fetches the `PlayerProfile` from the cache.
2.  **Max Level Check**: If the player is already at `ExperienceUtil.getMaxLevel()`, no XP is added, and their current XP might be set to 0.
3.  **Add XP**: The specified `amount` is added to the player's current `experience`.
4.  **Level-Up Loop**:
    *   It checks if `profile.getExperience() >= profile.getExperienceToNextLevel()`.
    *   If true, and the player is not yet at max level:
        *   The XP required for the level-up (`profile.getExperienceToNextLevel()`) is subtracted from `profile.getExperience()`.
        *   The player's `level` is incremented. This internally calls `profile.recalculateDerivedAttributes()`.
        *   A `PlayerLevelUpEvent` is dispatched via the `EventBusService`.
        *   This loop continues if the player has enough XP for multiple level-ups from a single XP gain.
    *   If max level is reached during the loop, XP is set to 0, and a log message indicates this.
5.  The player's current XP is ensured to not be negative after the loop.

### `PlayerLevelUpEvent.java`

(`com.x1f4r.mmocraft.playerdata.events.PlayerLevelUpEvent.java`)

This custom event is dispatched by the `EventBusService` whenever a player successfully levels up.

*   **Key Fields:**
    *   `UUID playerUUID`: The UUID of the player who leveled up.
    *   `int oldLevel`: The player's level before this event.
    *   `int newLevel`: The new level the player achieved.
    *   `PlayerProfile profileSnapshot`: An optional snapshot of the `PlayerProfile` at the time of level up. This allows event handlers to see the state of the player (stats, etc.) when they achieved the new level.

Other systems or modules within MMOCraft can listen for this event to trigger actions like:
*   Displaying level-up notifications or effects.
*   Granting stat/skill points.
*   Unlocking new abilities or content.
*   Announcing to other players.

---

## 5. Admin Commands (`/playerdata`)

The `/playerdata` command (aliases: `/pd`, `/mmocpd`) provides administrative functionalities for viewing and modifying player data. Access to the base command and its subcommands is controlled by permissions.

**Base Permission:** `mmocraft.admin.playerdata` (typically required to use any `/pd` subcommand)

### Subcommands:

*   **View Player Data**
    *   **Usage:** `/pd view <playerName>`
    *   **Permission:** `mmocraft.admin.playerdata.view`
    *   **Description:** Displays a comprehensive overview of the specified online player's `PlayerProfile`, including their level, XP, health/mana, currency, core stats, and derived combat statistics.

*   **Set Core Stat**
    *   **Usage:** `/pd setstat <playerName> <statName> <value>`
    *   **Permission:** `mmocraft.admin.playerdata.setstat`
    *   **Description:** Sets a specific core stat for an online player to the given value.
        *   `<statName>`: The name of the stat from the `Stat` enum (e.g., `STRENGTH`, `VITALITY`). Case-insensitive.
        *   `<value>`: The numerical value to set the stat to.
    *   **Effect:** This will trigger a recalculation of the player's derived attributes. The change is saved to the database.

*   **Set Player Level**
    *   **Usage:** `/pd setlevel <playerName> <level>`
    *   **Permission:** `mmocraft.admin.playerdata.setlevel`
    *   **Description:** Sets an online player's level to the specified value.
        *   `<level>`: The new level. Must be between 1 and the defined maximum level (`ExperienceUtil.getMaxLevel()`).
    *   **Effect:** The player's current experience is set to 0. Derived attributes are recalculated. The change is saved to the database.

*   **Add Experience Points**
    *   **Usage:** `/pd addxp <playerName> <amount>`
    *   **Permission:** `mmocraft.admin.playerdata.addxp`
    *   **Description:** Adds a specified amount of experience points to an online player.
        *   `<amount>`: The amount of XP to add (must be positive).
    *   **Effect:** This may cause the player to level up one or more times. Level-up events are fired, and derived attributes are recalculated accordingly. The change is saved.

*   **Add/Remove Currency**
    *   **Usage:** `/pd addcurrency <playerName> <amount>`
    *   **Permission:** `mmocraft.admin.playerdata.addcurrency`
    *   **Description:** Adds (or removes, if negative) a specified amount of currency to/from an online player's balance.
        *   `<amount>`: The amount of currency to add. Can be negative to subtract.
    *   **Effect:** The player's currency is updated. The change is saved.

**Note:** All modification commands currently require the target player to be online. The changes are saved to the database asynchronously after being applied to the in-memory profile.

---
