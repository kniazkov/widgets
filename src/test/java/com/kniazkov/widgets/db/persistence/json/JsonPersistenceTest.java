/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence.json;

import com.kniazkov.json.Json;
import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.db.persistence.ChangeSet;
import com.kniazkov.widgets.db.persistence.DatabaseMetadata;
import com.kniazkov.widgets.db.persistence.FieldMetadata;
import com.kniazkov.widgets.db.persistence.PersistenceException;
import com.kniazkov.widgets.db.persistence.StoreMetadata;
import com.kniazkov.widgets.db.persistence.StoredRecord;
import com.kniazkov.widgets.db.persistence.StoredValue;
import com.kniazkov.widgets.db.persistence.StoredValue.BooleanValue;
import com.kniazkov.widgets.db.persistence.StoredValue.IntegerValue;
import com.kniazkov.widgets.db.persistence.StoredValue.Kind;
import com.kniazkov.widgets.db.persistence.StoredValue.RealValue;
import com.kniazkov.widgets.db.persistence.StoredValue.StringValue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for native JSON persistence and its on-disk format.
 */
public final class JsonPersistenceTest {
    /**
     * Temporary filesystem root.
     */
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    /**
     * Verifies the schema catalog is written in a language-neutral JSON form.
     *
     * @throws Exception when temporary file access fails
     */
    @Test
    public void writesDatabaseMetadata() throws Exception {
        final Path directory = this.directory("metadata");
        open(directory).close();

        final JsonObject root = Json.parse(
            directory.resolve("database.metadata").toFile()
        ).toJsonObject();
        assertEquals(1, root.get("formatVersion").getIntValue());
        final JsonArray stores = root.get("stores").toJsonArray();
        assertEquals(4, stores.size());
        final JsonObject employees = stores.getElement(0).toJsonObject();
        assertEquals("employees", employees.get("name").getStringValue());
        assertEquals(0, employees.get("position").getIntValue());
        final JsonArray fields = employees.get("fields").toJsonArray();
        assertEquals(5, fields.size());
        final JsonObject reference = fields.getElement(4).toJsonObject();
        assertEquals("identifier", reference.get("type").getStringValue());
        assertEquals("STRING", reference.get("valueKind").getStringValue());
        assertEquals(
            "a843176c-36df-44bd-b8a2-b1d8c956c431",
            reference.get("defaultValue").getStringValue()
        );
        assertEquals("departments",
            reference.get("referencedStore").getStringValue());
    }

    /**
     * Verifies persisted metadata must match the configured Java schemas.
     *
     * @throws Exception when temporary file access fails
     */
    @Test
    public void rejectsMismatchedDatabaseMetadata() throws Exception {
        final Path directory = this.directory("metadata-mismatch");
        open(directory).close();
        final JsonPersistence persistence = new JsonPersistence(directory);
        final DatabaseMetadata different = new DatabaseMetadata(
            DatabaseMetadata.CURRENT_FORMAT_VERSION,
            List.of()
        );

        assertThrows(PersistenceException.class,
            () -> persistence.initialize(different));
    }

    /**
     * Verifies every supported value is written as a native JSON scalar.
     *
     * @throws Exception when temporary file access fails
     */
    @Test
    public void writesNativeJsonScalars() throws Exception {
        final Path directory = this.directory("native");
        final StoredRecord record = record("employees", UUID.randomUUID(), 3L);
        final JsonPersistence persistence = open(directory);
        persistence.commit(ChangeSet.upsert(record));

        final JsonObject object = firstObject(directory.resolve("employees.json"));
        assertTrue(object.get("revision").isNumber());
        assertFalse(object.get("revision").isString());
        final JsonObject fields = object.get("fields").toJsonObject();
        assertTrue(fields.get("name").isString());
        assertTrue(fields.get("age").isInteger());
        assertTrue(fields.get("score").isNumber());
        assertFalse(fields.get("score").isInteger());
        assertTrue(fields.get("active").isBoolean());
        assertTrue(fields.get("departmentId").isString());
    }

    /**
     * Verifies all native value kinds survive a close and reload.
     *
     * @throws Exception when temporary file access fails
     */
    @Test
    public void roundTripsAllNativeValues() throws Exception {
        final Path directory = this.directory("round-trip");
        final StoredRecord source = record("employees", UUID.randomUUID(), 7L);
        final JsonPersistence writer = open(directory);
        writer.commit(ChangeSet.upsert(source));
        writer.close();

        final JsonPersistence reader = open(directory);
        final StoredRecord restored = reader.load().getRecords().get(0);

        assertEquals(source.getStore(), restored.getStore());
        assertEquals(source.getId(), restored.getId());
        assertEquals(source.getCreatedAt(), restored.getCreatedAt());
        assertEquals(source.getRevision(), restored.getRevision());
        assertEquals(source.getFields(), restored.getFields());
        reader.close();
    }

    /**
     * Verifies each store is isolated in its own file.
     *
     * @throws Exception when temporary file access fails
     */
    @Test
    public void writesOneFilePerStore() throws Exception {
        final Path directory = this.directory("stores");
        final JsonPersistence persistence = open(directory);
        persistence.commit(ChangeSet.upsert(
            record("employees", UUID.randomUUID(), 1L)
        ));
        persistence.commit(ChangeSet.upsert(
            record("settings", UUID.randomUUID(), 1L)
        ));

        assertTrue(Files.isRegularFile(directory.resolve("employees.json")));
        assertTrue(Files.isRegularFile(directory.resolve("settings.json")));
        assertEquals(2, persistence.load().getRecords().size());
    }

    /**
     * Verifies arbitrary store names are encoded into safe canonical file names.
     *
     * @throws Exception when temporary file access fails
     */
    @Test
    public void encodesStoreNamesInFileNames() throws Exception {
        final Path directory = this.directory("encoded-name");
        final String store = "sales west/2026";
        final JsonPersistence persistence = open(directory);
        persistence.commit(ChangeSet.upsert(
            record(store, UUID.randomUUID(), 1L)
        ));

        assertTrue(Files.isRegularFile(
            directory.resolve("sales%20west%2F2026.json")
        ));
        assertEquals(store, persistence.load().getRecords().get(0).getStore());
    }

    /**
     * Verifies replacement updates one record and removes obsolete fields.
     *
     * @throws Exception when temporary file access fails
     */
    @Test
    public void replacesCompleteRecords() throws Exception {
        final Path directory = this.directory("replace");
        final UUID id = UUID.randomUUID();
        final JsonPersistence persistence = open(directory);
        persistence.commit(ChangeSet.upsert(record("employees", id, 1L)));
        final StoredRecord replacement = new StoredRecord(
            "employees",
            id,
            CREATED_AT,
            2L,
            Map.of("name", new StringValue("Bob"))
        );

        persistence.commit(ChangeSet.upsert(replacement));
        final StoredRecord restored = persistence.load().getRecords().get(0);

        assertEquals(2L, restored.getRevision());
        assertEquals(Map.of("name", new StringValue("Bob")),
            restored.getFields());
    }

    /**
     * Verifies deletions update memory and the store file.
     *
     * @throws Exception when temporary file access fails
     */
    @Test
    public void persistsDeletions() throws Exception {
        final Path directory = this.directory("delete");
        final UUID id = UUID.randomUUID();
        final JsonPersistence persistence = open(directory);
        persistence.commit(ChangeSet.upsert(record("employees", id, 1L)));

        persistence.commit(ChangeSet.delete("employees", id));

        assertTrue(persistence.load().getRecords().isEmpty());
        assertTrue(Json.parse(directory.resolve("employees.json").toFile())
            .toJsonArray().isEmpty());
    }

    /**
     * Verifies a JSON change set cannot span several store files.
     *
     * @throws Exception when temporary file access fails
     */
    @Test
    public void rejectsCrossStoreTransactions() throws Exception {
        final Path directory = this.directory("cross-store");
        final JsonPersistence persistence = open(directory);
        final ChangeSet changes = new ChangeSet(Arrays.asList(
            new ChangeSet.Upsert(record("employees", UUID.randomUUID(), 1L)),
            new ChangeSet.Upsert(record("settings", UUID.randomUUID(), 1L))
        ));

        assertThrows(PersistenceException.class,
            () -> persistence.commit(changes));
        assertTrue(Files.isRegularFile(directory.resolve("database.metadata")));
        assertFalse(Files.exists(directory.resolve("employees.json")));
        assertFalse(Files.exists(directory.resolve("settings.json")));
    }

    /**
     * Verifies empty change sets create no store files.
     *
     * @throws Exception when temporary file access fails
     */
    @Test
    public void ignoresEmptyChangeSets() throws Exception {
        final Path directory = this.directory("empty");
        final JsonPersistence persistence = open(directory);

        persistence.commit(new ChangeSet(List.of()));

        assertTrue(Files.isRegularFile(directory.resolve("database.metadata")));
    }

    /**
     * Verifies the obsolete string-only format is intentionally rejected.
     *
     * @throws Exception when temporary file access fails
     */
    @Test
    public void rejectsLegacyStringFormat() throws Exception {
        final Path directory = this.createdDirectory("legacy");
        Files.writeString(
            directory.resolve("employees.json"),
            "[{\"id\":\"" + UUID.randomUUID() + "\","
                + "\"createdAt\":\"" + CREATED_AT + "\","
                + "\"revision\":\"1\","
                + "\"fields\":[{\"name\":\"age\",\"value\":\"34\"}]}]"
        );

        assertThrows(PersistenceException.class,
            () -> open(directory));
    }

    /**
     * Verifies malformed roots, records, metadata, and field values are rejected.
     *
     * @throws Exception when temporary file access fails
     */
    @Test
    public void rejectsMalformedJsonStructures() throws Exception {
        final String id = UUID.randomUUID().toString();
        final List<String> invalid = Arrays.asList(
            "{}",
            "[null]",
            "[{\"id\":\"" + id + "\",\"createdAt\":\"" + CREATED_AT
                + "\",\"revision\":1,\"fields\":[]}]",
            "[{\"id\":\"" + id + "\",\"createdAt\":\"" + CREATED_AT
                + "\",\"revision\":1,\"fields\":{\"age\":null}}]",
            "[{\"id\":\"" + id + "\",\"createdAt\":\"" + CREATED_AT
                + "\",\"revision\":1,\"fields\":{\"age\":{}}}]"
        );

        for (int index = 0; index < invalid.size(); index++) {
            final Path directory = this.createdDirectory("invalid-" + index);
            Files.writeString(directory.resolve("employees.json"), invalid.get(index));
            assertThrows(PersistenceException.class,
                () -> open(directory));
        }
    }

    /**
     * Verifies duplicate identifiers in one store file are rejected.
     *
     * @throws Exception when temporary file access fails
     */
    @Test
    public void rejectsDuplicateRecordIdentifiers() throws Exception {
        final Path directory = this.createdDirectory("duplicate");
        final UUID id = UUID.randomUUID();
        final JsonArray array = new JsonArray();
        addRecord(array, id);
        addRecord(array, id);
        Files.writeString(
            directory.resolve("employees.json"),
            array.toText("  ")
        );

        assertThrows(PersistenceException.class,
            () -> open(directory));
    }

    /**
     * Verifies invalid encoded store file names are rejected.
     *
     * @throws Exception when temporary file access fails
     */
    @Test
    public void rejectsInvalidStoreFileNames() throws Exception {
        final Path directory = this.createdDirectory("bad-name");
        Files.writeString(directory.resolve("%ZZ.json"), "[]");

        assertThrows(PersistenceException.class,
            () -> open(directory));
    }

    /**
     * Verifies the configured database path cannot be a regular file.
     *
     * @throws Exception when temporary file access fails
     */
    @Test
    public void rejectsRegularFileAsDatabaseDirectory() throws Exception {
        final Path file = this.temporary.newFile("database.json").toPath();

        assertThrows(PersistenceException.class,
            () -> open(file));
    }

    /**
     * Creates a path below the temporary root without creating it.
     *
     * @param name path name
     * @return path
     */
    private Path directory(final String name) {
        return this.temporary.getRoot().toPath().resolve(name);
    }

    /**
     * Creates a temporary directory.
     *
     * @param name directory name
     * @return path
     * @throws Exception when creation fails
     */
    private Path createdDirectory(final String name) throws Exception {
        return this.temporary.newFolder(name).toPath();
    }

    /**
     * Parses the first object in a JSON file.
     *
     * @param file file
     * @return object
     * @throws Exception when parsing fails
     */
    private static JsonObject firstObject(final Path file) throws Exception {
        return Json.parse(file.toFile()).toJsonArray()
            .getElement(0).toJsonObject();
    }

    /**
     * Initializes and loads JSON persistence with the test schema.
     *
     * @param directory database directory
     * @return open persistence
     */
    private static JsonPersistence open(final Path directory) {
        final JsonPersistence persistence = new JsonPersistence(directory);
        persistence.initialize(METADATA);
        persistence.load();
        return persistence;
    }

    /**
     * Creates one complete stored record.
     *
     * @param store store name
     * @param id identifier
     * @param revision revision
     * @return record
     */
    private static StoredRecord record(
        final String store,
        final UUID id,
        final long revision
    ) {
        final Map<String, StoredValue> fields = new LinkedHashMap<>();
        fields.put("name", new StringValue("Alice"));
        fields.put("age", new IntegerValue(34));
        fields.put("score", new RealValue(1.5));
        fields.put("active", new BooleanValue(true));
        fields.put(
            "departmentId",
            new StringValue("11111111-1111-1111-1111-111111111111")
        );
        return new StoredRecord(store, id, CREATED_AT, revision, fields);
    }

    /**
     * Adds a minimal valid record to a JSON array.
     *
     * @param array array
     * @param id identifier
     */
    private static void addRecord(final JsonArray array, final UUID id) {
        final JsonObject object = array.createObject();
        object.addString("id", id.toString());
        object.addString("createdAt", CREATED_AT.toString());
        object.addNumber("revision", 1);
        object.createObject("fields");
    }

    /**
     * Deterministic creation time.
     */
    private static final Instant CREATED_AT =
        Instant.parse("2026-08-29T12:00:00Z");

    /**
     * Schema catalog shared by JSON persistence tests.
     */
    private static final DatabaseMetadata METADATA = new DatabaseMetadata(
        DatabaseMetadata.CURRENT_FORMAT_VERSION,
        Arrays.asList(
            new StoreMetadata(
                "employees",
                0,
                Arrays.asList(
                    new FieldMetadata(
                        "name", "string", Kind.STRING,
                        new StringValue(""), 0, null
                    ),
                    new FieldMetadata(
                        "age", "integer", Kind.INTEGER,
                        new IntegerValue(0), 1, null
                    ),
                    new FieldMetadata(
                        "score", "real", Kind.REAL,
                        new RealValue(0.0), 2, null
                    ),
                    new FieldMetadata(
                        "active", "boolean", Kind.BOOLEAN,
                        new BooleanValue(false), 3, null
                    ),
                    new FieldMetadata(
                        "departmentId",
                        "identifier",
                        Kind.STRING,
                        new StringValue(
                            "a843176c-36df-44bd-b8a2-b1d8c956c431"
                        ),
                        4,
                        "departments"
                    )
                )
            ),
            new StoreMetadata("settings", 1, List.of()),
            new StoreMetadata("sales west/2026", 2, List.of()),
            new StoreMetadata("departments", 3, List.of())
        )
    );
}
