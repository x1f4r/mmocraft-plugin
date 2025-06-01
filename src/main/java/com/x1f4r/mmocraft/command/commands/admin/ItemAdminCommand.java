package com.x1f4r.mmocraft.command.commands.admin;

import com.x1f4r.mmocraft.command.AbstractPluginCommand;
import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.util.StringUtil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ItemAdminCommand extends AbstractPluginCommand {

    private final MMOCraftPlugin plugin;
    private final CustomItemRegistry customItemRegistry;
    private final LoggingUtil logger;

    private static final String PERM_ITEM_BASE = "mmocraft.admin.item";
    private static final String PERM_ITEM_GIVE = PERM_ITEM_BASE + ".give";

    public ItemAdminCommand(MMOCraftPlugin plugin) {
        super("item", PERM_ITEM_BASE, "Admin commands for managing custom items.");
        this.plugin = plugin;
        this.customItemRegistry = plugin.getCustomItemRegistry();
        this.logger = plugin.getLoggingUtil();

        registerSubCommand("give", this::executeGive);
        // Add other item-related subcommands here like "list", "delete", "spawn"
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        // Base /mmocadm item command - show help for its subcommands
        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(StringUtil.colorize("&6--- Item Admin Help ---"));
        if (sender.hasPermission(PERM_ITEM_GIVE)) {
            sender.sendMessage(StringUtil.colorize("&e/mmocadm item give <player> <itemId> [amount] &7- Gives a custom item."));
        }
        // Add help for other subcommands
    }

    private boolean executeGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ITEM_GIVE)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission for this command.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /mmocadm item give <playerName> <customItemId> [amount]");
            return true;
        }

        String playerName = args[0];
        String customItemId = args[1];
        int amount = 1;

        if (args.length > 2) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    sender.sendMessage(ChatColor.RED + "Amount must be positive.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[2]);
                return true;
            }
        }

        Player targetPlayer = Bukkit.getPlayerExact(playerName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + playerName + "' not found or not online.");
            return true;
        }

        Optional<CustomItem> optCustomItem = customItemRegistry.getCustomItem(customItemId);
        if (optCustomItem.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Custom item with ID '" + customItemId + "' not found.");
            return true;
        }

        CustomItem customItem = optCustomItem.get();
        ItemStack itemToGive = customItem.createItemStack(amount);

        if (itemToGive == null || itemToGive.getType() == Material.AIR) {
            sender.sendMessage(ChatColor.RED + "Failed to create item stack for '" + customItemId + "'. Check server logs.");
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

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        // args[0] is "item" (this command's name as a subcommand of /mmocadm)
        // args[1] is the subcommand of "item" (e.g., "give")
        // args[2] is <playerName> for "give"
        // args[3] is <customItemId> for "give"
        // args[4] is [amount] for "give"

        if (args.length == 2) { // Suggesting for "give", etc.
            return super.onTabComplete(sender, args); // Let AbstractPluginCommand handle its own subcommands
        }

        String itemSubCommand = args[1].toLowerCase();

        if (itemSubCommand.equals("give")) {
            if (!sender.hasPermission(PERM_ITEM_GIVE)) return Collections.emptyList();

            if (args.length == 3) { // Player name completion
                return null; // Bukkit default
            }
            if (args.length == 4) { // Custom Item ID completion
                String inputItemId = args[3].toLowerCase();
                return customItemRegistry.getAllItems().stream()
                        .map(CustomItem::getItemId)
                        .filter(id -> id.toLowerCase().startsWith(inputItemId))
                        .collect(Collectors.toList());
            }
            if (args.length == 5) { // Amount, suggest "1"
                return List.of("1", "16", "32", "64");
            }
        }
        return Collections.emptyList();
    }
}
