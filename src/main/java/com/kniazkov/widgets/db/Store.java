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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public abstract class Store {
    private final List<Field<?>> fields;
    private final Map<UUID, Record> records;

    public Store(final List<Field<?>> fields) {
        this.fields = Collections.unmodifiableList(fields);
        this.records = new TreeMap<>();
    }

    public List<Field<?>> getFields() {
        return this.fields;
    }

    public Record createRecord() {
        final UUID id = UUID.randomUUID();
        return this.createRecord(id);
    }

    public Record createRecord(final UUID id) {
        final Record record = new PermanentRecord(id, this);
        this.records.put(id, record);
        return record;
    }

    public List<Record> getAllRecords() {
        return new ArrayList<>(this.records.values());
    }

    public Record getRecordById(final UUID id) {
        return this.records.get(id);
    }

    public abstract void save();
}
