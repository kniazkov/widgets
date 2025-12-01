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
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A {@link Store} implementation that persists records in a JSON file.
 * <p>
 * Each record is stored as a JSON object with:
 * <ul>
 *     <li>{@code id} – the UUID of the record;</li>
 *     <li>one entry per field that has an associated model.</li>
 * </ul>
 *
 * <p>
 * The JSON file contains a single root {@link JsonArray} where each element represents
 * one {@link Record}.
 * <br>
 * Saving always rewrites the entire file.
 */
public class JsonStore extends Store {
    /**
     * A type-specific serializer/deserializer for converting values between JSON representation
     * and model data. Each handler corresponds to exactly one {@link Type}, and is responsible for
     * both parsing JSON and serializing values back into JSON.
     *
     * @param <T> the Java type handled by this serializer
     */
    private static abstract class Handler<T> {
        /**
         * Parses a JSON element into a Java value of type {@code T}.
         *
         * @param element the JSON element to parse
         * @return the parsed value
         */
        protected abstract T parse(JsonElement element);

        /**
         * Converts a Java value into its JSON representation.
         *
         * @param data the value to serialize
         * @return a JSON element representing the value
         */
        protected abstract JsonElement serialize(T data);

        /**
         * Reads the value for the given field from JSON and writes it into the appropriate
         * {@link Model} of the target record.
         */
        @SuppressWarnings("unchecked")
        private void createFieldFromJsonElement(
            final Record record,
            final Field<?> field,
            final JsonElement element) {

            Model<T> model = record.getModel((Field<T>) field);
            model.setData(this.parse(element));
        }

        /**
         * Reads the value of a field from the record's model and returns its JSON representation.
         */
        @SuppressWarnings("unchecked")
        private JsonElement createJsonElementFromField(
            final Record record,
            final Field<?> field) {

            Model<T> model = record.getModel((Field<T>) field);
            return this.serialize(model.getData());
        }
    }

    /**
     * The global mapping of {@link Type} to {@link Handler} instances.
     */
    private static final Map<Type<?>, Handler<?>> HANDLERS;

    static {
        Map<Type<?>, Handler<?>> m = new HashMap<>();

        final Handler<String> stringHandler = new Handler<String>() {
            @Override
            protected String parse(final JsonElement element) {
                return element.getStringValue();
            }

            @Override
            protected JsonElement serialize(final String data) {
                return new JsonString(data.trim());
            }
        };
        m.put(Type.STRING, stringHandler);
        m.put(Type.NOT_EMPTY_STRING, stringHandler);

        final Handler<Integer> integerHandler = new Handler<Integer>() {
            @Override
            protected Integer parse(final JsonElement element) {
                return element.getIntValue();
            }

            @Override
            protected JsonElement serialize(final Integer data) {
                return new JsonNumber(data);
            }
        };
        m.put(Type.INTEGER, integerHandler);
        m.put(Type.POSITIVE_INTEGER, integerHandler);

        final Handler<Double> realNumberHandler = new Handler<Double>() {
            @Override
            protected Double parse(final JsonElement element) {
                return element.getDoubleValue();
            }

            @Override
            protected JsonElement serialize(final Double data) {
                return new JsonNumber(data);
            }
        };
        m.put(Type.REAL, realNumberHandler);
        m.put(Type.POSITIVE_REAL, realNumberHandler);

        HANDLERS = Collections.unmodifiableMap(m);
    }

    /**
     * Returns the handler associated with the given {@link Type}.
     *
     * @param type the value type
     * @return the corresponding handler
     * @throws IllegalStateException if the type is not supported
     */
    private static Handler<?> getHandler(final Type<?> type) {
        Handler<?> handler = HANDLERS.get(type);
        if (handler == null) {
            throw new IllegalStateException(
                "Unsupported type: " + type.getValueClass().getName()
            );
        }
        return handler;
    }

    /**
     * The file where all records are serialized.
     */
    private final File file;

    /**
     * Creates a JSON-backed store with the specified output file and schema.
     *
     * @param file   the JSON file used for persistence
     * @param fields the list of fields describing the schema
     */
    public JsonStore(final File file, final List<Field<?>> fields) {
        super(fields);
        this.file = file;
    }

    /**
     * Saves all records to the backing JSON file.
     * <p>
     * The entire file is rewritten. Each record becomes one object in the resulting JSON array.
     *
     * @throws IllegalStateException if writing fails
     */
    @Override
    public void save() {
        final JsonArray array = new JsonArray();

        for (final Record record : this.getAllRecords()) {
            final JsonObject object = array.createObject();
            object.addString("id", record.getId().toString());
            object.addString("timestamp", record.getTimestamp().toString());

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

        final File parentDir = this.file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        try (FileWriter writer = new FileWriter(this.file)) {
            writer.write(json);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Can't write '" + this.file.getAbsolutePath() + '\''
            );
        }
    }

    /**
     * Loads a JSON file and constructs a {@link JsonStore} instance populated
     * with the stored records.
     * <p>
     * Unsupported fields or malformed entries are silently skipped.
     *
     * @param file   the JSON file to read
     * @param fields the schema to use when interpreting records
     * @return a fully populated store instance
     */
    public static Store load(final File file, final List<Field<?>> fields) {
        final Store store = new JsonStore(file, fields);

        try {
            final JsonElement root = Json.parse(file);
            if (root == null) {
                return store;
            }

            final JsonArray array = root.toJsonArray();
            if (array == null) {
                return store;
            }

            for (final JsonElement entry : array) {
                final JsonObject object = entry.toJsonObject();
                if (object == null) {
                    continue;
                }

                final JsonElement idStr = object.get("id");
                final JsonElement tsStr = object.get("timestamp");
                if (idStr == null || !idStr.isString() || tsStr == null || !tsStr.isString()) {
                    continue;
                }

                final UUID id = UUID.fromString(idStr.getStringValue());
                final Instant timestamp = Instant.parse(tsStr.getStringValue());
                final Record record = store.createRecord(id, timestamp);

                for (final Field<?> field : fields) {
                    final JsonElement element = object.getElement(field.getName());
                    if (element == null) {
                        continue;
                    }

                    Handler<?> handler = getHandler(field.getType());
                    handler.createFieldFromJsonElement(record, field, element);
                }
            }
        } catch (JsonException ignored) {
        }

        return store;
    }
}
