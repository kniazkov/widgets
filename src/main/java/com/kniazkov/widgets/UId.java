/*
 * Copyright (c) 2025 Ivan Kniazkov
 */

package com.kniazkov.widgets;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A session-unique identifier.
 * <p>
 *     Instances of this class represent unique, monotonically increasing identifiers
 *     that are guaranteed to be distinct within the same application session.
 *     Identifiers are generated using an atomic counter, making this class safe for use
 *     in concurrent environments.
 * </p>
 *
 * <p>
 *     Identifiers can be serialized and deserialized using a string format: {@code #<number>},
 *     e.g., {@code #123}. The {@link #parse(String)} method supports conversion from this
 *     representation, typically used when identifiers are passed from clients (e.g., via JSON).
 * </p>
 *
 * <p>
 *     The class supports equality, ordering (via {@link Comparable}), and is hashable,
 *     making it compatible with all standard Java collections such as {@link java.util.HashSet},
 *     {@link java.util.HashMap}, {@link java.util.TreeSet}, etc.
 * </p>
 */
public final class UId implements Comparable<UId> {
    /**
     * Global atomic counter for generating unique identifiers.
     */
    private static final AtomicLong next = new AtomicLong(0);

    /**
     * Special constant representing an invalid or uninitialized identifier.
     */
    public static final UId INVALID = new UId(0);

    /**
     * Numeric ID value (non-zero for valid identifiers).
     */
    private final long id;

    /**
     * Constructs a new identifier from a numeric value.
     * Private to enforce controlled creation through {@link #create()} or {@link #parse(String)}.
     *
     * @param id the numeric ID
     */
    private UId(final long id) {
        this.id = id;
    }

    /**
     * Generates a new globally unique identifier.
     * <p>
     *     The returned value is guaranteed to be unique within the current session.
     * </p>
     *
     * @return a newly created unique identifier
     */
    public static UId create() {
        return new UId(next.incrementAndGet());
    }

    /**
     * Parses a {@link UId} from its string representation.
     * <p>
     *     The expected format is {@code #<number>}. If the string does not conform to this format,
     *     or the number is invalid, {@link #INVALID} is returned.
     * </p>
     *
     * @param str the string to parse
     * @return a parsed {@link UId} instance, or {@link #INVALID} if parsing fails
     */
    public static UId parse(final String str) {
        if (str.startsWith("#")) {
            try {
                final long id = Long.parseLong(str.substring(1));
                return new UId(id);
            } catch (NumberFormatException ignored) {
            }
        }
        return INVALID;
    }

    /**
     * Returns a string representation of this identifier.
     * <p>
     *     The format is {@code #<number>} for valid IDs, or {@code #?} for {@link #INVALID}.
     * </p>
     *
     * @return string representation of this identifier
     */
    @Override
    public String toString() {
        return "#" + (this.id > 0 ? this.id : "?");
    }

    /**
     * Compares this identifier to another based on numeric order.
     *
     * @param other the identifier to compare against
     * @return a negative number, zero, or a positive number as this ID is less than,
     *  equal to, or greater than the other
     */
    @Override
    public int compareTo(final UId other) {
        return Long.compare(this.id, other.id);
    }

    /**
     * Determines whether this identifier is equal to another object.
     * <p>
     *     Two {@code UId} instances are considered equal if they hold the same numeric value.
     * </p>
     *
     * @param obj the object to compare
     * @return {@code true} if the other object is a {@code UId} with the same value;
     *  {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof UId) {
            final UId other = (UId) obj;
            return this.id == other.id;
        }
        return false;
    }

    /**
     * Returns the hash code for this identifier, based on its numeric value.
     *
     * @return hash code of this identifier
     */
    @Override
    public int hashCode() {
        return Long.hashCode(this.id);
    }
}
