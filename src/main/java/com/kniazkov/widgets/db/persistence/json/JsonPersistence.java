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
import com.kniazkov.widgets.db.persistence.DatabaseMetadata;
import com.kniazkov.widgets.db.persistence.DatabaseSnapshot;
import com.kniazkov.widgets.db.persistence.FieldMetadata;
import com.kniazkov.widgets.db.persistence.Persistence;
import com.kniazkov.widgets.db.persistence.PersistenceException;
import com.kniazkov.widgets.db.persistence.StoreMetadata;
import com.kniazkov.widgets.db.persistence.StoredRecord;
import com.kniazkov.widgets.db.persistence.StoredValue;
import com.kniazkov.widgets.db.persistence.StoredValue.BooleanValue;
import com.kniazkov.widgets.db.persistence.StoredValue.IntegerValue;
import com.kniazkov.widgets.db.persistence.StoredValue.RealValue;
import com.kniazkov.widgets.db.persistence.StoredValue.StringValue;
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
import java.util.Objects;
import java.util.UUID;

/**
 * Persists each store in its own atomically replaced JSON file.
 */
public final class JsonPersistence implements Persistence {
    /**
     * Schema catalog file name.
     */
    private static final String METADATA_FILE = "database.metadata";

    /**
     * Database directory.
     */
    private final Path directory;

    /**
     * Current records grouped by store.
     */
    private Map<String, Map<UUID, StoredRecord>> stores;

    /**
     * Validated database metadata.
     */
    private DatabaseMetadata metadata;

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
    public synchronized void initialize(final DatabaseMetadata value) {
        final DatabaseMetadata expected = Objects.requireNonNull(
            value,
            "metadata"
        );
        this.requireDirectory();
        final Path file = this.directory.resolve(METADATA_FILE);
        if (Files.exists(file)) {
            final DatabaseMetadata stored = this.readMetadata(file);
            if (!stored.equals(expected)) {
                throw new PersistenceException(
                    "JSON database metadata does not match configured schemas"
                );
            }
        } else {
            this.writeMetadata(file, expected);
        }
        this.metadata = expected;
    }

    @Override
    public synchronized DatabaseSnapshot load() {
        this.requireInitialized();
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
        final long revision = requiredLong(object, "revision");
        final JsonElement fieldsElement = object.getElement("fields");
        final JsonObject fieldsObject =
            fieldsElement == null ? null : fieldsElement.toJsonObject();
        if (fieldsObject == null) {
            throw new PersistenceException("Record fields must be an object");
        }
        final Map<String, StoredValue> fields = new LinkedHashMap<>();
        for (final Map.Entry<String, JsonElement> entry
            : fieldsObject.entrySet()) {
            fields.put(entry.getKey(), parseValue(entry.getKey(), entry.getValue()));
        }
        return new StoredRecord(store, id, createdAt, revision, fields);
    }

    /**
     * Parses one native JSON scalar.
     *
     * @param name field name
     * @param element JSON value
     * @return stored value
     */
    private static StoredValue parseValue(
        final String name,
        final JsonElement element
    ) {
        if (element.isString()) {
            return new StringValue(element.getStringValue());
        }
        if (element.isBoolean()) {
            return new BooleanValue(element.getBooleanValue());
        }
        if (element.isInteger()) {
            return new IntegerValue(element.getIntValue());
        }
        if (element.isNumber()) {
            return new RealValue(element.getDoubleValue());
        }
        throw new PersistenceException(
            "Field '" + name + "' must be a scalar value"
        );
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

    /**
     * Reads a required integer property.
     *
     * @param object object
     * @param name property name
     * @return value
     */
    private static long requiredLong(
        final JsonObject object,
        final String name
    ) {
        final JsonElement value = object.getElement(name);
        if (value == null || !value.isLongInteger()) {
            throw new PersistenceException(
                "Property '" + name + "' must be an integer"
            );
        }
        return value.getLongValue();
    }

    @Override
    public synchronized void commit(final ChangeSet changes) {
        this.requireInitialized();
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
     * Creates the database directory or validates an existing one.
     */
    private void requireDirectory() {
        try {
            if (Files.exists(this.directory)) {
                if (!Files.isDirectory(this.directory)) {
                    throw new PersistenceException(
                        "JSON database path is not a directory: "
                            + this.directory
                    );
                }
            } else {
                Files.createDirectories(this.directory);
            }
        } catch (final IOException err) {
            throw new PersistenceException(
                "Cannot create JSON database directory " + this.directory,
                err
            );
        }
    }

    /**
     * Verifies that schema metadata was initialized.
     */
    private void requireInitialized() {
        if (this.metadata == null) {
            throw new IllegalStateException(
                "JSON persistence metadata is not initialized"
            );
        }
    }

    /**
     * Reads the schema catalog.
     *
     * @param file metadata file
     * @return metadata
     */
    private DatabaseMetadata readMetadata(final Path file) {
        try {
            final JsonElement rootElement = Json.parse(file.toFile());
            final JsonObject root = rootElement == null
                ? null : rootElement.toJsonObject();
            if (root == null) {
                throw new PersistenceException(
                    "JSON database metadata root must be an object"
                );
            }
            final long version = requiredLong(root, "formatVersion");
            if (version > Integer.MAX_VALUE) {
                throw new PersistenceException(
                    "JSON database format version is too large"
                );
            }
            final JsonElement storesElement = root.getElement("stores");
            final JsonArray storeArray = storesElement == null
                ? null : storesElement.toJsonArray();
            if (storeArray == null) {
                throw new PersistenceException(
                    "Metadata property 'stores' must be an array"
                );
            }
            final List<StoreMetadata> result = new ArrayList<>();
            for (final JsonElement storeElement : storeArray) {
                result.add(parseStoreMetadata(storeElement));
            }
            return new DatabaseMetadata((int) version, result);
        } catch (final JsonException | IllegalArgumentException err) {
            throw new PersistenceException(
                "Cannot read JSON database metadata from " + file,
                err
            );
        }
    }

    /**
     * Parses one store metadata object.
     *
     * @param element JSON element
     * @return store metadata
     */
    private static StoreMetadata parseStoreMetadata(
        final JsonElement element
    ) {
        final JsonObject object = element.toJsonObject();
        if (object == null) {
            throw new PersistenceException("Store metadata must be an object");
        }
        final String name = requiredString(object, "name");
        final int position = requiredPosition(object, "position");
        final JsonElement fieldsElement = object.getElement("fields");
        final JsonArray fieldArray = fieldsElement == null
            ? null : fieldsElement.toJsonArray();
        if (fieldArray == null) {
            throw new PersistenceException(
                "Store metadata property 'fields' must be an array"
            );
        }
        final List<FieldMetadata> fields = new ArrayList<>();
        for (final JsonElement fieldElement : fieldArray) {
            fields.add(parseFieldMetadata(fieldElement));
        }
        return new StoreMetadata(name, position, fields);
    }

    /**
     * Parses one field metadata object.
     *
     * @param element JSON element
     * @return field metadata
     */
    private static FieldMetadata parseFieldMetadata(
        final JsonElement element
    ) {
        final JsonObject object = element.toJsonObject();
        if (object == null) {
            throw new PersistenceException("Field metadata must be an object");
        }
        final JsonElement reference = object.getElement("referencedStore");
        if (reference != null && !reference.isString()) {
            throw new PersistenceException(
                "Field metadata property 'referencedStore' must be a string"
            );
        }
        final StoredValue.Kind kind = StoredValue.Kind.valueOf(
            requiredString(object, "valueKind")
        );
        return new FieldMetadata(
            requiredString(object, "name"),
            requiredString(object, "type"),
            kind,
            parseDefault(kind, requiredElement(object, "defaultValue")),
            requiredPosition(object, "position"),
            reference == null ? null : reference.getStringValue()
        );
    }

    /**
     * Parses a metadata default according to its declared scalar kind.
     *
     * @param kind declared scalar kind
     * @param element JSON value
     * @return stored default
     */
    private static StoredValue parseDefault(
        final StoredValue.Kind kind,
        final JsonElement element
    ) {
        return switch (kind) {
            case STRING -> {
                if (!element.isString()) {
                    throw invalidDefault(kind);
                }
                yield new StringValue(element.getStringValue());
            }
            case INTEGER -> {
                if (!element.isInteger()) {
                    throw invalidDefault(kind);
                }
                yield new IntegerValue(element.getIntValue());
            }
            case REAL -> {
                if (!element.isNumber()) {
                    throw invalidDefault(kind);
                }
                yield new RealValue(element.getDoubleValue());
            }
            case BOOLEAN -> {
                if (!element.isBoolean()) {
                    throw invalidDefault(kind);
                }
                yield new BooleanValue(element.getBooleanValue());
            }
        };
    }

    /**
     * Creates a malformed metadata default exception.
     *
     * @param kind expected kind
     * @return exception
     */
    private static PersistenceException invalidDefault(
        final StoredValue.Kind kind
    ) {
        return new PersistenceException(
            "Metadata default must be a " + kind + " value"
        );
    }

    /**
     * Reads a required property.
     *
     * @param object object
     * @param name property name
     * @return JSON element
     */
    private static JsonElement requiredElement(
        final JsonObject object,
        final String name
    ) {
        final JsonElement value = object.getElement(name);
        if (value == null) {
            throw new PersistenceException(
                "Property '" + name + "' is required"
            );
        }
        return value;
    }

    /**
     * Reads a required non-negative integer property.
     *
     * @param object object
     * @param name property name
     * @return position
     */
    private static int requiredPosition(
        final JsonObject object,
        final String name
    ) {
        final long value = requiredLong(object, name);
        if (value < 0 || value > Integer.MAX_VALUE) {
            throw new PersistenceException(
                "Property '" + name + "' must be a non-negative integer"
            );
        }
        return (int) value;
    }

    /**
     * Writes the schema catalog atomically.
     *
     * @param file metadata file
     * @param value metadata
     */
    private void writeMetadata(
        final Path file,
        final DatabaseMetadata value
    ) {
        final JsonObject root = new JsonObject();
        root.addNumber("formatVersion", value.formatVersion());
        final JsonArray storesArray = root.createArray("stores");
        for (final StoreMetadata store : value.stores()) {
            final JsonObject storeObject = storesArray.createObject();
            storeObject.addString("name", store.name());
            storeObject.addNumber("position", store.position());
            final JsonArray fieldsArray = storeObject.createArray("fields");
            for (final FieldMetadata field : store.fields()) {
                final JsonObject fieldObject = fieldsArray.createObject();
                fieldObject.addString("name", field.name());
                fieldObject.addString("type", field.type());
                fieldObject.addString("valueKind", field.valueKind().name());
                writeValue(
                    fieldObject,
                    "defaultValue",
                    field.defaultValue()
                );
                fieldObject.addNumber("position", field.position());
                if (field.referencedStore() != null) {
                    fieldObject.addString(
                        "referencedStore",
                        field.referencedStore()
                    );
                }
            }
        }
        this.writeAtomically(file, root.toText("  "));
    }

    /**
     * Writes text through a temporary file and atomically replaces the target.
     *
     * @param target target file
     * @param content file content
     */
    private void writeAtomically(final Path target, final String content) {
        Path temporary = null;
        try {
            Files.createDirectories(this.directory);
            temporary = Files.createTempFile(
                this.directory,
                target.getFileName().toString(),
                ".tmp"
            );
            Files.writeString(temporary, content);
            move(temporary, target);
            temporary = null;
        } catch (final IOException err) {
            throw new PersistenceException(
                "Cannot write JSON database file " + target,
                err
            );
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (final IOException ignored) {
                    /*
                     * Best-effort cleanup after a failed write.
                     */
                }
            }
        }
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
            object.addNumber("revision", record.getRevision());
            final JsonObject fields = object.createObject("fields");
            for (final Map.Entry<String, StoredValue> entry
                : record.getFields().entrySet()) {
                writeValue(fields, entry.getKey(), entry.getValue());
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
     * Writes one native JSON scalar.
     *
     * @param object target object
     * @param name field name
     * @param value stored value
     */
    private static void writeValue(
        final JsonObject object,
        final String name,
        final StoredValue value
    ) {
        switch (value.getKind()) {
            case STRING -> object.addString(name, value.getString());
            case INTEGER -> object.addNumber(name, value.getInteger());
            case REAL -> object.addNumber(name, value.getReal());
            case BOOLEAN -> object.addBoolean(name, value.getBoolean());
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
