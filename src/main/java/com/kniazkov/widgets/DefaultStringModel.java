/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * Standard model containing a string.
 */
public final class DefaultStringModel extends DefaultModel<String> {
    @Override
    public @NotNull String getDefaultData() {
        return "";
    }
}
