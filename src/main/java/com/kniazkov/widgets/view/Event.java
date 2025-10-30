/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.PointerEvent;

public abstract class Event<T> {
    public abstract String getName();
    public abstract Class<T> getEventDataClass();
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
    };
}
