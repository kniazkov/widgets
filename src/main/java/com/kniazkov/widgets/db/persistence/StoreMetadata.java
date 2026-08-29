/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Persistence-neutral description of one store.
 *
 * @param name store name
 * @param position declaration position
 * @param fields fields in declaration order
 */
public record StoreMetadata(
    String name,
    int position,
    List<FieldMetadata> fields
) {
    /**
     * Validates and creates store metadata.
     */
    public StoreMetadata {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(fields, "fields");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Store name cannot be blank");
        }
        if (position < 0) {
            throw new IllegalArgumentException("Store position cannot be negative");
        }
        fields = List.copyOf(fields);
        final Set<String> names = new HashSet<>();
        for (int index = 0; index < fields.size(); index++) {
            final FieldMetadata field = Objects.requireNonNull(
                fields.get(index),
                "field"
            );
            if (field.position() != index) {
                throw new IllegalArgumentException(
                    "Invalid position for field '" + field.name() + "'"
                );
            }
            if (!names.add(field.name())) {
                throw new IllegalArgumentException(
                    "Duplicate field metadata: '" + field.name() + "'"
                );
            }
        }
    }
}
