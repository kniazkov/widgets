/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.InputMode;

/**
 * Reactive keyboard hint, defaulting to text.
 */
public final class InputModeModel extends DefaultModel<InputMode> {
    /**
     * Creates a text keyboard hint.
     */
    public InputModeModel() { }
    /**
     * @param data initial keyboard hint
     */
    public InputModeModel(final InputMode data) {
        super(data);
    }
    @Override public InputMode getDefaultData() {
        return InputMode.TEXT;
    }
    @Override public Model<InputMode> deriveWithData(final InputMode data) {
        return new InputModeModel(data);
    }
}
