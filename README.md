# MMOCraft-Plugin

> **Vision**
> Provide a drop‑in, data‑driven engine that converts a plain Purpur server into an MMORPG platform akin to Hypixel Skyblock, yet fully customisable and open‑source.

## Overview

MMOCraft is a powerful, open-source engine that transforms your Minecraft server into a feature-rich Massively Multiplayer Online Role-Playing Game (MMORPG). Designed to be highly extensible and configurable, it provides the foundational systems required to build a unique and engaging MMO experience.

Whether you want to create a world with custom mobs and loot, intricate combat mechanics, deep player progression, or unique crafting recipes, MMOCraft provides the tools to make it happen.

## Prerequisites

- Java 21 JDK

## Getting Started

This repository is structured for easy setup and execution. The entire process is managed by the `server.sh` script.

1.  **Clone the repository:**
    ```sh
    git clone https://github.com/x1f4r/mmocraft-plugin
    cd mmocraft-plugin
    ```

2.  **Run the server:**
    ```sh
    # Make the script executable first (only needs to be done once)
    chmod +x ./server.sh

    ./server.sh start
    ```
    This single command will automatically:
    - Compile the latest plugin code.
    - Copy the plugin JAR to the server directory.
    - Download the Purpur server if it's not already present.
    - Accept the EULA.
    - Start the Minecraft server.

## Management Script (`server.sh`)

All common actions are handled by the `server.sh` script.

### Available Commands

-   `./server.sh start`: Builds, deploys, and starts the server.
-   `./server.sh stop`: Stops the server gracefully.
-   `./server.sh restart`: Stops, rebuilds, deploys, and restarts the server.
-   `./server.sh console`: Attach to the live server console to view logs.
-   `./server.sh status`: Check if the server is currently running.
-   `./server.sh build`: Compiles the plugin without starting the server.
-   `./server.sh deploy`: Copies the already-built plugin to the server directory.
-   `./server.sh setup`: Only downloads the server JAR and accepts the EULA, without starting the server.

## Features

MMOCraft is built on a collection of robust, interconnected systems.

### Player Data & Statistics
*   **Persistent Player Profiles:** Player data (stats, level, XP, currency) is stored in an SQLite database and managed asynchronously to prevent server lag.
*   **Core Stats System:** A flexible system based on Hypixel-style stats (`STRENGTH`, `HEALTH`, `DEFENSE`, `INTELLIGENCE`, `CRIT CHANCE`, etc.).
*   **Derived Attributes:** Player attributes like Max Health, Max Mana, Critical Hit Chance, and Evasion are dynamically calculated based on their core stats and level.
*   **Experience & Leveling:** A configurable leveling curve and an API for granting experience from various sources (mob kills, quests, etc.).

### Combat Mechanics
*   **Custom Damage System:** Overrides vanilla damage calculations to incorporate player stats, weapon damage, and mob defenses.
*   **Skills & Abilities Framework:** A system for creating custom active and passive skills with mana costs, cooldowns, and custom effects.
*   **Status Effects:** A framework for applying buffs and debuffs (e.g., Damage over Time, Stat Boosts) to entities.
*   **Mob Integration:** Vanilla mobs are integrated into the combat system with customizable base stats (health, damage, defense).

### Custom Items, Equipment & Loot
*   **Custom Item Creation:** Define custom items with unique IDs, display names, lore, materials, and stats.
*   **Item Rarity:** An item rarity system (`COMMON`, `RARE`, `LEGENDARY`, etc.) that automatically colors item names and adds lore.
*   **Equipment System:** Custom items with stat modifiers will automatically update a player's derived attributes when equipped.
*   **Loot Tables:** Create custom loot tables for mobs to drop specific custom items with configurable chances.

### Custom Mob Spawning
*   **Rule-Based Spawning:** Define complex rules for spawning specific mobs.
*   **Conditions:** Spawning can be restricted by biome, time of day, height, and nearby entity counts.
*   **Customization:** Spawned mobs can have custom display names, equipment, stats, and loot tables.

### Zoning System
*   **Define 3D Zones:** Create named, box-shaped regions in your world using `zones.yml`.
*   **Custom Properties:** Assign arbitrary properties to zones (e.g., `isSanctuary: true`, `pvpAllowed: false`) that can be used by other systems to alter gameplay mechanics within that area.

### Crafting System (Foundational)
*   **Custom Recipes:** A framework for defining custom shaped and shapeless recipes using custom or vanilla items.
*   **Custom Crafting UI:** A basic `/customcraft` command and UI, ready to be expanded into a full-featured custom crafting station.

### Observability & Diagnostics
*   **Structured Logging:** `LoggingUtil` emits JSON-formatted payloads for info, warning, and error events so that failures across combat, crafting, and commands surface with machine-readable context.
*   **Automated Health Checks:** `PluginDiagnosticsService` audits registered items, skills, gameplay configuration, persistence, resource nodes, demo content, crafting recipes, and required listeners, returning severity-tagged entries for each subsystem.
*   **Admin Tooling:** `/mmocadm diagnostics` generates the full report, `/mmocadm issues` filters to outstanding warnings or errors, and `/mmocadm reloadconfig` reapplies the configuration bundle while logging any problems that remain.

## Configuration

MMOCraft uses a combination of configuration files and programmatic setup.

### `mmocraft.conf`
This file, located in `/plugins/MMOCraft/`, contains the root plugin preferences and is copied from the JAR if it is missing.

```yaml
# === MMOCraft default configuration ===
# This file should be in YAML format for BasicConfigService to parse it correctly.

core:
  debug-logging: false # Controls debug level messages from LoggingUtil

stats:
  max-health: 100
  base-damage: 5
  default-mana: 50
  default-stamina: 50

# Example list
welcome-messages:
  - "Welcome to MMOCraft!"
  - "This server is running MMOCraft."
  - "Enjoy your adventure!"

# Example boolean for other features (can be namespaced too)
features:
  pvp-enabled: true
  # debug-mode: false # Kept for reference, but LoggingUtil uses core.debug-logging

demo:
  enabled: true # Master toggle for all sample/demo content bundled with MMOCraft.
  items: true # Registers showcase items such as the Simple Iron Sword.
  skills: true # Registers showcase skills like Strong Strike and Minor Heal.
  loot-tables: true # Enables the default loot table examples used by demo mobs/nodes.
  custom-spawns: true # Enables the sample skeletal warrior custom spawn rule.
  resource-nodes: true # Places sample resource nodes and registers their types.
  zones: true # Copies the default zones.yml containing the Spawn Sanctuary example.
```

`core.debug-logging` is consumed by `LoggingUtil` for runtime debug output, while the remaining values act as high-level defaults. The legacy demo flags remain in the file for compatibility, but the runtime demo module ultimately follows the toggles defined in `config/demo-content.yml` when `DemoContentSettings` are built and applied.

### `zones.yml`
This file, located in `/plugins/MMOCraft/`, is used to define custom zones.

```yaml
spawn_sanctuary:
  name: "Spawn Sanctuary"
  world: "world"
  min-x: -50
  min-y: 0
  min-z: -50
  max-x: 50
  max-y: 128
  max-z: 50
  properties:
    isSanctuary: true
    pvpAllowed: false
```

### Gameplay Configuration (`/plugins/MMOCraft/config/`)
`GameplayConfigService` creates this directory on startup, copies the bundled defaults, and keeps parsed issues so that admins can review them after reloads. Use `/mmocadm reloadconfig` whenever you change these files to reapply them without rebooting.

#### `stats.yml`
Controls player stat scaling, combat baselines, runtime combat/ability/gathering behaviour, and mob scaling curves. Each section lets you override per-stat base, per-point, and per-level values plus optional diminishing returns for bespoke tuning. Reloading this file immediately updates the live runtime attribute service for online players.

#### `loot_tables.yml`
Defines named loot tables and binds them to mobs. Entries specify loot type, identifier, drop chance, and amount, and mob assignments attach those tables to Bukkit `EntityType`s at runtime.

#### `crafting.toml`
Lists enabled custom recipes with `RecipeType`, optional permissions, and either shapeless ingredient arrays or shaped slot maps. Both vanilla materials and custom items are supported, and the loader unregisters stale recipes before applying new ones to keep the registry in sync.

#### `demo-content.yml`
Provides the data payload consumed by the demo module—toggles for each bundle, loot-table definitions, resource node types and placements, and custom spawn rules with biome, equipment, and timing constraints. When those features are enabled, `DemoContentModule` reads this configuration to register items, recipes, loot, spawns, and resource nodes for the showcase experience.

### Programmatic Extensions (For Developers)

Configuration files cover the common cases, but bespoke content is still authored in Java:
*   **Custom Items:** Implement new subclasses of `CustomItem` and register them with the item registry. The demo module shows how a bundle of items is published when item toggles are enabled.
*   **Advanced Recipes:** You can still craft infusion-style or scripted recipes by instantiating `CustomRecipe` objects and handing them to the registry, just as the demo module does for its crafting flow.
*   **Custom Mob Spawns:** Build bespoke encounters by composing `CustomSpawnRule` instances with conditions and registering them through the spawning service.

These hooks let you pair data-driven configuration with tailored Java logic whenever you need behaviour beyond what the shipped config files express.

## Commands

Here is a list of the available commands.

### Player Commands
| Command | Description | Permission |
| --- | --- | --- |
| `/mmoc [help|version|profile]` | Shows plugin info, help, and a stat sheet for the executing player. | `mmocraft.command.info` |
| `/useskill <skillId> [target]` | Executes a learned skill. | `mmocraft.command.useskill` |
| `/customcraft` | Opens the custom crafting interface. | `mmocraft.command.customcraft` |

### Admin Commands (`/mmocadm` or `/mmoadmin`)
The base permission for all admin commands is `mmocraft.admin`.

| Command | Description | Permission |
| --- | --- | --- |
| `/mmocadm item give <player> <itemId> [amount]` | Gives a player a custom item. | `mmocraft.admin.item.give` |
| `/mmocadm item list [filter] [page]` | Lists the registered custom items, optionally filtered. | `mmocraft.admin.item.list` |
| `/mmocadm resource <place|remove|info>` | Place, remove, or inspect resource nodes at targeted locations. | `mmocraft.admin.resource` |
| `/mmocadm combat testdamage <attacker> <victim> [weapon]` | Simulates a damage calculation. | `mmocraft.admin.combat.testdamage` |
| `/mmocadm playerdata view <player>` | Views a player's profile data. | `mmocraft.admin.playerdata.view` |
| `/mmocadm playerdata setstat <player> <stat> <value>` | Sets a player's core stat. | `mmocraft.admin.playerdata.setstat` |
| `/mmocadm playerdata setlevel <player> <level>` | Sets a player's level. | `mmocraft.admin.playerdata.setlevel` |
| `/mmocadm playerdata addxp <player> <amount>` | Adds experience to a player. | `mmocraft.admin.playerdata.addxp` |
| `/mmocadm playerdata addcurrency <player> <amount>`| Adds or removes currency from a player. | `mmocraft.admin.playerdata.addcurrency` |
| `/mmocadm demo <status|enable|disable|reload>` | Inspect or change the bundled demo content toggles. | `mmocraft.admin.demo` |
| `/mmocadm diagnostics` | Runs a plugin health report and logs warnings/errors. | `mmocraft.admin.diagnostics` |
| `/mmocadm issues` | Lists outstanding warnings or errors from the diagnostics service. | `mmocraft.admin.diagnostics` |
| `/mmocadm reloadconfig` | Reloads `mmocraft.conf` and the gameplay configuration bundle. | `mmocraft.admin.reload` |

## For Developers

MMOCraft is designed to be extensible. The core of the plugin is a set of services that manage different aspects of the game.

### Core Services
*   **`ConfigService`:** Manages `mmocraft.conf`.
*   **`EventBusService`:** A custom event bus for decoupled intra-plugin communication.
*   **`PersistenceService`:** Handles all database interactions with SQLite.
*   **`CommandRegistryService`:** Simplifies command registration.
*   **`CustomItemRegistry`:** Manages all custom item definitions.
*   **`RecipeRegistryService`:** Manages all custom crafting recipes.
*   **`CustomSpawningService`:** Manages custom mob spawning rules.
*   **`LootService`:** Manages loot tables.
*   **`PlayerDataService`:** Manages the lifecycle of `PlayerProfile` objects.

To add new features, you can interact with these services, listen to custom events, or create new implementations of the core models (e.g., new `Skill`s, new `CustomItem`s).

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
