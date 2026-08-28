/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence.jdbc;

import java.util.List;

/**
 * Supplies the small amount of DDL that differs between embedded SQL engines.
 */
public interface JdbcDialect {
    /**
     * Returns idempotent schema initialization statements.
     *
     * @return SQL statements
     */
    List<String> initializationSql();
}
