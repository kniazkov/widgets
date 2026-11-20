/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Application-owned source of bounded, filtered and sorted pages.
 */
@FunctionalInterface
public interface DataProvider {
    /**
     * Requests one page asynchronously. The provider must return promptly.
     * @param query requested filter, stable column key, order and zero-based page
     * @return cancellable result, never null
     */
    CompletableFuture<Page> load(Query query);

    /**
     * Immutable provider request.
     * @param filter user-entered filter
     * @param sortKey stable column key; empty for provider default order
     * @param descending true for reverse order
     * @param index zero-based page number
     * @param size maximum requested rows
     */
    record Query(String filter, String sortKey, boolean descending, int index, int size) {
        /**
         * Validates page coordinates independently of the provider.
         */
        public Query {
            Objects.requireNonNull(filter);
            Objects.requireNonNull(sortKey);
            if (filter.length() > 120 || sortKey.length() > 96 || index < 0
                    || size < 1 || size > 32) {
                throw new IllegalArgumentException("Invalid table query");
            }
        }

        /**
         * @return zero-based offset without integer overflow
         */
        public long offset() {
            return (long) this.index * this.size;
        }
    }

    /**
     * One immutable logical row. Ordinal position and visible captions are not identity.
     * @param key stable nonempty row key, shared across filters, sorts and pages
     * @param cells display text by stable column key; missing cells are blank
     */
    record DataRow(String key, Map<String, String> cells) {
        /**
         * Keeps the snapshot independent from mutable provider objects.
         */
        public DataRow {
            checkKey(key);
            cells = Map.copyOf(cells);
            if (cells.size() > 8 || cells.values().stream().anyMatch(value ->
                    value.length() > 160)) {
                throw new IllegalArgumentException("Table row exceeds presentation limits");
            }
            cells.keySet().forEach(DataProvider::checkKey);
        }
    }

    /**
     * One result and the total number of rows matching the filter before pagination.
     * @param rows current page in requested order
     * @param total filtered row count
     */
    record Page(List<DataRow> rows, int total) {
        /**
         * Freezes the result and validates the provider contract.
         */
        public Page {
            rows = List.copyOf(rows);
            if (total < rows.size() || rows.size() > 32
                    || rows.stream().map(DataRow::key).distinct().count() != rows.size()) {
                throw new IllegalArgumentException("Invalid table provider page");
            }
        }
    }

    /**
     * @param key stable user-independent identifier
     */
    private static void checkKey(final String key) {
        if (Objects.requireNonNull(key).isBlank() || key.length() > 96) {
            throw new IllegalArgumentException("Invalid table key");
        }
    }
}
