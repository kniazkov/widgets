/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.view.HasText;
import com.kniazkov.widgets.view.Widget;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Event<T> {

    public abstract String getName();
    public abstract T parseData(JsonObject object);
    public void updateWidget(final Widget widget, final T data) {
        // do nothing for default
    }

    @Override
    public String toString() {
        return this.getName();
    }

    public void process(final Widget widget, final JsonObject object) {
        final Controller<T> controller = widget.getController(this);
        final T data = this.parseData(object);
        this.updateWidget(widget, data);
        controller.handleEvent(data);
    }

    public static final Event<Object> STUB = new Event<Object>() {
        @Override
        public String getName() {
            return "?";
        }

        @Override
        public Object parseData(final JsonObject object) {
            return null;
        }
    };

    public static final Event<String> TEXT_INPUT = new Event<String>() {
        @Override
        public String getName() {
            return "text input";
        }

        @Override
        public String parseData(final JsonObject object) {
            return object.get("text").getStringValue();
        }

        @Override
        public void updateWidget(final Widget widget, final String data) {
            if (widget instanceof HasText) {
                ((HasText) widget).getTextModel().setData(data);
            }
        }
    };

    public static final Event<PointerEvent> CLICK = new Event<PointerEvent>() {
        @Override
        public String getName() {
            return "click";
        }

        @Override
        public PointerEvent parseData(final JsonObject object) {
            return object.toJavaObject(PointerEvent.class);
        }
    };

    public static final Event<PointerEvent> POINTER_ENTER = new Event<PointerEvent>() {
        @Override
        public String getName() {
            return "pointer enter";
        }

        @Override
        public PointerEvent parseData(final JsonObject object) {
            return object.toJavaObject(PointerEvent.class);
        }
    };

    public static final Event<PointerEvent> POINTER_LEAVE = new Event<PointerEvent>() {
        @Override
        public String getName() {
            return "pointer leave";
        }

        @Override
        public PointerEvent parseData(final JsonObject object) {
            return object.toJavaObject(PointerEvent.class);
        }
    };

    private static final Map<String, Event<?>> REGISTRY =
        Collections.unmodifiableMap(
            Stream.of(TEXT_INPUT, CLICK, POINTER_ENTER, POINTER_LEAVE)
                .collect(Collectors.toMap(Event::getName, e -> e))
        );

    public static Event<?> getByName(final String name) {
        return REGISTRY.getOrDefault(name, STUB);
    }
}
