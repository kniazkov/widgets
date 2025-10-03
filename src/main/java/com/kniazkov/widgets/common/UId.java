/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple unique identifier wrapper. Instances of {@code UId} are generated sequentially using an
 * {@link AtomicLong}, ensuring thread-safe local uniqueness within a single JVM.
 */
public final class UId implements Comparable<UId> {
    /**
     * Next unique number.
     */
    private static final AtomicLong next = new AtomicLong(0);

    /**
     * Represents an invalid ID.
     */
    public static final UId INVALID = new UId(0);

    /**
     * Integer identifier.
     */
    private final long id;

    /**
     * Private constructor.
     *
     * @param id integer identifier
     */
    private UId(final long id) {
        this.id = id;
    }

    /**
     * Creates a new unique ID.
     *
     * @return a newly generated UId with a positive value
     */
    public static UId create() {
        return new UId(next.incrementAndGet());
    }

    /**
     * Parses a string in the format {@code "#123"} into a {@code UId}.
     * If parsing fails or results in a non-positive value, {@link #INVALID} is returned.
     *
     * @param str the string to parse
     * @return a valid {@code UId} if the string is well-formed, otherwise {@link #INVALID}
     */
    public static UId parse(final String str) {
        if (str.startsWith("#")) {
            try {
                final long id = Long.parseLong(str.substring(1));
                if (id > 0) {
                    return new UId(id);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return INVALID;
    }

    @Override
    public String toString() {
        return "#" + (this.id > 0 ? this.id : "?");
    }

    @Override
    public int compareTo(final UId other) {
        return Long.compare(this.id, other.id);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj instanceof UId) {
            final UId other = (UId) obj;
            return this.id == other.id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.id);
    }

    /**
     * Checks if this ID is valid.
     *
     * @return true if the ID is greater than 0, false otherwise
     */
    public boolean isValid() {
        return this.id > 0;
    }
}
