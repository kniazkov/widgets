/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence.jdbc;

import java.util.List;

/**
 * SQLite schema dialect.
 */
public final class SqliteDialect implements JdbcDialect {
    /**
     * Creates a SQLite dialect.
     */
    public SqliteDialect() {
    }

    @Override
    public List<String> initializationSql() {
        return List.of(
            "CREATE TABLE IF NOT EXISTS db_metadata ("
                + "metadata_id INTEGER NOT NULL PRIMARY KEY "
                + "CHECK (metadata_id = 1), "
                + "format_version INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS db_store ("
                + "store_name TEXT NOT NULL PRIMARY KEY, "
                + "store_order INTEGER NOT NULL UNIQUE)",
            "CREATE TABLE IF NOT EXISTS db_field_definition ("
                + "store_name TEXT NOT NULL, "
                + "field_name TEXT NOT NULL, "
                + "field_order INTEGER NOT NULL, "
                + "type_name TEXT NOT NULL, "
                + "value_kind TEXT NOT NULL, "
                + "default_string TEXT, "
                + "default_integer INTEGER, "
                + "default_real REAL, "
                + "default_boolean INTEGER, "
                + "referenced_store TEXT, "
                + "PRIMARY KEY (store_name, field_name), "
                + "UNIQUE (store_name, field_order))",
            "CREATE TABLE IF NOT EXISTS db_record ("
                + "store_name TEXT NOT NULL, "
                + "record_id TEXT NOT NULL, "
                + "created_at TEXT NOT NULL, "
                + "revision INTEGER NOT NULL, "
                + "PRIMARY KEY (store_name, record_id))",
            "CREATE TABLE IF NOT EXISTS db_field ("
                + "store_name TEXT NOT NULL, "
                + "record_id TEXT NOT NULL, "
                + "field_name TEXT NOT NULL, "
                + "value_type TEXT NOT NULL, "
                + "string_value TEXT, "
                + "integer_value INTEGER, "
                + "real_value REAL, "
                + "boolean_value INTEGER, "
                + "PRIMARY KEY (store_name, record_id, field_name))"
        );
    }
}
