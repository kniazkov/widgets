/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.model.IntegerModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.StringModel;

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
    abstract Class<T> getValueClass();

    /**
     * Creates a new {@link Model} instance capable of storing values of this type.
     *
     * @return a new model associated with this type
     */
    abstract Model<T> createModel();

    /**
     * A built-in {@code Type} representing textual values.
     */
    public static final Type<String> STRING = new Type<String>() {
        @Override
        Class<String> getValueClass() {
            return String.class;
        }

        @Override
        Model<String> createModel() {
            return new StringModel();
        }
    };

    /**
     * A built-in {@code Type} representing integer values.
     */
    public static final Type<Integer> INTEGER = new Type<Integer>() {
        @Override
        Class<Integer> getValueClass() {
            return Integer.class;
        }

        @Override
        Model<Integer> createModel() {
            return new IntegerModel();
        }
    };
}
