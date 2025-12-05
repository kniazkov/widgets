/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

/**
 * A functional interface for filtering records based on custom criteria.
 * <p>
 * Implementations of this interface define the logic for determining whether
 * a given record matches specific conditions. Used in record sets and tables
 * to selectively include or exclude records from views or operations.
 */
public interface Filter {
    /**
     * Tests whether the specified record matches the filter criteria.
     *
     * @param record the record to test
     * @return {@code true} if the record matches the filter; {@code false} otherwise
     */
    boolean match(Record record);

    /**
     * Combines this filter with another filter using a logical AND operation.
     * <p>
     * The resulting filter matches records that satisfy both this filter
     * <b>and</b> the specified filter.
     *
     * @param other the other filter to combine with
     * @return a new filter representing the conjunction of both filters
     */
    default Filter and(Filter other) {
        return new And(this, other);
    }
}
