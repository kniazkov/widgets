/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * A default string model implementation.
 */
public final class DefaultStringModel extends DefaultModel<String> {
    @Override
    public String getDefaultData() {
        return "";
    }
}
