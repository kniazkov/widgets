/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.query;

import com.kniazkov.widgets.db.DataRecord;
import com.kniazkov.widgets.db.Field;
import java.util.Objects;
import java.util.Set;

/**
 * Internal logical NOT node.
 */
final class Not implements Condition {
    /**
     * Operand.
     */
    private final Condition source;

    /**
     * Creates a node.
     *
     * @param source operand
     */
    Not(final Condition source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    @Override
    public boolean matches(final DataRecord record) {
        return !this.source.matches(record);
    }

    @Override
    public Set<Field<?>> dependencies() {
        return this.source.dependencies();
    }
}
