/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    /**
     * Checks whether this catalog can be upgraded without removing or
     * changing existing stores and fields. Declaration order is irrelevant.
     *
     * @param next configured metadata
     * @return whether the upgrade is compatible
     */
    public boolean canUpgradeTo(final DatabaseMetadata next) {
        Objects.requireNonNull(next, "next");
        if (this.formatVersion != next.formatVersion) {
            return false;
        }
        final Map<String, StoreMetadata> nextStores = new HashMap<>();
        for (final StoreMetadata store : next.stores) {
            nextStores.put(store.name(), store);
        }
        for (final StoreMetadata currentStore : this.stores) {
            final StoreMetadata nextStore = nextStores.get(currentStore.name());
            if (nextStore == null
                || !fieldsRemainCompatible(currentStore, nextStore)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether all old field definitions remain in a store.
     *
     * @param currentStore persisted store metadata
     * @param nextStore configured store metadata
     * @return whether every old field remains unchanged
     */
    private static boolean fieldsRemainCompatible(
        final StoreMetadata currentStore,
        final StoreMetadata nextStore
    ) {
        final Map<String, FieldMetadata> nextFields = new HashMap<>();
        for (final FieldMetadata field : nextStore.fields()) {
            nextFields.put(field.name(), field);
        }
        for (final FieldMetadata currentField : currentStore.fields()) {
            final FieldMetadata nextField = nextFields.get(currentField.name());
            if (nextField == null
                || !sameDefinition(currentField, nextField)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares the persistent meaning of two fields without their positions.
     *
     * @param current persisted field
     * @param next configured field
     * @return whether both fields have the same persistent definition
     */
    private static boolean sameDefinition(
        final FieldMetadata current,
        final FieldMetadata next
    ) {
        return current.name().equals(next.name())
            && current.type().equals(next.type())
            && current.valueKind() == next.valueKind()
            && current.defaultValue().equals(next.defaultValue())
            && Objects.equals(
                current.referencedStore(),
                next.referencedStore()
            );
    }
}
