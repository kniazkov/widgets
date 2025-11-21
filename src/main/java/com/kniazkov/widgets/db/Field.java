/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.json.JsonElement;
import com.kniazkov.widgets.model.Model;

public interface Field<T> {
    String getName();
    Class<T> getValueClass();
    Model<T> createModel();
    JsonElement toJson(T data);
    T fromJson(JsonElement element);
}
