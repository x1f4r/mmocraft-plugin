# MMOCraft Combat Mechanics

This document outlines the core combat mechanics implemented in the MMOCraft plugin. It covers how damage is calculated, the skill and ability framework, the status effect system, and how mobs are integrated into these systems. Administrative commands related to combat are also detailed.

---
## Contents

1.  [Custom Damage System](#1-custom-damage-system)
    *   [Damage Calculation Flow](#damage-calculation-flow)
    *   [Key Calculation Inputs](#key-calculation-inputs)
    *   [DamageInstance Record](#damageinstance-record)
2.  [Skill & Ability Framework](#2-skill--ability-framework)
    *   [Skill Abstract Class](#skill-abstract-class)
    *   [SkillType Enum](#skilltype-enum)
    *   [Skill Cooldowns](#skill-cooldowns)
    *   [SkillRegistryService](#skillregistryservice)
3.  [Implemented Skills](#3-implemented-skills)
    *   [Strong Strike](#strong-strike)
    *   [Minor Heal](#minor-heal)
    *   [Using Skills (`/useskill`)](#using-skills-useskill)
4.  [Status Effect System](#4-status-effect-system-framework-overview)
    *   [Core Components](#core-components)
    *   [Lifecycle Methods](#lifecycle-methods)
    *   [Ticking Mechanism](#ticking-mechanism)
5.  [Mob Integration](#5-mob-integration)
    *   [MobStatProvider](#mobstatprovider)
6.  [Combat-Related Admin Commands](#6-combat-related-admin-commands)
    *   [`/mmocadm combat testdamage`](#mmocadm-combat-testdamage)

---

## 1. Custom Damage System

MMOCraft implements a custom damage system to allow for more control over combat interactions, incorporating player stats, skills, and potential future modifiers like equipment bonuses and status effects.

### Damage Calculation Flow

1.  **Event Interception:** The `PlayerCombatListener` listens to Bukkit's `EntityDamageByEntityEvent` at `HIGHEST` priority. This allows it to modify or react to damage events after most other plugins but before the damage is finalized.
2.  **Base Damage Determination:** The listener first determines a `baseWeaponDamage` value:
    *   For **Players**: It checks the item in their main hand using a predefined map of vanilla weapon damages (e.g., `DIAMOND_SWORD` has a base of 7.0). Unarmed attacks default to 1.0.
    *   For **Mobs**: It uses the `MobStatProvider` to get the mob's base attack damage (e.g., a Zombie might have a base of 3.0).
    *   For **Projectiles**: If the projectile source is an entity (e.g., a Skeleton's arrow or a Player's arrow), that entity is considered the `actualAttacker`. The damage value from the Bukkit event is often used as a starting point for projectiles, especially for mobs.
3.  **DamageCalculationService:** The listener then calls `DamageCalculationService.calculateDamage(attacker, victim, baseWeaponDamage, damageType)`.
    *   This service fetches `PlayerProfile` for player attackers/victims to access their stats.
    *   It applies offensive bonuses (e.g., Strength scaling for physical damage, Intelligence for magical) and critical hit checks for the attacker.
    *   It then applies defensive capabilities for the victim (e.g., evasion chance, physical/magical damage reduction from stats or mob defense values from `MobStatProvider`).
4.  **Finalizing Damage:** The service returns a `DamageInstance` object containing the `finalDamage`. The listener then updates the Bukkit event with this final damage using `event.setDamage(damageInstance.getFinalDamage())`.
5.  **Profile Update:** If the victim is a player and took damage, their `PlayerProfile.takeDamage()` method is called to update their internal health record.

### Key Calculation Inputs

The damage calculation considers several factors:

*   **Attacker's Stats (from `PlayerProfile` if Player):**
    *   `STRENGTH`: Increases physical damage.
    *   `INTELLIGENCE`: (Conceptually) Increases magical damage.
    *   `AGILITY` & `LUCK`: Contribute to critical hit chance.
    *   `criticalHitChance`: Derived stat determining the chance of a critical hit.
    *   `criticalDamageBonus`: Derived stat determining the damage multiplier on a critical hit.
*   **Victim's Stats (from `PlayerProfile` if Player, or `MobStatProvider` if Mob):**
    *   `AGILITY` & `LUCK` (Player): Contribute to evasion chance.
    *   `evasionChance` (Player): Derived stat determining chance to evade an attack.
    *   `DEFENSE` (Player/Mob): Contributes to physical damage reduction.
    *   `WISDOM` (Player/Mob, example): Can contribute to magical damage reduction.
    *   `physicalDamageReduction` / `magicDamageReduction` (Player): Derived stats reducing incoming damage.
*   **Base Weapon/Mob Attack Damage:** The initial damage value before most modifiers.
*   **`DamageType` Enum:**
    *   `PHYSICAL`: Standard physical attacks, subject to physical defenses.
    *   `MAGICAL`: Spell or ability damage, subject to magical defenses (conceptual for now, as no magical skills deal damage this way yet).
    *   `TRUE`: Bypasses most/all damage reductions.
    *   `ENVIRONMENTAL`: For future use (e.g., lava, fall damage).

### DamageInstance Record

(`com.x1f4r.mmocraft.combat.model.DamageInstance.java`)

This record holds all relevant information about a single damage interaction:

*   `attacker` (Entity): The entity that initiated the damage (can be null).
*   `victim` (Entity): The entity that received the damage.
*   `attackerId` / `victimId` (UUID).
*   `attackerProfile` / `victimProfile` (`PlayerProfile`): Profiles if attacker/victim are players.
*   `baseDamage` (double): Damage after attacker's offensive bonuses but *before* critical hits and victim's defenses.
*   `type` (DamageType): The type of damage.
*   `criticalHit` (boolean): Whether the attack was a critical hit.
*   `evaded` (boolean): Whether the attack was evaded.
*   `mitigationDetails` (String): A string to log how damage was reduced (e.g., "P.Reduc:20.0%. Evaded.").
*   `finalDamage` (double): The actual damage dealt after all calculations. If evaded, this is 0.

This comprehensive record is useful for logging, combat analysis, and potentially for other systems to react to specific damage events.

---

## 2. Skill & Ability Framework

MMOCraft includes a framework for defining and managing player skills and abilities.

### Skill Abstract Class

(`com.x1f4r.mmocraft.skill.model.Skill.java`)

This abstract class is the base for all skills. Concrete skills (e.g., "Fireball", "Strong Strike") will extend this class.

**Key Properties:**

*   `plugin` (MMOCraftPlugin): A reference to the main plugin instance, allowing skills to access core services.
*   `skillId` (String): A unique internal identifier (e.g., "strong_strike").
*   `skillName` (String): A user-friendly display name (e.g., "Strong Strike").
*   `description` (String): A textual description of the skill's effects.
*   `manaCost` (double): The amount of mana required to use the skill.
*   `cooldownSeconds` (double): The duration of the cooldown in seconds after the skill is used.
*   `castTimeSeconds` (double): The time it takes to cast the skill. 0.0 indicates an instant cast. (Full casting mechanics with interruption are a future enhancement).
*   `skillType` (SkillType): An enum defining the skill's targeting and activation behavior.

**Key Methods:**

*   **`Skill(MMOCraftPlugin plugin, String skillId, ...)` (Constructor):** Initializes the skill's properties.
*   **`boolean canUse(PlayerProfile caster)`:** Checks if the caster meets basic requirements. The base implementation checks for sufficient mana and if the skill is off cooldown for the caster. Concrete skills can override this to add more specific conditions (e.g., target validity, weapon requirements).
*   **`abstract void execute(PlayerProfile caster, Entity target, Location targetLocation)`:** This is the core logic of the skill. Each concrete skill must implement this method to define its effects.
    *   `casterProfile`: The profile of the player using the skill.
    *   `targetEntity`: The targeted entity (if applicable, e.g., for `ACTIVE_TARGETED_ENTITY` skills).
    *   `targetLocation`: The targeted location (if applicable, e.g., for `ACTIVE_AOE_POINT` skills).
*   **`void onCooldown(PlayerProfile caster)`:** Called after a skill is successfully executed to put it on cooldown for the caster. This updates the cooldown information stored in the `PlayerProfile`.

### SkillType Enum

(`com.x1f4r.mmocraft.skill.model.SkillType.java`)

This enum categorizes skills based on how they are used and what they target:

*   **`PASSIVE`**: Always active or triggers automatically under certain conditions.
*   **`ACTIVE_TARGETED_ENTITY`**: Requires the player to target a specific entity.
*   **`ACTIVE_SELF`**: Cast by the player on themselves.
*   **`ACTIVE_AOE_POINT`**: Targets a specific point/location for an Area of Effect.
*   **`ACTIVE_NO_TARGET`**: Actively used but doesn't require a specific entity or point target (e.g., an AoE around the caster).

### Skill Cooldowns

Skill cooldowns are managed within the `PlayerProfile` class:

*   `PlayerProfile.setSkillCooldown(String skillId, double cooldownSeconds)`: Sets the expiration time for a skill's cooldown.
*   `PlayerProfile.isSkillOnCooldown(String skillId)`: Checks if a specific skill is currently on cooldown for the player.
*   `PlayerProfile.getSkillRemainingCooldown(String skillId)`: Returns the remaining cooldown time in milliseconds.

The `Skill.onCooldown()` method utilizes these to manage cooldowns.

### SkillRegistryService

(`com.x1f4r.mmocraft.skill.service.BasicSkillRegistryService.java`)

This service acts as a central repository for all defined `Skill` objects in the game.

*   **`registerSkill(Skill skill)`**: Adds a skill to the registry. Skills are typically registered during plugin startup.
*   **`Optional<Skill> getSkill(String skillId)`**: Retrieves a skill by its unique ID.
*   **`Collection<Skill> getAllSkills()`**: Returns all registered skills.

Currently, all players "know" all registered skills. A more advanced system for players to learn or unlock skills would build upon this registry.

---

## 3. Implemented Skills

The following skills are currently implemented as examples of the skill framework.

### Strong Strike

*   **ID:** `strong_strike`
*   **Name:** "Strong Strike"
*   **Description:** "A powerful blow that deals 1.5x increased physical damage based on Strength."
*   **Type:** `ACTIVE_TARGETED_ENTITY`
*   **Mana Cost:** 10.0
*   **Cooldown:** 5.0 seconds
*   **Cast Time:** 0.0 seconds (Instant)
*   **Effect:**
    *   Calculates damage based on a base skill damage (`5.0`) plus a Strength scaling factor (`casterProfile.getStatValue(Stat.STRENGTH) * 1.2`).
    *   This sum is then multiplied by `1.5`.
    *   A critical hit check is performed based on the caster's stats.
    *   If the target is a player, evasion and physical damage reduction from their `PlayerProfile` are applied.
    *   Deals the final calculated damage to the target LivingEntity.
    *   Plays sound effects and sends feedback messages to the caster.
    *   Deducts mana from the caster.

### Minor Heal

*   **ID:** `minor_heal`
*   **Name:** "Minor Heal"
*   **Description:** "Heals yourself for a small amount, scaled with Wisdom."
*   **Type:** `ACTIVE_SELF`
*   **Mana Cost:** 15.0
*   **Cooldown:** 8.0 seconds
*   **Cast Time:** 0.5 seconds (Conceptual; execution is currently instant after command)
*   **Effect:**
    *   Calculates heal amount based on a base heal value (`10.0`) plus a Wisdom scaling factor (`casterProfile.getStatValue(Stat.WISDOM) * 0.8`).
    *   Restores health to the caster's `PlayerProfile` and updates their Bukkit entity health, clamped at max health.
    *   Plays sound effects and sends feedback messages to the caster.
    *   Deducts mana from the caster.

### Using Skills (`/useskill`)

Players can use their active skills (if they meet requirements like mana, cooldown, and have permission) via the command:

*   **Command:** `/useskill <skillId> [targetName]`
*   **Permission:** `mmocraft.command.useskill`
*   **Arguments:**
    *   `<skillId>`: The unique ID of the skill to use (e.g., `strong_strike`, `minor_heal`).
    *   `[targetName]` (optional): Required if the skill type is `ACTIVE_TARGETED_ENTITY`. This should be the name of an online player or a nearby living entity.

The command handles checking if the skill exists, if the player can use it (mana/cooldown), resolving the target if necessary, and then executing the skill's logic and applying its cooldown.

---

## 4. Status Effect System (Framework Overview)

MMOCraft includes a system for applying, managing, and ticking status effects (buffs and debuffs) on entities. While concrete effects like "Poison" or "Strength Buff" are yet to be implemented on top of this framework, the core infrastructure is in place.

### Core Components

*   **`StatusEffectType.java` (Enum):**
    *   Defines various types of status effects (e.g., `HEALTH_REGEN`, `POISON`, `STUN`, `STAT_BUFF_STRENGTH`).
    *   Each type can be categorized as a buff or harmful effect.
*   **`StatusEffect.java` (Abstract Class):**
    *   The base class for all specific status effect implementations.
    *   **Properties:** `effectType`, `durationSeconds` (-1 for permanent), `potency` (magnitude), `tickIntervalSeconds` (0 for non-ticking), `sourceEntityId`.
    *   Requires an `MMOCraftPlugin` instance in its constructor for service access.
*   **`ActiveStatusEffect.java` (Class):**
    *   Represents an instance of a `StatusEffect` currently active on a specific entity.
    *   Tracks the base `StatusEffect`, the `targetId` (UUID), `applicationTimeMillis`, `expirationTimeMillis`, and `nextTickTimeMillis` (for ticking effects). It also includes a `stacks` field for future stackable effects.
    *   Provides methods like `isExpired()` and `isReadyToTick()`.
*   **`StatusEffectManager.java` (Interface) & `BasicStatusEffectManager.java` (Implementation):**
    *   The central service for managing status effects.
    *   **Dependencies:** `MMOCraftPlugin`, `LoggingUtil`, `PlayerDataService`.
    *   Maintains a map (`activeEffectsMap`) of `UUID` (entity ID) to a `List<ActiveStatusEffect>`.

### Lifecycle Methods

Concrete `StatusEffect` implementations must define the following:

*   **`onApply(LivingEntity target, PlayerProfile targetProfileIfPlayer)`:** Called when the effect is first applied. This is where initial changes occur (e.g., if it's a stat buff, modify `PlayerProfile.coreStats` and call `recalculateDerivedAttributes()`).
*   **`onTick(LivingEntity target, PlayerProfile targetProfileIfPlayer)`:** Called periodically if the effect has a `tickIntervalSeconds > 0`. Used for effects like damage-over-time (DoT) or heal-over-time (HoT).
*   **`onExpire(LivingEntity target, PlayerProfile targetProfileIfPlayer)`:** Called when the effect's duration runs out. Used for cleanup (e.g., reverting stat changes made in `onApply`).
*   **`onRemove(LivingEntity target, PlayerProfile targetProfileIfPlayer)`:** Called if the effect is removed before its duration expires (e.g., by a dispel ability). Defaults to calling `onExpire`.

### Ticking Mechanism

*   The `BasicStatusEffectManager` has a `tickAllActiveEffects()` method.
*   In `MMOCraftPlugin#onEnable`, a repeating Bukkit scheduler task is started, which calls `statusEffectManager.tickAllActiveEffects()` every second (20 server ticks).
*   `tickAllActiveEffects()` iterates through all entities with active effects:
    *   Checks for expired effects, removes them, and calls their `onExpire()` method.
    *   For non-expired, ticking effects, if they are ready to tick (based on `nextTickTimeMillis`), their `onTick()` method is called, and their `nextTickTimeMillis` is updated.
    *   Handles cleanup if the target entity is no longer valid (e.g., logged off, dead).

This framework allows for the future implementation of diverse status effects by extending `StatusEffect` and defining their specific behaviors in the lifecycle methods.

---

## 5. Mob Integration

To ensure that non-player entities (mobs) can interact meaningfully with the custom combat system, their base statistics are managed by a dedicated provider.

### MobStatProvider

(`com.x1f4r.mmocraft.combat.service.MobStatProvider.java` and `DefaultMobStatProvider.java`)

*   **`MobStatProvider` (Interface):** Defines methods to retrieve base combat-related statistics for any given `EntityType`:
    *   `double getBaseHealth(EntityType type)`
    *   `double getBaseAttackDamage(EntityType type)`
    *   `double getBaseDefense(EntityType type)` (a conceptual defense value)
*   **`DefaultMobStatProvider` (Implementation):**
    *   Provides a hardcoded map of default stats (health, attack damage, defense points) for common vanilla mobs like Zombies, Skeletons, Spiders, Endermen, etc.
    *   Offers a fallback set of very basic stats for any `EntityType` not explicitly defined in its map.
    *   This implementation is injected into the `BasicDamageCalculationService` and `PlayerCombatListener`.

**How it's Used:**

*   **Mob Attack Damage:** When a mob attacks, `PlayerCombatListener` uses `mobStatProvider.getBaseAttackDamage()` to determine the initial base damage for the attack, which is then fed into the `DamageCalculationService`.
*   **Mob Defenses:** When a mob is attacked, `BasicDamageCalculationService` uses `mobStatProvider.getBaseDefense()` to get the mob's defense value. This defense value is then used to calculate a percentage-based damage reduction (e.g., each defense point might contribute 4% damage reduction, up to a cap).

This system allows for basic differentiation in mob strength and resilience within the custom combat framework and can be expanded later to load mob stats from configuration files for greater customization.

---

## 6. Combat-Related Admin Commands

Administrative commands provide tools for testing and managing aspects of the combat system.

### `/mmocadm combat testdamage`

This command allows administrators to simulate a damage calculation between two online players with a specified weapon.

*   **Usage:** `/mmocadm combat testdamage <attackerPlayerName> <victimPlayerName> [weaponMaterialName]`
*   **Permission:** `mmocraft.admin.combat.testdamage`
*   **Arguments:**
    *   `<attackerPlayerName>`: The name of an online player initiating the simulated attack.
    *   `<victimPlayerName>`: The name of an online player receiving the simulated attack.
    *   `[weaponMaterialName]` (Optional): The `Material` name of the weapon the attacker is assumed to be using (e.g., `DIAMOND_SWORD`, `IRON_AXE`). If omitted or invalid, defaults to "AIR" (unarmed).
*   **Functionality:**
    1.  Resolves the attacker and victim `Player` objects.
    2.  Determines the base damage for the specified (or default) weapon material using a predefined map.
    3.  Calls the `DamageCalculationService.calculateDamage()` method with the attacker, victim, base weapon damage, and `DamageType.PHYSICAL`.
    4.  Displays a detailed breakdown of the resulting `DamageInstance` to the command sender, including:
        *   Attacker and victim names.
        *   Simulated weapon and its base damage.
        *   Damage type.
        *   Calculated base damage (after attacker's offensive bonuses from stats).
        *   Whether the hit was critical.
        *   Whether the hit was evaded.
        *   Mitigation details (e.g., percentage reduction).
        *   The final calculated damage amount.
*   **Note:** This command *simulates* damage; it does not actually apply any damage to the entities. It is purely for testing and observing the calculation outputs based on current player stats and equipment (simulated weapon).

---
