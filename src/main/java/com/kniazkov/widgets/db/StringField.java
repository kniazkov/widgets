/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonString;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.StringModel;

public class StringField implements Field<String> {
    final String name;

    public StringField(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Class<String> getValueClass() {
        return String.class;
    }

    @Override
    public Model<String> createModel() {
        return new StringModel();
    }

    @Override
    public Model<String> createModel(final JsonElement element) {
        return new StringModel(element.getStringValue());
    }

    @Override
    public JsonElement toJson(final String data) {
        return new JsonString(data);
    }
}
