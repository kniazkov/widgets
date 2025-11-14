/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.BorderStyleModel;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Style definition for {@link Button}.
 */
public class ButtonStyle extends Style implements HasBgColor, HasBorder, HasAbsoluteWidth,
        HasAbsoluteHeight, HasMargin, HasPadding
{
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            State.NORMAL,
            State.HOVERED,
            State.ACTIVE,
            State.DISABLED
        )));

    /**
     * The global default button style.
     */
    public static final ButtonStyle DEFAULT = new ButtonStyle();

    /**
     * Creates the default button style.
     */
    private ButtonStyle() {
        this.setBgColor(State.NORMAL, new Color(224, 224, 224));
        this.setBgColor(State.HOVERED, new Color(240, 240, 240));
        this.setBgColor(State.ACTIVE, Color.WHITE);
        this.setBgColor(State.DISABLED, Color.LIGHT_GRAY);

        this.setBorderColor(State.NORMAL, Color.GRAY);
        this.setBorderColor(State.HOVERED, Color.DARK_GRAY);
        this.setBorderColor(State.ACTIVE, Color.BLACK);
        this.setBorderColor(State.DISABLED, Color.DARK_GRAY);

        final BorderStyleModel style = new BorderStyleModel(BorderStyle.SOLID);
        this.setBorderStyleModel(State.NORMAL, style);
        this.setBorderStyleModel(State.HOVERED, style.asCascading());
        this.setBorderStyleModel(State.ACTIVE, style.asCascading());
        this.setBorderStyle(State.DISABLED, BorderStyle.DASHED);

        this.setBorderWidth("1px");
        this.setBorderRadius("5px");

        this.setWidth(AbsoluteSize.UNDEFINED);
        this.setHeight(AbsoluteSize.UNDEFINED);
        this.setMargin("2px");
        this.setPadding("10px");
    }

    /**
     * Creates a new button style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public ButtonStyle(final ButtonStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public ButtonStyle derive() {
        return new ButtonStyle(this);
    }
}
