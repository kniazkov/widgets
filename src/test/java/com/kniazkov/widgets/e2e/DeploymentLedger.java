/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.e2e;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Test-only durable business counter: one committed row is one operation.
 * Two colocated JVMs use SQLite to demonstrate persistence, not distributed UI storage.
 */
public final class DeploymentLedger {
    /**
     * Shared local database URL, never a network filesystem in this fixture.
     */
    private final String url;

    /**
     * @param file durable SQLite file shared by the fixture processes
     */
    public DeploymentLedger(final String file) {
        this.url = "jdbc:sqlite:" + file;
        try (Connection connection = this.connect(); var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS commands (id TEXT PRIMARY KEY)");
        } catch (final SQLException error) {
            throw new IllegalStateException(error);
        }
    }

    /**
     * @param key application idempotency key, independent of UI event IDs
     * @return whether the business operation was committed for the first time
     */
    public boolean apply(final String key) {
        try (Connection connection = this.connect(); var statement = connection.prepareStatement(
                "INSERT OR IGNORE INTO commands (id) VALUES (?)")) {
            statement.setString(1, key);
            return statement.executeUpdate() == 1;
        } catch (final SQLException error) {
            throw new IllegalStateException(error);
        }
    }

    /**
     * @return count reconstructed from durable committed business operations
     */
    public int count() {
        try (Connection connection = this.connect(); var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT COUNT(*) FROM commands")) {
            if (!result.next()) {
                throw new IllegalStateException("Missing aggregate row");
            }
            return result.getInt(1);
        } catch (final SQLException error) {
            throw new IllegalStateException(error);
        }
    }

    /**
     * @return short-lived connection with bounded contention waiting
     * @throws SQLException if the durable store cannot be opened
     */
    private Connection connect() throws SQLException {
        final Connection connection = DriverManager.getConnection(this.url);
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = 5000");
        } catch (final SQLException error) {
            connection.close();
            throw error;
        }
        return connection;
    }
}
