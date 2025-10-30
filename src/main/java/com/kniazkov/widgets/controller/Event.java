/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.view.HasText;
import com.kniazkov.widgets.view.Widget;
import java.util.Optional;

public abstract class Event<T> {
    public abstract String getName();
    public abstract Class<T> getEventDataClass();
    public abstract void handle(Widget widget, JsonObject data, Controller<T> ctrl);

    @Override
    public String toString() {
        return this.getName();
    }

    public static final Event<String> TEXT_INPUT = new Event<String>() {
        @Override
        public String getName() {
            return "text input";
        }

        @Override
        public Class<String> getEventDataClass() {
            return String.class;
        }

        @Override
        public void handle(final Widget widget, final JsonObject data, final Controller<String> ctrl) {
            final String value = data.get("text").getStringValue();
            if (widget instanceof HasText) {
                ((HasText) widget).getTextModel().setData(value);
            }
            ctrl.handleEvent(value);
        }
    };

    public static final Event<PointerEvent> CLICK = new Event<PointerEvent>() {
        @Override
        public String getName() {
            return "click";
        }

        @Override
        public Class<PointerEvent> getEventDataClass() {
            return PointerEvent.class;
        }

        @Override
        public void handle(final Widget widget, final JsonObject data, final Controller<PointerEvent> ctrl) {
            final PointerEvent event = ProcessesPointerEvents.parsePointerEvent(data);
            ctrl.handleEvent(event);
        }
    };

    public static final Event<PointerEvent> POINTER_ENTER = new Event<PointerEvent>() {
        @Override
        public String getName() {
            return "pointer enter";
        }

        @Override
        public Class<PointerEvent> getEventDataClass() {
            return PointerEvent.class;
        }

        public void handle(final Widget widget, final JsonObject data, final Controller<PointerEvent> ctrl) {
            final PointerEvent event = ProcessesPointerEvents.parsePointerEvent(data);
            ctrl.handleEvent(event);
        }
    };

    public static final Event<PointerEvent> POINTER_LEAVE = new Event<PointerEvent>() {
        @Override
        public String getName() {
            return "pointer leave";
        }

        @Override
        public Class<PointerEvent> getEventDataClass() {
            return PointerEvent.class;
        }

        public void handle(final Widget widget, final JsonObject data, final Controller<PointerEvent> ctrl) {
            final PointerEvent event = ProcessesPointerEvents.parsePointerEvent(data);
            ctrl.handleEvent(event);
        }
    };
}
