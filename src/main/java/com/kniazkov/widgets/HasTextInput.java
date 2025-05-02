/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * An entity that contains user-editable text.
 * <p>
 *     Extends {@link HasText} to represent components like input fields or text areas,
 *     where the user can modify the textual content. This interface allows
 *     attaching a {@link TypedController} that reacts to user-driven text changes.
 * </p>
 */
public interface HasTextInput extends HasText {
    /**
     * Sets a controller that will be triggered when the user changes the text.
     *
     * @param ctrl Controller to invoke on text input
     */
    void onTextInput(TypedController<String> ctrl);
}
