/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import java.util.Objects;

/**
 * Application-owned typed choice. The key, not the caption, identifies it on the wire.
 * @param key stable nonempty key, at most 128 UTF-16 units
 * @param label caption to display
 * @param value typed payload, never sent to the browser
 * @param disabled whether the option may be chosen
 * @param <T> application value type
 */
public record ChoiceOption<T>(String key, String label, T value, boolean disabled) {
    /**
     * Checks and stores one immutable choice.
     */
    public ChoiceOption {
        if (Objects.requireNonNull(key).isBlank() || key.length() > 128) {
            throw new IllegalArgumentException("Invalid choice key");
        }
        Objects.requireNonNull(label);
        Objects.requireNonNull(value);
    }

    /**
     * Creates an enabled option.
     * @param key stable key
     * @param label caption
     * @param value value kept on the server
     */
    public ChoiceOption(final String key, final String label, final T value) {
        this(key, label, value, false);
    }
}
