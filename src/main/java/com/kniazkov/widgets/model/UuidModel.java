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
    public UUID getDefaultData() {
        return UUID.randomUUID();
    }

    @Override
    public Model<UUID> deriveWithData(final UUID data) {
        return new UuidModel(data);
    }
}
