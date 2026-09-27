/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.common.NumericRange;
import com.kniazkov.widgets.model.NumericRangeModel;
import com.kniazkov.widgets.db.query.Conditions;
import com.kniazkov.widgets.db.query.Query;
import com.kniazkov.widgets.db.persistence.Persistence;
import com.kniazkov.widgets.db.persistence.json.JsonPersistence;
import com.kniazkov.widgets.db.persistence.jdbc.JdbcPersistence;
import com.kniazkov.widgets.db.persistence.jdbc.H2Dialect;
import com.kniazkov.widgets.db.persistence.jdbc.SqliteDialect;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

/**
 * Closed-interval invariants, reactive search and persistence across all backends.
 */
public final class NumericRangeTest {
    /**
     * Isolated persistence files.
     */
    @Rule public final TemporaryFolder folder = new TemporaryFolder();
    /**
     * Searchable interval field.
     */
    private static final Field<NumericRange> SIZE = new Field<>(ValueType.NUMERIC_RANGE, "size");

    /**
     * Rejects ambiguous/nonfinite bounds and normalizes zero equality.
     */
    @Test public void validatesBounds() {
        assertThrows(IllegalArgumentException.class, () -> new NumericRange(2, 1));
        assertThrows(IllegalArgumentException.class, () -> new NumericRange(Double.NaN, 1));
        assertThrows(IllegalArgumentException.class, 
            () -> new NumericRange(1, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> Conditions.contains(SIZE, Double.NaN));
        assertEquals(new NumericRange(0, 0), new NumericRange(-0.0, 0.0));
        final var model = new NumericRangeModel();
        assertEquals(new NumericRange(0, 0), model.getData());
        assertTrue(model.isValid());
        assertFalse(new NumericRange(15, 16).contains(Double.NaN));
    }

    /**
     * Checks endpoints, gaps, negative/fractional values, composition and live updates.
     */
    @Test public void searchesAndReacts() {
        try (Database db = Database.builder().store("items", Schema.of(SIZE)).build()) {
            final Store store = db.getStore("items");
            final DataRecord a = add(store, 15, 16);
            final DataRecord b = add(store, 16, 18);
            final DataRecord c = add(store, 16, 16);
            final DataRecord d = add(store, -2.5, -1);
            assertEquals(Set.of(a.getId()), ids(store, Conditions.contains(SIZE, 15.5)));
            assertEquals(Set.of(a.getId(), b.getId(), c.getId()),
                ids(store, Conditions.contains(SIZE, 16)));
            assertTrue(ids(store, Conditions.contains(SIZE, 14.99)).isEmpty());
            assertEquals(Set.of(d.getId()), ids(store, Conditions.contains(SIZE, -2.5)));
            assertEquals(Set.of(d.getId()),
                ids(store, Conditions.intersects(SIZE, new NumericRange(-1, 0))));
            assertEquals(Set.of(a.getId(), b.getId(), c.getId()),
                ids(store, Conditions.intersects(SIZE, new NumericRange(15.75, 16.25))));
            assertEquals(Set.of(c.getId()), ids(store, SIZE.is(new NumericRange(16, 16))));
            assertEquals(Set.of(a.getId(), b.getId()), ids(store,
                Conditions.contains(SIZE, 16).and(SIZE.isNot(new NumericRange(16, 16)))));
            assertEquals(Set.of(a.getId(), d.getId()), ids(store,
                Conditions.contains(SIZE, 15).or(Conditions.contains(SIZE, -1))));
            assertEquals(Set.of(d.getId()), ids(store, Conditions.contains(SIZE, 16).not()));
            final var live = store.query(Query.where(Conditions.contains(SIZE, 15.5)));
            final List<RecordChange> changes = new ArrayList<>();
            final var subscription = live.subscribe(changes::add);
            b.model(SIZE).setData(new NumericRange(15, 18));
            assertEquals(2, live.getRecords().size());
            a.model(SIZE).setData(new NumericRange(20, 21));
            assertEquals(List.of(b), live.getRecords());
            b.remove();
            assertTrue(live.getRecords().isEmpty());
            assertEquals(3, changes.size());
            subscription.close();
        }
    }

    /**
     * Compares exhaustive small intervals against a discrete-point oracle.
     */
    @Test public void overlapSearchMatchesSharedPoints() {
        try (Database db = Database.builder().store("items", Schema.of(SIZE)).build()) {
            final Store store = db.getStore("items");
            for (int low = -3; low <= 3; low++) {
                for (int high = low; high <= 3; high++) {
                    add(store, low / 2.0, high / 2.0);
                }
            }
            for (int low = -4; low <= 4; low++) {
                for (int high = low; high <= 4; high++) {
                    final NumericRange query = new NumericRange(low / 2.0, high / 2.0);
                    final Set<UUID> expected = new java.util.HashSet<>();
                    for (final DataRecord record : store.getRecords()) {
                        final NumericRange range = record.model(SIZE).getData();
                        for (int point = low; point <= high; point++) {
                            if (range.contains(point / 2.0)) {
                                expected.add(record.getId());
                            }
                        }
                    }
                    assertEquals(expected, ids(store, Conditions.intersects(SIZE, query)));
                }
            }
        }
    }

    /**
     * Opens physical pre-range SQL tables, then checks idempotent column upgrades.
     * @throws Exception on SQL errors
     */
    @Test public void upgradesLegacySqlColumns() throws Exception {
        for (final boolean sqlite : List.of(false, true)) {
            final String url = sqlite ? "jdbc:sqlite:" + folder.getRoot().toPath().resolve("legacy")
                : "jdbc:h2:mem:range-legacy;DB_CLOSE_DELAY=-1";
            final com.kniazkov.widgets.db.persistence.jdbc.JdbcDialect dialect = sqlite
                ? new SqliteDialect() : new H2Dialect();
            try (var connection = java.sql.DriverManager.getConnection(url);
                var statement = connection.createStatement()) {
                for (final String sql : dialect.initializationSql()) {
                    statement.execute(sql);
                }
                statement.execute(
                    "INSERT INTO db_store(store_name, store_order) VALUES ('old', 0)");
            }
            for (int reopen = 0; reopen < 2; reopen++) {
                try (var persistence = new JdbcPersistence(url, dialect);
                    var connection = java.sql.DriverManager.getConnection(url);
                    var statement = connection.createStatement();
                    var result = statement.executeQuery("SELECT store_name FROM db_store")) {
                    assertTrue(result.next());
                    assertEquals("old", result.getString(1));
                    try (var columns = connection.createStatement();
                        var row = columns.executeQuery(
                            "SELECT range_lower, range_upper FROM db_field")) {
                        assertFalse(row.next());
                    }
                }
            }
        }
    }

    /**
     * Reopens old databases with an added range field and preserves numeric endpoints.
     * @throws Exception on persistence errors
     */
    @Test public void persistsAndUpgrades() throws Exception {
        final Field<String> name = new Field<>(ValueType.STRING, "name");
        for (final String backend : List.of("json", "sqlite", "h2")) {
            final Path path = folder.getRoot().toPath().resolve(backend);
            UUID id;
            try (Database db = Database.builder().persistence(open(backend, path))
                    .store("items", Schema.of(name)).build()) {
                final Draft draft = db.getStore("items").createDraft();
                draft.model(name).setData("Old record");
                id = draft.commit().getId();
            }
            for (int round = 0; round < 2; round++) {
                try (Database db = Database.builder().persistence(open(backend, path))
                        .store("items", Schema.of(name, SIZE)).build()) {
                    final DataRecord record = db.getStore("items").getRecord(id);
                    assertEquals("Old record", record.model(name).getData());
                    assertEquals(round == 0 ? new NumericRange(0, 0) : new NumericRange(15.25, 16),
                        record.model(SIZE).getData());
                    record.model(SIZE).setData(new NumericRange(15.25, 16));
                    assertEquals(Set.of(id),
                        ids(db.getStore("items"), Conditions.contains(SIZE, 15.5)));
                }
            }
        }
    }

    /**
     * @param backend backend name
     * @param path persistence location
     * @return backend instance
     */
    private static Persistence open(final String backend, final Path path) {
        return switch (backend) {
            case "json" -> new JsonPersistence(path);
            case "sqlite" -> new JdbcPersistence("jdbc:sqlite:" + path, new SqliteDialect());
            default -> new JdbcPersistence("jdbc:h2:" + path, new H2Dialect());
        };
    }

    /**
     * @param store target store
     * @param lower lower bound
     * @param upper upper bound
     * @return committed record
     */
    private static DataRecord add(final Store store, final double lower, final double upper) {
        final Draft draft = store.createDraft();
        draft.model(SIZE).setData(new NumericRange(lower, upper));
        return draft.commit();
    }

    /**
     * @param store source
     * @param condition filter
     * @return matching identifiers
     */
    private static Set<UUID> ids(final Store store,
        final com.kniazkov.widgets.db.query.Condition condition) {
        return store.query(Query.where(condition)).getRecords().stream()
            .map(DataRecord::getId).collect(Collectors.toSet());
    }
}
