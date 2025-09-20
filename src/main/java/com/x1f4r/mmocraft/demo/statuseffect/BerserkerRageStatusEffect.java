package com.x1f4r.mmocraft.demo.statuseffect;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.playerdata.runtime.PlayerRuntimeAttributeService;
import com.x1f4r.mmocraft.statuseffect.model.StatusEffect;
import com.x1f4r.mmocraft.statuseffect.model.StatusEffectType;
import com.x1f4r.mmocraft.util.StringUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * High impact berserk buff triggered by the Berserker Gauntlet.
 * Applies aggressive stat modifiers and visual flair while the effect lasts.
 */
public class BerserkerRageStatusEffect extends StatusEffect {

    private static final double DURATION_SECONDS = 8.0;
    private static final double TICK_INTERVAL_SECONDS = 1.0;
    private static final String STAT_SOURCE_KEY = "status:berserker_rage";

    private final Map<Stat, Double> statModifiers;

    public BerserkerRageStatusEffect(MMOCraftPlugin plugin, UUID sourceEntityId) {
        super(plugin, StatusEffectType.BERSERK, DURATION_SECONDS, 1.0, TICK_INTERVAL_SECONDS, sourceEntityId);
        Map<Stat, Double> modifiers = new EnumMap<>(Stat.class);
        modifiers.put(Stat.STRENGTH, 80.0);
        modifiers.put(Stat.ATTACK_SPEED, 35.0);
        modifiers.put(Stat.FEROCITY, 40.0);
        modifiers.put(Stat.CRITICAL_DAMAGE, 25.0);
        modifiers.put(Stat.DEFENSE, -20.0);
        this.statModifiers = Map.copyOf(modifiers);
    }

    @Override
    public void onApply(LivingEntity target, PlayerProfile targetProfileIfPlayer) {
        if (!(target instanceof Player player)) {
            return;
        }
        if (targetProfileIfPlayer != null) {
            targetProfileIfPlayer.setTemporaryStatModifiers(STAT_SOURCE_KEY, statModifiers);
            targetProfileIfPlayer.recalculateDerivedAttributes();
            syncRuntimeAttributes(player, targetProfileIfPlayer);
        }
        Location location = player.getLocation();
        World world = player.getWorld();
        world.spawnParticle(Particle.CRIT, location, 40, 0.6, 0.4, 0.6, 0.2);
        world.spawnParticle(Particle.LAVA, location, 15, 0.3, 0.2, 0.3, 0.05);
        try {
            playSoundSafely(world, location, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.2f, 0.7f);
        } catch (ExceptionInInitializerError | NoClassDefFoundError ignored) {
        }
        player.sendMessage(StringUtil.colorize("&cYou lose yourself to berserker fury!"));
    }

    @Override
    public void onTick(LivingEntity target, PlayerProfile targetProfileIfPlayer) {
        if (!(target instanceof Player player)) {
            return;
        }
        Location location = player.getLocation();
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, location.add(0, 0.2, 0), 20, 0.5, 0.3, 0.5, 0.02);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, location, 6, 0.2, 0.1, 0.2, 0.01);
    }

    @Override
    public void onExpire(LivingEntity target, PlayerProfile targetProfileIfPlayer) {
        if (!(target instanceof Player player)) {
            return;
        }
        if (targetProfileIfPlayer != null) {
            targetProfileIfPlayer.clearTemporaryStatModifiers(STAT_SOURCE_KEY);
            targetProfileIfPlayer.recalculateDerivedAttributes();
            syncRuntimeAttributes(player, targetProfileIfPlayer);
        }
        try {
            playSoundSafely(player.getWorld(), player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.3f);
        } catch (ExceptionInInitializerError | NoClassDefFoundError ignored) {
        }
        player.sendMessage(StringUtil.colorize("&7Your berserker trance fades."));
    }

    private void syncRuntimeAttributes(Player player, PlayerProfile profile) {
        Objects.requireNonNull(profile, "profile");
        PlayerRuntimeAttributeService runtimeService = plugin.getPlayerRuntimeAttributeService();
        if (runtimeService != null) {
            runtimeService.syncPlayer(player);
        }
    }

    private void playSoundSafely(World world, Location location, Sound sound, float volume, float pitch) {
        try {
            world.playSound(location, sound, volume, pitch);
        } catch (ExceptionInInitializerError | NoClassDefFoundError ignored) {
            // ignore if registry not available (e.g. during unit tests)
        }
    }
}

