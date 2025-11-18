/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.BorderStyle;
import java.util.Set;

/**
 * Style definition for {@link BaseImage}.
 */
public class ImageStyle extends Style implements HasBorder {
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(State.NORMAL);

    /**
     * The global default image widget style.
     */
    public static final ImageStyle DEFAULT = new ImageStyle();

    /**
     * Creates the default image style.
     */
    private ImageStyle() {
        this.setBorderStyle(BorderStyle.NONE);
    }

    /**
     * Creates a new image style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public ImageStyle(final ImageStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public ImageStyle derive() {
        return new ImageStyle(this);
    }
}
