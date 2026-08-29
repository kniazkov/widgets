/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence.jdbc;

import java.util.List;

/**
 * H2 schema dialect.
 */
public final class H2Dialect implements JdbcDialect {
    /**
     * Creates an H2 dialect.
     */
    public H2Dialect() {
    }

    @Override
    public List<String> initializationSql() {
        return List.of(
            "CREATE TABLE IF NOT EXISTS db_metadata ("
                + "metadata_id INTEGER NOT NULL PRIMARY KEY "
                + "CHECK (metadata_id = 1), "
                + "format_version INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS db_store ("
                + "store_name VARCHAR(255) NOT NULL PRIMARY KEY, "
                + "store_order INTEGER NOT NULL UNIQUE)",
            "CREATE TABLE IF NOT EXISTS db_field_definition ("
                + "store_name VARCHAR(255) NOT NULL, "
                + "field_name VARCHAR(255) NOT NULL, "
                + "field_order INTEGER NOT NULL, "
                + "type_name VARCHAR(255) NOT NULL, "
                + "value_kind VARCHAR(16) NOT NULL, "
                + "default_string CLOB, "
                + "default_integer INTEGER, "
                + "default_real DOUBLE PRECISION, "
                + "default_boolean BOOLEAN, "
                + "referenced_store VARCHAR(255), "
                + "PRIMARY KEY (store_name, field_name), "
                + "UNIQUE (store_name, field_order))",
            "CREATE TABLE IF NOT EXISTS db_record ("
                + "store_name VARCHAR(255) NOT NULL, "
                + "record_id VARCHAR(36) NOT NULL, "
                + "created_at VARCHAR(40) NOT NULL, "
                + "revision BIGINT NOT NULL, "
                + "PRIMARY KEY (store_name, record_id))",
            "CREATE TABLE IF NOT EXISTS db_field ("
                + "store_name VARCHAR(255) NOT NULL, "
                + "record_id VARCHAR(36) NOT NULL, "
                + "field_name VARCHAR(255) NOT NULL, "
                + "value_type VARCHAR(16) NOT NULL, "
                + "string_value CLOB, "
                + "integer_value INTEGER, "
                + "real_value DOUBLE PRECISION, "
                + "boolean_value BOOLEAN, "
                + "PRIMARY KEY (store_name, record_id, field_name))"
        );
    }
}
