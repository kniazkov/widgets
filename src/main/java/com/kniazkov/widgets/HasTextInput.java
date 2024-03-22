/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * Some entity that has text that can be modified by the user.
 */
public interface HasTextInput extends HasText {
    /**
     * Sets a controller that determines the behavior when the user modifies the text.
     * @param ctrl Controller
     */
    void onTextInput(@NotNull TypedController<String> ctrl);
}
