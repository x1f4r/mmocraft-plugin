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

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Applies zone-defined stat bonuses and penalties to player profiles via temporary modifiers.
 */
public class ZoneStatApplier {

    private static final String ZONE_SOURCE_PREFIX = "zone:";

    private final ZoneManager zoneManager;
    private final PlayerDataService playerDataService;
    private final EventBusService eventBusService;
    private final PlayerRuntimeAttributeService runtimeAttributeService;
    private final LoggingUtil logger;

    private final EventHandler<PlayerEnterZoneEvent> enterHandler = this::handleEnter;
    private final EventHandler<PlayerLeaveZoneEvent> leaveHandler = this::handleLeave;

    public ZoneStatApplier(ZoneManager zoneManager,
                           PlayerDataService playerDataService,
                           EventBusService eventBusService,
                           PlayerRuntimeAttributeService runtimeAttributeService,
                           LoggingUtil logger) {
        this.zoneManager = Objects.requireNonNull(zoneManager, "zoneManager");
        this.playerDataService = Objects.requireNonNull(playerDataService, "playerDataService");
        this.eventBusService = Objects.requireNonNull(eventBusService, "eventBusService");
        this.runtimeAttributeService = Objects.requireNonNull(runtimeAttributeService, "runtimeAttributeService");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void register() {
        eventBusService.register(PlayerEnterZoneEvent.class, enterHandler);
        eventBusService.register(PlayerLeaveZoneEvent.class, leaveHandler);
    }

    public void shutdown() {
        eventBusService.unregister(PlayerEnterZoneEvent.class, enterHandler);
        eventBusService.unregister(PlayerLeaveZoneEvent.class, leaveHandler);
    }

    private void handleEnter(PlayerEnterZoneEvent event) {
        applyZoneModifiers(event.getPlayer(), event.getZone());
    }

    private void handleLeave(PlayerLeaveZoneEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = playerDataService.getPlayerProfile(player.getUniqueId());
        if (profile == null) {
            return;
        }
        profile.clearTemporaryStatModifiers(buildSourceKey(event.getZone().getZoneId()));
        runtimeAttributeService.syncPlayer(player);
    }

    private void applyZoneModifiers(Player player, Zone zone) {
        PlayerProfile profile = playerDataService.getPlayerProfile(player.getUniqueId());
        if (profile == null) {
            return;
        }
        Map<Stat, Double> modifiers = extractModifiers(zone);
        String sourceKey = buildSourceKey(zone.getZoneId());
        if (modifiers.isEmpty()) {
            profile.clearTemporaryStatModifiers(sourceKey);
        } else {
            profile.setTemporaryStatModifiers(sourceKey, modifiers);
        }
        runtimeAttributeService.syncPlayer(player);
    }

    private Map<Stat, Double> extractModifiers(Zone zone) {
        Map<Stat, Double> modifiers = new EnumMap<>(Stat.class);
        zone.getProperties().forEach((key, value) -> {
            if (key == null) {
                return;
            }
            String lowered = key.toLowerCase(Locale.ROOT);
            if (!lowered.startsWith("stat.")) {
                return;
            }
            String statName = lowered.substring("stat.".length()).replace('-', '_');
            try {
                Stat stat = Stat.valueOf(statName.toUpperCase(Locale.ROOT));
                Double numericValue = toDouble(value);
                if (numericValue != null && numericValue != 0.0) {
                    modifiers.put(stat, numericValue);
                }
            } catch (IllegalArgumentException ex) {
                logger.warning("Zone " + zone.getZoneId() + " references unknown stat modifier '" + statName + "'.");
            }
        });
        return modifiers;
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String buildSourceKey(String zoneId) {
        return ZONE_SOURCE_PREFIX + zoneId.toLowerCase(Locale.ROOT);
    }
}
