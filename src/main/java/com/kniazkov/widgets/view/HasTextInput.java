/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.Controller;
import com.kniazkov.widgets.controller.HandlesTextEvents;

/**
 * An entity that contains user-editable text.
 * Extends {@link HasText} to represent components like input fields or text areas,
 * where the user can modify the textual content. This interface allows
 * attaching a {@link Controller} that reacts to user-driven text changes.
 */
public interface HasTextInput extends HasText, HandlesTextEvents {
    /**
     * Maximum accepted browser input length in UTF-16 code units.
     * Override for fields that intentionally accept larger documents.
     * @return positive input length limit
     */
    default int getMaxInputLength() {
        return 65536;
    }
}
