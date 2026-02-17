/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import java.util.UUID;

/**
 * A model implementation for storing UUID values.
 */
public final class UuidModel extends DefaultModel<UUID> {
    /**
     * An identifier that we consider invalid.
     */
    private static final UUID INVALID = UUID.fromString(
        "a843176c-36df-44bd-b8a2-b1d8c956c431"
    );

    /**
     * Creates a new UUID model initialized with a randomly generated UUID.
     */
    public UuidModel() {
    }

    /**
     * Creates a new UUID model initialized with the specified UUID value.
     *
     * @param data the initial UUID value
     */
    public UuidModel(final UUID data) {
        super(data);
    }

    @Override
    public boolean isValid() {
        return !this.getData().equals(INVALID);
    }

    @Override
    public UUID getDefaultData() {
        return INVALID;
    }

    @Override
    public Model<UUID> deriveWithData(final UUID data) {
        return new UuidModel(data);
    }
}
