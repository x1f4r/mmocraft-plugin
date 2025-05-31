package com.x1f4r.mmocraft.eventbus;

@FunctionalInterface
public interface EventHandler<T extends CustomEvent> {
    void handle(T event);
}
