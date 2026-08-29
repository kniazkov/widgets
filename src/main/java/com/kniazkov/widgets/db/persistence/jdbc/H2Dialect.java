/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence.jdbc;

import java.util.List;

/**
 * H2 schema dialect.
 */
public final class H2Dialect implements JdbcDialect {
    @Override
    public List<String> initializationSql() {
        return List.of(
            "CREATE TABLE IF NOT EXISTS widgets_record ("
                + "store_name VARCHAR(255) NOT NULL, "
                + "record_id VARCHAR(36) NOT NULL, "
                + "created_at VARCHAR(40) NOT NULL, "
                + "revision BIGINT NOT NULL, "
                + "PRIMARY KEY (store_name, record_id))",
            "CREATE TABLE IF NOT EXISTS widgets_field ("
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
