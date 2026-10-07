/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.json.JsonObject;
import java.util.Objects;

/**
 * A JSON object model with detached snapshots on both reads and writes.
 * Modify the object returned by {@link #getData()}, then call {@link #setData(JsonObject)}
 * to publish the change. Nested objects and arrays are copied too.
 * Equality compares serialized JSON, including member order.
 */
public final class JsonObjectModel extends SingleThreadModel<JsonObject> {
    /**
     * Private snapshot, never exposed to callers or listeners.
     */
    private JsonObject data;

    /**
     * Creates a model containing an empty object.
     */
    public JsonObjectModel() {
        this.data = new JsonObject();
    }

    /**
     * Creates a model with a deep copy of the supplied object.
     * @param data initial object
     */
    public JsonObjectModel(final JsonObject data) {
        this.data = Objects.requireNonNull(data, "data").clone().toJsonObject();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public JsonObject getData() {
        return this.data.clone().toJsonObject();
    }

    @Override
    public boolean setData(final JsonObject data) {
        if (data == null) {
            return false;
        }
        final JsonObject snapshot = data.clone().toJsonObject();
        if (this.data.toString().equals(snapshot.toString())) {
            return false;
        }
        this.data = snapshot;
        this.notifyListeners();
        return true;
    }

    @Override
    public Model<JsonObject> deriveWithData(final JsonObject data) {
        return new JsonObjectModel(data);
    }
}
