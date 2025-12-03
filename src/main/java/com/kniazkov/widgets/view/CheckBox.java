/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * A checkbox widget that allows users to toggle between checked and unchecked states.
 */
public class CheckBox extends InlineWidget implements HasCheckedState, HasDisabledState {
    /**
     * Constructs a new checkbox.
     */
    public CheckBox() {
        super(Style.getEmptyStyle());
        this.uncheck();
    }

    @Override
    public String getType() {
        return "checkbox";
    }
}
