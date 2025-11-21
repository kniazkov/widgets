package com.kniazkov.widgets.db;

import com.kniazkov.json.Json;
import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonException;
import com.kniazkov.json.JsonObject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class Store {
    private final File file;
    private final List<Field<?>> fields;
    private final Map<UUID, Record> records;

    private Store(final File file, final List<Field<?>> fields, Map<UUID, Record> records) {
        this.file = file;
        this.fields = fields;
        this.records = records;
    }

    public static Store load(final File file, List<Field<?>> fields) {
        final Map<UUID, Record> records = new TreeMap<>();
        final Store store = new Store(file, fields, records);

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
                    final JsonElement id = object.get("id");
                    if (id == null || !id.isString()) {
                        continue;
                    }
                    final UUID uuid = UUID.fromString(id.getStringValue());
                    final Record record = new Record(uuid, store);
                    for (final Field<?> field : fields) {
                        final JsonElement element = object.getElement(field.getName());
                        if (element == null) {
                            continue;
                        }
                        record.createModel(field, element);
                    }
                    records.put(uuid, record);
                }
            } while (false);
        } catch (final JsonException ignored) {
        }

        return store;
    }

    public void save() {
        final JsonArray array = new JsonArray();
        for (final Record record : this.records.values()) {
            final JsonObject object = array.createObject();
            object.addString("id", record.getId().toString());
            for (final Field<?> field : this.fields) {
                if (record.hasModel(field)) {
                    object.addElement(field.getName(), field.toJson(record));
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

    public Record createRecord() {
        final UUID id = UUID.randomUUID();
        final Record record = new Record(id, this);
        this.records.put(id, record);
        return record;
    }

    public List<Record> getAllRecords() {
        return new ArrayList<>(this.records.values());
    }
}
