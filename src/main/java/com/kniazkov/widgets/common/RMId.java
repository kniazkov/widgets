/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A run-local monotonic identifier (RMID).
 *
 * <p>Generated values are positive and strictly increase for the lifetime of the current JVM.
 * They are not globally unique and may be reused after the server restarts.</p>
 */
public final class RMId implements Comparable<RMId> {
    /**
     * Next unique number.
     */
    private static final AtomicLong NEXT = new AtomicLong(0);

    /**
     * Represents an invalid ID.
     */
    public static final RMId INVALID = new RMId(0);

    /**
     * Integer identifier.
     */
    private final long id;

    /**
     * Private constructor.
     *
     * @param id integer identifier
     */
    private RMId(final long id) {
        this.id = id;
    }

    /**
     * Creates the next identifier for the current JVM run.
     *
     * @return a positive RMId greater than all previously generated identifiers
     */
    public static RMId create() {
        return new RMId(NEXT.incrementAndGet());
    }

    /**
     * Parses a string in the format {@code "#123"} into a {@code RMId}.
     * If parsing fails or results in a non-positive value, {@link #INVALID} is returned.
     *
     * @param str the string to parse
     * @return a valid {@code RMId} if the string is well-formed, otherwise {@link #INVALID}
     */
    public static RMId parse(final String str) {
        if (str.startsWith("#")) {
            try {
                final long id = Long.parseLong(str.substring(1));
                if (id > 0) {
                    return new RMId(id);
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
    public int compareTo(final RMId other) {
        return Long.compare(this.id, other.id);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RMId) {
            final RMId other = (RMId) obj;
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
