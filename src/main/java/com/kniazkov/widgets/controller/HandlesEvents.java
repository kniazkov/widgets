package com.kniazkov.widgets.controller;

import com.kniazkov.json.JsonObject;

public interface HandlesEvents {
    <T> Controller<T> getController(Event<T> event);

    <T> void setController(Event<T> event, Controller<T> controller);

    void handleEvent(Event<?> event, final JsonObject object);

    void subscribeToEvent(Event<?> event);
}
