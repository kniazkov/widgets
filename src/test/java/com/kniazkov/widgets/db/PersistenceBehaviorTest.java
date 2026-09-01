/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.db.DatabaseTestSupport.Fixture;
import com.kniazkov.widgets.db.DatabaseTestSupport.RecordingPersistence;
import com.kniazkov.widgets.db.persistence.ChangeSet;
import com.kniazkov.widgets.db.persistence.DatabaseSnapshot;
import com.kniazkov.widgets.db.persistence.PersistenceException;
import com.kniazkov.widgets.db.persistence.StoredRecord;
import com.kniazkov.widgets.db.persistence.StoredValue;
import com.kniazkov.widgets.db.persistence.StoredValue.BooleanValue;
import com.kniazkov.widgets.db.persistence.StoredValue.IntegerValue;
import com.kniazkov.widgets.db.persistence.StoredValue.Kind;
import com.kniazkov.widgets.db.persistence.StoredValue.RealValue;
import com.kniazkov.widgets.db.persistence.StoredValue.StringValue;
import com.kniazkov.widgets.model.StringModel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static com.kniazkov.widgets.db.DatabaseTestSupport.AGE;
import static com.kniazkov.widgets.db.DatabaseTestSupport.NAME;
import static com.kniazkov.widgets.db.DatabaseTestSupport.addEmployee;
import static com.kniazkov.widgets.db.DatabaseTestSupport.open;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the boundary between canonical memory and persistence backends.
 */
public final class PersistenceBehaviorTest {
    /**
     * Verifies snapshot restoration does not wait for the class that builds the
     * database to finish static initialization.
     */
    @Test(timeout = 5000)
    public void loadsCustomTypesDuringStaticInitialization() {
        try (Database database = StaticInitializationFixture.DATABASE) {
            final DataRecord record = database.getStore("values")
                .getRecord(StaticInitializationFixture.RECORD_ID);

            assertEquals("restored", record.model(
                StaticInitializationFixture.VALUE
            ).getData());
        }
    }

    /**
     * Verifies configured records are loaded with metadata and native field values.
     */
    @Test
    public void loadsInitialSnapshot() {
        final UUID id = UUID.randomUUID();
        final Instant createdAt = Instant.parse("2026-08-29T12:00:00Z");
        final StoredRecord stored = employee(id, createdAt, 7L, "Alice", 34);
        final RecordingPersistence persistence = new RecordingPersistence(
            new DatabaseSnapshot(List.of(stored))
        );

        try (Fixture fixture = open(persistence)) {
            final DataRecord record = fixture.store().getRecord(id);

            assertEquals(id, record.getId());
            assertEquals(createdAt, record.getCreatedAt());
            assertEquals(7L, record.getRevision());
            assertEquals("Alice", record.model(NAME).getData());
            assertEquals(Integer.valueOf(34), record.model(AGE).getData());
            assertEquals(Integer.valueOf(1),
                fixture.store().getRecordCountModel().getData());
        }
    }

    /**
     * Verifies absent persisted fields lazily use their model defaults.
     */
    @Test
    public void suppliesDefaultsForMissingStoredFields() {
        final StoredRecord stored = new StoredRecord(
            "employees",
            UUID.randomUUID(),
            Instant.now(),
            1L,
            Map.of("name", new StringValue("Alice"))
        );
        final RecordingPersistence persistence = new RecordingPersistence(
            new DatabaseSnapshot(List.of(stored))
        );

        try (Fixture fixture = open(persistence)) {
            final DataRecord record = fixture.store().getRecord(stored.getId());

            assertEquals(Integer.valueOf(0), record.model(AGE).getData());
            assertEquals(Boolean.FALSE,
                record.model(DatabaseTestSupport.ACTIVE).getData());
            assertEquals(Double.valueOf(0.0),
                record.model(DatabaseTestSupport.SCORE).getData());
        }
    }

    /**
     * Verifies snapshots cannot refer to an unconfigured store.
     */
    @Test
    public void rejectsUnknownStoredStore() {
        final StoredRecord stored = new StoredRecord(
            "missing",
            UUID.randomUUID(),
            Instant.now(),
            1L,
            Map.of()
        );
        final RecordingPersistence persistence = new RecordingPersistence(
            new DatabaseSnapshot(List.of(stored))
        );

        assertThrows(PersistenceException.class, () -> open(persistence));
    }

    /**
     * Verifies snapshots cannot contain duplicate record identifiers.
     */
    @Test
    public void rejectsDuplicateStoredIdentifiers() {
        final UUID id = UUID.randomUUID();
        final Instant createdAt = Instant.now();
        final DatabaseSnapshot snapshot = new DatabaseSnapshot(
            Arrays.asList(
                employee(id, createdAt, 1L, "Alice", 34),
                employee(id, createdAt, 2L, "Bob", 35)
            )
        );

        assertThrows(
            IllegalStateException.class,
            () -> open(new RecordingPersistence(snapshot))
        );
    }

    /**
     * Verifies snapshots cannot contain fields absent from the schema.
     */
    @Test
    public void rejectsUnknownStoredFields() {
        final StoredRecord stored = new StoredRecord(
            "employees",
            UUID.randomUUID(),
            Instant.now(),
            1L,
            Map.of("unknown", new StringValue("value"))
        );

        assertThrows(
            IllegalStateException.class,
            () -> open(new RecordingPersistence(
                new DatabaseSnapshot(List.of(stored))
            ))
        );
    }

    /**
     * Verifies snapshots cannot supply a scalar of the wrong native type.
     */
    @Test
    public void rejectsMismatchedStoredTypes() {
        final StoredRecord stored = new StoredRecord(
            "employees",
            UUID.randomUUID(),
            Instant.now(),
            1L,
            Map.of("age", new StringValue("34"))
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> open(new RecordingPersistence(
                new DatabaseSnapshot(List.of(stored))
            ))
        );
    }

    /**
     * Verifies persistence happens before a new record becomes observable.
     */
    @Test
    public void persistsBeforePublishingCreation() {
        final RecordingPersistence persistence = new RecordingPersistence();
        try (Fixture fixture = open(persistence)) {
            persistence.setObserver(changes -> {
                assertTrue(fixture.store().getRecords().isEmpty());
                assertEquals(Integer.valueOf(0),
                    fixture.store().getRecordCountModel().getData());
            });

            final DataRecord record = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );

            assertSame(record, fixture.store().getRecord(record.getId()));
        }
    }

    /**
     * Verifies persistence happens before a direct change becomes observable.
     */
    @Test
    public void persistsBeforePublishingUpdate() {
        final RecordingPersistence persistence = new RecordingPersistence();
        try (Fixture fixture = open(persistence)) {
            final DataRecord record = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );
            persistence.setObserver(changes -> {
                assertEquals("Alice", record.model(NAME).getData());
                assertEquals(1L, record.getRevision());
            });

            record.model(NAME).setData("Alicia");

            assertEquals("Alicia", record.model(NAME).getData());
        }
    }

    /**
     * Verifies failed creation does not modify canonical memory.
     */
    @Test
    public void rollsBackMemoryWhenCreationPersistenceFails() {
        final RecordingPersistence persistence = new RecordingPersistence();
        try (Fixture fixture = open(persistence)) {
            persistence.setFailCommit(true);
            final Draft draft = fixture.store().createDraft();
            draft.model(NAME).setData("Alice");

            assertThrows(PersistenceException.class, draft::commit);

            assertTrue(fixture.store().getRecords().isEmpty());
            assertEquals(Integer.valueOf(0),
                fixture.store().getRecordCountModel().getData());
            persistence.setFailCommit(false);
            assertEquals("Alice", draft.commit().model(NAME).getData());
        }
    }

    /**
     * Verifies failed updates preserve value, revision, and listeners.
     */
    @Test
    public void rollsBackMemoryWhenUpdatePersistenceFails() {
        final RecordingPersistence persistence = new RecordingPersistence();
        try (Fixture fixture = open(persistence)) {
            final DataRecord record = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );
            final List<String> observed = new ArrayList<>();
            record.model(NAME).addListener(observed::add);
            persistence.setFailCommit(true);

            assertThrows(PersistenceException.class,
                () -> record.model(NAME).setData("Alicia"));

            assertEquals("Alice", record.model(NAME).getData());
            assertEquals(1L, record.getRevision());
            assertTrue(observed.isEmpty());
        }
    }

    /**
     * Verifies failed deletion leaves the record in the store.
     */
    @Test
    public void rollsBackMemoryWhenDeletePersistenceFails() {
        final RecordingPersistence persistence = new RecordingPersistence();
        try (Fixture fixture = open(persistence)) {
            final DataRecord record = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );
            persistence.setFailCommit(true);

            assertThrows(PersistenceException.class, record::remove);

            assertSame(record, fixture.store().getRecord(record.getId()));
            assertEquals(Integer.valueOf(1),
                fixture.store().getRecordCountModel().getData());
        }
    }

    /**
     * Verifies deletion is represented by an exact persistence mutation.
     */
    @Test
    public void persistsDeletionMutation() {
        final RecordingPersistence persistence = new RecordingPersistence();
        try (Fixture fixture = open(persistence)) {
            final DataRecord record = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );
            record.remove();

            final ChangeSet.Delete mutation = (ChangeSet.Delete) persistence
                .getCommits().get(1).getMutations().get(0);
            assertEquals("employees", mutation.store());
            assertEquals(record.getId(), mutation.id());
        }
    }

    /**
     * Verifies model listeners can reenter the serialized database safely.
     */
    @Test
    public void allowsReentrantOperationsFromListeners() {
        try (Fixture fixture = open()) {
            final DataRecord record = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );
            final List<Integer> sizes = new ArrayList<>();
            record.model(NAME).addListener(
                ignored -> sizes.add(fixture.store().getRecords().size())
            );

            record.model(NAME).setData("Alicia");

            assertEquals(Arrays.asList(1), sizes);
        }
    }

    /**
     * Verifies concurrent callers are serialized without dropping commits.
     *
     * @throws Exception when a worker fails or times out
     */
    @Test
    public void serializesConcurrentModelWrites() throws Exception {
        final int operations = 80;
        final ExecutorService executor = Executors.newFixedThreadPool(8);
        final CountDownLatch start = new CountDownLatch(1);
        try (Fixture fixture = open()) {
            final DataRecord record = addEmployee(
                fixture.store(), "initial", 34, true, 1.5
            );
            final List<Future<?>> tasks = new ArrayList<>();
            for (int index = 0; index < operations; index++) {
                final String value = "name-" + index;
                tasks.add(executor.submit(() -> {
                    start.await();
                    record.model(NAME).setData(value);
                    return null;
                }));
            }
            start.countDown();
            for (final Future<?> task : tasks) {
                task.get(20, TimeUnit.SECONDS);
            }

            assertEquals(operations + 1L, record.getRevision());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    /**
     * Creates a complete stored employee.
     *
     * @param id identifier
     * @param createdAt creation time
     * @param revision revision
     * @param name name
     * @param age age
     * @return stored record
     */
    private static StoredRecord employee(
        final UUID id,
        final Instant createdAt,
        final long revision,
        final String name,
        final int age
    ) {
        return new StoredRecord(
            "employees",
            id,
            createdAt,
            revision,
            Map.of(
                "name", new StringValue(name),
                "age", new IntegerValue(age),
                "active", new BooleanValue(true),
                "score", new RealValue(1.5)
            )
        );
    }

    /**
     * Database declared in the same class as its custom value conversion.
     */
    private static final class StaticInitializationFixture {
        /**
         * Store name.
         */
        private static final String STORE = "values";

        /**
         * Stored record identifier.
         */
        private static final UUID RECORD_ID = UUID.randomUUID();

        /**
         * Custom value type whose converter belongs to this initializing class.
         */
        private static final ValueType<String> TYPE = ValueType.of(
            "static-initialization-value",
            String.class,
            StringModel::new,
            Kind.STRING,
            StringValue::new,
            StaticInitializationFixture::restore,
            String::compareTo
        );

        /**
         * Custom field.
         */
        private static final Field<String> VALUE = new Field<>(TYPE, "value");

        /**
         * Database built while this class is still being initialized.
         */
        private static final Database DATABASE = createDatabase();

        /**
         * Utility class.
         */
        private StaticInitializationFixture() {
        }

        /**
         * Builds a database whose initial snapshot uses the custom field.
         *
         * @return initialized database
         */
        private static Database createDatabase() {
            final StoredRecord record = new StoredRecord(
                STORE,
                RECORD_ID,
                Instant.now(),
                1L,
                Map.of("value", new StringValue("restored"))
            );
            return Database.builder()
                .persistence(new RecordingPersistence(
                    new DatabaseSnapshot(List.of(record))
                ))
                .store(STORE, Schema.of(VALUE))
                .build();
        }

        /**
         * Restores one custom value. Calling this method from another thread
         * during class initialization reproduces the original deadlock.
         *
         * @param stored persisted scalar
         * @return restored value
         */
        private static String restore(
            final StoredValue stored
        ) {
            return stored.getString();
        }
    }
}
