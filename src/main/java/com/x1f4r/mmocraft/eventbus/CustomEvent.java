package com.x1f4r.mmocraft.eventbus;

public abstract class CustomEvent {

    private final String eventName;

    public CustomEvent() {
        this.eventName = this.getClass().getSimpleName();
    }

    public String getEventName() {
        return eventName;
    }

    // Potential future additions:
    // - Timestamp
    // - Asynchronous flag
    // - Cancellable interface implementation
}
