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
    Model<T> createModel(JsonElement element);
    JsonElement toJson(T data);
}
