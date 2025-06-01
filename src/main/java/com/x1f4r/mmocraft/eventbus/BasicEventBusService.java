package com.x1f4r.mmocraft.eventbus;

import com.x1f4r.mmocraft.util.LoggingUtil; // Added

import java.util.ArrayList;
// import java.util.HashMap; // Not directly used
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BasicEventBusService implements EventBusService {

    private final Map<Class<? extends CustomEvent>, List<EventHandler<?>>> handlers = new ConcurrentHashMap<>();
    private final LoggingUtil logger; // Added

    public BasicEventBusService(LoggingUtil logger) {
        this.logger = logger;
        logger.debug("BasicEventBusService initialized.");
    }

    @Override
    public <T extends CustomEvent> void register(Class<T> eventType, EventHandler<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
        logger.fine("Registered handler " + handler.getClass().getName() + " for event type " + eventType.getName());
    }

    @Override
    public <T extends CustomEvent> void unregister(Class<T> eventType, EventHandler<T> handler) {
        handlers.computeIfPresent(eventType, (k, v) -> {
            boolean removed = v.remove(handler);
            if (removed) {
                logger.fine("Unregistered handler " + handler.getClass().getName() + " for event type " + eventType.getName());
            }
            return v.isEmpty() ? null : v;
        });
    }

    @Override
    public void call(CustomEvent event) {
        if (event == null) {
            logger.warning("Attempted to call a null event.");
            return;
        }

        logger.finest("Calling event: " + event.getEventName() + " of type " + event.getClass().getName());
        Class<?> currentEventType = event.getClass();

        // Iterate through handlers registered for this specific event type and its superclasses
        while (currentEventType != null && CustomEvent.class.isAssignableFrom(currentEventType)) {
            List<EventHandler<?>> registeredHandlers = handlers.get(currentEventType);
            if (registeredHandlers != null) {
                logger.finest("Found " + registeredHandlers.size() + " handlers for type " + currentEventType.getName());
                for (EventHandler handler : registeredHandlers) {
                    try {
                        // Unchecked cast, but theoretically safe due to registration logic
                        @SuppressWarnings("unchecked") // Suppress warning for this specific cast
                        EventHandler<CustomEvent> castedHandler = (EventHandler<CustomEvent>) handler;
                        castedHandler.handle(event);
                    } catch (ClassCastException e) {
                        logger.severe("ClassCastException when handling event: " + event.getEventName() +
                                           " with handler: " + handler.getClass().getName() +
                                           ". Expected event type: " + currentEventType.getName(), e);
                    } catch (Exception e) {
                        logger.severe("Exception in event handler " + handler.getClass().getName() +
                                           " for event " + event.getEventName() + ": " + e.getMessage(), e);
                    }
                }
            }
             // Move to the superclass to support polymorphic event handling
            currentEventType = currentEventType.getSuperclass(); // Corrected variable name
            if (currentEventType == Object.class || currentEventType == null) break; // Stop before reaching Object or if no superclass
        }
    }
}
