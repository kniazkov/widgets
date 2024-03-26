package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * Unique identifier. It is never repeated within the same session.
 */
public final class UId implements Comparable<UId> {
    /**
     * Next identifier.
     */
    private static long next = 0;

    /**
     * Invalid unique identifier.
     */
    private static final UId INVALID_UID = new UId(0);

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
        return new UId(++next);
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
        return INVALID_UID;
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
}
