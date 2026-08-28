/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.query;

import com.kniazkov.widgets.db.DataRecord;
import com.kniazkov.widgets.db.Field;
import java.util.Set;

/**
 * A node in an inspectable query condition tree.
 */
public interface Condition {
    /**
     * Checks whether a record matches.
     *
     * @param record record
     * @return match flag
     */
    boolean matches(DataRecord record);

    /**
     * Returns fields that can affect the result.
     *
     * @return dependent fields
     */
    Set<Field<?>> dependencies();

    /**
     * Combines conditions with logical AND.
     *
     * @param other other condition
     * @return combined condition
     */
    default Condition and(final Condition other) {
        return new And(this, other);
    }

    /**
     * Combines conditions with logical OR.
     *
     * @param other other condition
     * @return combined condition
     */
    default Condition or(final Condition other) {
        return new Or(this, other);
    }

    /**
     * Negates this condition.
     *
     * @return negated condition
     */
    default Condition not() {
        return new Not(this);
    }
}
