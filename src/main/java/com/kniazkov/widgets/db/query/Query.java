/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.query;

import com.kniazkov.widgets.db.DataRecord;
import com.kniazkov.widgets.db.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An immutable condition and ordering specification for a live query.
 */
public final class Query {
    /**
     * Filter condition.
     */
    private final Condition condition;

    /**
     * Ordering clauses.
     */
    private final List<Order> orders;

    /**
     * Creates a query.
     *
     * @param condition condition
     * @param orders ordering clauses
     */
    private Query(final Condition condition, final List<Order> orders) {
        this.condition = Objects.requireNonNull(condition, "condition");
        this.orders = List.copyOf(orders);
    }

    /**
     * Creates a query matching every record.
     *
     * @return query
     */
    public static Query all() {
        return new Query(Conditions.all(), List.of());
    }

    /**
     * Creates a filtered query.
     *
     * @param condition condition
     * @return query
     */
    public static Query where(final Condition condition) {
        return new Query(condition, List.of());
    }

    /**
     * Adds the first ordering clause.
     *
     * @param order ordering
     * @return new query
     */
    public Query orderBy(final Order order) {
        return this.thenBy(order);
    }

    /**
     * Adds another ordering clause.
     *
     * @param order ordering
     * @return new query
     */
    public Query thenBy(final Order order) {
        final List<Order> result = new ArrayList<>(this.orders);
        result.add(Objects.requireNonNull(order, "order"));
        return new Query(this.condition, result);
    }

    /**
     * Checks whether a record matches.
     *
     * @param record record
     * @return match flag
     */
    public boolean matches(final DataRecord record) {
        return this.condition.matches(record);
    }

    /**
     * Returns a comparator for matching records.
     *
     * @return comparator
     */
    public Comparator<DataRecord> comparator() {
        Comparator<DataRecord> result =
            Comparator.comparing(DataRecord::getCreatedAt);
        if (!this.orders.isEmpty()) {
            result = this.orders.get(0)::compare;
            for (int index = 1; index < this.orders.size(); index++) {
                result = result.thenComparing(this.orders.get(index)::compare);
            }
        }
        return result.thenComparing(DataRecord::getId);
    }

    /**
     * Returns all fields that can change membership or ordering.
     *
     * @return dependent fields
     */
    public Set<Field<?>> dependencies() {
        final Set<Field<?>> fields = new HashSet<>(this.condition.dependencies());
        for (final Order order : this.orders) {
            fields.add(order.getField());
        }
        return Set.copyOf(fields);
    }
}
