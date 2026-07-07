/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.model.*;

import java.util.UUID;

/**
 * Represents a logical data type used by the primitive database layer.
 *
 * @param <T> the Java type of the values represented by this {@code Type}
 */
public abstract class Type<T> {
    /**
     * Returns the Java {@link Class} that represents this type's value.
     *
     * @return the Java class of the values handled by this type
     */
    public abstract Class<T> getValueClass();

    /**
     * Creates a new {@link Model} instance capable of storing values of this type.
     *
     * @return a new model associated with this type
     */
    public abstract Model<T> createModel();

    /**
     * A built-in {@code Type} representing boolean values.
     */
    public static final Type<Boolean> BOOLEAN = new Type<Boolean>() {
        @Override
        public Class<Boolean> getValueClass() {
            return Boolean.class;
        }

        @Override
        public Model<Boolean> createModel() {
            return new BooleanModel();
        }
    };

    /**
     * A built-in {@code Type} representing textual values.
     */
    public static final Type<String> STRING = new Type<String>() {
        @Override
        public Class<String> getValueClass() {
            return String.class;
        }

        @Override
        public Model<String> createModel() {
            return new StringModel();
        }
    };

    /**
     * A built-in {@code Type} representing non-empty textual values.
     */
    public static final Type<String> NOT_EMPTY_STRING = new Type<String>() {
        @Override
        public Class<String> getValueClass() {
            return String.class;
        }

        @Override
        public Model<String> createModel() {
            return new NotEmptyStringModel();
        }
    };

    /**
     * A built-in {@code Type} representing username textual values without spaces.
     */
    public static final Type<String> USERNAME = new Type<String>() {
        @Override
        public Class<String> getValueClass() {
            return String.class;
        }

        @Override
        public Model<String> createModel() {
            return new UsernameModel();
        }
    };

    /**
     * A built-in {@code Type} representing integer values.
     */
    public static final Type<Integer> INTEGER = new Type<Integer>() {
        @Override
        public Class<Integer> getValueClass() {
            return Integer.class;
        }

        @Override
        public Model<Integer> createModel() {
            return new IntegerModel();
        }
    };

    /**
     * A built-in {@code Type} representing strictly positive integer values.
     */
    public static final Type<Integer> POSITIVE_INTEGER = new Type<Integer>() {
        @Override
        public Class<Integer> getValueClass() {
            return Integer.class;
        }

        @Override
        public Model<Integer> createModel() {
            return new ValidatedIntegerModel(ValidatedIntegerModel.POSITIVE);
        }
    };

    /**
     * A built-in {@code Type} representing real (double-precision) numeric values.
     */
    public static final Type<Double> REAL = new Type<Double>() {
        @Override
        public Class<Double> getValueClass() {
            return Double.class;
        }

        @Override
        public Model<Double> createModel() {
            return new RealNumberModel();
        }
    };

    /**
     * A built-in {@code Type} representing strictly positive real (double-precision)
     * numeric values.
     */
    public static final Type<Double> POSITIVE_REAL = new Type<Double>() {
        @Override
        public Class<Double> getValueClass() {
            return Double.class;
        }

        @Override
        public Model<Double> createModel() {
            return new ValidatedRealNumberModel(ValidatedRealNumberModel.POSITIVE);
        }
    };

    /**
     * A built-in {@code Type} representing unique identifiers.
     */
    public static final Type<UUID> IDENTIFIER = new Type<UUID>() {
        @Override
        public Class<UUID> getValueClass() {
            return UUID.class;
        }

        @Override
        public Model<UUID> createModel() {
            return new UuidModel();
        }
    };
}
