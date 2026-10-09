/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Style definition for {@link NoWrap}.
 */
public class NoWrapStyle extends Style
        implements HasMargin, HasPadding, HasHiddenState {
    /**
     * The global default no-wrap style.
     */
    public static final NoWrapStyle DEFAULT = new NoWrapStyle();

    /**
     * Creates the default no-wrap style.
     */
    private NoWrapStyle() {
        this.setMargin(0);
        this.setPadding(0);
        this.setHiddenFlag(false);
    }

    /**
     * Creates a new no-wrap style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public NoWrapStyle(final NoWrapStyle parent) {
        super(parent);
    }

    @Override
    public NoWrapStyle derive() {
        return new NoWrapStyle(this);
    }
}
