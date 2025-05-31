# MMOCraft Core Systems Documentation

This document provides an overview of the core systems and services available in the MMOCraft plugin. These systems are designed to be foundational for building various game modules and features. They handle common tasks such as configuration management, event handling, data persistence, command registration, and utility functions.

Access to most services is typically provided via the main `MMOCraftPlugin` instance.

---
## Contents

1.  [ConfigService](#configservice)
2.  [EventBusService](#eventbusservice)
3.  [PersistenceService](#persistenceservice)
4.  [CommandRegistryService](#commandregistryservice)
5.  [Core Utilities](#core-utilities)
    *   [LoggingUtil](#loggingutil)
    *   [StringUtil](#stringutil)

---

## ConfigService

The `ConfigService` is responsible for managing the plugin's primary configuration file, `mmocraft.conf`. It allows other parts of the plugin to easily access configuration values.

### Getting the Service Instance

You can obtain an instance of the `ConfigService` from the `MMOCraftPlugin` main class:

```java
MMOCraftPlugin plugin = (MMOCraftPlugin) Bukkit.getPluginManager().getPlugin("MMOCraft");
if (plugin != null) {
    ConfigService configService = plugin.getConfigService();
    // Use configService instance
}
```

### Accessing Configuration Values

The service provides type-safe methods to retrieve values from the configuration file.

*   **`getString(String path)`**: Gets a string value.
    ```java
    String welcomeMessage = configService.getString("welcome-messages.default");
    // Assuming path in mmocraft.conf:
    // welcome-messages:
    //   default: "Welcome!"
    ```

*   **`getInt(String path)`**: Gets an integer value.
    ```java
    int maxHealth = configService.getInt("stats.max-health");
    // stats:
    //   max-health: 100
    ```

*   **`getBoolean(String path)`**: Gets a boolean value.
    ```java
    boolean pvpEnabled = configService.getBoolean("features.pvp-enabled");
    // features:
    //   pvp-enabled: true
    ```

*   **`getStringList(String path)`**: Gets a list of strings.
    ```java
    List<String> motdLines = configService.getStringList("motd.lines");
    // motd:
    //   lines:
    //     - "Line 1"
    //     - "Line 2"
    ```

*   **`getDouble(String path)`**: Gets a double value.
    ```java
    double baseDamage = configService.getDouble("stats.base-damage");
    // stats:
    //   base-damage: 5.5
    ```

### Reloading Configuration

The plugin's configuration can be reloaded using a command (if implemented, e.g., `/mmoc reload`) which typically calls a method in `MMOCraftPlugin`:

```java
// In MMOCraftPlugin.java
public void reloadPluginConfig() {
    if (configService != null) {
        configService.reloadConfig();
        loggingUtil.info("MMOCraft configuration reloaded.");
        // ... any other actions on reload ...
        if (eventBusService != null) {
            eventBusService.call(new PluginReloadedEvent());
        }
    }
}
```
This reloads `mmocraft.conf` from disk.

### `mmocraft.conf` Structure

The `mmocraft.conf` file is located in the plugin's data folder (`plugins/MMOCraft/mmocraft.conf`). It uses the **YAML** format. The service copies a default version from the plugin JAR to the data folder if one doesn't already exist.

**Example `mmocraft.conf`:**
```yaml
# core settings
core:
  debug-logging: false

# player stats
stats:
  max-health: 100
  base-damage: 5.0 # Numbers can be integers or doubles

# feature toggles
features:
  pvp-enabled: true

# list example
welcome-messages:
  - "Welcome to the MMOCraft server!"
  - "Enjoy your stay."
```

---

## EventBusService

The `EventBusService` provides a custom event bus for intra-plugin communication. This allows different modules or components of MMOCraft to interact with each other in a decoupled manner, reducing direct dependencies.

### Getting the Service Instance

```java
MMOCraftPlugin plugin = (MMOCraftPlugin) Bukkit.getPluginManager().getPlugin("MMOCraft");
if (plugin != null) {
    EventBusService eventBus = plugin.getEventBusService();
    // Use eventBus instance
}
```

### Defining a Custom Event

Custom events must extend the `com.x1f4r.mmocraft.eventbus.CustomEvent` abstract class.

```java
package com.x1f4r.mmocraft.somefeature.events;

import com.x1f4r.mmocraft.eventbus.CustomEvent;
import org.bukkit.entity.Player;

public class PlayerLeveledUpEvent extends CustomEvent {
    private final Player player;
    private final int newLevel;

    public PlayerLeveledUpEvent(Player player, int newLevel) {
        super(); // Calls CustomEvent constructor which sets eventName
        this.player = player;
        this.newLevel = newLevel;
    }

    public Player getPlayer() {
        return player;
    }

    public int getNewLevel() {
        return newLevel;
    }
}
```
The `CustomEvent` base class automatically provides a `getEventName()` method (which returns the simple class name).

### Defining an Event Handler

Event handlers implement the `com.x1f4r.mmocraft.eventbus.EventHandler<T>` functional interface, where `T` is the type of event they handle.

```java
package com.x1f4r.mmocraft.somefeature.listeners;

import com.x1f4r.mmocraft.somefeature.events.PlayerLeveledUpEvent;
import com.x1f4r.mmocraft.eventbus.EventHandler;
import org.bukkit.Bukkit;

public class LevelUpNotificationHandler implements EventHandler<PlayerLeveledUpEvent> {
    @Override
    public void handle(PlayerLeveledUpEvent event) {
        event.getPlayer().sendMessage("Congratulations! You reached level " + event.getNewLevel() + "!");
        Bukkit.broadcastMessage(event.getPlayer().getName() + " has reached level " + event.getNewLevel() + "!");
    }
}
```
Alternatively, you can use a lambda expression for simpler handlers.

### Registering Handlers and Calling Events

Handlers are registered with the `EventBusService`. Events are dispatched using the `call` method.

```java
// Somewhere during plugin initialization (e.g., in a module's enable logic)
EventBusService eventBus = mmocraftPluginInstance.getEventBusService();
LevelUpNotificationHandler levelUpHandler = new LevelUpNotificationHandler();

// Register the handler
eventBus.register(PlayerLeveledUpEvent.class, levelUpHandler);

// Using a lambda for another handler (if the event is simple enough)
eventBus.register(PluginReloadedEvent.class, event -> {
    mmocraftPluginInstance.getLoggingUtil().info("PluginReloadedEvent was handled by a lambda! Name: " + event.getEventName());
});


// Somewhere in your code when a player levels up
Player player = /* ... get the player ... */;
int newLevel = /* ... get the new level ... */;
PlayerLeveledUpEvent levelUpEvent = new PlayerLeveledUpEvent(player, newLevel);

// Call/dispatch the event. All registered handlers for PlayerLeveledUpEvent (and its supertypes) will be invoked.
eventBus.call(levelUpEvent);
```

The event bus supports polymorphic dispatch, meaning a handler registered for `CustomEvent` would receive all custom events, or a handler for an intermediate abstract event class would receive events from all its concrete subclasses.

The service also provides an `unregister(Class<T> eventType, EventHandler<T> handler)` method to remove handlers.

---

## PersistenceService

The `PersistenceService` is responsible for handling all database interactions. Currently, it's implemented to use an SQLite database stored in `plugins/MMOCraft/mmocraft_data.db`.

### Getting the Service Instance

```java
MMOCraftPlugin plugin = (MMOCraftPlugin) Bukkit.getPluginManager().getPlugin("MMOCraft");
if (plugin != null) {
    PersistenceService persistenceService = plugin.getPersistenceService();
    // Use persistenceService instance
}
```

### Database Initialization and Shutdown

*   **`initDatabase()`**: This method is automatically called when `MMOCraftPlugin` is enabled. It sets up the database connection and ensures necessary tables are created (e.g., a sample `plugin_info` table).
*   **`close()`**: This method is automatically called when `MMOCraftPlugin` is disabled, closing the database connection gracefully.

### Getting a JDBC Connection

For complex database operations not covered by helper methods, you can get a direct JDBC `Connection`:

```java
try (Connection conn = persistenceService.getConnection()) {
    // Perform complex SQL operations using conn
    // Example: Start a transaction, use complex PreparedStatement, etc.
} catch (SQLException e) {
    // Handle exception
    plugin.getLoggingUtil().severe("Database operation failed: " + e.getMessage(), e);
}
```
It's crucial to use try-with-resources or manually close the connection when done if you are not using the helper methods. However, for most common operations, the helper methods are preferred as they manage connection and statement lifecycles.

### Helper Methods

The service provides helper methods to simplify common database tasks. These methods automatically handle `Connection` and `PreparedStatement` creation and closing.

#### `RowMapper<T>` Interface

Many query methods use a `RowMapper<T>` functional interface to map a `ResultSet` row to an object of type `T`.

```java
@FunctionalInterface
public interface RowMapper<T> {
    T mapRow(ResultSet rs) throws SQLException;
}

// Example implementation for a hypothetical PlayerData class
RowMapper<PlayerData> playerDataMapper = rs -> new PlayerData(
    UUID.fromString(rs.getString("uuid")),
    rs.getString("name"),
    rs.getInt("level")
);
```

#### `int executeUpdate(String sql, Object... params)`

Executes a DML statement (INSERT, UPDATE, DELETE).
*   `sql`: The SQL query with `?` placeholders.
*   `params`: Varargs objects to replace the placeholders.
*   Returns the number of affected rows.

```java
String sql = "INSERT INTO player_stats (uuid, health, mana) VALUES (?, ?, ?);";
int affectedRows = persistenceService.executeUpdate(sql, playerUUID.toString(), 100, 50);
```

#### `<T> Optional<T> executeQuerySingle(String sql, RowMapper<T> mapper, Object... params)`

Executes a query expected to return a single row (or none).
*   Returns an `Optional<T>` containing the mapped object if a row is found, otherwise `Optional.empty()`.

```java
String sql = "SELECT uuid, name, level FROM players WHERE name = ?;";
Optional<PlayerData> playerData = persistenceService.executeQuerySingle(sql, playerDataMapper, "PlayerName");

playerData.ifPresent(data -> {
    // Use data
});
```

#### `<T> List<T> executeQueryList(String sql, RowMapper<T> mapper, Object... params)`

Executes a query expected to return multiple rows.
*   Returns a `List<T>` of mapped objects. The list will be empty if no rows are found.

```java
String sql = "SELECT uuid, name, level FROM players WHERE level > ? ORDER BY level DESC;";
List<PlayerData> highLevelPlayers = persistenceService.executeQueryList(sql, playerDataMapper, 10);

for (PlayerData data : highLevelPlayers) {
    // Process each player
}
```

These helper methods significantly reduce boilerplate JDBC code and improve resource management.

---

## CommandRegistryService

The `CommandRegistryService` simplifies the registration of plugin commands. It works in conjunction with the `AbstractPluginCommand` base class to handle permissions, subcommands, and tab completion with less boilerplate.

### Getting the Service Instance

```java
MMOCraftPlugin plugin = (MMOCraftPlugin) Bukkit.getPluginManager().getPlugin("MMOCraft");
if (plugin != null) {
    CommandRegistryService commandRegistry = plugin.getCommandRegistryService();
    // Use commandRegistry instance
}
```

### Creating a Command Class

Custom commands should extend `com.x1f4r.mmocraft.command.AbstractPluginCommand`.

```java
package com.x1f4r.mmocraft.yourmodule.commands;

import com.x1f4r.mmocraft.command.AbstractPluginCommand;
import com.x1f4r.mmocraft.util.LoggingUtil; // If you need logging
import com.x1f4r.mmocraft.util.StringUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin; // If you need plugin instance

import java.util.Collections;
import java.util.List;

public class MyCustomCommand extends AbstractPluginCommand {

    private final JavaPlugin plugin;
    private final LoggingUtil logger;

    public MyCustomCommand(JavaPlugin plugin, String commandName, String permission, String description, LoggingUtil logger) {
        super(commandName, permission, description); // Call super constructor
        this.plugin = plugin;
        this.logger = logger;

        // Register a subcommand (e.g., /mycmd sub)
        registerSubCommand("sub", (sender, args) -> {
            sender.sendMessage(StringUtil.colorize("&aYou executed the 'sub' subcommand!"));
            if (args.length > 0) {
                sender.sendMessage(StringUtil.colorize("&bArguments: " + String.join(" ", args)));
            }
            return true;
        });
    }

    // This method is called if no subcommand matches, or if the base command is called.
    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        sender.sendMessage(StringUtil.colorize("&eThis is MyCustomCommand! Args length: " + args.length));
        logger.info(sender.getName() + " executed MyCustomCommand.");
        return true;
    }

    // This method is called for tab completion for the base command or unhandled subcommands.
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) { // Suggesting for the first argument (potential subcommand or direct arg)
            // AbstractPluginCommand handles suggesting registered subcommand names automatically.
            // You can add more suggestions here if your base command takes direct arguments.
            // e.g., return Arrays.asList("arg1", "arg2", "sub"); // "sub" would be redundant if registered
        }
        // If a registered subcommand is typed, its own onTabComplete (if defined) will be called.
        return Collections.emptyList(); // No further suggestions for this example
    }
}
```

**Key features of `AbstractPluginCommand`:**
*   **Constructor**: Takes command name, permission string (can be null/empty), and description.
*   **`registerSubCommand(String name, CommandExecutable executable)`**: Easily add subcommands. The `CommandExecutable` can be a separate class or a lambda.
*   **Permission Handling**: Automatically checks the permission defined in the constructor. If the sender lacks permission, a message is sent, and the command execution is halted.
*   **Subcommand Delegation**: If the first argument to the command matches a registered subcommand, execution and tab-completion are delegated to that subcommand's `CommandExecutable`.
*   **`onCommand(CommandSender sender, String[] args)`**: Abstract method you implement for the command's main logic (or logic when no subcommands are matched).
*   **`onTabComplete(CommandSender sender, String[] args)`**: Abstract method for tab completion logic. `AbstractPluginCommand` already provides tab completion for registered subcommand names.

### Registering the Command

In your plugin's `onEnable` method (or module initialization):

```java
// In MMOCraftPlugin.java or your module's setup
CommandRegistryService commandRegistry = plugin.getCommandRegistryService();
LoggingUtil logger = plugin.getLoggingUtil(); // Assuming logger is already initialized

commandRegistry.registerCommand(
    "mycmd", // The command name as defined in plugin.yml
    new MyCustomCommand(plugin, "mycmd", "myplugin.command.mycmd", "Does something custom.", logger)
);
```

### `plugin.yml` Definition

The command must still be defined in your `plugin.yml` file for Bukkit to recognize it. The `CommandRegistryService` then links your executor class to the command Bukkit knows.

```yaml
name: MMOCraft # Or your plugin's name
version: 0.1.0
main: com.x1f4r.mmocraft.core.MMOCraftPlugin # Or your plugin's main class
api-version: "1.21" # Or your target API

commands:
  mycmd: # This MUST match the name used in registerCommand
    description: "A custom command for doing things."
    usage: "/<command> [subcommand/args]"
    aliases: [customcmd, mc]
    permission: myplugin.command.mycmd # Optional: Base permission for Bukkit to check.
                                       # AbstractPluginCommand also checks its own permission string.
                                       # Good for consistency or if AbstractPluginCommand's permission is more granular.
  # Example from MMOCraft itself:
  mmoc:
    description: Base command for MMOCraft.
    usage: /<command> [subcommand]
    aliases: [mmo, mmocraft]
    permission: mmocraft.command.info
```

By using `AbstractPluginCommand` and `CommandRegistryService`, you centralize command logic, reduce boilerplate, and gain a consistent way to handle permissions and subcommands.

---

## Core Utilities

MMOCraft provides a set of utility classes to help with common tasks.

### LoggingUtil

`LoggingUtil` offers a standardized way to log messages to the server console, with plugin-specific prefixes and configurable debug levels.

#### Getting the Instance

```java
MMOCraftPlugin plugin = (MMOCraftPlugin) Bukkit.getPluginManager().getPlugin("MMOCraft");
if (plugin != null) {
    LoggingUtil logger = plugin.getLoggingUtil();
    // Use logger instance
}
```
Most services within MMOCraft receive a `LoggingUtil` instance via their constructor (Dependency Injection).

#### Usage Examples

All messages are automatically prefixed with `[YourPluginName]`.

*   **`logger.info(String message)`**: For general information.
    ```java
    logger.info("Player data loaded successfully for " + player.getName());
    ```

*   **`logger.warning(String message)`**: For potential issues that don't halt functionality.
    ```java
    logger.warning("Could not find optional dependency 'SomePlugin', feature X will be disabled.");
    ```

*   **`logger.severe(String message)`**: For critical errors.
    ```java
    logger.severe("Failed to initialize database connection! Plugin might not work correctly.");
    ```
*   **`logger.severe(String message, Throwable throwable)`**: For critical errors with an exception.
    ```java
    try {
        // some operation
    } catch (Exception e) {
        logger.severe("An unexpected error occurred: " + e.getMessage(), e);
    }
    ```

*   **`logger.debug(String message)`**: For detailed messages useful during development/debugging.
    These messages only appear if `core.debug-logging` is set to `true` in `mmocraft.conf`.
    ```java
    logger.debug("Player " + player.getName() + " attempting to perform action Y with data: " + someData);
    ```
    When debug logging is enabled, messages are prefixed with `[YourPluginName] [DEBUG]`.

The `LoggingUtil` also offers `fine`, `finer`, and `finest` methods for even more granular logging levels, corresponding to `java.util.logging.Level`.

### StringUtil

`StringUtil` is a class containing static methods for common string manipulations.

#### `StringUtil.colorize(String text)`

Translates Minecraft color codes (e.g., `&c`, `&l`, `&#RRGGBB`) into their displayable `ChatColor` equivalents.

```java
String rawMessage = "&cImportant: &aTask completed! &eDetails: &#FFFF00Warning!";
String coloredMessage = StringUtil.colorize(rawMessage);
player.sendMessage(coloredMessage);
// Output to player (with actual colors): Important: Task completed! Details: Warning!
```
This method relies on Bukkit's `ChatColor.translateAlternateColorCodes('&', text)`, which on modern platforms like Paper/Purpur also supports hex color codes (`&#RRGGBB`).

#### `StringUtil.stripColor(String text)`

Removes all Minecraft color codes from a string.

```java
String colored = "&aHello &cWorld";
String plain = StringUtil.stripColor(colored); // "Hello World"
// Useful for logging messages to console without color codes or for data storage.
```

#### `StringUtil.joinString(String[] args, String delimiter, int startIndex)`

Joins elements of a string array into a single string, starting from a specified index, using a given delimiter.

```java
String[] words = {"This", "is", "a", "sentence."};

String joined1 = StringUtil.joinString(words, " ", 0); // "This is a sentence."
String joined2 = StringUtil.joinString(words, "_", 1);  // "is_a_sentence."
```

---
