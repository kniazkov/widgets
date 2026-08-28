/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.db.internal.MemoryDatabase;
import com.kniazkov.widgets.db.persistence.NoPersistence;
import com.kniazkov.widgets.db.persistence.Persistence;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configures schemas and persistence before creating a {@link Database}.
 */
public final class DatabaseBuilder {
    /**
     * Configured store schemas.
     */
    private final Map<String, Schema> schemas;

    /**
     * Persistence backend.
     */
    private Persistence persistence;

    /**
     * Creates an empty builder using memory-only persistence.
     */
    public DatabaseBuilder() {
        this.schemas = new LinkedHashMap<>();
        this.persistence = new NoPersistence();
    }

    /**
     * Registers a store schema.
     *
     * @param name store name
     * @param schema store schema
     * @return this builder
     */
    public DatabaseBuilder store(final String name, final Schema schema) {
        final String key = Objects.requireNonNull(name, "name");
        Objects.requireNonNull(schema, "schema");
        if (key.isBlank()) {
            throw new IllegalArgumentException("Store name cannot be blank");
        }
        if (this.schemas.putIfAbsent(key, schema) != null) {
            throw new IllegalStateException("Store '" + key + "' is already configured");
        }
        return this;
    }

    /**
     * Selects a persistence backend.
     *
     * @param backend persistence backend
     * @return this builder
     */
    public DatabaseBuilder persistence(final Persistence backend) {
        this.persistence = Objects.requireNonNull(backend, "backend");
        return this;
    }

    /**
     * Builds and loads the database.
     *
     * @return database
     */
    public Database build() {
        return new MemoryDatabase(this.schemas, this.persistence);
    }
}
