/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import java.util.Set;

/**
 * Style definition for {@link ActiveText}.
 */
public class ActiveTextStyle extends Style implements HasStyledText, HasColor {
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(
        State.NORMAL,
        State.HOVERED,
        State.ACTIVE
    );

    /**
     * The global default active text widget style.
     */
    public static final ActiveTextStyle DEFAULT = new ActiveTextStyle();

    /**
     * Creates the default active text style.
     */
    private ActiveTextStyle() {
        this.setFontFace(FontFace.DEFAULT);
        this.setFontSize(FontSize.DEFAULT);
        this.setFontWeight(FontWeight.NORMAL);
        this.setItalic(false);
        this.setColor(State.NORMAL, Color.BLACK);
        this.setColor(State.HOVERED, Color.NAVY);
        this.setColor(State.HOVERED, Color.BLUE);
    }

    /**
     * Creates a new active text style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public ActiveTextStyle(final ActiveTextStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public ActiveTextStyle derive() {
        return new ActiveTextStyle(this);
    }
}
