/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.common.HorizontalAlignment;
import com.kniazkov.widgets.db.DataRecord;
import com.kniazkov.widgets.db.Database;
import com.kniazkov.widgets.db.Draft;
import com.kniazkov.widgets.db.Field;
import com.kniazkov.widgets.db.LiveRecordSet;
import com.kniazkov.widgets.db.RecordChange;
import com.kniazkov.widgets.db.Schema;
import com.kniazkov.widgets.db.Store;
import com.kniazkov.widgets.db.ValueType;
import com.kniazkov.widgets.db.persistence.json.JsonPersistence;
import com.kniazkov.widgets.db.query.Query;
import com.kniazkov.widgets.model.IntegerToStringModel;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Cell;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.Row;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.Table;
import com.kniazkov.widgets.view.TextWidget;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demonstrates shared reactive records with JSON persistence.
 */
public final class SimpleDb {
    /**
     * Employee name field.
     */
    private static final Field<String> NAME =
        new Field<>(ValueType.STRING, "name");

    /**
     * Employee age field.
     */
    private static final Field<Integer> AGE =
        new Field<>(ValueType.POSITIVE_INTEGER, "age");

    /**
     * Example database.
     */
    private static final Database DATABASE = Database.builder()
        .persistence(new JsonPersistence(Paths.get("database")))
        .store("employee", Schema.of(NAME, AGE))
        .build();

    /**
     * Employee store.
     */
    private static final Store EMPLOYEES = DATABASE.getStore("employee");

    /**
     * Utility class.
     */
    private SimpleDb() {
    }

    /**
     * Starts the example.
     *
     * @param args program arguments
     */
    public static void main(final String[] args) {
        final Page page = (root, parameters) -> {
            final Table table = new Table();
            root.add(table);
            createHeader(table);

            final Map<UUID, Row> rows = new ConcurrentHashMap<>();
            final LiveRecordSet records = EMPLOYEES.query(Query.all());
            for (final DataRecord record : records.getRecords()) {
                rows.put(record.getId(), createRow(table, record));
            }
            records.subscribe(change -> updateRows(table, rows, change));

            final Section buttons = new Section();
            root.add(buttons);
            final Button create = new Button("Create record");
            buttons.add(create);
            create.onClick(event -> {
                final Draft draft = EMPLOYEES.createDraft();
                draft.model(NAME);
                draft.model(AGE);
                draft.commit();
            });
        };

        final Application application = new Application(page);
        final Options options = new Options.Builder().build();
        Server.start(application, options);
    }

    /**
     * Creates the table header.
     *
     * @param table table
     */
    private static void createHeader(final Table table) {
        final Row header = new Row();
        table.add(header);
        header.getCell(0).createText("Name");
        header.getCell(1).createText("Age");
        for (final Section section : header.collectChildren(Section.class)) {
            section.setHorizontalAlignment(HorizontalAlignment.CENTER);
        }
        for (final TextWidget text : header.collectChildren(TextWidget.class)) {
            text.setFontWeight(FontWeight.BOLD);
        }
    }

    /**
     * Applies a live record-set change to one page table.
     *
     * @param table table
     * @param rows rows by record identifier
     * @param change change
     */
    private static void updateRows(
        final Table table,
        final Map<UUID, Row> rows,
        final RecordChange change
    ) {
        if (change.kind() == RecordChange.Kind.ADDED) {
            rows.computeIfAbsent(
                change.record().getId(),
                ignored -> createRow(table, change.record())
            );
        } else if (change.kind() == RecordChange.Kind.REMOVED) {
            final Row row = rows.remove(change.record().getId());
            if (row != null) {
                table.remove(row);
            }
        }
    }

    /**
     * Creates an editable row bound directly to record models.
     *
     * @param table table
     * @param record record
     * @return row
     */
    private static Row createRow(final Table table, final DataRecord record) {
        final Row row = new Row();
        table.add(row);

        Cell cell = row.getCell(0);
        Section section = new Section();
        cell.add(section);
        InputField input = new InputField();
        section.add(input);
        input.setTextModel(record.model(NAME));

        cell = row.getCell(1);
        section = new Section();
        cell.add(section);
        input = new InputField();
        section.add(input);
        input.setTextModel(new IntegerToStringModel(record.model(AGE)));

        cell = row.getCell(2);
        section = new Section();
        cell.add(section);
        final Button remove = new Button("Remove");
        section.add(remove);
        remove.onClick(event -> record.remove());
        return row;
    }
}
