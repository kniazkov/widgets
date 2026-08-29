/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence.jdbc;

import com.kniazkov.widgets.db.persistence.ChangeSet;
import com.kniazkov.widgets.db.persistence.DatabaseMetadata;
import com.kniazkov.widgets.db.persistence.DatabaseSnapshot;
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Integration-style unit tests for the generic JDBC persistence contract using H2.
 */
public final class JdbcPersistenceTest {
    /**
     * Verifies JDBC exposes the complete schema catalog through SQL tables.
     *
     * @throws Exception when direct SQL inspection fails
     */
    @Test
    public void writesDatabaseMetadata() throws Exception {
        final String url = url();
        open(url).close();

        try (
            Connection connection = DriverManager.getConnection(url);
            Statement statement = connection.createStatement()
        ) {
            assertEquals(1, scalar(statement,
                "SELECT format_version FROM db_metadata"));
            assertEquals(2, count(statement, "db_store"));
            assertEquals(5, count(statement, "db_field_definition"));
            try (ResultSet result = statement.executeQuery(
                "SELECT type_name, value_kind, default_string, field_order, "
                    + "referenced_store "
                    + "FROM db_field_definition "
                    + "WHERE store_name = 'employees' "
                    + "AND field_name = 'departmentId'"
            )) {
                assertTrue(result.next());
                assertEquals("identifier", result.getString(1));
                assertEquals("STRING", result.getString(2));
                assertEquals(
                    "a843176c-36df-44bd-b8a2-b1d8c956c431",
                    result.getString(3)
                );
                assertEquals(4, result.getInt(4));
                assertEquals("departments", result.getString(5));
            }
        }
    }

    /**
     * Verifies persisted metadata must match the configured Java schemas.
     */
    @Test
    public void rejectsMismatchedDatabaseMetadata() {
        final String url = url();
        open(url).close();
        final JdbcPersistence persistence = new JdbcPersistence(
            url,
            new H2Dialect()
        );
        final DatabaseMetadata different = withoutLastEmployeeField();

        assertThrows(PersistenceException.class,
            () -> persistence.initialize(different));
        persistence.close();
    }

    /**
     * Verifies fields appended to a store upgrade the SQL catalog.
     *
     * @throws Exception when direct SQL inspection fails
     */
    @Test
    public void upgradesMetadataWithAppendedFields() throws Exception {
        final String url = url();
        final JdbcPersistence old = new JdbcPersistence(url, new H2Dialect());
        old.initialize(withoutLastEmployeeField());
        old.close();

        open(url).close();

        try (
            Connection connection = DriverManager.getConnection(url);
            Statement statement = connection.createStatement()
        ) {
            assertEquals(5, count(statement, "db_field_definition"));
            assertEquals(1, scalar(statement,
                "SELECT COUNT(*) FROM db_field_definition "
                    + "WHERE store_name = 'employees' "
                    + "AND field_name = 'departmentId' "
                    + "AND field_order = 4"));
        }
    }

    /**
     * Verifies all native scalar types survive a JDBC close and reopen.
     */
    @Test
    public void roundTripsAllNativeValues() {
        final String url = url();
        final StoredRecord source = record("employees", UUID.randomUUID(), 5L);
        try (JdbcPersistence writer = open(url)) {
            assertTrue(writer.load().getRecords().isEmpty());
            writer.commit(ChangeSet.upsert(source));
        }

        try (JdbcPersistence reader = open(url)) {
            final StoredRecord restored = reader.load().getRecords().get(0);
            assertEquals(source.getStore(), restored.getStore());
            assertEquals(source.getId(), restored.getId());
            assertEquals(source.getCreatedAt(), restored.getCreatedAt());
            assertEquals(source.getRevision(), restored.getRevision());
            assertEquals(source.getFields(), restored.getFields());
        }
    }

    /**
     * Verifies JDBC stores each value only in its matching native column.
     *
     * @throws Exception when direct SQL inspection fails
     */
    @Test
    public void usesNativeSqlColumns() throws Exception {
        final String url = url();
        try (JdbcPersistence persistence = open(url)) {
            persistence.commit(ChangeSet.upsert(
                record("employees", UUID.randomUUID(), 1L)
            ));
            try (
                Connection connection = DriverManager.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                    "SELECT field_name, value_type, string_value, integer_value, "
                        + "real_value, boolean_value FROM db_field"
                )
            ) {
                int rows = 0;
                while (result.next()) {
                    rows++;
                    assertNativeRow(result);
                }
                assertEquals(4, rows);
            }
        }
    }

    /**
     * Verifies replacement removes fields absent from the newer snapshot.
     */
    @Test
    public void replacesCompleteRecords() {
        final String url = url();
        final UUID id = UUID.randomUUID();
        try (JdbcPersistence persistence = open(url)) {
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
    }

    /**
     * Verifies record and field rows are both deleted.
     *
     * @throws Exception when direct SQL inspection fails
     */
    @Test
    public void deletesCompleteRecords() throws Exception {
        final String url = url();
        final UUID id = UUID.randomUUID();
        try (JdbcPersistence persistence = open(url)) {
            persistence.commit(ChangeSet.upsert(record("employees", id, 1L)));
            persistence.commit(ChangeSet.delete("employees", id));

            assertTrue(persistence.load().getRecords().isEmpty());
            try (
                Connection connection = DriverManager.getConnection(url);
                Statement statement = connection.createStatement()
            ) {
                assertEquals(0, count(statement, "db_record"));
                assertEquals(0, count(statement, "db_field"));
            }
        }
    }

    /**
     * Verifies all mutations roll back when a later mutation fails.
     */
    @Test
    public void rollsBackWholeChangeSetOnFailure() {
        final String url = url();
        final String oversizedStore = "x".repeat(300);
        final ChangeSet changes = new ChangeSet(Arrays.asList(
            new ChangeSet.Upsert(record("employees", UUID.randomUUID(), 1L)),
            new ChangeSet.Upsert(record(oversizedStore, UUID.randomUUID(), 1L))
        ));
        try (JdbcPersistence persistence = open(url)) {
            assertThrows(PersistenceException.class,
                () -> persistence.commit(changes));

            assertTrue(persistence.load().getRecords().isEmpty());
        }
    }

    /**
     * Verifies malformed rows with a missing typed payload are rejected.
     *
     * @throws Exception when test row insertion fails
     */
    @Test
    public void rejectsMissingTypedColumnValue() throws Exception {
        final String url = url();
        try (JdbcPersistence persistence = open(url)) {
            insertMalformedField(url, "INTEGER", null);

            assertThrows(PersistenceException.class, persistence::load);
        }
    }

    /**
     * Verifies rows with unknown type tags are rejected.
     *
     * @throws Exception when test row insertion fails
     */
    @Test
    public void rejectsUnknownValueType() throws Exception {
        final String url = url();
        try (JdbcPersistence persistence = open(url)) {
            insertMalformedField(url, "BINARY", 7);

            assertThrows(PersistenceException.class, persistence::load);
        }
    }

    /**
     * Verifies initialization can run repeatedly against the same schema.
     */
    @Test
    public void initializesSchemaIdempotently() {
        final String url = url();
        try (
            JdbcPersistence first = open(url);
            JdbcPersistence second = open(url)
        ) {
            assertTrue(first.load().getRecords().isEmpty());
            assertTrue(second.load().getRecords().isEmpty());
        }
    }

    /**
     * Verifies an externally supplied connection is owned and closed.
     *
     * @throws Exception when connection setup fails
     */
    @Test
    public void closesSuppliedConnection() throws Exception {
        final Connection connection = DriverManager.getConnection(url());
        final JdbcPersistence persistence = new JdbcPersistence(
            connection,
            new H2Dialect()
        );

        persistence.close();

        assertTrue(connection.isClosed());
    }

    /**
     * Verifies invalid connection configuration is wrapped consistently.
     */
    @Test
    public void wrapsConnectionFailures() {
        assertThrows(
            PersistenceException.class,
            () -> new JdbcPersistence("jdbc:missing:test", new H2Dialect())
        );
        assertThrows(NullPointerException.class,
            () -> new JdbcPersistence((Connection) null, new H2Dialect()));
    }

    /**
     * Verifies both SQL dialects define the typed EAV columns.
     */
    @Test
    public void dialectsDefineNativeValueColumns() {
        for (final JdbcDialect dialect : Arrays.asList(
            new H2Dialect(),
            new SqliteDialect()
        )) {
            final String sql = String.join(" ", dialect.initializationSql());
            assertTrue(sql.contains("value_type"));
            assertTrue(sql.contains("string_value"));
            assertTrue(sql.contains("integer_value"));
            assertTrue(sql.contains("real_value"));
            assertTrue(sql.contains("boolean_value"));
            assertTrue(sql.contains("db_metadata"));
            assertTrue(sql.contains("db_store"));
            assertTrue(sql.contains("db_field_definition"));
            assertTrue(sql.contains("type_name"));
            assertTrue(sql.contains("value_kind"));
            assertTrue(sql.contains("default_string"));
            assertTrue(sql.contains("default_integer"));
            assertTrue(sql.contains("default_real"));
            assertTrue(sql.contains("default_boolean"));
            assertTrue(sql.contains("referenced_store"));
            assertFalse(sql.contains("field_value"));
            assertFalse(sql.contains("widget"));
        }
    }

    /**
     * Verifies empty JDBC change sets commit without side effects.
     */
    @Test
    public void acceptsEmptyChangeSets() {
        try (JdbcPersistence persistence = open(url())) {
            persistence.commit(new ChangeSet(List.of()));
            final DatabaseSnapshot snapshot = persistence.load();
            assertTrue(snapshot.getRecords().isEmpty());
        }
    }

    /**
     * Verifies one native field row.
     *
     * @param result current result row
     * @throws Exception when a column cannot be read
     */
    private static void assertNativeRow(final ResultSet result) throws Exception {
        final String field = result.getString(1);
        final String type = result.getString(2);
        switch (field) {
            case "name" -> {
                assertEquals("STRING", type);
                assertEquals("Alice", result.getString(3));
                assertNull(result.getObject(4));
                assertNull(result.getObject(5));
                assertNull(result.getObject(6));
            }
            case "age" -> {
                assertEquals("INTEGER", type);
                assertNull(result.getObject(3));
                assertEquals(34, result.getInt(4));
                assertNull(result.getObject(5));
                assertNull(result.getObject(6));
            }
            case "score" -> {
                assertEquals("REAL", type);
                assertNull(result.getObject(3));
                assertNull(result.getObject(4));
                assertEquals(1.5, result.getDouble(5), 0.0);
                assertNull(result.getObject(6));
            }
            case "active" -> {
                assertEquals("BOOLEAN", type);
                assertNull(result.getObject(3));
                assertNull(result.getObject(4));
                assertNull(result.getObject(5));
                assertTrue(result.getBoolean(6));
            }
            default -> throw new AssertionError("Unexpected field: " + field);
        }
    }

    /**
     * Counts rows in a test-owned table.
     *
     * @param statement statement
     * @param table known table name
     * @return row count
     * @throws Exception when the query fails
     */
    private static int count(
        final Statement statement,
        final String table
    ) throws Exception {
        try (ResultSet result = statement.executeQuery(
            "SELECT COUNT(*) FROM " + table
        )) {
            result.next();
            return result.getInt(1);
        }
    }

    /**
     * Reads one integer returned by a query.
     *
     * @param statement statement
     * @param sql query
     * @return integer value
     * @throws Exception when the query fails
     */
    private static int scalar(
        final Statement statement,
        final String sql
    ) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    /**
     * Inserts one record with a deliberately malformed field row.
     *
     * @param url JDBC URL
     * @param type stored type tag
     * @param integer integer payload, or {@code null}
     * @throws Exception when insertion fails
     */
    private static void insertMalformedField(
        final String url,
        final String type,
        final Integer integer
    ) throws Exception {
        final UUID id = UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection(url)) {
            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO db_record "
                    + "(store_name, record_id, created_at, revision) "
                    + "VALUES (?, ?, ?, ?)"
            )) {
                statement.setString(1, "employees");
                statement.setString(2, id.toString());
                statement.setString(3, CREATED_AT.toString());
                statement.setLong(4, 1L);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO db_field "
                    + "(store_name, record_id, field_name, value_type, "
                    + "integer_value) VALUES (?, ?, ?, ?, ?)"
            )) {
                statement.setString(1, "employees");
                statement.setString(2, id.toString());
                statement.setString(3, "age");
                statement.setString(4, type);
                statement.setObject(5, integer);
                statement.executeUpdate();
            }
        }
    }

    /**
     * Creates a complete stored record.
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
        return new StoredRecord(store, id, CREATED_AT, revision, fields);
    }

    /**
     * Creates an isolated H2 database URL.
     *
     * @return JDBC URL
     */
    private static String url() {
        return "jdbc:h2:mem:"
            + UUID.randomUUID().toString().replace("-", "")
            + ";DB_CLOSE_DELAY=-1";
    }

    /**
     * Initializes JDBC persistence with the test schema.
     *
     * @param url JDBC URL
     * @return open persistence
     */
    private static JdbcPersistence open(final String url) {
        final JdbcPersistence persistence = new JdbcPersistence(
            url,
            new H2Dialect()
        );
        persistence.initialize(METADATA);
        return persistence;
    }

    /**
     * Returns test metadata without the last employee field.
     *
     * @return older compatible metadata
     */
    private static DatabaseMetadata withoutLastEmployeeField() {
        final List<StoreMetadata> stores = new ArrayList<>();
        for (final StoreMetadata store : METADATA.stores()) {
            if (store.name().equals("employees")) {
                stores.add(new StoreMetadata(
                    store.name(),
                    store.position(),
                    store.fields().subList(0, store.fields().size() - 1)
                ));
            } else {
                stores.add(store);
            }
        }
        return new DatabaseMetadata(METADATA.formatVersion(), stores);
    }

    /**
     * Deterministic creation time.
     */
    private static final Instant CREATED_AT =
        Instant.parse("2026-08-29T12:00:00Z");

    /**
     * Schema catalog shared by JDBC persistence tests.
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
            new StoreMetadata("departments", 1, List.of())
        )
    );
}
