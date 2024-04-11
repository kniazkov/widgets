package com.kniazkov.widgets;

import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;

/**
 * Unique identifier. It is never repeated within the same session.
 */
public final class UId implements Comparable<UId> {
    /**
     * Next identifier.
     */
    private static final AtomicLong next = new AtomicLong(0);

    /**
     * Invalid unique identifier.
     */
    public static final UId INVALID = new UId(0);

    /**
     * Identifier.
     */
    private final long id;

    /**
     * Constructor.
     * @param id Unique number
     */
    private UId(final long id) {
        this.id = id;
    }

    /**
     * Creates a new unique identifier.
     * @return An identifier that did not previously exist within this session
     */
    public static @NotNull UId create() {
        return new UId(next.incrementAndGet());
    }

    /**
     * Parses an identifier from a string representation.
     * This is necessary to use identifiers that come as part of JSON requests from clients.
     * @param str String representation of the identifier
     * @return Parsed identifier
     */
    public static @NotNull UId parse(final @NotNull String str) {
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
     * Returns string representation of the identifier.
     * @return String representation of the identifier
     */
    @Override
    public String toString() {
        return "#" + (this.id > 0 ? this.id : "?");
    }

    /**
     * Compares an identifier to other one for order
     *  (this needed for {@link java.util.TreeSet} and {@link java.util.TreeMap} classes).
     * @param other Other identifier.
     * @return Comparison result
     */
    @Override
    public int compareTo(final @NotNull UId other) {
        return Long.compare(this.id, other.id);
    }

    /**
     * Compares an identifier to other object
     * @param obj Another object
     * @return Comparison result ({@code true} if other object is unique identifier and has
     *  the same number)
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
     * Returns a hash code for the identifier
     *  (this needed for {@link java.util.HashSet} and {@link java.util.HashMap} classes).
     * @return Hash code
     */
    @Override
    public int hashCode() {
        return Long.hashCode(this.id);
    }
}
