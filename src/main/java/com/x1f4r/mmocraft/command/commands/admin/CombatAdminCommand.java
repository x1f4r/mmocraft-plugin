package com.x1f4r.mmocraft.command.commands.admin;

import com.x1f4r.mmocraft.command.AbstractPluginCommand;
import com.x1f4r.mmocraft.command.CommandExecutable;
import com.x1f4r.mmocraft.combat.model.DamageInstance;
import com.x1f4r.mmocraft.combat.model.DamageType;
import com.x1f4r.mmocraft.combat.service.DamageCalculationService;
import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.util.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CombatAdminCommand extends AbstractPluginCommand {

    private final MMOCraftPlugin plugin;
    private final DamageCalculationService damageCalcService;
    private final LoggingUtil logger;

    private static final String PERM_BASE = "mmocraft.admin"; // Base for all /mmocadm commands
    private static final String PERM_COMBAT_TESTDAMAGE = PERM_BASE + ".combat.testdamage";

    // Simplified weapon damage map (can be expanded or moved to a shared utility)
    private static final Map<Material, Double> VANILLA_WEAPON_DAMAGE_MAP = new HashMap<>();
    static {
        VANILLA_WEAPON_DAMAGE_MAP.put(Material.WOODEN_SWORD, 4.0);
        VANILLA_WEAPON_DAMAGE_MAP.put(Material.STONE_SWORD, 5.0);
        VANILLA_WEAPON_DAMAGE_MAP.put(Material.IRON_SWORD, 6.0);
        VANILLA_WEAPON_DAMAGE_MAP.put(Material.GOLDEN_SWORD, 4.0);
        VANILLA_WEAPON_DAMAGE_MAP.put(Material.DIAMOND_SWORD, 7.0);
        VANILLA_WEAPON_DAMAGE_MAP.put(Material.NETHERITE_SWORD, 8.0);
        VANILLA_WEAPON_DAMAGE_MAP.put(Material.WOODEN_AXE, 3.0);
        VANILLA_WEAPON_DAMAGE_MAP.put(Material.STONE_AXE, 4.0);
        VANILLA_WEAPON_DAMAGE_MAP.put(Material.IRON_AXE, 5.0);
        VANILLA_WEAPON_DAMAGE_MAP.put(Material.GOLDEN_AXE, 3.0);
        VANILLA_WEAPON_DAMAGE_MAP.put(Material.DIAMOND_AXE, 6.0);
        VANILLA_WEAPON_DAMAGE_MAP.put(Material.NETHERITE_AXE, 7.0);
        VANILLA_WEAPON_DAMAGE_MAP.put(Material.TRIDENT, 9.0);
        VANILLA_WEAPON_DAMAGE_MAP.put(Material.AIR, 1.0); // Unarmed
    }


    public CombatAdminCommand(MMOCraftPlugin plugin) {
        super("combat", PERM_BASE + ".combat", "Admin commands for combat system."); // This command will be a subcommand of /mmocadm
        this.plugin = plugin;
        this.damageCalcService = plugin.getDamageCalculationService();
        this.logger = plugin.getLoggingUtil();

        registerSubCommand("testdamage", new CommandExecutable() {
            @Override
            public boolean onCommand(CommandSender sender, String[] args) {
                return executeTestDamage(sender, args);
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String[] args) {
                if (!sender.hasPermission(PERM_COMBAT_TESTDAMAGE)) {
                    return Collections.emptyList();
                }
                if (args.length == 0) {
                    return Collections.emptyList();
                }
                if (args.length == 1 || args.length == 2) {
                    return null; // Delegate to Bukkit for player name completion
                }
                if (args.length == 3) {
                    String prefix = args[2].toLowerCase();
                    return VANILLA_WEAPON_DAMAGE_MAP.keySet().stream()
                            .map(Enum::name)
                            .filter(name -> name.toLowerCase().startsWith(prefix))
                            .sorted()
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();
            }
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        // This is for `/mmocadm combat` base command
        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(StringUtil.colorize("&6--- Combat Admin Help ---"));
        if (sender.hasPermission(PERM_COMBAT_TESTDAMAGE)) {
            sender.sendMessage(StringUtil.colorize("&e/mmocadm combat testdamage <attacker> <victim> [weaponMaterial] &7- Simulates damage."));
        }
         // Add more combat subcommands here
    }

    private boolean executeTestDamage(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_COMBAT_TESTDAMAGE)) {
            sender.sendMessage(Component.text("You don't have permission for this command.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /mmocadm combat testdamage <attackerPlayerName> <victimPlayerName> [weaponMaterialName]", NamedTextColor.RED));
            return true;
        }

        String attackerName = args[0];
        String victimName = args[1];
        String weaponMaterialName = (args.length > 2) ? args[2].toUpperCase() : "AIR"; // Default to unarmed (AIR)

        Player attacker = Bukkit.getPlayerExact(attackerName);
        Player victim = Bukkit.getPlayerExact(victimName);

        if (attacker == null) {
            sender.sendMessage(Component.text("Attacker player '" + attackerName + "' not found or not online.", NamedTextColor.RED));
            return true;
        }
        if (victim == null) {
            sender.sendMessage(Component.text("Victim player '" + victimName + "' not found or not online.", NamedTextColor.RED));
            return true;
        }

        Material weaponMaterial;
        try {
            weaponMaterial = Material.valueOf(weaponMaterialName);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Invalid weapon material: " + weaponMaterialName, NamedTextColor.RED));
            // Send list of valid weapon materials? Could be long.
            return true;
        }

        double baseDamage = VANILLA_WEAPON_DAMAGE_MAP.getOrDefault(weaponMaterial, 1.0); // Default to 1.0 if material not in map
        if (weaponMaterialName.equalsIgnoreCase("UNARMED")) { // Common alias
            baseDamage = VANILLA_WEAPON_DAMAGE_MAP.getOrDefault(Material.AIR, 1.0);
        }


        DamageInstance instance = damageCalcService.calculateDamage(attacker, victim, baseDamage, DamageType.PHYSICAL);

        sender.sendMessage(StringUtil.colorize("&6--- Damage Test Result ---"));
        sender.sendMessage(StringUtil.colorize("&eAttacker: &f" + attacker.getName() + (instance.attackerProfile() != null ? " (Profiled)" : " (Not Profiled)")));
        sender.sendMessage(StringUtil.colorize("&eVictim: &f" + victim.getName() + (instance.victimProfile() != null ? " (Profiled)" : " (Not Profiled)")));
        sender.sendMessage(StringUtil.colorize("&eWeapon Base: &f" + String.format("%.2f", baseDamage) + " (&7Simulated " + weaponMaterial.name() + "&7)"));
        sender.sendMessage(StringUtil.colorize("&eDamage Type: &f" + instance.type()));
        sender.sendMessage(StringUtil.colorize("&eCalculated Base (after attacker bonuses): &f" + String.format("%.2f", instance.baseDamage())));
        sender.sendMessage(StringUtil.colorize("&bCritical Hit: &f" + (instance.criticalHit() ? "&cYes" : "&aNo")));
        sender.sendMessage(StringUtil.colorize("&bEvaded: &f" + (instance.evaded() ? "&cYes" : "&aNo")));
        if (!instance.mitigationDetails().isEmpty()) {
             sender.sendMessage(StringUtil.colorize("&bMitigation: &f" + instance.mitigationDetails()));
        }
        sender.sendMessage(StringUtil.colorize("&aFinal Damage: &f" + String.format("%.2f", instance.finalDamage())));

        logger.info(sender.getName() + " performed damage test: " + instance.toString());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
