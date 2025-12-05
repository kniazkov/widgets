/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

/**
 * A composite filter that represents a logical AND operation between two filters.
 * <p>
 * This filter matches a record only when <b>both</b> of the underlying filters
 * match the record. Useful for combining multiple filtering criteria.
 */
public class And implements Filter {
    /**
     * Two filters.
     */
    private final Filter first, second;

    /**
     * Constructs an AND filter from two component filters.
     *
     * @param first the first filter to apply
     * @param second the second filter to apply
     */
    public And(final Filter first, final Filter second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean match(final Record record) {
        return this.first.match(record) && this.second.match(record);
    }
}
