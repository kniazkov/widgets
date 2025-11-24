/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.model.IntegerModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.StringModel;

public abstract class Type<T> {
    abstract Class<T> getValueClass();
    abstract Model<T> createModel();

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
