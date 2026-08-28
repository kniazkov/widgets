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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists each store in its own atomically replaced JSON file.
 */
public final class JsonPersistence implements Persistence {
    /**
     * Database directory.
     */
    private final Path directory;

    /**
     * Current records grouped by store.
     */
    private Map<String, Map<UUID, StoredRecord>> stores;

    /**
     * Creates a JSON backend.
     *
     * @param directory database directory
     */
    public JsonPersistence(final Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
        this.stores = new LinkedHashMap<>();
    }

    @Override
    public synchronized DatabaseSnapshot load() {
        if (!Files.exists(this.directory)) {
            this.stores = new LinkedHashMap<>();
            return DatabaseSnapshot.empty();
        }
        if (!Files.isDirectory(this.directory)) {
            throw new PersistenceException(
                "JSON database path is not a directory: " + this.directory
            );
        }
        final Map<String, Map<UUID, StoredRecord>> loaded =
            new LinkedHashMap<>();
        final List<Path> files = this.listStoreFiles();
        for (final Path file : files) {
            final String store = this.storeFromFile(file);
            for (final StoredRecord record : this.parseFile(file, store)) {
                final Map<UUID, StoredRecord> records =
                    loaded.computeIfAbsent(
                        store,
                        ignored -> new LinkedHashMap<>()
                    );
                if (records.putIfAbsent(record.getId(), record) != null) {
                    throw new PersistenceException(
                        "Duplicate record " + record.getId()
                    );
                }
            }
        }
        this.stores = loaded;
        return new DatabaseSnapshot(flatten(loaded));
    }

    /**
     * Lists store files in stable order.
     *
     * @return store files
     */
    private List<Path> listStoreFiles() {
        final List<Path> files = new ArrayList<>();
        try (
            DirectoryStream<Path> stream =
                Files.newDirectoryStream(this.directory, "*.json")
        ) {
            for (final Path file : stream) {
                if (Files.isRegularFile(file)) {
                    files.add(file.toAbsolutePath().normalize());
                }
            }
        } catch (final IOException err) {
            throw new PersistenceException(
                "Cannot list JSON database directory " + this.directory,
                err
            );
        }
        files.sort(Comparator.comparing(Path::toString));
        return files;
    }

    /**
     * Parses one store file.
     *
     * @param file store file
     * @param store store name
     * @return records
     */
    private List<StoredRecord> parseFile(
        final Path file,
        final String store
    ) {
        try {
            final JsonElement root = Json.parse(file.toFile());
            final JsonArray array = root == null ? null : root.toJsonArray();
            if (array == null) {
                throw new PersistenceException(
                    "Store JSON root must be an array: " + file
                );
            }
            final List<StoredRecord> records = new ArrayList<>();
            for (final JsonElement element : array) {
                records.add(parseRecord(element, store));
            }
            return records;
        } catch (final JsonException | IllegalArgumentException err) {
            throw new PersistenceException(
                "Cannot read JSON store from " + file,
                err
            );
        }
    }

    /**
     * Parses one stored record.
     *
     * @param element JSON element
     * @param store store name
     * @return stored record
     */
    private static StoredRecord parseRecord(
        final JsonElement element,
        final String store
    ) {
        final JsonObject object = element.toJsonObject();
        if (object == null) {
            throw new PersistenceException("Database record must be an object");
        }
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
        String store = null;
        for (final ChangeSet.Mutation mutation : changes.getMutations()) {
            final String mutationStore = storeOf(mutation);
            if (store == null) {
                store = mutationStore;
            } else if (!store.equals(mutationStore)) {
                throw new PersistenceException(
                    "JSON persistence cannot atomically change multiple stores"
                );
            }
        }
        if (store == null) {
            return;
        }
        final Map<UUID, StoredRecord> updated = new LinkedHashMap<>(
            this.stores.getOrDefault(store, Map.of())
        );
        for (final ChangeSet.Mutation mutation : changes.getMutations()) {
            if (mutation instanceof ChangeSet.Upsert upsert) {
                final StoredRecord record = upsert.record();
                updated.put(record.getId(), record);
            } else if (mutation instanceof ChangeSet.Delete delete) {
                updated.remove(delete.id());
            }
        }
        this.write(store, updated.values());
        final Map<String, Map<UUID, StoredRecord>> next =
            new LinkedHashMap<>(this.stores);
        next.put(store, updated);
        this.stores = next;
    }

    /**
     * Returns the store affected by a mutation.
     *
     * @param mutation mutation
     * @return store name
     */
    private static String storeOf(final ChangeSet.Mutation mutation) {
        if (mutation instanceof ChangeSet.Upsert upsert) {
            return upsert.record().getStore();
        }
        return ((ChangeSet.Delete) mutation).store();
    }

    /**
     * Flattens records grouped by store.
     *
     * @param source records by store
     * @return flat records
     */
    private static List<StoredRecord> flatten(
        final Map<String, Map<UUID, StoredRecord>> source
    ) {
        final List<StoredRecord> records = new ArrayList<>();
        for (final Map<UUID, StoredRecord> store : source.values()) {
            records.addAll(store.values());
        }
        return records;
    }

    /**
     * Writes one store to a temporary file and atomically replaces the target.
     *
     * @param store store name
     * @param source records
     */
    private void write(
        final String store,
        final Collection<StoredRecord> source
    ) {
        final JsonArray array = new JsonArray();
        final List<StoredRecord> ordered = new ArrayList<>(source);
        ordered.sort(Comparator.comparing(StoredRecord::getId));
        for (final StoredRecord record : ordered) {
            final JsonObject object = array.createObject();
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
        final Path target = this.fileForStore(store);
        Path temporary = null;
        try {
            Files.createDirectories(this.directory);
            temporary = Files.createTempFile(
                this.directory,
                target.getFileName().toString(),
                ".tmp"
            );
            Files.writeString(temporary, array.toText("  "));
            move(temporary, target);
            temporary = null;
        } catch (final IOException err) {
            throw new PersistenceException(
                "Cannot write JSON store to " + target,
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
     * Returns the file assigned to a store.
     *
     * @param store store name
     * @return store file
     */
    private Path fileForStore(final String store) {
        final String encoded = URLEncoder.encode(
            store,
            StandardCharsets.UTF_8
        ).replace("+", "%20");
        return this.directory.resolve(encoded + ".json").normalize();
    }

    /**
     * Decodes and validates the store name represented by a file.
     *
     * @param file store file
     * @return store name
     */
    private String storeFromFile(final Path file) {
        final String fileName = file.getFileName().toString();
        final String suffix = ".json";
        if (!fileName.endsWith(suffix)) {
            throw new PersistenceException("Not a JSON store file: " + file);
        }
        final String encoded = fileName.substring(
            0,
            fileName.length() - suffix.length()
        );
        final String store;
        try {
            store = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        } catch (final IllegalArgumentException err) {
            throw new PersistenceException(
                "Invalid encoded JSON store file name: " + file,
                err
            );
        }
        if (store.isBlank() || !this.fileForStore(store).equals(file)) {
            throw new PersistenceException(
                "Non-canonical JSON store file name: " + file
            );
        }
        return store;
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

    @Override
    public void close() {
        /*
         * No open resources.
         */
    }
}
