/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence;

import com.kniazkov.widgets.db.persistence.StoredValue.Kind;
import java.util.Objects;

/**
 * Persistence-neutral description of one store field.
 *
 * @param name field name
 * @param type semantic value type
 * @param valueKind physical scalar kind
 * @param defaultValue default stored value
 * @param position declaration position
 * @param referencedStore referenced store, or {@code null}
 */
public record FieldMetadata(
    String name,
    String type,
    Kind valueKind,
    StoredValue defaultValue,
    int position,
    String referencedStore
) {
    /**
     * Validates and creates field metadata.
     */
    public FieldMetadata {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(valueKind, "valueKind");
        Objects.requireNonNull(defaultValue, "defaultValue");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Field name cannot be blank");
        }
        if (type.isBlank()) {
            throw new IllegalArgumentException("Field type cannot be blank");
        }
        if (position < 0) {
            throw new IllegalArgumentException("Field position cannot be negative");
        }
        if (defaultValue.getKind() != valueKind) {
            throw new IllegalArgumentException(
                "Default value kind does not match field value kind"
            );
        }
        if (referencedStore != null && referencedStore.isBlank()) {
            throw new IllegalArgumentException(
                "Referenced store name cannot be blank"
            );
        }
    }
}
