/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.db.DataRecord;
import com.kniazkov.widgets.db.Database;
import com.kniazkov.widgets.db.Draft;
import com.kniazkov.widgets.db.Field;
import com.kniazkov.widgets.db.Schema;
import com.kniazkov.widgets.db.Store;
import com.kniazkov.widgets.db.ValueType;
import com.kniazkov.widgets.db.query.Query;
import com.kniazkov.widgets.view.RootWidget;
import com.kniazkov.widgets.view.Row;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.Table;
import com.kniazkov.widgets.view.TextWidget;

/**
 * Demonstrates filtering and ordering records in a store.
 */
public final class DatabaseSearch {
    /**
     * Employee name field.
     */
    private static final Field<String> NAME =
        new Field<>(ValueType.NOT_EMPTY_STRING, "name");

    /**
     * Employee age field.
     */
    private static final Field<Integer> AGE =
        new Field<>(ValueType.POSITIVE_INTEGER, "age");

    /**
     * Employee activity flag.
     */
    private static final Field<Boolean> ACTIVE =
        new Field<>(ValueType.BOOLEAN, "active");

    /**
     * Employee department field.
     */
    private static final Field<String> DEPARTMENT =
        new Field<>(ValueType.NOT_EMPTY_STRING, "department");

    /**
     * Example database.
     */
    private static final Database DATABASE = Database.builder()
        .store("employees", Schema.of(NAME, AGE, ACTIVE, DEPARTMENT))
        .build();

    /**
     * Employee store.
     */
    private static final Store EMPLOYEES = DATABASE.getStore("employees");

    static {
        addEmployee("Alice", 34, true, "Engineering");
        addEmployee("Bob", 27, true, "Sales");
        addEmployee("Carol", 41, false, "Engineering");
        addEmployee("Dave", 38, true, "Support");
        addEmployee("Eve", 29, false, "Sales");
    }

    /**
     * Utility class.
     */
    private DatabaseSearch() {
    }

    /**
     * Starts the example.
     *
     * @param args program arguments
     */
    public static void main(final String[] args) {
        final Page page = (root, parameters) -> {
            addTitle(root, "All employees, ordered by name");
            addResults(root, Query.all().orderBy(NAME.ascending()));

            addTitle(root, "Active employees older than 30");
            addResults(
                root,
                Query.where(AGE.greaterThan(30).and(ACTIVE.is(true)))
                    .orderBy(AGE.descending())
            );

            addTitle(root, "Engineering department or Bob");
            addResults(
                root,
                Query.where(DEPARTMENT.is("Engineering").or(NAME.is("Bob")))
                    .orderBy(DEPARTMENT.ascending())
                    .thenBy(NAME.ascending())
            );

            addTitle(root, "Inactive employees younger than 40");
            addResults(
                root,
                Query.where(ACTIVE.is(true).not().and(AGE.lessThan(40)))
                    .orderBy(NAME.ascending())
            );

            addTitle(root, "Everyone except Sales");
            addResults(
                root,
                Query.where(DEPARTMENT.isNot("Sales"))
                    .orderBy(DEPARTMENT.ascending())
                    .thenBy(NAME.ascending())
            );
        };

        Server.start(new Application(page), new Options.Builder().build());
    }

    /**
     * Adds an employee to the example store.
     *
     * @param name employee name
     * @param age employee age
     * @param active activity flag
     * @param department department name
     */
    private static void addEmployee(
        final String name,
        final int age,
        final boolean active,
        final String department
    ) {
        final Draft draft = EMPLOYEES.createDraft();
        draft.model(NAME).setData(name);
        draft.model(AGE).setData(age);
        draft.model(ACTIVE).setData(active);
        draft.model(DEPARTMENT).setData(department);
        draft.commit();
    }

    /**
     * Adds a query title.
     *
     * @param root page root
     * @param title title text
     */
    private static void addTitle(final RootWidget root, final String title) {
        final TextWidget text = new TextWidget(title);
        text.setFontWeight(FontWeight.BOLD);
        root.add(new Section(text));
    }

    /**
     * Adds a snapshot of query results.
     *
     * @param root page root
     * @param query query to execute
     */
    private static void addResults(final RootWidget root, final Query query) {
        final Table table = new Table();
        root.add(table);
        final Row header = new Row();
        table.add(header);
        header.getCell(0).createText("Name");
        header.getCell(1).createText("Age");
        header.getCell(2).createText("Active");
        header.getCell(3).createText("Department");

        for (final DataRecord record : EMPLOYEES.query(query).getRecords()) {
            final Row row = new Row();
            table.add(row);
            row.getCell(0).createText(record.model(NAME).getData());
            row.getCell(1).createText(record.model(AGE).getData().toString());
            row.getCell(2).createText(record.model(ACTIVE).getData().toString());
            row.getCell(3).createText(record.model(DEPARTMENT).getData());
        }
    }
}
