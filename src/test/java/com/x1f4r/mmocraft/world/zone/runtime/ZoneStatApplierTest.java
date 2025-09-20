package com.x1f4r.mmocraft.world.zone.runtime;

import com.x1f4r.mmocraft.eventbus.EventBusService;
import com.x1f4r.mmocraft.eventbus.EventHandler;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.playerdata.runtime.PlayerRuntimeAttributeService;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.world.zone.event.PlayerEnterZoneEvent;
import com.x1f4r.mmocraft.world.zone.event.PlayerLeaveZoneEvent;
import com.x1f4r.mmocraft.world.zone.model.Zone;
import com.x1f4r.mmocraft.world.zone.service.ZoneManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ZoneStatApplierTest {

    @Test
    void appliesAndClearsZoneModifiers() {
        ZoneManager zoneManager = mock(ZoneManager.class);
        PlayerDataService playerDataService = mock(PlayerDataService.class);
        EventBusService eventBusService = mock(EventBusService.class);
        PlayerRuntimeAttributeService runtimeAttributeService = mock(PlayerRuntimeAttributeService.class);
        LoggingUtil loggingUtil = mock(LoggingUtil.class);

        ZoneStatApplier applier = new ZoneStatApplier(zoneManager, playerDataService, eventBusService, runtimeAttributeService, loggingUtil);
        applier.register();

        ArgumentCaptor<EventHandler<PlayerEnterZoneEvent>> enterCaptor = ArgumentCaptor.forClass(EventHandler.class);
        ArgumentCaptor<EventHandler<PlayerLeaveZoneEvent>> leaveCaptor = ArgumentCaptor.forClass(EventHandler.class);
        verify(eventBusService).register(eq(PlayerEnterZoneEvent.class), enterCaptor.capture());
        verify(eventBusService).register(eq(PlayerLeaveZoneEvent.class), leaveCaptor.capture());

        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Explorer");

        PlayerProfile profile = mock(PlayerProfile.class);
        when(playerDataService.getPlayerProfile(playerId)).thenReturn(profile);

        Zone zone = new Zone("ancient_ruins", "Ancient Ruins", "world", 0, 0, 0, 10, 10, 10,
                Map.<String, Object>of("stat.speed", 5.0, "stat.strength", -2.0));

        enterCaptor.getValue().handle(new PlayerEnterZoneEvent(player, zone));

        ArgumentCaptor<Map<Stat, Double>> modifierCaptor = ArgumentCaptor.forClass(Map.class);
        verify(profile).setTemporaryStatModifiers(eq("zone:ancient_ruins"), modifierCaptor.capture());
        assertEquals(2, modifierCaptor.getValue().size());
        assertEquals(5.0, modifierCaptor.getValue().get(Stat.SPEED));
        assertEquals(-2.0, modifierCaptor.getValue().get(Stat.STRENGTH));
        verify(runtimeAttributeService).syncPlayer(player);

        leaveCaptor.getValue().handle(new PlayerLeaveZoneEvent(player, zone));
        verify(profile).clearTemporaryStatModifiers("zone:ancient_ruins");
        verify(runtimeAttributeService, times(2)).syncPlayer(player);
    }
}
