/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.model.Model;

public interface Field<T> {
    String getName();
    Class<T> getValueClass();
    Model<T> createModel();
    Model<T> createModel(JsonElement element);
    JsonElement toJson(T data);

    default JsonElement toJson(final Record record) {
        final Model<T> model = record.getModel(this);
        return this.toJson(model.getData());
    }

    default Model<T> createModel(final JsonElement element, final Record record) {
        final Model<T> model;
        if (element != null) {
            model = this.createModel(element);
        } else {
            model = this.createModel();
        }
        model.addListener(data -> record.save());
        return model;
    }
}
