package com.x1f4r.mmocraft.combat.listeners;

import com.x1f4r.mmocraft.combat.model.DamageInstance;
import com.x1f4r.mmocraft.combat.model.DamageType;
import com.x1f4r.mmocraft.combat.service.DamageCalculationService;
import com.x1f4r.mmocraft.combat.service.MobStatProvider;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.runtime.PlayerRuntimeAttributeService;
import com.x1f4r.mmocraft.statuseffect.manager.StatusEffectManager;
import com.x1f4r.mmocraft.statuseffect.model.StatusEffectType;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.util.StringUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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
    private final PlayerDataService playerDataService;
    private final LoggingUtil logger;
    private final MobStatProvider mobStatProvider;
    private final StatusEffectManager statusEffectManager;
    private final PlayerRuntimeAttributeService runtimeAttributeService;

    private static final ThreadLocal<Boolean> abilityGuard = ThreadLocal.withInitial(() -> false);

    private static final Map<Material, Double> VANILLA_WEAPON_BASE_DAMAGE = new HashMap<>();
    // private static final Map<org.bukkit.entity.EntityType, Double> MOB_BASE_DAMAGE = new HashMap<>(); // Replaced by MobStatProvider

    static {
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

        // Mob base damages are now provided via MobStatProvider
    }


    public PlayerCombatListener(DamageCalculationService damageCalculationService,
                                PlayerDataService playerDataService,
                                LoggingUtil logger,
                                MobStatProvider mobStatProvider,
                                StatusEffectManager statusEffectManager,
                                PlayerRuntimeAttributeService runtimeAttributeService) {
        this.damageCalculationService = damageCalculationService;
        this.playerDataService = playerDataService;
        this.logger = logger;
        this.mobStatProvider = mobStatProvider;
        this.statusEffectManager = statusEffectManager;
        this.runtimeAttributeService = runtimeAttributeService;
        logger.debug("PlayerCombatListener initialized with MobStatProvider.");
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
            // TODO: Add logic for custom weapons from MMOCraft
            // TODO: Add logic for magical attacks if player casts a spell (set DamageType.MAGICAL)
        } else if (actualAttacker instanceof LivingEntity) { // Mob attacker
            baseWeaponDamage = mobStatProvider.getBaseAttackDamage(actualAttacker.getType());
            if (damager instanceof Arrow) { // Arrow from mob (e.g. Skeleton)
                 // For projectiles from mobs, Bukkit's event.getDamage() might be more reliable
                 // or we can define projectile damages in MobStatProvider too.
                 // For now, let's assume MobStatProvider's base attack is for its primary/melee attack
                 // and use event.getDamage() for its projectiles as a starting point.
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

        if (actualAttacker instanceof Player attackerPlayer) {
            String feedback = String.format("&f%s &7- &c%.1f", describeEntity(victim), damageInstance.finalDamage());
            attackerPlayer.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(feedback));
        }

        handleBerserkEffects(actualAttacker, (LivingEntity) victim, damageInstance);

        String attackerName = describeEntity(actualAttacker != null ? actualAttacker : damager);
        String victimName = describeEntity(victim);

        if (damageInstance.evaded()) {
            logger.finer(StringUtil.colorize(attackerName + "'s attack on " + victimName + " was &eEVADED&f."));
            if (victim instanceof Player && damageInstance.victimProfile() != null) {
                 ((Player) victim).sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize("&7&oEvaded attack from " + attackerName));
            }
             if (actualAttacker instanceof Player && damageInstance.attackerProfile() != null) {
                 ((Player) actualAttacker).sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize("&7&oYour attack was evaded by " + victimName));
            }
        } else {
            String critMessage = damageInstance.criticalHit() ? " &c(Critical!)&f" : "";
            String message = String.format("%s hit %s for %.2f %s damage%s. Base: %.2f. Mitigation: %s",
                    attackerName, victimName, damageInstance.finalDamage(),
                    damageInstance.type().name(), critMessage,
                    damageInstance.baseDamage(), damageInstance.mitigationDetails());
            logger.finest(StringUtil.colorize(message));

            // Update PlayerProfile health if the victim is a player and took damage
            if (victim instanceof Player victimPlayer && damageInstance.finalDamage() > 0 && !damageInstance.evaded()) {
                PlayerProfile victimProfile = playerDataService.getPlayerProfile(victimPlayer.getUniqueId());
                if (victimProfile != null) {
                    // PlayerProfile's currentHealth will be updated based on the final damage.
                    // Bukkit's event.setDamage() handles the actual health reduction on the LivingEntity.
                    // We record this damage into our profile system.
                    // Note: This assumes that Bukkit's health and our profile health are 1:1.
                    // If PlayerProfile has a different health scale, this logic needs adjustment.
                    victimProfile.takeDamage(damageInstance.finalDamage());
                    logger.finest("Updated PlayerProfile health for " + victimName + " after taking " + String.format("%.2f", damageInstance.finalDamage()) + " damage. New profile health: " + victimProfile.getCurrentHealth());
                    if (runtimeAttributeService != null) {
                        runtimeAttributeService.syncPlayer(victimPlayer);
                    }

                    // If victimProfile.getCurrentHealth() <= 0, you might trigger a custom death event or logic here
                    // For now, Bukkit will handle the death event.
                }
            }
        }
        // Future: if finalDamage > 0 and attacker is player, apply on-hit effects from equipment/buffs.
        // Future: if attacker used a skill for this attack, skill's onHit method could be called here.
    }

    private void handleBerserkEffects(Entity attacker, LivingEntity victim, DamageInstance damageInstance) {
        if (!(attacker instanceof Player player)) {
            return;
        }
        if (statusEffectManager == null || !statusEffectManager.hasEffect(player, StatusEffectType.BERSERK)) {
            return;
        }
        if (damageInstance.finalDamage() <= 0 || damageInstance.evaded()) {
            return;
        }
        PlayerProfile profile = damageInstance.attackerProfile();
        if (profile == null) {
            profile = playerDataService.getPlayerProfile(player.getUniqueId());
        }
        if (profile == null) {
            return;
        }

        double lifesteal = Math.max(0.0, damageInstance.finalDamage() * 0.25);
        if (lifesteal > 0) {
            profile.heal(Math.round(lifesteal));
            if (runtimeAttributeService != null) {
                runtimeAttributeService.syncPlayer(player);
            }
        }

        if (abilityGuard.get()) {
            return;
        }
        abilityGuard.set(true);
        try {
            for (Entity nearby : victim.getNearbyEntities(3.0, 1.5, 3.0)) {
                if (!(nearby instanceof LivingEntity living) || living.equals(victim) || living.equals(player)) {
                    continue;
                }
                DamageInstance cleave = damageCalculationService.calculateDamage(player, living,
                        damageInstance.finalDamage() * 0.35, DamageType.PHYSICAL);
                if (cleave.finalDamage() <= 0) {
                    continue;
                }
                living.damage(cleave.finalDamage(), player);
            }
        } finally {
            abilityGuard.set(false);
        }
    }

    private String describeEntity(Entity entity) {
        if (entity == null) {
            return "Unknown";
        }
        if (entity instanceof Player player) {
            return player.getName();
        }
        String name = entity.getName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        EntityType type = entity.getType();
        String raw = type.name().toLowerCase().replace('_', ' ');
        StringBuilder builder = new StringBuilder();
        for (String part : raw.split(" ")) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return builder.toString().trim();
    }
}
