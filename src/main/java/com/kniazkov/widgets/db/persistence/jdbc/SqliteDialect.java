/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence.jdbc;

import java.util.List;

/**
 * SQLite schema dialect.
 */
public final class SqliteDialect implements JdbcDialect {
    @Override
    public List<String> initializationSql() {
        return List.of(
            "CREATE TABLE IF NOT EXISTS widgets_record ("
                + "store_name TEXT NOT NULL, "
                + "record_id TEXT NOT NULL, "
                + "created_at TEXT NOT NULL, "
                + "revision INTEGER NOT NULL, "
                + "PRIMARY KEY (store_name, record_id))",
            "CREATE TABLE IF NOT EXISTS widgets_field ("
                + "store_name TEXT NOT NULL, "
                + "record_id TEXT NOT NULL, "
                + "field_name TEXT NOT NULL, "
                + "field_value TEXT NOT NULL, "
                + "PRIMARY KEY (store_name, record_id, field_name))"
        );
    }
}
