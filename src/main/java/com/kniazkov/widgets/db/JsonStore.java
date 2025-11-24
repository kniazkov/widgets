/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.json.Json;
import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonException;
import com.kniazkov.json.JsonNumber;
import com.kniazkov.json.JsonObject;
import com.kniazkov.json.JsonString;
import com.kniazkov.widgets.model.Model;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JsonStore extends Store {
    private static abstract class Handler<T> {
        protected abstract T parse(JsonElement element);

        protected abstract JsonElement serialize(T data);

        @SuppressWarnings("unchecked")
        private void createFieldFromJsonElement(final Record record, final Field<?> field,
                final JsonElement element) {
            Model<T> model = record.getModel((Field<T>)field);
            model.setData(this.parse(element));
        }

        @SuppressWarnings("unchecked")
        private JsonElement createJsonElementFromField(final Record record, final Field<?> field) {
            Model<T> model = record.getModel((Field<T>)field);
            return this.serialize(model.getData());
        }
    }

    private static Map<Type<?>, Handler<?>> initHandlers() {
        Map<Type<?>, Handler<?>> map = new HashMap<>();
        map.put(Type.STRING, new Handler<String>(){
            @Override
            protected String parse(final JsonElement element) {
                return element.getStringValue();
            }

            @Override
            protected JsonElement serialize(final String data) {
                return new JsonString(data);
            }
        });
        map.put(Type.INTEGER, new Handler<Integer>(){
            @Override
            protected Integer parse(final JsonElement element) {
                return element.getIntValue();
            }

            @Override
            protected JsonElement serialize(final Integer data) {
                return new JsonNumber(data);
            }
        });
        return map;
    }

    private static final Map<Type<?>, Handler<?>> HANDLERS = initHandlers();

    private static Handler<?> getHandler(final Type<?> type) {
        Handler<?> handler = HANDLERS.get(type);
        if (handler == null) {
            throw new IllegalStateException("Unsupported type: " + type.getValueClass().getName());
        }
        return handler;
    }

    private final File file;

    public JsonStore(final File file, final List<Field<?>> fields) {
        super(fields);
        this.file = file;
    }

    @Override
    public void save() {
        final JsonArray array = new JsonArray();
        for (final Record record : this.getAllRecords()) {
            final JsonObject object = array.createObject();
            object.addString("id", record.getId().toString());
            for (final Field<?> field : this.getFields()) {
                if (record.hasModel(field)) {
                    final Handler<?> handler = getHandler(field.getType());
                    object.addElement(
                        field.getName(),
                        handler.createJsonElementFromField(record, field)
                    );
                }
            }
        }
        final String json = array.toText("  ");
        try {
            final FileWriter writer = new FileWriter(this.file);
            writer.write(json);
            writer.close();
        } catch (final IOException ignored) {
            throw new IllegalStateException("Can't write '" + this.file.getAbsolutePath() + '\'');
        }
    }

    public static Store load(final File file, final List<Field<?>> fields) {
        final Store store = new JsonStore(file, fields);

        try {
            do {
                final JsonElement root = Json.parse(file);
                if (root == null) {
                    break;
                }

                final JsonArray array = root.toJsonArray();
                if (array == null) {
                    break;
                }

                for (final JsonElement entry : array) {
                    final JsonObject object = entry.toJsonObject();
                    if (object == null) {
                        continue;
                    }
                    final JsonElement idStr = object.get("id");
                    if (idStr == null || !idStr.isString()) {
                        continue;
                    }
                    final UUID id = UUID.fromString(idStr.getStringValue());
                    final Record record = store.createRecord(id);
                    for (final Field<?> field : fields) {
                        final JsonElement element = object.getElement(field.getName());
                        if (element == null) {
                            continue;
                        }
                        Handler<?> handler = getHandler(field.getType());
                        handler.createFieldFromJsonElement(record, field, element);
                    }
                }
            } while (false);
        } catch (final JsonException ignored) {
        }

        return store;
    }
}
