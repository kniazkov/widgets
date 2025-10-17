/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

public final class EmptyStyle extends Style {
    public static final Style INSTANCE = new EmptyStyle();

    private EmptyStyle() {
        super(null);
    }

    @Override
    public Style derive() {
        return this;
    }
}
