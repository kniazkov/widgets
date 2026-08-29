/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence.jdbc;

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
import com.kniazkov.widgets.db.persistence.StoredValue.Kind;
import com.kniazkov.widgets.db.persistence.StoredValue.RealValue;
import com.kniazkov.widgets.db.persistence.StoredValue.StringValue;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Transactional persistence implemented with only the JDK JDBC API.
 * The application supplies an H2, SQLite, or compatible JDBC driver at runtime.
 */
public final class JdbcPersistence implements Persistence {
    /**
     * Format version loading query.
     */
    private static final String SELECT_METADATA =
        "SELECT format_version FROM db_metadata WHERE metadata_id = 1";

    /**
     * Store metadata loading query.
     */
    private static final String SELECT_STORES =
        "SELECT store_name, store_order FROM db_store ORDER BY store_order";

    /**
     * Field metadata loading query.
     */
    private static final String SELECT_FIELD_DEFINITIONS =
        "SELECT fd.store_name, fd.field_name, fd.field_order, "
            + "fd.type_name, fd.value_kind, fd.default_string, "
            + "fd.default_integer, fd.default_real, fd.default_boolean, "
            + "fd.referenced_store "
            + "FROM db_field_definition fd JOIN db_store st "
            + "ON fd.store_name = st.store_name "
            + "ORDER BY st.store_order, fd.field_order";

    /**
     * Format version insertion command.
     */
    private static final String INSERT_METADATA =
        "INSERT INTO db_metadata (metadata_id, format_version) VALUES (1, ?)";

    /**
     * Store metadata insertion command.
     */
    private static final String INSERT_STORE =
        "INSERT INTO db_store (store_name, store_order) VALUES (?, ?)";

    /**
     * Field metadata insertion command.
     */
    private static final String INSERT_FIELD_DEFINITION =
        "INSERT INTO db_field_definition "
            + "(store_name, field_name, field_order, type_name, value_kind, "
            + "default_string, default_integer, default_real, "
            + "default_boolean, referenced_store) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /**
     * Record loading query.
     */
    private static final String SELECT_RECORDS =
        "SELECT store_name, record_id, created_at, revision FROM db_record";

    /**
     * Field loading query.
     */
    private static final String SELECT_FIELDS =
        "SELECT store_name, record_id, field_name, value_type, string_value, "
            + "integer_value, real_value, boolean_value FROM db_field";

    /**
     * Record deletion command.
     */
    private static final String DELETE_RECORD =
        "DELETE FROM db_record WHERE store_name = ? AND record_id = ?";

    /**
     * Field deletion command.
     */
    private static final String DELETE_FIELDS =
        "DELETE FROM db_field WHERE store_name = ? AND record_id = ?";

    /**
     * Record insertion command.
     */
    private static final String INSERT_RECORD =
        "INSERT INTO db_record "
            + "(store_name, record_id, created_at, revision) VALUES (?, ?, ?, ?)";

    /**
     * Field insertion command.
     */
    private static final String INSERT_FIELD =
        "INSERT INTO db_field "
            + "(store_name, record_id, field_name, value_type, string_value, "
            + "integer_value, real_value, boolean_value) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    /**
     * JDBC connection.
     */
    private final Connection connection;

    /**
     * Validated database metadata.
     */
    private DatabaseMetadata metadata;

    /**
     * Creates and initializes a backend.
     *
     * @param jdbcUrl JDBC URL
     * @param dialect SQL dialect
     */
    public JdbcPersistence(
        final String jdbcUrl,
        final JdbcDialect dialect
    ) {
        try {
            this.connection = DriverManager.getConnection(jdbcUrl);
            this.initialize(Objects.requireNonNull(dialect, "dialect"));
        } catch (final SQLException err) {
            throw new PersistenceException(
                "Cannot open JDBC database " + jdbcUrl,
                err
            );
        }
    }

    /**
     * Creates and initializes a backend using an existing connection.
     *
     * @param connection connection
     * @param dialect SQL dialect
     */
    public JdbcPersistence(
        final Connection connection,
        final JdbcDialect dialect
    ) {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.initialize(Objects.requireNonNull(dialect, "dialect"));
    }

    /**
     * Creates persistence tables.
     *
     * @param dialect SQL dialect
     */
    private void initialize(final JdbcDialect dialect) {
        try (Statement statement = this.connection.createStatement()) {
            for (final String sql : dialect.initializationSql()) {
                statement.execute(sql);
            }
        } catch (final SQLException err) {
            throw new PersistenceException("Cannot initialize JDBC schema", err);
        }
    }

    @Override
    public synchronized void initialize(final DatabaseMetadata value) {
        final DatabaseMetadata expected = Objects.requireNonNull(
            value,
            "metadata"
        );
        final DatabaseMetadata stored = this.readMetadata();
        if (stored == null) {
            this.writeMetadata(expected);
        } else if (!stored.equals(expected)) {
            throw new PersistenceException(
                "JDBC database metadata does not match configured schemas"
            );
        }
        this.metadata = expected;
    }

    /**
     * Reads the persisted schema catalog.
     *
     * @return metadata, or {@code null} when the catalog is empty
     */
    private DatabaseMetadata readMetadata() {
        final Integer formatVersion;
        try (
            Statement statement = this.connection.createStatement();
            ResultSet result = statement.executeQuery(SELECT_METADATA)
        ) {
            formatVersion = result.next() ? result.getInt(1) : null;
        } catch (final SQLException err) {
            throw new PersistenceException("Cannot load JDBC metadata", err);
        }
        if (formatVersion == null) {
            return null;
        }
        final Map<String, MutableStoreMetadata> stores = new LinkedHashMap<>();
        try (
            Statement statement = this.connection.createStatement();
            ResultSet result = statement.executeQuery(SELECT_STORES)
        ) {
            while (result.next()) {
                final String name = result.getString(1);
                stores.put(name, new MutableStoreMetadata(
                    name,
                    result.getInt(2)
                ));
            }
        } catch (final SQLException err) {
            throw new PersistenceException("Cannot load JDBC store metadata", err);
        }
        try (
            Statement statement = this.connection.createStatement();
            ResultSet result = statement.executeQuery(SELECT_FIELD_DEFINITIONS)
        ) {
            while (result.next()) {
                final MutableStoreMetadata store = stores.get(
                    result.getString(1)
                );
                if (store == null) {
                    throw new PersistenceException(
                        "Field metadata refers to an unknown store"
                    );
                }
                store.fields.add(new FieldMetadata(
                    result.getString(2),
                    result.getString(4),
                    Kind.valueOf(result.getString(5)),
                    readValue(result, 5, 6),
                    result.getInt(3),
                    result.getString(10)
                ));
            }
        } catch (final SQLException | IllegalArgumentException err) {
            throw new PersistenceException("Cannot load JDBC field metadata", err);
        }
        final List<StoreMetadata> result = new ArrayList<>();
        for (final MutableStoreMetadata store : stores.values()) {
            result.add(store.freeze());
        }
        try {
            return new DatabaseMetadata(formatVersion, result);
        } catch (final IllegalArgumentException err) {
            throw new PersistenceException("Invalid JDBC database metadata", err);
        }
    }

    /**
     * Writes the complete schema catalog in one transaction.
     *
     * @param value metadata
     */
    private void writeMetadata(final DatabaseMetadata value) {
        final boolean autoCommit;
        try {
            autoCommit = this.connection.getAutoCommit();
        } catch (final SQLException err) {
            throw new PersistenceException(
                "Cannot inspect JDBC auto-commit mode",
                err
            );
        }
        SQLException failure = null;
        try {
            this.connection.setAutoCommit(false);
            try (PreparedStatement statement =
                this.connection.prepareStatement(INSERT_METADATA)) {
                statement.setInt(1, value.formatVersion());
                statement.executeUpdate();
            }
            try (PreparedStatement statement =
                this.connection.prepareStatement(INSERT_STORE)) {
                for (final StoreMetadata store : value.stores()) {
                    statement.setString(1, store.name());
                    statement.setInt(2, store.position());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            try (PreparedStatement statement = this.connection.prepareStatement(
                INSERT_FIELD_DEFINITION
            )) {
                for (final StoreMetadata store : value.stores()) {
                    for (final FieldMetadata field : store.fields()) {
                        statement.setString(1, store.name());
                        statement.setString(2, field.name());
                        statement.setInt(3, field.position());
                        statement.setString(4, field.type());
                        statement.setString(5, field.valueKind().name());
                        writeDefaultValue(statement, field.defaultValue());
                        if (field.referencedStore() == null) {
                            statement.setNull(10, Types.VARCHAR);
                        } else {
                            statement.setString(10, field.referencedStore());
                        }
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
            }
            this.connection.commit();
        } catch (final SQLException err) {
            failure = err;
            this.rollback(err);
        } finally {
            try {
                this.connection.setAutoCommit(autoCommit);
            } catch (final SQLException err) {
                if (failure == null) {
                    throw new PersistenceException(
                        "Cannot restore JDBC auto-commit mode",
                        err
                    );
                }
                failure.addSuppressed(err);
            }
        }
        if (failure != null) {
            throw new PersistenceException("Cannot write JDBC metadata", failure);
        }
    }

    @Override
    public synchronized DatabaseSnapshot load() {
        this.requireInitialized();
        final Map<String, MutableRecord> records = new LinkedHashMap<>();
        try (
            Statement statement = this.connection.createStatement();
            ResultSet result = statement.executeQuery(SELECT_RECORDS)
        ) {
            while (result.next()) {
                final MutableRecord record = new MutableRecord(
                    result.getString(1),
                    UUID.fromString(result.getString(2)),
                    Instant.parse(result.getString(3)),
                    result.getLong(4)
                );
                records.put(key(record.store, record.id), record);
            }
        } catch (final SQLException | IllegalArgumentException err) {
            throw new PersistenceException("Cannot load JDBC records", err);
        }
        try (
            Statement statement = this.connection.createStatement();
            ResultSet result = statement.executeQuery(SELECT_FIELDS)
        ) {
            while (result.next()) {
                final String store = result.getString(1);
                final UUID id = UUID.fromString(result.getString(2));
                final MutableRecord record = records.get(key(store, id));
                if (record == null) {
                    throw new PersistenceException(
                        "Field row refers to missing record " + id
                    );
                }
                record.fields.put(
                    result.getString(3),
                    readValue(result, 4, 5)
                );
            }
        } catch (final SQLException | IllegalArgumentException err) {
            throw new PersistenceException("Cannot load JDBC fields", err);
        }
        final List<StoredRecord> snapshot = new ArrayList<>();
        for (final MutableRecord record : records.values()) {
            snapshot.add(record.freeze());
        }
        return new DatabaseSnapshot(snapshot);
    }

    @Override
    public synchronized void commit(final ChangeSet changes) {
        this.requireInitialized();
        final boolean autoCommit;
        try {
            autoCommit = this.connection.getAutoCommit();
        } catch (final SQLException err) {
            throw new PersistenceException(
                "Cannot inspect JDBC auto-commit mode",
                err
            );
        }
        SQLException failure = null;
        try {
            this.connection.setAutoCommit(false);
            for (final ChangeSet.Mutation mutation : changes.getMutations()) {
                if (mutation instanceof ChangeSet.Upsert upsert) {
                    this.upsert(upsert.record());
                } else if (mutation instanceof ChangeSet.Delete delete) {
                    this.delete(delete.store(), delete.id());
                }
            }
            this.connection.commit();
        } catch (final SQLException err) {
            failure = err;
            this.rollback(err);
        } finally {
            try {
                this.connection.setAutoCommit(autoCommit);
            } catch (final SQLException err) {
                if (failure == null) {
                    throw new PersistenceException(
                        "Cannot restore JDBC auto-commit mode",
                        err
                    );
                }
                failure.addSuppressed(err);
            }
        }
        if (failure != null) {
            throw new PersistenceException("Cannot commit JDBC changes", failure);
        }
    }

    /**
     * Verifies that schema metadata was initialized.
     */
    private void requireInitialized() {
        if (this.metadata == null) {
            throw new IllegalStateException(
                "JDBC persistence metadata is not initialized"
            );
        }
    }

    /**
     * Inserts or replaces a complete record.
     *
     * @param record record
     * @throws SQLException when persistence fails
     */
    private void upsert(final StoredRecord record) throws SQLException {
        this.delete(record.getStore(), record.getId());
        try (PreparedStatement statement = this.connection.prepareStatement(INSERT_RECORD)) {
            statement.setString(1, record.getStore());
            statement.setString(2, record.getId().toString());
            statement.setString(3, record.getCreatedAt().toString());
            statement.setLong(4, record.getRevision());
            statement.executeUpdate();
        }
        try (PreparedStatement statement = this.connection.prepareStatement(INSERT_FIELD)) {
            for (final Map.Entry<String, StoredValue> field
                : record.getFields().entrySet()) {
                statement.setString(1, record.getStore());
                statement.setString(2, record.getId().toString());
                statement.setString(3, field.getKey());
                writeValue(statement, field.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    /**
     * Writes one typed field value to an insert statement.
     *
     * @param statement statement
     * @param value stored value
     * @throws SQLException when binding fails
     */
    private static void writeValue(
        final PreparedStatement statement,
        final StoredValue value
    ) throws SQLException {
        statement.setString(4, value.getKind().name());
        statement.setNull(5, Types.VARCHAR);
        statement.setNull(6, Types.INTEGER);
        statement.setNull(7, Types.DOUBLE);
        statement.setNull(8, Types.BOOLEAN);
        switch (value.getKind()) {
            case STRING -> statement.setString(5, value.getString());
            case INTEGER -> statement.setInt(6, value.getInteger());
            case REAL -> statement.setDouble(7, value.getReal());
            case BOOLEAN -> statement.setBoolean(8, value.getBoolean());
        }
    }

    /**
     * Writes one typed field default to metadata columns.
     *
     * @param statement statement
     * @param value stored default
     * @throws SQLException when binding fails
     */
    private static void writeDefaultValue(
        final PreparedStatement statement,
        final StoredValue value
    ) throws SQLException {
        statement.setNull(6, Types.VARCHAR);
        statement.setNull(7, Types.INTEGER);
        statement.setNull(8, Types.DOUBLE);
        statement.setNull(9, Types.BOOLEAN);
        switch (value.getKind()) {
            case STRING -> statement.setString(6, value.getString());
            case INTEGER -> statement.setInt(7, value.getInteger());
            case REAL -> statement.setDouble(8, value.getReal());
            case BOOLEAN -> statement.setBoolean(9, value.getBoolean());
        }
    }

    /**
     * Reads one typed field value from a result row.
     *
     * @param result result row
     * @param typeIndex type-tag column index
     * @param valueIndex first typed-value column index
     * @return stored value
     * @throws SQLException when the row is malformed
     */
    private static StoredValue readValue(
        final ResultSet result,
        final int typeIndex,
        final int valueIndex
    ) throws SQLException {
        final String type = result.getString(typeIndex);
        if (type == null) {
            throw new SQLException("Field value type is null");
        }
        final Kind kind;
        try {
            kind = Kind.valueOf(type);
        } catch (final IllegalArgumentException err) {
            throw new SQLException("Unknown field value type: " + type, err);
        }
        return switch (kind) {
            case STRING -> new StringValue(requiredString(result, valueIndex));
            case INTEGER -> new IntegerValue(requiredInteger(
                result,
                valueIndex + 1
            ));
            case REAL -> new RealValue(requiredReal(result, valueIndex + 2));
            case BOOLEAN -> new BooleanValue(requiredBoolean(
                result,
                valueIndex + 3
            ));
        };
    }

    /**
     * Reads a required string column.
     *
     * @param result result row
     * @param index column index
     * @return value
     * @throws SQLException when the column is null
     */
    private static String requiredString(
        final ResultSet result,
        final int index
    ) throws SQLException {
        final String value = result.getString(index);
        if (value == null) {
            throw new SQLException("Required string field value is null");
        }
        return value;
    }

    /**
     * Reads a required integer column.
     *
     * @param result result row
     * @param index column index
     * @return value
     * @throws SQLException when the column is null
     */
    private static int requiredInteger(
        final ResultSet result,
        final int index
    ) throws SQLException {
        final int value = result.getInt(index);
        if (result.wasNull()) {
            throw new SQLException("Required integer field value is null");
        }
        return value;
    }

    /**
     * Reads a required real column.
     *
     * @param result result row
     * @param index column index
     * @return value
     * @throws SQLException when the column is null
     */
    private static double requiredReal(
        final ResultSet result,
        final int index
    ) throws SQLException {
        final double value = result.getDouble(index);
        if (result.wasNull()) {
            throw new SQLException("Required real field value is null");
        }
        return value;
    }

    /**
     * Reads a required boolean column.
     *
     * @param result result row
     * @param index column index
     * @return value
     * @throws SQLException when the column is null
     */
    private static boolean requiredBoolean(
        final ResultSet result,
        final int index
    ) throws SQLException {
        final boolean value = result.getBoolean(index);
        if (result.wasNull()) {
            throw new SQLException("Required boolean field value is null");
        }
        return value;
    }

    /**
     * Deletes a complete record.
     *
     * @param store store name
     * @param id identifier
     * @throws SQLException when persistence fails
     */
    private void delete(final String store, final UUID id) throws SQLException {
        executeDelete(DELETE_FIELDS, store, id);
        executeDelete(DELETE_RECORD, store, id);
    }

    /**
     * Executes a two-key deletion.
     *
     * @param sql SQL command
     * @param store store name
     * @param id identifier
     * @throws SQLException when persistence fails
     */
    private void executeDelete(
        final String sql,
        final String store,
        final UUID id
    ) throws SQLException {
        try (PreparedStatement statement = this.connection.prepareStatement(sql)) {
            statement.setString(1, store);
            statement.setString(2, id.toString());
            statement.executeUpdate();
        }
    }

    /**
     * Attempts to roll back and attaches rollback failure to the original error.
     *
     * @param original original error
     */
    private void rollback(final SQLException original) {
        try {
            this.connection.rollback();
        } catch (final SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }

    /**
     * Creates a compound key.
     *
     * @param store store name
     * @param id identifier
     * @return key
     */
    private static String key(final String store, final UUID id) {
        return store + '\u0000' + id;
    }

    @Override
    public synchronized void close() {
        try {
            this.connection.close();
        } catch (final SQLException err) {
            throw new PersistenceException("Cannot close JDBC database", err);
        }
    }

    /**
     * Mutable record used while loading normalized JDBC rows.
     */
    private static final class MutableRecord {
        /**
         * Store name.
         */
        private final String store;

        /**
         * Identifier.
         */
        private final UUID id;

        /**
         * Creation time.
         */
        private final Instant createdAt;

        /**
         * Revision.
         */
        private final long revision;

        /**
         * Typed field values.
         */
        private final Map<String, StoredValue> fields;

        /**
         * Creates a loading record.
         *
         * @param store store name
         * @param id identifier
         * @param createdAt creation time
         * @param revision revision
         */
        private MutableRecord(
            final String store,
            final UUID id,
            final Instant createdAt,
            final long revision
        ) {
            this.store = store;
            this.id = id;
            this.createdAt = createdAt;
            this.revision = revision;
            this.fields = new LinkedHashMap<>();
        }

        /**
         * Creates an immutable stored record.
         *
         * @return stored record
         */
        private StoredRecord freeze() {
            return new StoredRecord(
                this.store,
                this.id,
                this.createdAt,
                this.revision,
                this.fields
            );
        }
    }

    /**
     * Mutable store metadata used while loading the JDBC catalog.
     */
    private static final class MutableStoreMetadata {
        /**
         * Store name.
         */
        private final String name;

        /**
         * Store position.
         */
        private final int position;

        /**
         * Field metadata.
         */
        private final List<FieldMetadata> fields;

        /**
         * Creates mutable store metadata.
         *
         * @param name store name
         * @param position store position
         */
        private MutableStoreMetadata(final String name, final int position) {
            this.name = name;
            this.position = position;
            this.fields = new ArrayList<>();
        }

        /**
         * Creates immutable store metadata.
         *
         * @return store metadata
         */
        private StoreMetadata freeze() {
            return new StoreMetadata(this.name, this.position, this.fields);
        }
    }
}
