/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

import com.kniazkov.json.JsonObject;
import java.util.Arrays;
import java.util.Set;

/**
 * A subscribed, non-composing, non-repeat keyboard notification.
 * Printable key characters and input values are never part of the notification.
 * @param shortcut the normalized key and modifiers
 * @param phase the subscribed physical phase
 */
public record KeyboardEvent(Shortcut shortcut, Phase phase) {
    /**
     * Browser event phases supported by the editor.
     */
    public enum Phase {
        /**
         * Physical press.
         */
        DOWN,
        /**
         * Physical release.
         */
        UP
    }

    /**
     * Named, non-printing keys that may be explicitly subscribed.
     */
    public enum Key {
        /**
         * Enter, including the numeric keypad Enter.
         */
        ENTER("Enter"),
        /**
         * Escape.
         */
        ESCAPE("Escape"),
        /**
         * Up arrow.
         */
        UP("ArrowUp"),
        /**
         * Down arrow.
         */
        DOWN("ArrowDown"),
        /**
         * Left arrow.
         */
        LEFT("ArrowLeft"),
        /**
         * Right arrow.
         */
        RIGHT("ArrowRight"),
        /**
         * Home.
         */
        HOME("Home"),
        /**
         * End.
         */
        END("End"),
        /**
         * Page up.
         */
        PAGE_UP("PageUp"),
        /**
         * Page down.
         */
        PAGE_DOWN("PageDown");

        /**

         * DOM KeyboardEvent.key wire spelling.

         */
        private final String wire;

        /**
         * Stores the browser key name.
         * @param wire DOM spelling
         */
        Key(final String wire) {
            this.wire = wire;
        }

        /**

         * @return the normalized wire name

         */
        public String wire() {
            return this.wire;
        }
    }

    /**
     * A key with an exact set of modifiers.
     * @param key the named key
     * @param ctrl whether Control is held
     * @param alt whether Alt is held
     * @param shift whether Shift is held
     * @param meta whether Meta is held
     */
    public record Shortcut(Key key, boolean ctrl, boolean alt, boolean shift, boolean meta) {
        /**
         * Checks the key at API entry.
         */
        public Shortcut {
            java.util.Objects.requireNonNull(key);
        }

        /**
         * Creates an unmodified shortcut.
         * @param key the named key
         */
        public Shortcut(final Key key) {
            this(key, false, false, false, false);
        }
    }

    /**
     * Compact signature for the browser subscription table; does not contain field data.
     * @return phase, named key and four modifier bits
     */
    public String signature() {
        final Shortcut key = this.shortcut;
        return this.phase.name() + ":" + key.key().wire() + ":"
            + (key.ctrl() ? "1" : "0") + (key.alt() ? "1" : "0")
            + (key.shift() ? "1" : "0") + (key.meta() ? "1" : "0");
    }

    /**
     * Checks the entire fixed payload before constructing a typed notification.
     * @param data browser payload
     * @return null if the payload has unknown names, values or fields
     */
    public static KeyboardEvent parse(final JsonObject data) {
        if (data == null || !data.keySet().equals(
                Set.of("key", "phase", "ctrl", "alt", "shift", "meta"))
                || !data.get("key").isString() || !data.get("phase").isString()
                || !data.get("ctrl").isBoolean() || !data.get("alt").isBoolean()
                || !data.get("shift").isBoolean() || !data.get("meta").isBoolean()) {
            return null;
        }
        final String name = data.get("key").stringValue();
        final String phase = data.get("phase").stringValue();
        if (name.length() > 10 || !("DOWN".equals(phase) || "UP".equals(phase))) {
            return null;
        }
        final Key key = Arrays.stream(Key.values()).filter(item -> item.wire().equals(name))
            .findFirst().orElse(null);
        return key == null ? null : new KeyboardEvent(
            new Shortcut(key, data.get("ctrl").booleanValue(), data.get("alt").booleanValue(),
                data.get("shift").booleanValue(), data.get("meta").booleanValue()),
            Phase.valueOf(phase));
    }
}
