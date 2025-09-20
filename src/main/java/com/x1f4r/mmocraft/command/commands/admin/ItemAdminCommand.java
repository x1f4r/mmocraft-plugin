package com.x1f4r.mmocraft.command.commands.admin;

import com.x1f4r.mmocraft.command.AbstractPluginCommand;
import com.x1f4r.mmocraft.command.CommandExecutable;
import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.item.model.ItemAbilityDescriptor;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.util.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ItemAdminCommand extends AbstractPluginCommand {

    private final CustomItemRegistry customItemRegistry;
    private final LoggingUtil logger;

    private static final String PERM_ITEM_BASE = "mmocraft.admin.item";
    private static final String PERM_ITEM_GIVE = PERM_ITEM_BASE + ".give";
    private static final String PERM_ITEM_LIST = PERM_ITEM_BASE + ".list";

    public ItemAdminCommand(MMOCraftPlugin plugin) {
        super("item", PERM_ITEM_BASE, "Admin commands for managing custom items.");
        this.customItemRegistry = plugin.getCustomItemRegistry();
        this.logger = plugin.getLoggingUtil();

        registerSubCommand("give", new CommandExecutable() {
            @Override
            public boolean onCommand(CommandSender sender, String[] args) {
                return executeGive(sender, args);
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String[] args) {
                if (!sender.hasPermission(PERM_ITEM_GIVE)) {
                    return Collections.emptyList();
                }

                if (args.length == 0) {
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .collect(Collectors.toList());
                }

                if (args.length == 1) {
                    String partialPlayer = args[0].toLowerCase(Locale.ROOT);
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(partialPlayer))
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .collect(Collectors.toList());
                }

                if (args.length == 2) {
                    String inputItemId = args[1].toLowerCase(Locale.ROOT);
                    return customItemRegistry.getAllItems().stream()
                            .map(CustomItem::getItemId)
                            .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(inputItemId))
                            .sorted()
                            .collect(Collectors.toList());
                }

                if (args.length == 3) {
                    return List.of("1", "16", "32", "64");
                }

                return Collections.emptyList();
            }
        });
        registerSubCommand("list", new CommandExecutable() {
            @Override
            public boolean onCommand(CommandSender sender, String[] args) {
                return executeList(sender, args);
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String[] args) {
                if (!sender.hasPermission(PERM_ITEM_LIST)) {
                    return Collections.emptyList();
                }

                if (args.length == 0) {
                    return new ArrayList<>(subCommands.keySet());
                }

                if (args.length == 1) {
                    String partial = args[0].toLowerCase(Locale.ROOT);
                    return customItemRegistry.getAllItems().stream()
                            .map(CustomItem::getItemId)
                            .filter(id -> id.toLowerCase(Locale.ROOT).contains(partial))
                            .sorted()
                            .collect(Collectors.toList());
                }

                if (args.length == 2) {
                    return List.of("1", "2", "3");
                }
                return Collections.emptyList();
            }
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (permission != null && !permission.isEmpty() && !sender.hasPermission(permission)) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommandName = args[0].toLowerCase(Locale.ROOT);
        CommandExecutable executable = subCommands.get(subCommandName);
        if (executable != null) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return executable.onCommand(sender, subArgs);
        }

        sender.sendMessage(Component.text("Unknown item admin subcommand '" + args[0] + "'.", NamedTextColor.RED));
        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(StringUtil.colorize("&6--- Item Admin Help ---"));
        if (sender.hasPermission(PERM_ITEM_GIVE)) {
            sender.sendMessage(StringUtil.colorize("&e/mmocadm item give <player> <itemId> [amount] &7- Gives a custom item."));
        }
        if (sender.hasPermission(PERM_ITEM_LIST)) {
            sender.sendMessage(StringUtil.colorize("&e/mmocadm item list [filter] [page] &7- Lists registered custom items."));
        }
    }

    private boolean executeGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ITEM_GIVE)) {
            sender.sendMessage(Component.text("You don't have permission for this command.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /mmocadm item give <playerName> <customItemId> [amount]", NamedTextColor.RED));
            return true;
        }

        String playerName = args[0];
        String customItemId = args[1];
        int amount = 1;

        if (args.length > 2) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    sender.sendMessage(Component.text("Amount must be positive.", NamedTextColor.RED));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid amount: " + args[2], NamedTextColor.RED));
                return true;
            }
        }

        Player targetPlayer = Bukkit.getPlayerExact(playerName);
        if (targetPlayer == null) {
            sender.sendMessage(Component.text("Player '" + playerName + "' not found or not online.", NamedTextColor.RED));
            return true;
        }

        Optional<CustomItem> optCustomItem = customItemRegistry.getCustomItem(customItemId);
        if (optCustomItem.isEmpty()) {
            sender.sendMessage(Component.text("Custom item with ID '" + customItemId + "' not found.", NamedTextColor.RED));
            return true;
        }

        CustomItem customItem = optCustomItem.get();
        ItemStack itemToGive = customItem.createItemStack(amount);

        if (itemToGive == null || itemToGive.getType() == Material.AIR) {
            sender.sendMessage(Component.text("Failed to create item stack for '" + customItemId + "'. Check server logs.", NamedTextColor.RED));
            logger.severe("Failed to create valid ItemStack for CustomItem ID: " + customItemId + " in ItemAdminCommand.");
            return true;
        }

        targetPlayer.getInventory().addItem(itemToGive);
        String message = StringUtil.colorize("&aGave " + amount + " of " + customItem.getDisplayName() + " &a(&f" + customItem.getItemId() + "&a) to " + targetPlayer.getName() + ".");
        sender.sendMessage(message);
        targetPlayer.sendMessage(StringUtil.colorize("&aYou received " + amount + " of " + customItem.getDisplayName() + "&a."));
        logger.info(sender.getName() + " gave " + amount + " of " + customItem.getItemId() + " to " + targetPlayer.getName());
        return true;
    }

    private boolean executeList(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ITEM_LIST)) {
            sender.sendMessage(Component.text("You don't have permission for this command.", NamedTextColor.RED));
            return true;
        }

        if (customItemRegistry == null) {
            sender.sendMessage(Component.text("Item registry is unavailable.", NamedTextColor.RED));
            logger.severe("ItemAdminCommand#executeList invoked but customItemRegistry was null.");
            return true;
        }

        String filter = args.length >= 1 ? args[0].toLowerCase(Locale.ROOT) : "";
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ex) {
                sender.sendMessage(Component.text("Invalid page number '" + args[1] + "'.", NamedTextColor.RED));
                return true;
            }
        }

        List<CustomItem> items = new ArrayList<>(customItemRegistry.getAllItems());
        items.sort(Comparator.comparing(CustomItem::getItemId));

        List<CustomItem> filtered = items.stream()
                .filter(item -> filter.isEmpty()
                        || item.getItemId().toLowerCase(Locale.ROOT).contains(filter)
                        || StringUtil.stripColor(item.getDisplayName()).toLowerCase(Locale.ROOT).contains(filter))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            sender.sendMessage(StringUtil.colorize("&eNo custom items matched your query."));
            return true;
        }

        final int pageSize = 8;
        int totalPages = (int) Math.ceil(filtered.size() / (double) pageSize);
        page = Math.min(page, Math.max(totalPages, 1));
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());

        sender.sendMessage(StringUtil.colorize("&6--- Registered Custom Items (" + filtered.size() + " found, page " + page + "/" + totalPages + ") ---"));
        filtered.subList(fromIndex, toIndex).forEach(item -> {
            String header = "&e- &f" + item.getItemId() + " &7-> &r" + item.getDisplayName()
                    + " &7[" + item.getRarity().getDisplayName() + "&7]";
            sender.sendMessage(StringUtil.colorize(header));

            Map<Stat, Double> statModifiers = item.getStatModifiers();
            if (statModifiers != null && !statModifiers.isEmpty()) {
                String statSummary = statModifiers.entrySet().stream()
                        .map(this::formatStatEntry)
                        .collect(Collectors.joining("&7, "));
                sender.sendMessage(StringUtil.colorize("   &7Stats: " + statSummary));
            }

            List<ItemAbilityDescriptor> abilities = item.getAbilityDescriptors();
            if (abilities != null && !abilities.isEmpty()) {
                sender.sendMessage(StringUtil.colorize("   &6Abilities:"));
                for (ItemAbilityDescriptor ability : abilities) {
                    List<String> metaParts = new ArrayList<>();
                    if (!ability.activationHint().isBlank()) {
                        metaParts.add("&f" + ability.activationHint());
                    }
                    if (ability.consumesMana()) {
                        metaParts.add("&b" + formatNumber(ability.manaCost()) + " Mana");
                    }
                    if (ability.hasCooldown()) {
                        metaParts.add("&b" + formatNumber(ability.cooldownSeconds()) + "s CD");
                    }
                    String metaSection = metaParts.isEmpty()
                            ? ""
                            : " &7(" + String.join("&7 | ", metaParts) + ")";
                    sender.sendMessage(StringUtil.colorize("     &6• &e" + ability.displayName() + metaSection));

                    if (ability.hasSummary()) {
                        for (String line : wrapText(ability.summary(), 60)) {
                            sender.sendMessage(StringUtil.colorize("       &7" + line));
                        }
                    }
                }
            }

            List<String> recipeHints = item.getRecipeHints();
            if (recipeHints != null && !recipeHints.isEmpty()) {
                sender.sendMessage(StringUtil.colorize("   &7Recipe Hints:"));
                for (String hint : recipeHints) {
                    List<String> wrapped = wrapText(hint, 60);
                    if (wrapped.isEmpty()) {
                        continue;
                    }
                    for (int i = 0; i < wrapped.size(); i++) {
                        String prefix = i == 0 ? "     &8• &f" : "       &f";
                        sender.sendMessage(StringUtil.colorize(prefix + wrapped.get(i)));
                    }
                }
            }

            sender.sendMessage(StringUtil.colorize("   &8--------------------------------"));
        });

        logger.info("Item list requested by " + sender.getName() + ": " + filtered.size() + " match(es)");
        return true;
    }

    private String formatStatEntry(Map.Entry<Stat, Double> entry) {
        double value = entry.getValue();
        String color;
        String prefix;
        double magnitude = Math.abs(value);
        if (value > 0) {
            color = "&a";
            prefix = "+";
        } else if (value < 0) {
            color = "&c";
            prefix = "-";
        } else {
            color = "&7";
            prefix = "";
        }
        return "&f" + entry.getKey().getDisplayName() + ": " + color + prefix + formatNumber(magnitude);
    }

    private String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private List<String> wrapText(String text, int maxLineLength) {
        if (text == null) {
            return Collections.emptyList();
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> lines = new ArrayList<>();
        String[] words = trimmed.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.length() == 0) {
                current.append(word);
                continue;
            }
            if (current.length() + 1 + word.length() <= maxLineLength) {
                current.append(' ').append(word);
            } else {
                lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (permission != null && !permission.isEmpty() && !sender.hasPermission(permission)) {
            return Collections.emptyList();
        }

        if (args.length == 0) {
            return new ArrayList<>(subCommands.keySet());
        }

        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            return subCommands.keySet().stream()
                    .filter(name -> name.startsWith(partial))
                    .sorted()
                    .collect(Collectors.toList());
        }

        CommandExecutable sub = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (sub != null) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return sub.onTabComplete(sender, subArgs);
        }

        return Collections.emptyList();
    }
}
