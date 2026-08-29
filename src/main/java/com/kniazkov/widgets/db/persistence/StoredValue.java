/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence;

import java.util.Objects;

/**
 * A persistence-neutral scalar value that retains its native type.
 */
public sealed interface StoredValue permits StoredValue.StringValue,
    StoredValue.IntegerValue, StoredValue.RealValue, StoredValue.BooleanValue {
    /**
     * Supported scalar kinds.
     */
    enum Kind {
        /**
         * Text value.
         */
        STRING,

        /**
         * Integer value.
         */
        INTEGER,

        /**
         * Real value.
         */
        REAL,

        /**
         * Boolean value.
         */
        BOOLEAN
    }

    /**
     * Returns the scalar kind.
     *
     * @return kind
     */
    Kind getKind();

    /**
     * Returns a text value.
     *
     * @return value
     */
    default String getString() {
        if (this instanceof StringValue stored) {
            return stored.value();
        }
        throw this.typeMismatch(Kind.STRING);
    }

    /**
     * Returns an integer value.
     *
     * @return value
     */
    default int getInteger() {
        if (this instanceof IntegerValue stored) {
            return stored.value();
        }
        throw this.typeMismatch(Kind.INTEGER);
    }

    /**
     * Returns a real value, accepting an integer without loss.
     *
     * @return value
     */
    default double getReal() {
        if (this instanceof RealValue stored) {
            return stored.value();
        }
        if (this instanceof IntegerValue stored) {
            return stored.value();
        }
        throw this.typeMismatch(Kind.REAL);
    }

    /**
     * Returns a boolean value.
     *
     * @return value
     */
    default boolean getBoolean() {
        if (this instanceof BooleanValue stored) {
            return stored.value();
        }
        throw this.typeMismatch(Kind.BOOLEAN);
    }

    /**
     * Creates a type mismatch exception.
     *
     * @param expected expected kind
     * @return exception
     */
    private IllegalArgumentException typeMismatch(final Kind expected) {
        return new IllegalArgumentException(
            "Expected " + expected + " value, got " + this.getKind()
        );
    }

    /**
     * A text value.
     *
     * @param value value
     */
    record StringValue(String value) implements StoredValue {
        /**
         * Validates the value.
         */
        public StringValue {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public Kind getKind() {
            return Kind.STRING;
        }
    }

    /**
     * A signed 32-bit integer value.
     *
     * @param value value
     */
    record IntegerValue(int value) implements StoredValue {
        @Override
        public Kind getKind() {
            return Kind.INTEGER;
        }
    }

    /**
     * A finite double-precision real value.
     *
     * @param value value
     */
    record RealValue(double value) implements StoredValue {
        /**
         * Validates the value.
         */
        public RealValue {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException(
                    "Stored real value must be finite"
                );
            }
        }

        @Override
        public Kind getKind() {
            return Kind.REAL;
        }
    }

    /**
     * A boolean value.
     *
     * @param value value
     */
    record BooleanValue(boolean value) implements StoredValue {
        @Override
        public Kind getKind() {
            return Kind.BOOLEAN;
        }
    }
}
