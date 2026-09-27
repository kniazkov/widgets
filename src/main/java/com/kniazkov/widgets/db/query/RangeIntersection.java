/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.query;

import com.kniazkov.widgets.common.NumericRange;
import com.kniazkov.widgets.db.DataRecord;
import com.kniazkov.widgets.db.Field;
import java.util.Objects;
import java.util.Set;

/**
 * Inspectable interval-overlap query node.
 * @param field interval field
 * @param value query interval
 */
public record RangeIntersection(Field<NumericRange> field, NumericRange value)
    implements Condition {
    /**
     * Validates operands.
     */
    public RangeIntersection {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(value, "value");
    }
    @Override public boolean matches(final DataRecord record) {
        return record.model(field).getData().intersects(value);
    }
    @Override public Set<Field<?>> dependencies() {
        return Set.of(field);
    }
}
