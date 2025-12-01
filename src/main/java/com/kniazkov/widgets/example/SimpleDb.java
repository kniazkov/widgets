/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.common.HorizontalAlignment;
import com.kniazkov.widgets.db.Field;
import com.kniazkov.widgets.db.JsonStore;
import com.kniazkov.widgets.db.Record;
import com.kniazkov.widgets.db.Store;
import com.kniazkov.widgets.db.Type;
import com.kniazkov.widgets.model.IntegerToStringModel;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Cell;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.Row;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.Table;
import com.kniazkov.widgets.view.TextWidget;
import java.io.File;
import java.util.Arrays;

/**
 * A minimal demonstration program showcasing the usage of the primitive database,
 * JSON persistence, and UI components working together in a simple editable table.
 *
 * <p>
 * The application:
 * <ul>
 *     <li>loads a small database from a JSON file,</li>
 *     <li>displays all records as editable rows in a table,</li>
 *     <li>allows creating new records dynamically,</li>
 *     <li>allows editing all fields directly in the browser,</li>
 *     <li>saves all changes back to the JSON file on demand.</li>
 * </ul>
 *
 * <p>
 * Each row in the UI corresponds to one {@link Record}. The models stored
 * inside the record are directly bound to input widgets, so any modifications in the browser
 * update the underlying database models immediately.
 *
 * <b>How to use</b>
 * <ol>
 *   <li>Run the program;</li>
 *   <li>
 *     Open your browser and go to
 *     <a href="http://localhost:8000">http://localhost:8000</a>.
 *   </li>
 *   <li>Edit the cells or click “Create record” to add new rows;</li>
 *   <li>Press “Save” to write all records to {@code database.json}.</li>
 * </ol>
 *
 * <p>
 * This example is intentionally minimal:
 * it demonstrates the interaction between records, models, stores, and widgets
 * without additional abstractions, validation logic, or styling.
 */
public class SimpleDb {
    static final Field<String> name = new Field<>(Type.STRING, "name");
    static final Field<Integer> age = new Field<>(Type.POSITIVE_INTEGER, "age");
    static final Store store = JsonStore.load(new File("database.json"), Arrays.asList(
        name,
        age
    ));

    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Page page = (root, parameters) -> {
            final Table table = new Table();
            root.add(table);

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

            for(final Record record : store.getRecordsChronological()) {
                createRowFromRecord(table, record);
            }

            final Section buttonSection = new Section();
            root.add(buttonSection);
            final Button createRecord = new Button("Create record");
            buttonSection.add(createRecord);
            createRecord.onClick(evt -> {
                final Record record = store.createRecord();
                createRowFromRecord(table, record);
            });

            final Button save = new Button("Save");
            buttonSection.add(save);
            save.onClick(evt -> {
                store.save();
            });
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }

    private static void createRowFromRecord(final Table table, final Record record) {
        final Row row = new Row();
        table.add(row);
        Cell cell = row.getCell(0);
        Section section = new Section();
        cell.add(section);
        InputField field = new InputField();
        section.add(field);
        field.setTextModel(record.getModel(name));
        cell = row.getCell(1);
        section = new Section();
        cell.add(section);
        field = new InputField();
        section.add(field);
        field.setTextModel(new IntegerToStringModel(record.getModel(age)));
        cell = row.getCell(2);
        section = new Section();
        cell.add(section);
        final Button button = new Button("Save");
        section.add(button);
        button.onClick(data -> {
            record.save();
        });
    }
}
