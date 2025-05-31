package com.x1f4r.mmocraft.eventbus.events;

import com.x1f4r.mmocraft.eventbus.CustomEvent;

public class PluginReloadedEvent extends CustomEvent {
    // This event carries no additional data for now,
    // but it could carry a timestamp or the cause of the reload.
    public PluginReloadedEvent() {
        super();
    }
}
