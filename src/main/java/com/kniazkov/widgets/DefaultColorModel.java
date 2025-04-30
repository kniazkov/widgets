/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Standard model containing a color.
 */
public final class DefaultColorModel extends DefaultModel<Color> {
    @Override
    protected Model<Color> createInstance() {
        return new DefaultColorModel();
    }

    @Override
    public Color getDefaultData() {
        return Color.BLACK;
    }
}
