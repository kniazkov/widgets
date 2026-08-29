/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.db.DatabaseTestSupport.Fixture;
import com.kniazkov.widgets.db.DatabaseTestSupport.RecordingPersistence;
import com.kniazkov.widgets.db.persistence.ChangeSet;
import com.kniazkov.widgets.db.persistence.DatabaseMetadata;
import com.kniazkov.widgets.db.persistence.StoredRecord;
import com.kniazkov.widgets.db.persistence.StoredValue.IntegerValue;
import com.kniazkov.widgets.db.persistence.StoredValue.Kind;
import com.kniazkov.widgets.model.Model;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

import static com.kniazkov.widgets.db.DatabaseTestSupport.ACTIVE;
import static com.kniazkov.widgets.db.DatabaseTestSupport.AGE;
import static com.kniazkov.widgets.db.DatabaseTestSupport.NAME;
import static com.kniazkov.widgets.db.DatabaseTestSupport.SCHEMA;
import static com.kniazkov.widgets.db.DatabaseTestSupport.SCORE;
import static com.kniazkov.widgets.db.DatabaseTestSupport.addEmployee;
import static com.kniazkov.widgets.db.DatabaseTestSupport.open;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Contract tests for fields, schemas, databases, stores, records, and drafts.
 */
public final class DatabaseCoreTest {
    /**
     * Verifies field construction and metadata.
     */
    @Test
    public void validatesFieldConstruction() {
        final Field<String> field = new Field<>(ValueType.STRING, "title");
        final Field<UUID> reference = new Field<>(
            ValueType.IDENTIFIER,
            "departmentId",
            "departments"
        );

        assertEquals("title", field.getName());
        assertSame(ValueType.STRING, field.getType());
        assertEquals(null, field.getReferencedStore());
        assertEquals("departments", reference.getReferencedStore());
        assertThrows(NullPointerException.class,
            () -> new Field<String>(null, "title"));
        assertThrows(NullPointerException.class,
            () -> new Field<>(ValueType.STRING, null));
        assertThrows(IllegalArgumentException.class,
            () -> new Field<>(ValueType.STRING, " \t"));
        assertThrows(IllegalArgumentException.class,
            () -> new Field<>(ValueType.STRING, "name", "employees"));
        assertThrows(IllegalArgumentException.class,
            () -> new Field<>(ValueType.IDENTIFIER, "parentId", " "));
    }

    /**
     * Verifies schema ordering, lookup, identity checks, and immutability.
     */
    @Test
    public void enforcesSchemaContracts() {
        assertEquals(Arrays.asList(NAME, AGE, ACTIVE, SCORE), SCHEMA.getFields());
        assertSame(NAME, SCHEMA.getField("name"));
        assertEquals(null, SCHEMA.getField("missing"));
        SCHEMA.require(NAME);

        final Field<String> alias = new Field<>(ValueType.STRING, "name");
        assertThrows(IllegalArgumentException.class, () -> SCHEMA.require(alias));
        assertThrows(UnsupportedOperationException.class,
            () -> SCHEMA.getFields().add(alias));
        assertThrows(IllegalArgumentException.class,
            () -> Schema.of(NAME, alias));
        assertThrows(NullPointerException.class,
            () -> Schema.of(NAME, null));
    }

    /**
     * Verifies database builder validation.
     */
    @Test
    public void validatesDatabaseBuilderConfiguration() {
        final DatabaseBuilder builder = new DatabaseBuilder();
        builder.store("employees", SCHEMA);

        assertThrows(NullPointerException.class,
            () -> new DatabaseBuilder().store(null, SCHEMA));
        assertThrows(NullPointerException.class,
            () -> new DatabaseBuilder().store("employees", null));
        assertThrows(IllegalArgumentException.class,
            () -> new DatabaseBuilder().store(" ", SCHEMA));
        assertThrows(IllegalStateException.class,
            () -> builder.store("employees", SCHEMA));
        assertThrows(NullPointerException.class,
            () -> builder.persistence(null));
    }

    /**
     * Verifies store lookup and metadata.
     */
    @Test
    public void exposesConfiguredStores() {
        try (Fixture fixture = open()) {
            assertEquals("employees", fixture.store().getName());
            assertSame(SCHEMA, fixture.store().getSchema());
            assertSame(fixture.store(), fixture.database().getStore("employees"));
            assertThrows(IllegalArgumentException.class,
                () -> fixture.database().getStore("missing"));
        }
    }

    /**
     * Verifies configured schemas initialize persistence metadata before load.
     */
    @Test
    public void initializesPersistenceMetadata() {
        final RecordingPersistence persistence = new RecordingPersistence();
        try (Fixture ignored = open(persistence)) {
            final DatabaseMetadata metadata = persistence.getMetadata();

            assertEquals(1, metadata.formatVersion());
            assertEquals(1, metadata.stores().size());
            assertEquals("employees", metadata.stores().get(0).name());
            assertEquals(4, metadata.stores().get(0).fields().size());
            assertEquals("integer",
                metadata.stores().get(0).fields().get(1).type());
            assertEquals(
                Kind.INTEGER,
                metadata.stores().get(0).fields().get(1).valueKind()
            );
            assertEquals(0,
                metadata.stores().get(0).fields().get(1)
                    .defaultValue().getInteger());
        }
    }

    /**
     * Verifies new-record defaults and first commit metadata.
     */
    @Test
    public void createsRecordsFromDrafts() {
        try (Fixture fixture = open()) {
            final Draft draft = fixture.store().createDraft();
            final Model<String> name = draft.model(NAME);

            assertNotNull(draft.getId());
            assertEquals("", name.getData());
            assertSame(name, draft.model(NAME));
            name.setData("Alice");
            draft.model(AGE).setData(34);
            final DataRecord record = draft.commit();

            assertEquals(draft.getId(), record.getId());
            assertNotNull(record.getCreatedAt());
            assertEquals(1L, record.getRevision());
            assertEquals("Alice", record.model(NAME).getData());
            assertEquals(Integer.valueOf(34), record.model(AGE).getData());
            assertSame(record, fixture.store().getRecord(record.getId()));
            assertEquals(Arrays.asList(record), fixture.store().getRecords());
            assertEquals(Integer.valueOf(1),
                fixture.store().getRecordCountModel().getData());
        }
    }

    /**
     * Verifies draft edits stay isolated until one atomic commit.
     */
    @Test
    public void isolatesAndAtomicallyCommitsEdits() {
        final RecordingPersistence persistence = new RecordingPersistence();
        try (Fixture fixture = open(persistence)) {
            final DataRecord record = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );
            final Draft edit = record.edit();
            edit.model(NAME).setData("Alicia");
            edit.model(AGE).setData(35);

            assertEquals("Alice", record.model(NAME).getData());
            assertEquals(Integer.valueOf(34), record.model(AGE).getData());
            final DataRecord committed = edit.commit();

            assertSame(record, committed);
            assertEquals("Alicia", record.model(NAME).getData());
            assertEquals(Integer.valueOf(35), record.model(AGE).getData());
            assertEquals(2L, record.getRevision());
            assertEquals(2, persistence.getCommits().size());
            final ChangeSet.Upsert mutation = (ChangeSet.Upsert) persistence
                .getCommits().get(1).getMutations().get(0);
            final StoredRecord stored = mutation.record();
            assertEquals("Alicia", stored.getFields().get("name").getString());
            assertEquals(35, stored.getFields().get("age").getInteger());
        }
    }

    /**
     * Verifies cancelled and committed drafts cannot be reused.
     */
    @Test
    public void closesDraftsAfterCompletion() {
        try (Fixture fixture = open()) {
            final DataRecord record = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );
            final Draft cancelled = record.edit();
            cancelled.model(NAME).setData("Ignored");
            cancelled.cancel();

            assertEquals("Alice", record.model(NAME).getData());
            assertThrows(IllegalStateException.class, cancelled::commit);
            assertThrows(IllegalStateException.class, cancelled::cancel);
            assertThrows(IllegalStateException.class,
                () -> cancelled.model(NAME));

            final Draft committed = record.edit();
            committed.commit();
            assertThrows(IllegalStateException.class, committed::commit);
            assertThrows(IllegalStateException.class, committed::cancel);
        }
    }

    /**
     * Verifies optimistic revision conflicts.
     */
    @Test
    public void rejectsStaleDrafts() {
        try (Fixture fixture = open()) {
            final DataRecord record = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );
            final Draft first = record.edit();
            final Draft stale = record.edit();
            first.model(NAME).setData("First");
            stale.model(NAME).setData("Stale");
            first.commit();

            assertThrows(ConflictException.class, stale::commit);
            assertEquals("First", record.model(NAME).getData());
            assertEquals(2L, record.getRevision());
        }
    }

    /**
     * Verifies deleting a record invalidates drafts and canonical writes.
     */
    @Test
    public void invalidatesRemovedRecords() {
        try (Fixture fixture = open()) {
            final DataRecord record = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );
            final Draft edit = record.edit();
            record.remove();

            assertEquals(null, fixture.store().getRecord(record.getId()));
            assertTrue(fixture.store().getRecords().isEmpty());
            assertEquals(Integer.valueOf(0),
                fixture.store().getRecordCountModel().getData());
            assertThrows(ConflictException.class, edit::commit);
            assertThrows(IllegalStateException.class,
                () -> record.model(NAME).setData("Removed"));
            assertThrows(IllegalStateException.class, record::edit);
            assertThrows(IllegalStateException.class, record::remove);
        }
    }

    /**
     * Verifies direct model writes persist, revise, and notify only on changes.
     */
    @Test
    public void commitsDirectModelWrites() {
        final RecordingPersistence persistence = new RecordingPersistence();
        try (Fixture fixture = open(persistence)) {
            final DataRecord record = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );
            final List<String> observed = new ArrayList<>();
            final Listener<String> listener = observed::add;
            record.model(NAME).addListener(listener);

            assertFalse(record.model(NAME).setData("Alice"));
            assertTrue(record.model(NAME).setData("Alicia"));

            assertEquals(Arrays.asList("Alicia"), observed);
            assertEquals(2L, record.getRevision());
            assertEquals(2, persistence.getCommits().size());
            record.model(NAME).removeListener(listener);
        }
    }

    /**
     * Verifies schemas reject equal-looking fields from another schema.
     */
    @Test
    public void rejectsForeignFieldInstances() {
        final Field<String> foreign = new Field<>(ValueType.STRING, "name");
        try (Fixture fixture = open()) {
            final DataRecord record = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );

            assertThrows(IllegalArgumentException.class,
                () -> record.model(foreign));
            assertThrows(IllegalArgumentException.class,
                () -> record.edit().model(foreign));
        }
    }

    /**
     * Verifies record snapshots returned by stores are detached lists.
     */
    @Test
    public void returnsIndependentRecordSnapshots() {
        try (Fixture fixture = open()) {
            final DataRecord first = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );
            final List<DataRecord> snapshot = fixture.store().getRecords();
            addEmployee(fixture.store(), "Bob", 28, false, 2.0);

            assertEquals(Arrays.asList(first), snapshot);
            snapshot.clear();
            assertEquals(2, fixture.store().getRecords().size());
        }
    }

    /**
     * Verifies database shutdown is idempotent and rejects later operations.
     */
    @Test
    public void closesDatabaseAndPersistenceOnce() {
        final RecordingPersistence persistence = new RecordingPersistence();
        final Fixture fixture = open(persistence);
        final Store store = fixture.store();

        fixture.database().close();
        fixture.database().close();

        assertEquals(1, persistence.getCloseCount());
        assertThrows(IllegalStateException.class, store::getRecords);
        assertThrows(IllegalStateException.class, store::createDraft);
    }

    /**
     * Verifies built-in types create independent models and native values.
     */
    @Test
    public void exposesBuiltInValueTypeContracts() {
        assertEquals(String.class, ValueType.STRING.getValueClass());
        assertEquals("string", ValueType.STRING.getName());
        assertEquals(
            Kind.STRING,
            ValueType.STRING.getStoredKind()
        );
        assertEquals("", ValueType.STRING.getStoredDefault().getString());
        assertEquals(Integer.class, ValueType.INTEGER.getValueClass());
        assertEquals(Boolean.class, ValueType.BOOLEAN.getValueClass());
        assertNotSame(ValueType.STRING.createModel(), ValueType.STRING.createModel());
        assertEquals("value", ValueType.STRING.createModel("value").getData());
        assertEquals(7, ValueType.INTEGER.toStoredValue(7).getInteger());
        assertEquals(Integer.valueOf(7),
            ValueType.INTEGER.fromStoredValue(new IntegerValue(7)));
        assertTrue(ValueType.INTEGER.compare(2, 1) > 0);
        assertThrows(UnsupportedOperationException.class,
            () -> ValueType.BOOLEAN.compare(false, true));
        assertFalse(ValueType.POSITIVE_INTEGER.createModel().isValid());
        assertTrue(ValueType.POSITIVE_INTEGER.createModel(1).isValid());
    }

    /**
     * Verifies custom value types use their supplied conversions and comparator.
     */
    @Test
    public void supportsCustomValueTypes() {
        final ValueType<String> length = ValueType.of(
            "length",
            String.class,
            com.kniazkov.widgets.model.StringModel::new,
            Kind.INTEGER,
            value -> new IntegerValue(value.length()),
            value -> "x".repeat(value.getInteger()),
            String::compareToIgnoreCase
        );

        assertEquals(4, length.toStoredValue("test").getInteger());
        assertEquals("xxx", length.fromStoredValue(new IntegerValue(3)));
        assertTrue(length.compare("B", "a") > 0);
        assertThrows(NullPointerException.class,
            () -> length.toStoredValue(null));
        assertThrows(NullPointerException.class,
            () -> length.fromStoredValue(null));
        assertNotSame(length.createModel(), length.createModel());
    }
}
