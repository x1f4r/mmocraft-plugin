package com.x1f4r.mmocraft.world.spawning.listeners;

import com.x1f4r.mmocraft.combat.service.MobStatProvider;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.world.spawning.service.BasicCustomSpawningService;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Locale;
import java.util.Objects;

/**
 * Applies ambient name tags and stat hints to naturally spawned mobs.
 */
public class MobNameplateListener implements Listener {

    private final MobStatProvider mobStatProvider;
    private final LoggingUtil logger;

    public MobNameplateListener(MobStatProvider mobStatProvider,
                                LoggingUtil logger) {
        this.mobStatProvider = Objects.requireNonNull(mobStatProvider);
        this.logger = Objects.requireNonNull(logger);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.getCustomName() != null) {
            return; // Already custom-named
        }
        if (entity.hasMetadata(BasicCustomSpawningService.METADATA_KEY_CUSTOM_MOB_ID)) {
            return; // Managed by custom spawn system
        }
        double baseHealth = mobStatProvider.getBaseHealth(entity.getType());
        double baseDamage = mobStatProvider.getBaseAttackDamage(entity.getType());
        if (baseHealth <= 0 && baseDamage <= 0) {
            return;
        }
        int suggestedLevel = Math.max(1, (int) Math.round(baseHealth / 20.0));
        String colour = entity instanceof Monster ? "&c" : "&a";
        String name = String.format("%s[Lv. %d] &f%s &c❤%.0f &7⚔%.1f",
                colour,
                suggestedLevel,
                formatEntityName(entity.getType()),
                baseHealth,
                baseDamage);
        entity.customName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));
        entity.setCustomNameVisible(true);
        logger.finest("Applied nameplate to " + entity.getType() + " at " + entity.getLocation());
    }

    private String formatEntityName(EntityType type) {
        String raw = type.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = raw.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return builder.toString().trim();
    }
}
