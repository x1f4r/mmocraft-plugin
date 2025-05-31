package com.x1f4r.mmocraft.combat.listeners;

import com.x1f4r.mmocraft.combat.model.DamageInstance;
import com.x1f4r.mmocraft.combat.model.DamageType;
import com.x1f4r.mmocraft.combat.service.DamageCalculationService;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.util.StringUtil;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;

public class PlayerCombatListener implements Listener {

    private final DamageCalculationService damageCalculationService;
    private final PlayerDataService playerDataService; // To potentially update health/mana if not using Bukkit's mechanics directly
    private final LoggingUtil logger;

    private static final Map<Material, Double> VANILLA_WEAPON_BASE_DAMAGE = new HashMap<>();
    private static final Map<org.bukkit.entity.EntityType, Double> MOB_BASE_DAMAGE = new HashMap<>();

    static {
        // Melee Weapons
        VANILLA_WEAPON_BASE_DAMAGE.put(Material.WOODEN_SWORD, 4.0);
        VANILLA_WEAPON_BASE_DAMAGE.put(Material.STONE_SWORD, 5.0);
        VANILLA_WEAPON_BASE_DAMAGE.put(Material.IRON_SWORD, 6.0);
        VANILLA_WEAPON_BASE_DAMAGE.put(Material.GOLDEN_SWORD, 4.0);
        VANILLA_WEAPON_BASE_DAMAGE.put(Material.DIAMOND_SWORD, 7.0);
        VANILLA_WEAPON_BASE_DAMAGE.put(Material.NETHERITE_SWORD, 8.0);
        VANILLA_WEAPON_BASE_DAMAGE.put(Material.WOODEN_AXE, 3.0); // Axes are typically a bit less than swords
        VANILLA_WEAPON_BASE_DAMAGE.put(Material.STONE_AXE, 4.0);
        VANILLA_WEAPON_BASE_DAMAGE.put(Material.IRON_AXE, 5.0);
        VANILLA_WEAPON_BASE_DAMAGE.put(Material.GOLDEN_AXE, 3.0);
        VANILLA_WEAPON_BASE_DAMAGE.put(Material.DIAMOND_AXE, 6.0);
        VANILLA_WEAPON_BASE_DAMAGE.put(Material.NETHERITE_AXE, 7.0);
        // Add other tools if they are to be used as weapons: Shovel, Pickaxe, Hoe
        VANILLA_WEAPON_BASE_DAMAGE.put(Material.TRIDENT, 9.0); // Melee Trident damage

        // Mob default damages (examples, can be configured or fetched from mob attributes)
        MOB_BASE_DAMAGE.put(org.bukkit.entity.EntityType.ZOMBIE, 3.0);
        MOB_BASE_DAMAGE.put(org.bukkit.entity.EntityType.SKELETON, 2.5); // Arrow damage often separate
        MOB_BASE_DAMAGE.put(org.bukkit.entity.EntityType.SPIDER, 2.0);
        MOB_BASE_DAMAGE.put(org.bukkit.entity.EntityType.CREEPER, 49.0); // Explosion base, handled differently
        MOB_BASE_DAMAGE.put(org.bukkit.entity.EntityType.ENDERMAN, 7.0);
    }


    public PlayerCombatListener(DamageCalculationService damageCalculationService,
                                PlayerDataService playerDataService, LoggingUtil logger) {
        this.damageCalculationService = damageCalculationService;
        this.playerDataService = playerDataService;
        this.logger = logger;
        logger.debug("PlayerCombatListener initialized.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        if (!(victim instanceof LivingEntity)) {
            logger.finer("Victim is not a LivingEntity, skipping custom damage calculation.");
            return;
        }

        Entity actualAttacker = damager;
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Entity) {
                actualAttacker = (Entity) shooter;
            } else {
                actualAttacker = null; // Projectile from dispenser or unknown source
            }
        }

        if (actualAttacker == null && !(damager instanceof Projectile)) {
             // If actualAttacker is still null here, it means non-projectile, non-entity source, or unresolvable projectile.
             // This case might be complex (e.g. TNT minecart from player action). For now, we might skip.
             logger.finer("Attacker is null and not a resolvable projectile, using default Bukkit damage for now.");
             return;
        }


        double baseWeaponDamage = event.getDamage(); // Fallback to Bukkit's original damage
        DamageType damageType = DamageType.PHYSICAL; // Default to physical for most direct entity interactions

        if (actualAttacker instanceof Player playerAttacker) {
            ItemStack mainHand = playerAttacker.getInventory().getItemInMainHand();
            if (mainHand != null && VANILLA_WEAPON_BASE_DAMAGE.containsKey(mainHand.getType())) {
                baseWeaponDamage = VANILLA_WEAPON_BASE_DAMAGE.get(mainHand.getType());
            } else if (mainHand == null || mainHand.getType() == Material.AIR) {
                baseWeaponDamage = 1.0; // Unarmed damage
            }
            // TODO: Add logic for custom weapons, ranged weapons (bows - arrow damage might be separate event or property)
            // TODO: Add logic for magical attacks if player casts a spell
        } else if (actualAttacker != null) { // Mob or other non-player entity attacker
            baseWeaponDamage = MOB_BASE_DAMAGE.getOrDefault(actualAttacker.getType(), event.getDamage());
            if (damager instanceof Arrow) { // Arrow from mob (e.g. Skeleton)
                 // Bukkit's event.getDamage() for arrows is usually quite accurate.
                 // Or use a base arrow damage + mob-specific bonuses.
                 baseWeaponDamage = event.getDamage(); // Use Bukkit's calculated arrow damage as base
            }
        } else if (damager instanceof Projectile && actualAttacker == null) {
            // Projectile from unknown source (e.g. dispenser)
            // Use event.getDamage() or define specific damages for these projectiles
            baseWeaponDamage = event.getDamage();
            logger.finer("Damage from unowned projectile " + damager.getType() + ", using Bukkit base damage: " + baseWeaponDamage);
        }


        DamageInstance damageInstance = damageCalculationService.calculateDamage(actualAttacker != null ? actualAttacker : damager, victim, baseWeaponDamage, damageType);

        event.setDamage(damageInstance.finalDamage());

        // Logging and PlayerProfile updates (like health reduction)
        String attackerName = (actualAttacker != null) ? actualAttacker.getName() : (damager != null ? damager.getType().toString() : "Unknown Attacker");
        String victimName = victim.getName();

        if (damageInstance.evaded()) {
            logger.info(StringUtil.colorize(attackerName + "'s attack on " + victimName + " was &eEVADED&f."));
            if (victim instanceof Player && damageInstance.victimProfile() != null) {
                 ((Player) victim).sendActionBar(StringUtil.colorize("&7&oEvaded attack from " + attackerName));
            }
             if (actualAttacker instanceof Player && damageInstance.attackerProfile() != null) {
                 ((Player) actualAttacker).sendActionBar(StringUtil.colorize("&7&oYour attack was evaded by " + victimName));
            }
        } else {
            String critMessage = damageInstance.criticalHit() ? " &c(Critical!)&f" : "";
            String message = String.format("%s hit %s for %.2f %s damage%s. Base: %.2f. Mitigation: %s",
                    attackerName, victimName, damageInstance.finalDamage(),
                    damageInstance.type().name(), critMessage,
                    damageInstance.baseDamage(), damageInstance.mitigationDetails());
            logger.info(StringUtil.colorize(message));

            // If we want to manage health directly via PlayerProfile instead of letting Bukkit handle it post-event:
            // (This is advanced and needs careful handling of death, etc.)
            /*
            if (victim instanceof Player && damageInstance.victimProfile() != null) {
                PlayerProfile victimProfile = damageInstance.victimProfile();
                victimProfile.takeDamage((long) damageInstance.finalDamage()); // Assumes takeDamage exists
                // Need to update Bukkit's health too, or cancel event and manage manually
                // ((LivingEntity) victim).setHealth(((LivingEntity) victim).getHealth() - damageInstance.finalDamage());
                // This part is complex due to Bukkit's own health management.
                // For now, setting event.setDamage() is the primary goal.
            }
            */
        }

        // Future: if finalDamage > 0, apply on-hit effects, trigger other events.
        // Future: if attacker used a skill, deduct mana/resource from attackerProfile.
    }
}
