package com.x1f4r.mmocraft.demo.statuseffect;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.playerdata.runtime.PlayerRuntimeAttributeService;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BerserkerRageStatusEffectTest {

    @Mock
    private MMOCraftPlugin plugin;
    @Mock
    private PlayerRuntimeAttributeService runtimeAttributeService;
    @Mock
    private Player player;
    @Mock
    private PlayerProfile profile;

    @Test
    void onApply_setsTemporaryModifiersAndSyncsAttributes() {
        when(plugin.getPlayerRuntimeAttributeService()).thenReturn(runtimeAttributeService);

        World world = mock(World.class);
        Location location = new Location(world, 0, 70, 0);
        when(player.getLocation()).thenReturn(location);
        when(player.getWorld()).thenReturn(world);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        lenient().doNothing().when(world).spawnParticle(Mockito.any(Particle.class), Mockito.any(Location.class), Mockito.anyInt(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble());
        lenient().doNothing().when(world).playSound(Mockito.any(Location.class), Mockito.any(Sound.class), Mockito.anyFloat(), Mockito.anyFloat());

        BerserkerRageStatusEffect effect = new BerserkerRageStatusEffect(plugin, player.getUniqueId());
        ArgumentCaptor<Map<Stat, Double>> modifierCaptor = ArgumentCaptor.forClass(Map.class);

        effect.onApply(player, profile);

        verify(profile).setTemporaryStatModifiers(Mockito.eq("status:berserker_rage"), modifierCaptor.capture());
        Map<Stat, Double> applied = modifierCaptor.getValue();
        assertEquals(80.0, applied.get(Stat.STRENGTH));
        assertEquals(35.0, applied.get(Stat.ATTACK_SPEED));
        assertEquals(40.0, applied.get(Stat.FEROCITY));
        assertEquals(-20.0, applied.get(Stat.DEFENSE));
        assertTrue(applied.containsKey(Stat.CRITICAL_DAMAGE));
        verify(profile).recalculateDerivedAttributes();
        verify(runtimeAttributeService).syncPlayer(player);
    }

    @Test
    void onExpire_clearsModifiersAndSyncs() {
        when(plugin.getPlayerRuntimeAttributeService()).thenReturn(runtimeAttributeService);

        World world = mock(World.class);
        Location location = new Location(world, 0, 70, 0);
        when(player.getLocation()).thenReturn(location);
        when(player.getWorld()).thenReturn(world);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        lenient().doNothing().when(world).spawnParticle(Mockito.any(Particle.class), Mockito.any(Location.class), Mockito.anyInt(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble());
        lenient().doNothing().when(world).playSound(Mockito.any(Location.class), Mockito.any(Sound.class), Mockito.anyFloat(), Mockito.anyFloat());

        BerserkerRageStatusEffect effect = new BerserkerRageStatusEffect(plugin, player.getUniqueId());
        effect.onExpire(player, profile);

        verify(profile).clearTemporaryStatModifiers("status:berserker_rage");
        verify(profile).recalculateDerivedAttributes();
        verify(runtimeAttributeService).syncPlayer(player);
    }
}

