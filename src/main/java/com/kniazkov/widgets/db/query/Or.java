/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.query;

import com.kniazkov.widgets.db.DataRecord;
import com.kniazkov.widgets.db.Field;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Internal logical OR node.
 */
final class Or implements Condition {
    /**
     * First operand.
     */
    private final Condition first;

    /**
     * Second operand.
     */
    private final Condition second;

    /**
     * Creates a node.
     *
     * @param first first operand
     * @param second second operand
     */
    Or(final Condition first, final Condition second) {
        this.first = Objects.requireNonNull(first, "first");
        this.second = Objects.requireNonNull(second, "second");
    }

    @Override
    public boolean matches(final DataRecord record) {
        return this.first.matches(record) || this.second.matches(record);
    }

    @Override
    public Set<Field<?>> dependencies() {
        final Set<Field<?>> fields = new HashSet<>(this.first.dependencies());
        fields.addAll(this.second.dependencies());
        return Set.copyOf(fields);
    }
}
