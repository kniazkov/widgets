/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence.json;

import com.kniazkov.json.Json;
import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonException;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.db.persistence.ChangeSet;
import com.kniazkov.widgets.db.persistence.DatabaseSnapshot;
import com.kniazkov.widgets.db.persistence.Persistence;
import com.kniazkov.widgets.db.persistence.PersistenceException;
import com.kniazkov.widgets.db.persistence.StoredRecord;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists the complete database snapshot in one atomically replaced JSON file.
 */
public final class JsonPersistence implements Persistence {
    /**
     * Target file.
     */
    private final Path file;

    /**
     * Current persisted records.
     */
    private Map<String, StoredRecord> records;

    /**
     * Creates a JSON backend.
     *
     * @param file target file
     */
    public JsonPersistence(final Path file) {
        this.file = file.toAbsolutePath();
        this.records = new LinkedHashMap<>();
    }

    @Override
    public synchronized DatabaseSnapshot load() {
        if (!Files.exists(this.file)) {
            this.records = new LinkedHashMap<>();
            return DatabaseSnapshot.empty();
        }
        try {
            final JsonElement root = Json.parse(this.file.toFile());
            final JsonArray array = root == null ? null : root.toJsonArray();
            if (array == null) {
                throw new PersistenceException("Database JSON root must be an array");
            }
            final Map<String, StoredRecord> loaded = new LinkedHashMap<>();
            for (final JsonElement element : array) {
                final StoredRecord record = parseRecord(element);
                if (loaded.put(key(record.getStore(), record.getId()), record) != null) {
                    throw new PersistenceException(
                        "Duplicate record " + record.getId()
                    );
                }
            }
            this.records = loaded;
            return new DatabaseSnapshot(new ArrayList<>(loaded.values()));
        } catch (final JsonException | IllegalArgumentException err) {
            throw new PersistenceException(
                "Cannot read database JSON from " + this.file,
                err
            );
        }
    }

    /**
     * Parses one stored record.
     *
     * @param element JSON element
     * @return stored record
     */
    private static StoredRecord parseRecord(final JsonElement element) {
        final JsonObject object = element.toJsonObject();
        if (object == null) {
            throw new PersistenceException("Database record must be an object");
        }
        final String store = requiredString(object, "store");
        final UUID id = UUID.fromString(requiredString(object, "id"));
        final Instant createdAt = Instant.parse(requiredString(object, "createdAt"));
        final long revision = Long.parseLong(requiredString(object, "revision"));
        final JsonElement fieldsElement = object.getElement("fields");
        final JsonArray fieldsArray =
            fieldsElement == null ? null : fieldsElement.toJsonArray();
        if (fieldsArray == null) {
            throw new PersistenceException("Record fields must be an array");
        }
        final Map<String, String> fields = new LinkedHashMap<>();
        for (final JsonElement fieldElement : fieldsArray) {
            final JsonObject field = fieldElement.toJsonObject();
            if (field == null) {
                throw new PersistenceException("Field value must be an object");
            }
            final String name = requiredString(field, "name");
            final String value = requiredString(field, "value");
            if (fields.putIfAbsent(name, value) != null) {
                throw new PersistenceException("Duplicate field '" + name + "'");
            }
        }
        return new StoredRecord(store, id, createdAt, revision, fields);
    }

    /**
     * Reads a required string property.
     *
     * @param object object
     * @param name property name
     * @return value
     */
    private static String requiredString(
        final JsonObject object,
        final String name
    ) {
        final JsonElement value = object.getElement(name);
        if (value == null || !value.isString()) {
            throw new PersistenceException(
                "Property '" + name + "' must be a string"
            );
        }
        return value.getStringValue();
    }

    @Override
    public synchronized void commit(final ChangeSet changes) {
        final Map<String, StoredRecord> updated =
            new LinkedHashMap<>(this.records);
        for (final ChangeSet.Mutation mutation : changes.getMutations()) {
            if (mutation instanceof ChangeSet.Upsert upsert) {
                final StoredRecord record = upsert.record();
                updated.put(key(record.getStore(), record.getId()), record);
            } else if (mutation instanceof ChangeSet.Delete delete) {
                updated.remove(key(delete.store(), delete.id()));
            }
        }
        this.write(updated.values());
        this.records = updated;
    }

    /**
     * Writes records to a temporary file and atomically replaces the target.
     *
     * @param source records
     */
    private void write(final java.util.Collection<StoredRecord> source) {
        final JsonArray array = new JsonArray();
        final List<StoredRecord> ordered = new ArrayList<>(source);
        ordered.sort(
            Comparator.comparing(StoredRecord::getStore)
                .thenComparing(StoredRecord::getId)
        );
        for (final StoredRecord record : ordered) {
            final JsonObject object = array.createObject();
            object.addString("store", record.getStore());
            object.addString("id", record.getId().toString());
            object.addString("createdAt", record.getCreatedAt().toString());
            object.addString("revision", Long.toString(record.getRevision()));
            final JsonArray fields = object.createArray("fields");
            for (final Map.Entry<String, String> entry
                : record.getFields().entrySet()) {
                final JsonObject field = fields.createObject();
                field.addString("name", entry.getKey());
                field.addString("value", entry.getValue());
            }
        }
        final Path parent = this.file.getParent();
        Path temporary = null;
        try {
            Files.createDirectories(parent);
            temporary = Files.createTempFile(
                parent,
                this.file.getFileName().toString(),
                ".tmp"
            );
            Files.writeString(temporary, array.toText("  "));
            move(temporary, this.file);
            temporary = null;
        } catch (final IOException err) {
            throw new PersistenceException(
                "Cannot write database JSON to " + this.file,
                err
            );
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (final IOException ignored) {
                    /*
                     * Best-effort cleanup after a failed commit.
                     */
                }
            }
        }
    }

    /**
     * Moves a completed temporary file into place.
     *
     * @param source source file
     * @param target target file
     * @throws IOException when replacement fails
     */
    private static void move(final Path source, final Path target)
        throws IOException {
        try {
            Files.move(
                source,
                target,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            );
        } catch (final AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Creates a stable compound key.
     *
     * @param store store name
     * @param id record identifier
     * @return key
     */
    private static String key(final String store, final UUID id) {
        return store + '\u0000' + id;
    }

    @Override
    public void close() {
        /*
         * No open resources.
         */
    }
}
