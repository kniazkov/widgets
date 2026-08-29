/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Persistence-neutral database schema catalog.
 *
 * @param formatVersion persistence format version
 * @param stores stores in declaration order
 */
public record DatabaseMetadata(
    int formatVersion,
    List<StoreMetadata> stores
) {
    /**
     * Current persistence format version.
     */
    public static final int CURRENT_FORMAT_VERSION = 1;

    /**
     * Validates and creates database metadata.
     */
    public DatabaseMetadata {
        Objects.requireNonNull(stores, "stores");
        if (formatVersion <= 0) {
            throw new IllegalArgumentException(
                "Format version must be positive"
            );
        }
        stores = List.copyOf(stores);
        final Set<String> names = new HashSet<>();
        for (int index = 0; index < stores.size(); index++) {
            final StoreMetadata store = Objects.requireNonNull(
                stores.get(index),
                "store"
            );
            if (store.position() != index) {
                throw new IllegalArgumentException(
                    "Invalid position for store '" + store.name() + "'"
                );
            }
            if (!names.add(store.name())) {
                throw new IllegalArgumentException(
                    "Duplicate store metadata: '" + store.name() + "'"
                );
            }
        }
        for (final StoreMetadata store : stores) {
            for (final FieldMetadata field : store.fields()) {
                if (field.referencedStore() != null
                    && !names.contains(field.referencedStore())) {
                    throw new IllegalArgumentException(
                        "Field '" + store.name() + "." + field.name()
                            + "' references unknown store '"
                            + field.referencedStore() + "'"
                    );
                }
            }
        }
    }
}
