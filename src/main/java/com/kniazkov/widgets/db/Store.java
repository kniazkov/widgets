package com.kniazkov.widgets.db;

import com.kniazkov.json.Json;
import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonException;
import com.kniazkov.json.JsonObject;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class Store {
    private final File file;
    private final Map<String, Field<?>> fields;
    private final Map<UUID, Record> records;

    private Store(final File file, final Map<String, Field<?>> fields, Map<UUID, Record> records) {
        this.file = file;
        this.fields = fields;
        this.records = records;
    }

    public static Store load(final File file, List<Field<?>> fields) {
        final Map<UUID, Record> records = new TreeMap<>();

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
                    final Record record = new Record(uuid);
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
        return new Store(file, createFieldsMap(fields), records);
    }

    private static Map<String, Field<?>> createFieldsMap(final List<Field<?>> fields) {
        final Map<String, Field<?>> map = new TreeMap<>();
        for (final Field<?> field : fields) {
            map.put(field.getName(), field);
        }
        return map;
    }
}
