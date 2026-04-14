/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.HandlesPointerEvents;

/**
 * A checkbox widget that allows users to toggle between checked and unchecked states.
 */
public class CheckBox extends InlineWidget<CheckBoxStyle> implements HasCheckedState, HasDisabledState,
        HasColor, HasBgColor, HasAbsoluteWidth, HasAbsoluteHeight, HasSelectableImage, HandlesPointerEvents
{
    /**
     * Returns the default style instance used by check boxes.
     *
     * @return the singleton default {@link CheckBoxStyle} instance
     */
    public static CheckBoxStyle getDefaultStyle() {
        return CheckBoxStyle.DEFAULT;
    }

    /**
     * Constructs a new checkbox.
     */
    public CheckBox() {
        super(getDefaultStyle());
        this.uncheck();
    }

    @Override
    public String getType() {
        return "checkbox";
    }
}
