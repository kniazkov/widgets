/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.BoxShadow;
import com.kniazkov.widgets.common.BoxSizing;
import com.kniazkov.widgets.common.Cursor;
import com.kniazkov.widgets.common.Transition;
import java.util.Set;

/**
 * Style definition for {@link ImageWidget}.
 */
public class ImageWidgetStyle extends Style implements HasBorder, HasMargin,
        HasAbsoluteWidth, HasAbsoluteHeight, HasOpacity, HasBoxShadow, HasCursor, HasTransition,
        HasBoxSizing {
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(State.NORMAL);

    /**
     * The global default image widget style.
     */
    public static final ImageWidgetStyle DEFAULT = new ImageWidgetStyle();

    /**
     * Creates the default image style.
     */
    @SuppressWarnings("this-escape")
    protected ImageWidgetStyle() {
        /*
         * Construction initializes inherited style models before publication.
         */
        this.setBoxShadow(BoxShadow.NONE);
        this.setCursor(Cursor.AUTO);
        this.setTransition(Transition.NONE);
        this.setBoxSizing(BoxSizing.CONTENT_BOX);
        this.setBorderStyle(BorderStyle.NONE);
        this.setMargin(0);
        this.setOpacity(1.0);
    }

    /**
     * Creates a new image style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public ImageWidgetStyle(final ImageWidgetStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public ImageWidgetStyle derive() {
        return new ImageWidgetStyle(this);
    }
}
