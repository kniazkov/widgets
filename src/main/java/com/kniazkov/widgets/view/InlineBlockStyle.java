/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.VerticalAlignment;
import java.util.Set;

/**
 * Style definition for {@link InlineBlock}.
 */
public class InlineBlockStyle extends Style implements HasBgColor, HasBorder, HasWidth, HasHeight,
        HasMargin, HasPadding
{
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(
        State.NORMAL,
        State.HOVERED,
        State.ACTIVE
    );

    /**
     * The global default inline block style.
     */
    public static final InlineBlockStyle DEFAULT = new InlineBlockStyle();

    /**
     * Creates the default cell style.
     */
    private InlineBlockStyle() {
        this.setBgColor(Color.TRANSPARENT);
        this.setBorderStyle(BorderStyle.NONE);
        this.setWidth(AbsoluteSize.UNDEFINED);
        this.setHeight(AbsoluteSize.UNDEFINED);
        this.setMargin(0);
        this.setPadding(0);
    }

    /**
     * Creates a new inline block style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public InlineBlockStyle(final InlineBlockStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public InlineBlockStyle derive() {
        return new InlineBlockStyle(this);
    }
}
