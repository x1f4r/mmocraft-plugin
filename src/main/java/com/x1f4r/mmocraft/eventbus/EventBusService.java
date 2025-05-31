package com.x1f4r.mmocraft.eventbus;

public interface EventBusService {
    <T extends CustomEvent> void register(Class<T> eventType, EventHandler<T> handler);
    <T extends CustomEvent> void unregister(Class<T> eventType, EventHandler<T> handler); // Added unregister for completeness
    void call(CustomEvent event);
}
