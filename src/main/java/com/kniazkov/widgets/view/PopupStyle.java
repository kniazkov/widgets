/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.BoxShadow;
import com.kniazkov.widgets.common.BoxSizing;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.HorizontalAlignment;
import com.kniazkov.widgets.common.VerticalAlignment;
import java.util.Set;

/**
 * Style definition shared by modal and non-modal popup windows.
 */
public class PopupStyle extends Style implements HasBgColor, HasBorder,
        HasWidth, HasHeight, HasPadding, HasBoxShadow, HasBoxSizing,
        HasHorizontalAlignment, HasVerticalAlignment {
    /**
     * Supported visual states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(State.NORMAL);

    /**
     * Global default popup style.
     */
    public static final PopupStyle DEFAULT = new PopupStyle();

    /**
     * Creates the default popup style.
     */
    protected PopupStyle() {
        this.setBgColor(Color.WHITE);
        this.setBorderColor(DefaultTheme.BORDER);
        this.setBorderStyle(BorderStyle.SOLID);
        this.setBorderWidth(1);
        this.setBorderRadius(12);
        this.setWidth(AbsoluteSize.UNDEFINED);
        this.setHeight(AbsoluteSize.UNDEFINED);
        this.setPadding(20);
        this.setBoxShadow(new BoxShadow(0, 12, 32, new Color(15, 23, 42, 45)));
        this.setBoxSizing(BoxSizing.BORDER_BOX);
        this.setHorizontalAlignment(HorizontalAlignment.CENTER);
        this.setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    /**
     * Creates a derived popup style.
     *
     * @param parent parent style
     */
    public PopupStyle(final PopupStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public PopupStyle derive() {
        return new PopupStyle(this);
    }
}
