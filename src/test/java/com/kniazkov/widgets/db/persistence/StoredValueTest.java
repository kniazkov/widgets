/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence;

import com.kniazkov.widgets.db.ValueType;
import com.kniazkov.widgets.db.persistence.StoredValue.BooleanValue;
import com.kniazkov.widgets.db.persistence.StoredValue.IntegerValue;
import com.kniazkov.widgets.db.persistence.StoredValue.Kind;
import com.kniazkov.widgets.db.persistence.StoredValue.RealValue;
import com.kniazkov.widgets.db.persistence.StoredValue.StringValue;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for persistence-neutral scalar and snapshot value objects.
 */
public final class StoredValueTest {
    /**
     * Verifies every built-in value type converts to and from native scalars.
     */
    @Test
    public void roundTripsBuiltInValueTypes() {
        final UUID id = UUID.randomUUID();

        assertEquals("text", ValueType.STRING.fromStoredValue(
            ValueType.STRING.toStoredValue("text")
        ));
        assertEquals(Integer.valueOf(42), ValueType.INTEGER.fromStoredValue(
            ValueType.INTEGER.toStoredValue(42)
        ));
        assertEquals(Double.valueOf(1.5), ValueType.REAL.fromStoredValue(
            ValueType.REAL.toStoredValue(1.5)
        ));
        assertEquals(Boolean.TRUE, ValueType.BOOLEAN.fromStoredValue(
            ValueType.BOOLEAN.toStoredValue(true)
        ));
        assertEquals(id, ValueType.IDENTIFIER.fromStoredValue(
            ValueType.IDENTIFIER.toStoredValue(id)
        ));
        assertEquals(Double.valueOf(42.0),
            ValueType.REAL.fromStoredValue(new IntegerValue(42)));
    }

    /**
     * Verifies every scalar retains its exact native kind and value.
     */
    @Test
    public void retainsNativeScalarKinds() {
        final StoredValue text = new StringValue("value");
        final StoredValue integer = new IntegerValue(42);
        final StoredValue real = new RealValue(1.5);
        final StoredValue bool = new BooleanValue(true);

        assertEquals(Kind.STRING, text.getKind());
        assertEquals("value", text.getString());
        assertEquals(Kind.INTEGER, integer.getKind());
        assertEquals(42, integer.getInteger());
        assertEquals(Kind.REAL, real.getKind());
        assertEquals(1.5, real.getReal(), 0.0);
        assertEquals(Kind.BOOLEAN, bool.getKind());
        assertTrue(bool.getBoolean());
    }

    /**
     * Verifies integers can be consumed as real numbers without loss.
     */
    @Test
    public void widensIntegersToRealValues() {
        assertEquals(42.0, new IntegerValue(42).getReal(), 0.0);
    }

    /**
     * Verifies incompatible scalar access fails with a useful contract error.
     */
    @Test
    public void rejectsMismatchedScalarAccess() {
        final StoredValue text = new StringValue("value");

        assertThrows(IllegalArgumentException.class, text::getInteger);
        assertThrows(IllegalArgumentException.class, text::getReal);
        assertThrows(IllegalArgumentException.class, text::getBoolean);
        assertThrows(IllegalArgumentException.class,
            () -> new BooleanValue(true).getString());
    }

    /**
     * Verifies invalid scalar payloads are rejected.
     */
    @Test
    public void rejectsInvalidScalarPayloads() {
        assertThrows(NullPointerException.class, () -> new StringValue(null));
        assertThrows(IllegalArgumentException.class,
            () -> new RealValue(Double.NaN));
        assertThrows(IllegalArgumentException.class,
            () -> new RealValue(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
            () -> new RealValue(Double.NEGATIVE_INFINITY));
    }

    /**
     * Verifies stored records preserve order and defensively copy fields.
     */
    @Test
    public void copiesAndProtectsStoredRecordFields() {
        final Map<String, StoredValue> fields = new LinkedHashMap<>();
        fields.put("name", new StringValue("Alice"));
        fields.put("age", new IntegerValue(34));
        final StoredRecord record = record(fields);
        fields.clear();

        assertEquals(Arrays.asList("name", "age"),
            new ArrayList<>(record.getFields().keySet()));
        assertEquals("Alice", record.getFields().get("name").getString());
        assertThrows(UnsupportedOperationException.class,
            () -> record.getFields().clear());
    }

    /**
     * Verifies stored record metadata and null validation.
     */
    @Test
    public void validatesStoredRecords() {
        final UUID id = UUID.randomUUID();
        final Instant createdAt = Instant.now();
        final StoredRecord record = new StoredRecord(
            "employees",
            id,
            createdAt,
            9L,
            Map.of("name", new StringValue("Alice"))
        );

        assertEquals("employees", record.getStore());
        assertEquals(id, record.getId());
        assertEquals(createdAt, record.getCreatedAt());
        assertEquals(9L, record.getRevision());
        assertThrows(NullPointerException.class,
            () -> new StoredRecord(null, id, createdAt, 1L, Map.of()));
        assertThrows(NullPointerException.class,
            () -> new StoredRecord("employees", null, createdAt, 1L, Map.of()));
        assertThrows(NullPointerException.class,
            () -> new StoredRecord("employees", id, null, 1L, Map.of()));
        assertThrows(NullPointerException.class,
            () -> new StoredRecord("employees", id, createdAt, 1L, null));
    }

    /**
     * Verifies change set convenience factories and mutation payloads.
     */
    @Test
    public void createsTypedChangeSetMutations() {
        final StoredRecord record = record(Map.of());
        final ChangeSet upsert = ChangeSet.upsert(record);
        final ChangeSet delete = ChangeSet.delete("employees", record.getId());

        assertEquals(record,
            ((ChangeSet.Upsert) upsert.getMutations().get(0)).record());
        final ChangeSet.Delete deletion =
            (ChangeSet.Delete) delete.getMutations().get(0);
        assertEquals("employees", deletion.store());
        assertEquals(record.getId(), deletion.id());
    }

    /**
     * Verifies change sets defensively copy and protect mutation lists.
     */
    @Test
    public void protectsChangeSetMutations() {
        final List<ChangeSet.Mutation> mutations = new ArrayList<>();
        mutations.add(new ChangeSet.Upsert(record(Map.of())));
        final ChangeSet changes = new ChangeSet(mutations);
        mutations.clear();

        assertEquals(1, changes.getMutations().size());
        assertThrows(UnsupportedOperationException.class,
            () -> changes.getMutations().clear());
        assertThrows(NullPointerException.class,
            () -> new ChangeSet.Upsert(null));
        assertThrows(NullPointerException.class,
            () -> new ChangeSet.Delete(null, UUID.randomUUID()));
        assertThrows(NullPointerException.class,
            () -> new ChangeSet.Delete("employees", null));
    }

    /**
     * Verifies database snapshots are immutable defensive copies.
     */
    @Test
    public void protectsDatabaseSnapshots() {
        final List<StoredRecord> records = new ArrayList<>();
        records.add(record(Map.of()));
        final DatabaseSnapshot snapshot = new DatabaseSnapshot(records);
        records.clear();

        assertEquals(1, snapshot.getRecords().size());
        assertThrows(UnsupportedOperationException.class,
            () -> snapshot.getRecords().clear());
        assertTrue(DatabaseSnapshot.empty().getRecords().isEmpty());
        assertThrows(NullPointerException.class,
            () -> new DatabaseSnapshot(null));
    }

    /**
     * Verifies persistence metadata is immutable and validates ordering,
     * uniqueness, and references.
     */
    @Test
    public void validatesDatabaseMetadata() {
        final List<FieldMetadata> fields = new ArrayList<>();
        fields.add(new FieldMetadata(
            "departmentId",
            "identifier",
            Kind.STRING,
            new StringValue("11111111-1111-1111-1111-111111111111"),
            0,
            "departments"
        ));
        final StoreMetadata employees = new StoreMetadata(
            "employees",
            0,
            fields
        );
        fields.clear();
        final StoreMetadata departments = new StoreMetadata(
            "departments",
            1,
            List.of()
        );
        final DatabaseMetadata metadata = new DatabaseMetadata(
            DatabaseMetadata.CURRENT_FORMAT_VERSION,
            Arrays.asList(employees, departments)
        );

        assertEquals(1, metadata.formatVersion());
        assertEquals(2, metadata.stores().size());
        assertEquals("departmentId",
            metadata.stores().get(0).fields().get(0).name());
        assertThrows(UnsupportedOperationException.class,
            () -> metadata.stores().clear());
        assertThrows(UnsupportedOperationException.class,
            () -> employees.fields().clear());
        assertThrows(IllegalArgumentException.class,
            () -> new DatabaseMetadata(0, List.of()));
        assertThrows(IllegalArgumentException.class,
            () -> new DatabaseMetadata(1, List.of(
                new StoreMetadata("employees", 1, List.of())
            )));
        assertThrows(IllegalArgumentException.class,
            () -> new FieldMetadata(
                "name",
                "string",
                Kind.STRING,
                new StringValue(""),
                -1,
                null
            ));
        assertThrows(IllegalArgumentException.class,
            () -> new FieldMetadata(
                "age",
                "integer",
                Kind.INTEGER,
                new StringValue("0"),
                0,
                null
            ));
        assertThrows(IllegalArgumentException.class,
            () -> new DatabaseMetadata(1, List.of(
                new StoreMetadata("employees", 0, List.of(
                    new FieldMetadata(
                        "missingId",
                        "identifier",
                        Kind.STRING,
                        new StringValue(
                            "11111111-1111-1111-1111-111111111111"
                        ),
                        0,
                        "missing"
                    )
                ))
            )));
    }

    /**
     * Verifies the memory-only backend implements the complete no-op contract.
     */
    @Test
    public void supportsNoPersistenceBackend() {
        final NoPersistence persistence = new NoPersistence();

        persistence.initialize(new DatabaseMetadata(
            DatabaseMetadata.CURRENT_FORMAT_VERSION,
            List.of()
        ));
        assertTrue(persistence.load().getRecords().isEmpty());
        persistence.commit(new ChangeSet(List.of()));
        persistence.close();
        assertThrows(NullPointerException.class,
            () -> persistence.initialize(null));
    }

    /**
     * Verifies persistence exceptions retain their message and optional cause.
     */
    @Test
    public void retainsPersistenceFailureDetails() {
        final Exception cause = new Exception("cause");
        final PersistenceException withCause =
            new PersistenceException("message", cause);
        final PersistenceException withoutCause =
            new PersistenceException("plain");

        assertEquals("message", withCause.getMessage());
        assertEquals(cause, withCause.getCause());
        assertEquals("plain", withoutCause.getMessage());
        assertEquals(null, withoutCause.getCause());
    }

    /**
     * Creates a stored record for value-object tests.
     *
     * @param fields fields
     * @return stored record
     */
    private static StoredRecord record(final Map<String, StoredValue> fields) {
        return new StoredRecord(
            "employees",
            UUID.randomUUID(),
            Instant.parse("2026-08-29T12:00:00Z"),
            1L,
            fields
        );
    }
}
