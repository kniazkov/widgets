/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

public class Field<T> {
    private final Type<T> type;
    private final String name;

    public Field(final Type<T> type, final String name) {
        this.type = type;
        this.name = name;
    }

    public Type<T> getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }
}
