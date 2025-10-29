/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;

/**
 * Style definition for {@link InputField}.
 */
public class InputFieldStyle extends Style implements HasStyledText, HasColor {
    /**
     * The global default input field style.
     */
    public static final InputFieldStyle DEFAULT = new InputFieldStyle();

    /**
     * Creates the default input field style.
     */
    private InputFieldStyle() {
        this.bindData(State.NORMAL, Property.COLOR, Color.BLACK);
        this.bindData(State.HOVERED, Property.COLOR, Color.BLUE);
        this.bindData(State.ACTIVE, Property.COLOR, Color.RED);

        this.bindData(State.NORMAL, Property.BG_COLOR, Color.WHITE);
    }

    /**
     * Creates a new input field style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public InputFieldStyle(final InputFieldStyle parent) {
        super(parent);
    }

    @Override
    public InputFieldStyle derive() {
        return new InputFieldStyle(this);
    }
}
