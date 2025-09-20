# MMOCraft Expansion Plan

## Vision
Transform the existing MMOCraft demo into a modular, data-driven MMORPG engine that can power a Hypixel SkyBlock-inspired experience on a procedurally generated Purpur server. The expanded engine must:

* Remain purely server-side while persisting all player progression and world state.
* Allow builders to assemble custom worlds, islands, and dungeons on top of randomly generated terrain.
* Blend vanilla Minecraft mechanics with deep, configurable RPG systems (combat, gathering, crafting, economy, social play).
* Enable content authors to ship features primarily through structured configuration packs while exposing extension points in code for bespoke behaviour.

## Guiding Principles
1. **Modularity First:** Each gameplay pillar (combat, items, crafting, skills, economy, world) owns a clear lifecycle service, config schema, and event surface.
2. **Config Pack Driven:** All content definitions (items, abilities, mobs, resources, quests, UI text) load from versioned data packs under `/plugins/MMOCraft/content/`. Hot-reload via `/mmocadm reloadconfig` remains the workflow.
3. **Persistence Everywhere:** SQLite (pluggable to MySQL later) tracks every per-player progression system, world node cooldown, auction listings, etc. Provide DAO services with async execution.
4. **Vanilla Compatibility:** Never break vanilla crafting, damage, interactions, or AI. Custom systems decorate rather than replace baseline behaviour.
5. **Observability:** Diagnostics extend to every subsystem. Each loader emits granular warnings/errors for misconfigured content.

## Target File & Config Layout
```
plugins/MMOCraft/
├── mmocraft.conf                # Global toggles & tuning
├── zones.yml                    # Static region definitions
├── content/
│   ├── packs.yml                # Pack registry & load order
│   ├── items/
│   │   ├── armor.yml            # Armor, accessories, set bonuses
│   │   ├── weapons.yml          # Melee/ranged/magic weapons
│   │   ├── tools.yml            # Gathering tools with tiers
│   │   └── consumables.yml      # Food, potions, scrolls
│   ├── abilities/
│   │   ├── active.yml           # Triggered skills & mana costs
│   │   └── passive.yml          # Stat modifiers, procs
│   ├── crafting/
│   │   ├── recipes.toml         # Custom + vanilla overrides
│   │   └── trees.yml            # Unlock trees per profession
│   ├── combat/
│   │   ├── mob_families.yml     # Shared stat templates
│   │   └── encounters.yml       # Bosses, minibosses, dungeons
│   ├── gathering/
│   │   ├── mining.yml           # Ore nodes, respawns, drops
│   │   ├── farming.yml          # Crops, replant rules, seasons
│   │   └── foraging.yml         # Trees, rare drops
│   ├── economy/
│   │   ├── vendors.yml          # NPC shops, stock, pricing
│   │   ├── auction.yml          # Auction house parameters
│   │   └── currency.yml         # Multiple currencies, sinks
│   ├── progression/
│   │   ├── skills.yml           # Player skill XP tables
│   │   ├── quests.yml           # Story & daily quests
│   │   └── achievements.yml     # Long-term goals
│   ├── world/
│   │   ├── islands.yml          # Schematics, biome themes
│   │   └── instanced.yml        # Dungeons, arenas, raids
│   └── ui/
│       ├── crafting_book.yml    # Crafting book categories
│       └── localization.yml     # Text overrides
└── runtime/                     # Generated state (databases, caches)
    ├── database.sqlite
    ├── nodes/                   # Resource node cooldown snapshots
    └── profiles/                # Optional JSON cache per player
```

Each pack may ship subsets of these files. Missing files imply defaults. Future tasks will introduce pack versioning & migration metadata.

## Phased Execution Roadmap
The implementation will progress through staged milestones. Each milestone concludes with integration tests, config examples, and documentation updates.

### Phase 0 – Discovery & Stabilisation
* Audit existing services, listeners, and configs.
* Refactor demo toggles into modular pack loader scaffolding.
* Expand diagnostics to report missing mandatory packs.
* Deliverable: `content/packs.yml` bootstrap, migration of demo content into pack format, documentation for new layout.

### Phase 1 – Item & Ability Overhaul
* Implement `ContentPackService` with pack registry, dependency resolution, version checks, and hot reload.
* Replace the current `CustomItemRegistry` data source with YAML/TOML-driven item descriptors.
* Support modular stat/attribute modifiers, rarity, requirement metadata, socketing placeholders, and ability hooks.
* Build `AbilityRegistry` loading active & passive abilities from configuration, mapping to scripted handlers.
* Extend equipment pipeline to watch for set bonuses and on-hit/on-use triggers.
* Deliverable: Config-defined item catalog with at least 20 showcase items across rarities and professions.

### Phase 2 – Combat Loop Expansion
* Unify vanilla and custom damage via a new `CombatCalculationPipeline` that respects vanilla armour, enchantments, and potion effects before applying custom modifiers.
* Introduce configurable mob families (shared stat templates, behaviour tags) and ability loadouts.
* Add boss encounter scripting (phases, enrage timers, spawn adds) leveraging the event bus.
* Implement threat & aggro tables for cooperative combat.
* Deliverable: Two combat zones (overworld + dungeon) with unique mobs, loot tables, and boss fight defined via config.

### Phase 3 – Gathering Professions
* Rework resource node system to support mining, farming, foraging, fishing, and special events.
* Nodes defined by config: respawn timers, tier requirements, drop tables, shared vs instanced state.
* Track profession XP and levels, granting perks and recipe unlocks.
* Deliverable: Mining tunnels, farming plots, tree groves with escalating node tiers and profession experience gains.

### Phase 4 – Crafting System & Custom Table
* Replace `/customcraft` UI with a rebranded `Arcane Workbench` tied to a custom block or GUI trigger.
* Support multi-tab recipe browser (vanilla + custom) with search, filtering by profession, and recipe detail pages.
* Ensure vanilla recipes remain functional within the custom UI and standard crafting table.
* Add recipe unlock conditions (quest, skill level, discovery) and track them per player.
* Deliverable: Fully featured crafting UI backed by `crafting_book.yml` and recipe definitions, plus data migration for vanilla recipes.

### Phase 5 – Economy & Social Systems
* Implement configurable vendors, buy/sell orders, and rotating stock.
* Build server-side auction house with listings persisted and tax sinks.
* Add trading post NPC or GUI for player-to-player trades with verification.
* Introduce daily/weekly quests, achievements, and challenges rewarding currencies and items.
* Deliverable: Economic loop enabling players to farm, craft, sell, and reinvest.

### Phase 6 – World & Content Pipeline
* Procedural island generator orchestrated by config-defined templates and biome palettes.
* Integrate structure placement & schematic pasting for handcrafted POIs.
* Instanced world support for dungeons/raids with entry requirements and reset logic.
* Deliverable: Randomised overworld with seeded islands plus at least one instanced dungeon accessible via portal NPC.

### Phase 7 – Polishing & Live Ops Tooling
* Comprehensive diagnostics covering every subsystem.
* Admin dashboards (commands + GUI) for monitoring queues, combat metrics, and player progression.
* Analytics hooks for external dashboards.
* Documentation refresh, sample content packs, and migration guide from the legacy demo.

## Cross-Cutting Tasks & Technical Debt
* Abstract persistence layer to support multiple databases.
* Improve async task scheduling and ensure thread safety across services.
* Create automated test suites (unit + integration) for loaders and gameplay calculations.
* Establish content validation CLI for CI pipelines.

## Step-by-Step Implementation Backlog
The following backlog breaks the phases into actionable tickets. Each ticket should result in a commit/PR with tests and documentation.

1. **Pack Loader Foundation**
   * Create `ContentPackService`, `ContentIndex`, and pack schema classes.
   * Migrate demo assets into `/content/default_pack/`.
2. **Diagnostics Integration**
   * Extend `PluginDiagnosticsService` to validate packs, item definitions, and recipe references.
3. **Item Schema Definition**
   * Design YAML schema for items with inheritance & modifiers.
   * Build parser, validation, and registry adapters.
4. **Ability Engine Update**
   * Load ability metadata, map to handler classes, wire to item triggers and skills.
5. **Combat Pipeline Refactor**
   * Insert new damage calculation stages, ensure vanilla compatibility tests.
6. **Mob Family Config**
   * Implement mob templates and spawn rule references.
7. **Boss Encounter Script DSL**
   * Create configuration-driven phase script with event triggers.
8. **Resource Node Overhaul**
   * Support multi-profession nodes with shared cooldown persistence.
9. **Profession Progression Service**
   * Track XP, perks, and gating for gathering and crafting.
10. **Crafting UI Redesign**
    * Build new GUI, integrate recipe book, unify vanilla/custom recipes.
11. **Recipe Unlock Tracking**
    * Persist unlock states, tie to quests/professions.
12. **Vendor & Auction Services**
    * Implement vendor config loader, NPC bindings, and auction house backend.
13. **Quest & Achievement System**
    * Config-driven questlines, progress tracking, rewards.
14. **Procedural World Generator**
    * Define island templates, integrate with chunk generation events.
15. **Instance Manager**
    * Handle dungeon instances, player queues, resets.
16. **Admin Tooling Enhancements**
    * Commands/GUI for pack status, economy, world management.
17. **Testing & CI Automation**
    * Build test harnesses, add GitHub Actions or Gradle tasks.
18. **Documentation & Samples**
    * Update docs per feature, ship reference content pack.

## Success Criteria
* Content creators can author a complete MMO gameplay loop (combat, gathering, crafting, economy, quests) without modifying Java code.
* Vanilla mechanics remain intuitive; players can switch between vanilla and custom interactions seamlessly.
* Server operators can hot-reload content safely and diagnose issues quickly.
* The plugin scales from a single-server adventure to a sharded multi-world experience with minimal code changes.

## Next Steps
With this plan approved, begin executing Backlog Item 1 (Pack Loader Foundation), ensuring new code remains fully covered by tests and documentation updates.
