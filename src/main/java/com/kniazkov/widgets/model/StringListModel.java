/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import java.util.List;

/**
 * An ordered list of strings stored as immutable snapshots.
 * Replace the list through {@link #setData(List)} to notify listeners.
 */
public final class StringListModel extends DefaultModel<List<String>> {
    /**
     * Creates an empty list model.
     */
    public StringListModel() {
    }

    /**
     * Creates a model with a defensive copy of the supplied list.
     * @param data initial non-null strings
     */
    public StringListModel(final List<String> data) {
        super(List.copyOf(data));
    }

    @Override
    public List<String> getDefaultData() {
        return List.of();
    }

    @Override
    public boolean setData(final List<String> data) {
        if (data == null || data.stream().anyMatch(value -> value == null)) {
            return false;
        }
        return super.setData(List.copyOf(data));
    }

    @Override
    public Model<List<String>> deriveWithData(final List<String> data) {
        return new StringListModel(data);
    }
}
