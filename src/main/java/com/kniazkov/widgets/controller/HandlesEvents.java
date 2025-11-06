/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

import com.kniazkov.json.JsonObject;

/**
 * Defines an entity that can store and manage {@link Controller controllers} associated
 * with specific {@link Event events}.
 */
public interface HandlesEvents {
    /**
     * Returns the {@link Controller} currently bound to the specified {@link Event}.
     *
     * @param event the event descriptor whose controller should be retrieved
     * @param <T> the type of the event’s data payload
     * @return the controller associated with this event, or {@code null} if none is set
     */
    <T> Controller<T> getController(Event<T> event);

    /**
     * Associates a new {@link Controller} with the specified {@link Event}.
     * Any previously registered controller for the same event will be replaced.
     *
     * @param event the event descriptor
     * @param controller the controller to bind to this event
     * @param <T> the type of the event’s data payload
     */
    <T> void setController(Event<T> event, Controller<T> controller);

    /**
     * Handles an incoming event payload.
     *
     * @param event the event descriptor
     * @param object the JSON payload representing the event data
     */
    void handleEvent(Event<?> event, final JsonObject object);

    /**
     * Subscribes the entity to receive notifications for a specific event type.
     *
     * @param event the event type to subscribe to
     */
    void subscribeToEvent(Event<?> event);
}
