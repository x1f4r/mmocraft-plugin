package com.x1f4r.mmocraft.world.zone.event;

import com.x1f4r.mmocraft.eventbus.CustomEvent;
import com.x1f4r.mmocraft.world.zone.model.Zone;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * Event dispatched when a player leaves a defined zone.
 */
public class PlayerLeaveZoneEvent extends CustomEvent {

    private final Player player;
    private final Zone zone;

    public PlayerLeaveZoneEvent(Player player, Zone zone) {
        super();
        this.player = Objects.requireNonNull(player, "Player cannot be null");
        this.zone = Objects.requireNonNull(zone, "Zone cannot be null");
    }

    public Player getPlayer() {
        return player;
    }

    public Zone getZone() {
        return zone;
    }

    @Override
    public String toString() {
        return "PlayerLeaveZoneEvent{" +
               "eventName='" + getEventName() + '\'' +
               ", player=" + player.getName() +
               ", zoneId='" + zone.getZoneId() + '\'' +
               ", zoneName='" + zone.getZoneName() + '\'' +
               '}';
    }
}
