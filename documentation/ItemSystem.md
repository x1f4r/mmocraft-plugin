# MMOCraft Item, Equipment & Loot System

This document provides a comprehensive overview of the custom item system, how equipment affects player statistics, the loot drop mechanics, and the foundational framework for custom crafting within MMOCraft.

---
## Contents

1.  [Custom Item Framework](#1-custom-item-framework)
    *   [Creating Custom Items (`CustomItem.java`)](#creating-custom-items-customitemjava)
    *   [NBT Tagging](#nbt-tagging)
    *   [Custom Item Registry (`CustomItemRegistry`)](#custom-item-registry-customitemregistry)
2.  [Item Stat Modifiers](#2-item-stat-modifiers)
3.  [Item Rarity (`ItemRarity.java`)](#3-item-rarity-itemrarityjava)
4.  [Equipment System](#4-equipment-system)
    *   [`PlayerEquipmentManager`](#playerequipmentmanager)
    *   [`PlayerEquipmentListener`](#playerequipmentlistener)
    *   [Integration with `PlayerProfile`](#integration-with-playerprofile)
5.  [Item Generation & Loot Drops](#5-item-generation--loot-drops)
    *   [`LootTableEntry` and `LootTable`](#loottableentry-and-loottable)
    *   [`LootService`](#lootservice)
    *   [`MobDeathLootListener`](#mobdeathlootlistener)
    *   [Example Loot Table](#example-loot-table)
6.  [Basic Crafting System (Foundation)](#6-basic-crafting-system-foundation)
    *   [Core Crafting Models](#core-crafting-models)
    *   [`RecipeRegistryService`](#reciperegistryservice)
    *   [`CraftingUIManager` and `/customcraft`](#craftinguimanager-and-customcraft)
7.  [Item-Related Admin Commands](#7-item-related-admin-commands)
    *   [`/mmocadm item give`](#mmocadm-item-give)

---

## 1. Custom Item Framework

MMOCraft features a robust system for creating and managing custom items that go beyond vanilla Minecraft capabilities. These items can have unique IDs, display names, lore, materials, custom model data, and special NBT tags.

### Creating Custom Items (`CustomItem.java`)

(`com.x1f4r.mmocraft.item.model.CustomItem.java`)

To define a new custom item, you extend the `CustomItem` abstract class. This class provides a structured way to specify the properties of your item.

**Key Abstract Methods/Properties (to be implemented by subclasses):**

*   **`public abstract String getItemId();`**
    *   Returns a unique internal string identifier for the item (e.g., `"ruby_sword"`, `"health_potion_tier1"`). This ID is crucial for internal tracking and NBT tagging.
*   **`public abstract Material getMaterial();`**
    *   Returns the base Bukkit `Material` that this custom item will use (e.g., `Material.IRON_SWORD`, `Material.POTION`).
*   **`public abstract String getDisplayName();`**
    *   Returns the item's display name as it should appear in-game. This string should typically be uncolored; the item's rarity will automatically prepend the appropriate color. Supports Bukkit color codes (e.g., `&cMy Item`).
*   **`public abstract List<String> getLore();`**
    *   Returns a list of strings that will form the item's lore. Each string is a new line. Supports Bukkit color codes. Stat modifiers and rarity are often added to the lore automatically by the system.

**Optional Overrides (with default behaviors):**

*   **`public int getCustomModelData() { return 0; }`**
    *   Allows specifying a custom model data value for resource packs, enabling unique item textures. Defaults to 0 (no custom model).
*   **`public boolean isUnbreakable() { return false; }`**
    *   Determines if the item should be unbreakable. Defaults to `false`.
*   **`public boolean hasEnchantGlint() { return false; }`**
    *   If true, the item will have an enchantment glint effect, even without actual enchantments. (Note: The actual implementation for this might involve dummy enchantments and item flags, currently a placeholder).
*   **`public ItemRarity getRarity() { return ItemRarity.COMMON; }`** (See [ItemRarity](#3-item-rarity-itemrarityjava))
    *   Determines the item's rarity, which affects display name color and lore. Defaults to `COMMON`.
*   **`public Map<Stat, Double> getStatModifiers() { return Collections.emptyMap(); }`** (See [Item Stat Modifiers](#2-item-stat-modifiers))
    *   Defines stat bonuses the item provides when equipped or used.

**Core Method: `createItemStack(int amount)`**

Each `CustomItem` subclass inherits the `createItemStack(int amount)` method. This method:
1.  Creates a new `ItemStack` of the defined `getMaterial()` and specified `amount`.
2.  Retrieves the `ItemMeta`.
3.  Applies the display name (prefixing the rarity color, and colorizing the name).
4.  Applies the base lore (colorizing each line).
5.  Automatically appends stat modifiers and rarity information to the lore.
6.  Sets custom model data and unbreakability if specified.
7.  **Crucially, tags the item with its unique ID.** (See [NBT Tagging](#nbt-tagging)).
8.  Returns the fully constructed `ItemStack`.

### NBT Tagging

(`com.x1f4r.mmocraft.item.api.NBTUtil.java`)

To distinguish custom items from vanilla items and to store persistent data on them, MMOCraft uses NBT (Named Binary Tag) tags via Bukkit's `PersistentDataContainer` API.

*   The `NBTUtil` class provides static helper methods to easily read and write NBT data to `ItemStack`s.
    *   `setString(ItemStack item, String key, String value, MMOCraftPlugin plugin)`
    *   `getString(ItemStack item, String key, MMOCraftPlugin plugin)`
    *   Similar methods for `Int`, `Double`, `Boolean`.
    *   `hasTag(ItemStack item, String key, MMOCraftPlugin plugin)`
*   All custom NBT tags are namespaced using a `NamespacedKey` (e.g., `new NamespacedKey(plugin, "my_custom_tag")`) to prevent conflicts with vanilla or other plugins.
*   **`CustomItem.CUSTOM_ITEM_ID_NBT_KEY` ("MMOCRAFT_ITEM_ID"):** When a `CustomItem` is created via `createItemStack()`, it automatically receives an NBT string tag with this key, and the value is the item's unique `itemId` (e.g., "ruby_sword"). This tag is fundamental for the system to identify an `ItemStack` as a specific custom item.

The static method `CustomItem.getItemId(ItemStack itemStack, MMOCraftPlugin plugin)` uses `NBTUtil` to retrieve this ID tag from any given `ItemStack`.

### Custom Item Registry (`CustomItemRegistry`)

(`com.x1f4r.mmocraft.item.service.BasicCustomItemRegistry.java`)

The `CustomItemRegistry` service is responsible for managing all defined `CustomItem` types within the plugin.

*   **Registration:**
    *   `void registerItem(CustomItem item)`: Concrete `CustomItem` instances are registered with this service, typically during plugin startup in `MMOCraftPlugin#onEnable()`.
*   **Retrieval:**
    *   `Optional<CustomItem> getCustomItem(String itemId)`: Fetches a `CustomItem` definition by its unique ID (case-insensitive).
    *   `Optional<CustomItem> getCustomItem(ItemStack itemStack)`: This is a key method that inspects an `ItemStack`, reads its "MMOCRAFT_ITEM_ID" NBT tag using `CustomItem.getItemId()`, and then retrieves the corresponding `CustomItem` definition from the registry. This allows the system to understand what custom item an `ItemStack` represents.
*   **Creation:**
    *   `ItemStack createItemStack(String itemId, int amount)`: A convenience method to directly create an `ItemStack` of a registered custom item by its ID.
*   **Listing:**
    *   `Collection<CustomItem> getAllItems()`: Returns all registered `CustomItem` definitions.

This registry is essential for looking up item behaviors, stats, and other properties when interacting with custom items in inventories, loot drops, or crafting.

---

## 2. Item Stat Modifiers

Custom items can directly modify a player's core stats (defined in the `Stat` enum) when equipped.

**Defining Stat Modifiers in `CustomItem`:**

To make a custom item provide stat bonuses, override the `getStatModifiers()` method in your `CustomItem` subclass:

```java
// In your CustomItem subclass (e.g., MightyHelmet.java)
@Override
public Map<Stat, Double> getStatModifiers() {
    Map<Stat, Double> mods = new EnumMap<>(Stat.class);
    mods.put(Stat.STRENGTH, 5.0);  // Adds 5 Strength
    mods.put(Stat.VITALITY, 10.0); // Adds 10 Vitality
    mods.put(Stat.DEFENSE, -2.0); // Example: Reduces Defense by 2
    return mods;
}
```

**Automatic Lore Generation:**

When an `ItemStack` is created for a custom item using `customItem.createItemStack()`, these stat modifiers are automatically formatted and added to the item's lore.
*   Positive values are shown in green (e.g., `+5.0 Strength`).
*   Negative values are shown in red (e.g., `-2.0 Defense`).
*   A separator line (`&7----------`) is added between the item's base lore and the stat modifiers section if both are present.

Example Lore:
```
A helm of ancient power.
Provides good protection.
&7----------
&a+10.0 Vitality
&c-2.0 Defense
```

**Integration with `PlayerProfile`:**

*   The `PlayerProfile` class now contains a map `equipmentStatModifiers` (`EnumMap<Stat, Double>`) to store the sum of all stat modifiers from currently equipped custom items.
*   The `PlayerEquipmentManager` (see [Equipment System](#4-equipment-system)) is responsible for updating these modifiers when a player's equipment changes.
*   **`PlayerProfile.getBaseStatValue(Stat stat)`**: Returns the player's natural base value for a stat (from leveling, character creation, etc.).
*   **`PlayerProfile.getEquipmentStatModifier(Stat stat)`**: Returns the total bonus for a stat from all equipped items.
*   **`PlayerProfile.getStatValue(Stat stat)`**: This crucial method now returns the player's *effective* stat value:
    `getBaseStatValue(stat) + getEquipmentStatModifier(stat)`.
*   Whenever equipment modifiers change (and thus `getStatValue` would return a different result), the `PlayerProfile.recalculateDerivedAttributes()` method is called by the `PlayerEquipmentManager`. This ensures that attributes like Max Health (from Vitality), Max Mana (from Wisdom), critical hit chance, etc., are all updated to reflect the player's current gear.

This system allows items to directly influence player capabilities in a dynamic and integrated way.

---

## 3. Item Rarity (`ItemRarity.java`)

(`com.x1f4r.mmocraft.item.model.ItemRarity.java`)

The `ItemRarity` enum defines different tiers of item quality, which primarily affect the visual presentation of custom items.

**Defined Rarities:**

Each rarity has a `displayName` (a pre-colorized string for lore) and a `ChatColor` (for coloring the item's name).

*   `COMMON` ("&fCommon", `ChatColor.WHITE`)
*   `UNCOMMON` ("&aUncommon", `ChatColor.GREEN`)
*   `RARE` ("&9Rare", `ChatColor.BLUE`)
*   `EPIC` ("&5Epic", `ChatColor.DARK_PURPLE`)
*   `LEGENDARY` ("&6Legendary", `ChatColor.GOLD`)
*   `MYTHIC` ("&cMythic", `ChatColor.RED`)
*   `UNIQUE` ("&eUnique", `ChatColor.YELLOW`)

**Usage in `CustomItem`:**

*   Subclasses of `CustomItem` can override the `getRarity()` method:
    ```java
    @Override
    public ItemRarity getRarity() {
        return ItemRarity.LEGENDARY;
    }
    ```
    If not overridden, it defaults to `ItemRarity.COMMON`.
*   **Display Name Coloring:** When `customItem.createItemStack()` is called, the `ItemRarity`'s `ChatColor` is automatically prepended to the item's display name. For example, a `LEGENDARY` item named "Sun Blade" will have its display name rendered in gold. The base display name returned by `getDisplayName()` in the `CustomItem` subclass should ideally be uncolored to allow the rarity system to apply coloring consistently.
*   **Lore Addition:** The rarity's `displayName` (e.g., "&6Legendary") is automatically added as a line in the item's lore if the rarity is not `COMMON` (or if other lore/stats are present, to maintain a consistent structure). This appears after stat modifiers.

This system provides an immediate visual cue to players about an item's quality and potential power level.

---

## 4. Equipment System

The Equipment System is responsible for tracking what custom items a player has equipped and applying their stat modifiers to the player's overall stats.

### `PlayerEquipmentManager`

(`com.x1f4r.mmocraft.item.equipment.service.PlayerEquipmentManager.java`)

This service contains the core logic for updating a player's stats based on their gear.

*   **`updateEquipmentStats(Player player)` Method:**
    1.  Retrieves the `PlayerProfile` for the given Bukkit `Player`.
    2.  Calls `playerProfile.clearEquipmentStatModifiers()` to remove any stat bonuses from previously equipped items. This method itself *does not* trigger a recalculation of derived stats.
    3.  Iterates through the player's relevant equipment slots:
        *   Main hand (`player.getInventory().getItemInMainHand()`)
        *   Off-hand (`player.getInventory().getItemInOffHand()`)
        *   Armor slots (`getHelmet()`, `getChestplate()`, `getLeggings()`, `getBoots()`)
    4.  For each `ItemStack` found in these slots:
        *   It uses `CustomItemRegistry.getCustomItem(itemStack)` to check if the item is a registered custom item (by looking for the "MMOCRAFT_ITEM_ID" NBT tag).
        *   If it is a custom item, it calls `customItem.getStatModifiers()` to get the stats provided by that item.
        *   These stats are aggregated into a temporary map (`collectiveModifiers`).
    5.  After checking all equipment, `playerProfile.addAllEquipmentStatModifiers(collectiveModifiers)` is called. This method also *does not* trigger a recalculation by itself.
    6.  Finally, `playerProfile.recalculateDerivedAttributes()` is called **once**. This ensures all derived stats (Max Health, Max Mana, crit chance, etc.) are updated based on the new sum of equipment stat modifiers and the player's base stats.

This process ensures that player stats accurately reflect their current gear.

### `PlayerEquipmentListener`

(`com.x1f4r.mmocraft.item.equipment.listeners.PlayerEquipmentListener.java`)

This Bukkit event listener triggers the `PlayerEquipmentManager.updateEquipmentStats()` method when events occur that might indicate a change in a player's equipped items.

**Listened Events:**

*   **`PlayerJoinEvent`**: Updates stats when a player logs in, based on what they are wearing.
*   **`PlayerRespawnEvent`**: Updates stats after a player respawns, as their inventory/equipment might have changed.
*   **`InventoryCloseEvent`**: If the inventory closed belongs to a player, their equipment stats are updated. This is a broader event that catches many cases of equipment changes (e.g., equipping armor, changing items in hand if the main inventory was accessed).
*   **`PlayerItemHeldEvent`**: When a player switches their held item slot (e.g., scrolling through the hotbar). This is important for items held in the main or off-hand that might provide stats.

For events like `PlayerJoinEvent`, `PlayerRespawnEvent`, and `PlayerItemHeldEvent`, the call to `updateEquipmentStats` is often scheduled with a 1-tick delay (`Bukkit.getScheduler().runTaskLater(plugin, ..., 1L);`). This can help ensure that Bukkit has fully processed the inventory change before MMOCraft attempts to read it.

More granular events (like `InventoryClickEvent` specifically for armor slots) could be added in the future for more immediate updates or performance tuning, but the current set provides good coverage for most common equipment change scenarios.

### Integration with `PlayerProfile`

As described in the [Item Stat Modifiers](#2-item-stat-modifiers) section:
*   `PlayerProfile` stores the sum of equipment stat bonuses in its `equipmentStatModifiers` map.
*   The `getStatValue(Stat stat)` method in `PlayerProfile` now combines the player's base core stat value with the total modifier for that stat from `equipmentStatModifiers`.
*   This combined value is then used by `recalculateDerivedAttributes()` to determine the final values for Max Health, Max Mana, critical hit chance, evasion, etc.

This ensures a clean separation of concerns: `CustomItem` defines the stats it gives, `PlayerEquipmentManager` aggregates these from equipped items, and `PlayerProfile` applies them to its overall stat calculations.

---

## 5. Item Generation & Loot Drops

This system defines how custom items are distributed in the game, primarily through mob drops.

### `LootTableEntry` and `LootTable`

*   **`LootTableEntry.java` (Record):**
    (`com.x1f4r.mmocraft.loot.model.LootTableEntry.java`)
    *   Defines a single potential item drop within a loot table.
    *   **Fields:**
        *   `customItemId` (String): The ID of the `CustomItem` to drop.
        *   `dropChance` (double): Probability from 0.0 to 1.0 for this item to drop.
        *   `minAmount` (int): Minimum quantity to drop if selected.
        *   `maxAmount` (int): Maximum quantity to drop.
*   **`LootTable.java` (Class):**
    (`com.x1f4r.mmocraft.loot.model.LootTable.java`)
    *   Represents a collection of `LootTableEntry` objects.
    *   **Fields:**
        *   `lootTableId` (String): A unique identifier for the loot table (e.g., "zombie_common_drops").
        *   `entries` (List<LootTableEntry>): The list of potential drops.
    *   **Method: `List<ItemStack> generateLoot(CustomItemRegistry itemRegistry, MMOCraftPlugin plugin)`:**
        *   This is the core logic for determining drops. It iterates through its entries.
        *   For each entry, it rolls against `dropChance`.
        *   If successful, it determines a random amount between `minAmount` and `maxAmount`.
        *   It then uses the provided `itemRegistry` to create the actual `ItemStack` for the `customItemId`.
        *   Returns a list of all `ItemStack`s that were successfully generated.

### `LootService`

(`com.x1f4r.mmocraft.loot.service.BasicLootService.java`)

The `LootService` manages all loot tables and handles the action of dropping items in the world.

*   **Registration:**
    *   `registerLootTable(EntityType mobType, LootTable lootTable)`: Associates a specific `LootTable` with a vanilla `EntityType`.
    *   `registerLootTableById(LootTable lootTable)`: Registers a `LootTable` by its ID, allowing for non-mob-specific loot sources (e.g., chests, quest rewards - future use).
*   **Retrieval:**
    *   `Optional<LootTable> getLootTable(EntityType mobType)`
    *   `Optional<LootTable> getLootTableById(String lootTableId)`
*   **Spawning Loot:**
    *   `void spawnLoot(Location location, List<ItemStack> itemsToDrop)`: Takes a list of items and drops them naturally at the specified world location (`world.dropItemNaturally()`).

### `MobDeathLootListener`

(`com.x1f4r.mmocraft.loot.listeners.MobDeathLootListener.java`)

This listener ties the loot system to mob deaths.

*   Listens to `EntityDeathEvent`.
*   Checks if the killed entity was a mob and if it was killed by a player.
*   If so, it retrieves the appropriate `LootTable` for the mob's `EntityType` from the `LootService`.
*   If a loot table is found:
    1.  It calls `lootTable.generateLoot()` to get a list of custom item drops.
    2.  If custom drops are generated, it **clears all vanilla drops** from the event (`event.getDrops().clear()`). This ensures that only the custom-defined loot is dropped for mobs managed by this system.
    3.  It then calls `lootService.spawnLoot()` to drop the custom items at the mob's death location.
*   If no custom loot table is registered for the mob type, vanilla drops are unaffected.

### Example Loot Table

During plugin startup (`MMOCraftPlugin#registerDefaultLootTables()`), a sample loot table is registered for Zombies:

```java
// In MMOCraftPlugin.java
LootTable zombieLootTable = new LootTable("zombie_common_drops", List.of(
    new LootTableEntry("simple_sword", 0.05, 1, 1) // 5% chance to drop 1 "simple_sword"
));
lootService.registerLootTable(EntityType.ZOMBIE, zombieLootTable);
```
This means when a player kills a Zombie, there's a 5% chance it will drop the "simple_sword" custom item, and its vanilla drops (like rotten flesh) will be cleared.

---

## 6. Basic Crafting System (Foundation)

MMOCraft includes a foundational framework for a custom crafting system, designed to eventually support complex, multi-step crafting processes potentially using custom user interfaces (UIs).

### Core Crafting Models

*   **`RecipeType.java` (Enum):**
    (`com.x1f4r.mmocraft.crafting.model.RecipeType.java`)
    *   Defines the nature of a recipe. Current relevant types for custom UI crafting:
        *   `CUSTOM_SHAPED`: Ingredients must be in a specific pattern within a custom UI.
        *   `CUSTOM_SHAPELESS`: Ingredient order and placement do not matter within a custom UI.
    *   (Includes `WORKBENCH_SHAPED` and `WORKBENCH_SHAPELESS` for potential future vanilla workbench integration).
*   **`CustomRecipeIngredient.java` (Class):**
    (`com.x1f4r.mmocraft.crafting.model.CustomRecipeIngredient.java`)
    *   Represents a single ingredient required for a recipe.
    *   **Fields:**
        *   `type` (Enum `IngredientType`: `VANILLA_MATERIAL`, `CUSTOM_ITEM`): Specifies if the ingredient is a standard Minecraft material or a registered `CustomItem`.
        *   `identifier` (String): The `Material.name()` for vanilla items, or the `customItemId` for custom items.
        *   `quantity` (int): The amount of this ingredient required.
        *   `matchNBT` (boolean): (Advanced) Whether NBT data should be strictly matched for this ingredient (currently defaults to false, primarily matching by item ID for custom items).
    *   **Method `matches(ItemStack itemStack, ...)`:** Determines if a given `ItemStack` in a crafting grid slot satisfies this ingredient's criteria.
*   **`CustomRecipe.java` (Class):**
    (`com.x1f4r.mmocraft.crafting.recipe.CustomRecipe.java`)
    *   Defines a complete crafting recipe.
    *   **Fields:**
        *   `recipeId` (String): A unique identifier for the recipe.
        *   `outputItemStack` (ItemStack): The item produced by this recipe. This can be a vanilla item or a custom item (fully defined `ItemStack` with NBT).
        *   `recipeType` (RecipeType): The type of this recipe (e.g., `CUSTOM_SHAPED`).
        *   `ingredients`:
            *   For shapeless: `List<CustomRecipeIngredient> shapelessIngredients`.
            *   For shaped: `Map<Integer, CustomRecipeIngredient> shapedIngredients` (where the key is the slot index in the crafting grid, e.g., 0-8 for a 3x3 grid).
        *   `permissionRequired` (String, optional): A permission node a player might need to craft this recipe.

### `RecipeRegistryService`

(`com.x1f4r.mmocraft.crafting.service.BasicRecipeRegistryService.java`)

This service manages all defined custom recipes.

*   **`registerRecipe(CustomRecipe recipe)`**: Adds new recipes to the system, typically at startup.
*   **`getRecipeById(String recipeId)`**: Retrieves a specific recipe.
*   **`findMatchingRecipe(RecipeType type, Inventory craftingGridInventory)`**:
    *   This is the core logic for determining if the items in a crafting grid match a registered recipe.
    *   The current implementation in `BasicRecipeRegistryService` has **very basic placeholder logic for shapeless recipes** (it attempts to count items and match).
    *   **Shaped recipe matching is not yet fully implemented** and will require significant work to compare the grid's item layout against stored recipe patterns.
*   **`getAllRecipes()`**: Returns all registered recipes.

### `CraftingUIManager` and `/customcraft`

*   **`CraftingUIManager.java` (Class):**
    (`com.x1f4r.mmocraft.crafting.ui.CraftingUIManager.java`)
    *   Intended to manage custom crafting interfaces for players.
    *   **`openCraftingUI(Player player)`:** Currently opens a very basic 5-row inventory titled "&8Custom Crafting Table" with some filler items and a placeholder "Craft" button. This is *not* yet a functional crafting grid.
    *   It includes basic `InventoryClickEvent` and `InventoryCloseEvent` handlers to manage this placeholder UI (e.g., returning items placed in the grid on close).
    *   The full implementation of a custom crafting grid UI, handling item placement, recipe checking, and crafting execution, is a significant future task.
*   **`/customcraft` Command:**
    *   (`com.x1f4r.mmocraft.command.commands.CustomCraftCommand.java`)
    *   **Permission:** `mmocraft.command.customcraft`
    *   When executed by a player, it calls `craftingUIManager.openCraftingUI(player)`, displaying the current placeholder UI.

The crafting system is in its very early stages. The models for recipes and ingredients are defined, and a basic UI can be opened, but the critical logic for matching items in a grid to shaped/shapeless recipes and executing the craft is largely placeholder and requires future development.

---

## 7. Item-Related Admin Commands

Administrators have commands to manage and interact with the custom item system. These are typically grouped under `/mmocadm item`.

### `/mmocadm item give`

*   **Purpose:** Gives a specified custom item to a player.
*   **Usage:** `/mmocadm item give <playerName> <customItemId> [amount]`
*   **Permission:** `mmocraft.admin.item.give`
*   **Arguments:**
    *   `<playerName>`: The name of the online player to receive the item(s).
    *   `<customItemId>`: The unique ID of the custom item (as registered in `CustomItemRegistry`).
    *   `[amount]` (Optional): The quantity of the item to give. Defaults to 1 if not specified.
*   **Functionality:**
    1.  Validates arguments and permissions.
    2.  Ensures the target player is online.
    3.  Looks up the `customItemId` in the `CustomItemRegistry`.
    4.  If found, creates the specified `amount` of the item using `customItem.createItemStack()`.
    5.  Adds the item(s) to the target player's inventory.
    6.  Sends confirmation messages to the command sender and the target player.

---
