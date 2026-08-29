/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.db.DatabaseTestSupport.Fixture;
import com.kniazkov.widgets.db.query.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static com.kniazkov.widgets.db.DatabaseTestSupport.ACTIVE;
import static com.kniazkov.widgets.db.DatabaseTestSupport.AGE;
import static com.kniazkov.widgets.db.DatabaseTestSupport.NAME;
import static com.kniazkov.widgets.db.DatabaseTestSupport.addEmployee;
import static com.kniazkov.widgets.db.DatabaseTestSupport.names;
import static com.kniazkov.widgets.db.DatabaseTestSupport.open;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for live query membership, ordering, and subscriptions.
 */
public final class LiveRecordSetTest {
    /**
     * Verifies initial live results are filtered and ordered snapshots.
     */
    @Test
    public void evaluatesInitialResult() {
        try (Fixture fixture = open()) {
            addEmployee(fixture.store(), "Carol", 41, false, 3.0);
            addEmployee(fixture.store(), "Alice", 34, true, 1.5);
            addEmployee(fixture.store(), "Bob", 28, true, 2.0);

            final LiveRecordSet result = fixture.store().query(
                Query.where(ACTIVE.is(true)).orderBy(NAME.ascending())
            );

            assertEquals(Arrays.asList("Alice", "Bob"), names(result.getRecords()));
        }
    }

    /**
     * Verifies committing a matching record emits an added event.
     */
    @Test
    public void reportsAddedRecords() {
        try (Fixture fixture = open()) {
            final LiveRecordSet result = fixture.store().query(
                Query.where(AGE.greaterThan(30)).orderBy(NAME.ascending())
            );
            final List<RecordChange> changes = new ArrayList<>();
            result.subscribe(changes::add);

            final DataRecord alice = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );

            assertEquals(1, changes.size());
            assertChange(changes.get(0), RecordChange.Kind.ADDED, alice, -1, 0);
        }
    }

    /**
     * Verifies a field change can add an existing record to a result.
     */
    @Test
    public void reportsRecordsEnteringFilter() {
        try (Fixture fixture = open()) {
            final DataRecord alice = addEmployee(
                fixture.store(), "Alice", 20, true, 1.5
            );
            final LiveRecordSet result = fixture.store().query(
                Query.where(AGE.greaterThan(30))
            );
            final List<RecordChange> changes = new ArrayList<>();
            result.subscribe(changes::add);

            alice.model(AGE).setData(34);

            assertChange(changes.get(0), RecordChange.Kind.ADDED, alice, -1, 0);
            assertEquals(Arrays.asList(alice), result.getRecords());
        }
    }

    /**
     * Verifies a field change can remove a record from a result.
     */
    @Test
    public void reportsRecordsLeavingFilter() {
        try (Fixture fixture = open()) {
            final DataRecord alice = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );
            final LiveRecordSet result = fixture.store().query(
                Query.where(AGE.greaterThan(30))
            );
            final List<RecordChange> changes = new ArrayList<>();
            result.subscribe(changes::add);

            alice.model(AGE).setData(20);

            assertChange(changes.get(0), RecordChange.Kind.REMOVED, alice, 0, -1);
            assertTrue(result.getRecords().isEmpty());
        }
    }

    /**
     * Verifies deleting a matching record emits a removed event.
     */
    @Test
    public void reportsDeletedRecords() {
        try (Fixture fixture = open()) {
            final DataRecord alice = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );
            final LiveRecordSet result = fixture.store().query(Query.all());
            final List<RecordChange> changes = new ArrayList<>();
            result.subscribe(changes::add);

            alice.remove();

            assertChange(changes.get(0), RecordChange.Kind.REMOVED, alice, 0, -1);
        }
    }

    /**
     * Verifies sorting changes emit moved events with both indices.
     */
    @Test
    public void reportsMovedRecords() {
        try (Fixture fixture = open()) {
            final DataRecord alice = addEmployee(
                fixture.store(), "Alice", 30, true, 1.5
            );
            addEmployee(fixture.store(), "Bob", 40, true, 2.0);
            final LiveRecordSet result = fixture.store().query(
                Query.all().orderBy(AGE.ascending())
            );
            final List<RecordChange> changes = new ArrayList<>();
            result.subscribe(changes::add);

            alice.model(AGE).setData(50);

            assertChange(changes.get(0), RecordChange.Kind.MOVED, alice, 0, 1);
            assertEquals(Arrays.asList("Bob", "Alice"), names(result.getRecords()));
        }
    }

    /**
     * Verifies dependent changes without movement emit updated events.
     */
    @Test
    public void reportsDependentUpdatesWithoutMovement() {
        try (Fixture fixture = open()) {
            final DataRecord alice = addEmployee(
                fixture.store(), "Alice", 30, true, 1.5
            );
            addEmployee(fixture.store(), "Bob", 40, true, 2.0);
            final LiveRecordSet result = fixture.store().query(
                Query.all().orderBy(AGE.ascending())
            );
            final List<RecordChange> changes = new ArrayList<>();
            result.subscribe(changes::add);

            alice.model(AGE).setData(31);

            assertChange(changes.get(0), RecordChange.Kind.UPDATED, alice, 0, 0);
        }
    }

    /**
     * Verifies unrelated field changes still update matching rendered rows.
     */
    @Test
    public void reportsUnrelatedUpdatesForMatchingRecords() {
        try (Fixture fixture = open()) {
            final DataRecord alice = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );
            final LiveRecordSet result = fixture.store().query(
                Query.where(AGE.greaterThan(30))
            );
            final List<RecordChange> changes = new ArrayList<>();
            result.subscribe(changes::add);

            alice.model(ACTIVE).setData(false);

            assertChange(changes.get(0), RecordChange.Kind.UPDATED, alice, 0, 0);
        }
    }

    /**
     * Verifies unrelated changes to unmatched records emit nothing.
     */
    @Test
    public void ignoresUnrelatedUpdatesForUnmatchedRecords() {
        try (Fixture fixture = open()) {
            final DataRecord alice = addEmployee(
                fixture.store(), "Alice", 20, true, 1.5
            );
            final LiveRecordSet result = fixture.store().query(
                Query.where(AGE.greaterThan(30))
            );
            final List<RecordChange> changes = new ArrayList<>();
            result.subscribe(changes::add);

            alice.model(ACTIVE).setData(false);

            assertTrue(changes.isEmpty());
        }
    }

    /**
     * Verifies closing a subscription detaches its listener and is idempotent.
     */
    @Test
    public void detachesSubscriptions() {
        try (Fixture fixture = open()) {
            final LiveRecordSet result = fixture.store().query(Query.all());
            final List<RecordChange> changes = new ArrayList<>();
            final Subscription subscription = result.subscribe(changes::add);
            subscription.close();
            subscription.close();

            addEmployee(fixture.store(), "Alice", 34, true, 1.5);

            assertTrue(changes.isEmpty());
        }
    }

    /**
     * Verifies result snapshots can be modified without changing the live result.
     */
    @Test
    public void returnsIndependentResultSnapshots() {
        try (Fixture fixture = open()) {
            addEmployee(fixture.store(), "Alice", 34, true, 1.5);
            final LiveRecordSet result = fixture.store().query(Query.all());
            final List<DataRecord> snapshot = result.getRecords();
            snapshot.clear();

            assertEquals(1, result.getRecords().size());
        }
    }

    /**
     * Verifies invalid live-query arguments fail immediately.
     */
    @Test
    public void rejectsNullQueryAndListener() {
        try (Fixture fixture = open()) {
            assertThrows(NullPointerException.class,
                () -> fixture.store().query(null));
            final LiveRecordSet result = fixture.store().query(Query.all());
            assertThrows(NullPointerException.class,
                () -> result.subscribe(null));
        }
    }

    /**
     * Verifies one record change.
     *
     * @param change actual change
     * @param kind expected kind
     * @param record expected record
     * @param oldIndex expected old index
     * @param newIndex expected new index
     */
    private static void assertChange(
        final RecordChange change,
        final RecordChange.Kind kind,
        final DataRecord record,
        final int oldIndex,
        final int newIndex
    ) {
        assertEquals(kind, change.kind());
        assertEquals(record, change.record());
        assertEquals(oldIndex, change.oldIndex());
        assertEquals(newIndex, change.newIndex());
    }
}
