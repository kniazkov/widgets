/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * Standard model containing a color.
 */
public final class DefaultColorModel extends DefaultModel<Color> {
    @Override
    public @NotNull Color getDefaultData() {
        return Color.BLACK;
    }
}
