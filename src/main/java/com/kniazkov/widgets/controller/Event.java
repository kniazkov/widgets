/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.view.HasCheckedState;
import com.kniazkov.widgets.view.HasText;
import com.kniazkov.widgets.view.Widget;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a logical user interface event such as a click, text input, or pointer movement.
 * <p>
 * Each {@code Event<T>} describes:
 * <ul>
 *   <li>the event’s unique name ({@link #getName()});</li>
 *   <li>the type of its payload ({@code T});</li>
 *   <li>how to parse that payload from a {@link JsonObject} ({@link #parseData(JsonObject)});</li>
 *   <li>optional logic to apply to a {@link Widget} when the event is received
 *       ({@link #updateWidget(Widget, Object)}).</li>
 * </ul>
 *
 * @param <T> the type of the event data payload
 */
public abstract class Event<T> {
    /**
     * Returns the unique string identifier of this event.
     * The name is used for serialization, deserialization, and lookup
     * in the global event registry.
     *
     * @return the unique event name (e.g. {@code "click"})
     */
    public abstract String getName();

    /**
     * Parses the event data from a JSON object representation.
     *
     * @param object the JSON object describing the event
     * @return the deserialized event payload
     */
    public abstract T parseData(JsonObject object);

    /**
     * Applies the event’s effect to the specified widget.
     * <p>
     * Default implementation does nothing. Concrete event types may override this
     * to synchronize widget models with event data (for example,
     * {@link #TEXT_INPUT} updates the widget’s text model).
     *
     * @param widget the widget affected by this event
     * @param data the parsed event payload
     */
    public void updateWidget(final Widget widget, final T data) {
        // do nothing for default
    }

    @Override
    public String toString() {
        return this.getName();
    }

    /**
     * Processes the event on the given widget.
     *
     * @param widget the widget receiving the event
     * @param object the serialized event data
     */
    public void process(final Widget widget, final JsonObject object) {
        final Controller<T> controller = widget.getController(this);
        final T data = this.parseData(object);
        this.updateWidget(widget, data);
        controller.handleEvent(data);
    }

    /**
     * A no-op placeholder event used when lookup fails.
     */
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

    /**
     * Event triggered when text input occurs in a text field.
     */
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

    /**
     * Event triggered when a widget's checked state changes.
     */
    public static final Event<Boolean> CHECK = new Event<Boolean>() {
        @Override
        public String getName() {
            return "check";
        }

        @Override
        public Boolean parseData(final JsonObject object) {
            return object.get("state").getBooleanValue();
        }

        @Override
        public void updateWidget(final Widget widget, final Boolean data) {
            if (widget instanceof HasCheckedState) {
                ((HasCheckedState) widget).getCheckedStateModel().setData(data);
            }
        }
    };

    /**
     * Event triggered when a pointer click occurs on a widget.
     */
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

    /**
     * Event triggered when the pointer enters the widget’s area (hover start).
     */
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

    /**
     * Event triggered when the pointer leaves the widget’s area (hover end).
     */
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

    /**
     * Event triggered when a pointer button is pressed down over the widget.
     */
    public static final Event<PointerEvent> POINTER_DOWN = new Event<PointerEvent>() {
        @Override
        public String getName() {
            return "pointer down";
        }

        @Override
        public PointerEvent parseData(final JsonObject object) {
            return object.toJavaObject(PointerEvent.class);
        }
    };

    /**
     * Event triggered when a pointer button is released over the widget.
     */
    public static final Event<PointerEvent> POINTER_UP = new Event<PointerEvent>() {
        @Override
        public String getName() {
            return "pointer up";
        }

        @Override
        public PointerEvent parseData(final JsonObject object) {
            return object.toJavaObject(PointerEvent.class);
        }
    };

    /**
     * Global immutable registry of all known events, keyed by their unique names.
     */
    private static final Map<String, Event<?>> REGISTRY =
        Collections.unmodifiableMap(
            Stream.of(
                TEXT_INPUT,
                CHECK,
                CLICK,
                POINTER_ENTER,
                POINTER_LEAVE
            ).collect(Collectors.toMap(Event::getName, e -> e))
        );

    /**
     * Looks up an event by its name.
     *
     * @param name the event name (case-sensitive)
     * @return the matching {@link Event}, or {@link #STUB} if not found
     */
    public static Event<?> getByName(final String name) {
        return REGISTRY.getOrDefault(name, STUB);
    }
}
