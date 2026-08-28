/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence.jdbc;

import com.kniazkov.widgets.db.persistence.ChangeSet;
import com.kniazkov.widgets.db.persistence.DatabaseSnapshot;
import com.kniazkov.widgets.db.persistence.Persistence;
import com.kniazkov.widgets.db.persistence.PersistenceException;
import com.kniazkov.widgets.db.persistence.StoredRecord;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
     * Record loading query.
     */
    private static final String SELECT_RECORDS =
        "SELECT store_name, record_id, created_at, revision FROM widgets_record";

    /**
     * Field loading query.
     */
    private static final String SELECT_FIELDS =
        "SELECT store_name, record_id, field_name, field_value FROM widgets_field";

    /**
     * Record deletion command.
     */
    private static final String DELETE_RECORD =
        "DELETE FROM widgets_record WHERE store_name = ? AND record_id = ?";

    /**
     * Field deletion command.
     */
    private static final String DELETE_FIELDS =
        "DELETE FROM widgets_field WHERE store_name = ? AND record_id = ?";

    /**
     * Record insertion command.
     */
    private static final String INSERT_RECORD =
        "INSERT INTO widgets_record "
            + "(store_name, record_id, created_at, revision) VALUES (?, ?, ?, ?)";

    /**
     * Field insertion command.
     */
    private static final String INSERT_FIELD =
        "INSERT INTO widgets_field "
            + "(store_name, record_id, field_name, field_value) VALUES (?, ?, ?, ?)";

    /**
     * JDBC connection.
     */
    private final Connection connection;

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
    public synchronized DatabaseSnapshot load() {
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
                record.fields.put(result.getString(3), result.getString(4));
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
            for (final Map.Entry<String, String> field
                : record.getFields().entrySet()) {
                statement.setString(1, record.getStore());
                statement.setString(2, record.getId().toString());
                statement.setString(3, field.getKey());
                statement.setString(4, field.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
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
         * Encoded field values.
         */
        private final Map<String, String> fields;

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
}
