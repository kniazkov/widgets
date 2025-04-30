/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Standard model containing a string.
 */
public final class DefaultStringModel extends DefaultModel<String> {
    @Override
    protected Model<String> createInstance() {
        return new DefaultStringModel();
    }

    @Override
    public String getDefaultData() {
        return "";
    }
}
