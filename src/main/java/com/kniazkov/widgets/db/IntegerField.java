/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonNumber;
import com.kniazkov.json.JsonString;
import com.kniazkov.widgets.model.IntegerModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.StringModel;

public class IntegerField implements Field<Integer> {
    final String name;

    public IntegerField(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Class<Integer> getValueClass() {
        return Integer.class;
    }

    @Override
    public Model<Integer> createModel() {
        return new IntegerModel();
    }

    @Override
    public Model<Integer> createModel(final JsonElement element) {
        return new IntegerModel(element.getIntValue());
    }

    @Override
    public JsonElement toJson(final Integer data) {
        return new JsonNumber(data);
    }
}
