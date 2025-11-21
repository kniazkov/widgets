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
import com.kniazkov.widgets.db.IntegerField;
import com.kniazkov.widgets.db.Record;
import com.kniazkov.widgets.db.Store;
import com.kniazkov.widgets.db.StringField;
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
 * .....
 *
 * <b>How to use</b>
 * <ol>
 *   <li>Run the program;</li>
 *   <li>
 *     Open your browser and go to
 *     <a href="http://localhost:8000">http://localhost:8000</a>.
 *   </li>
 * </ol>
 */
public class SimpleDb {
    static final StringField NAME = new StringField("name");
    static final IntegerField AGE = new IntegerField("age");
    static final Store STORE = Store.load(new File("database.json"), Arrays.asList(
        NAME,
        AGE
    ));
    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Page page = root -> {
            final Table table = new Table();
            root.add(table);

            final Row header = new Row();
            table.add(header);
            header.getCell(0).setText("Name");
            header.getCell(1).setText("Age");
            for (final Section section : header.collectChildren(Section.class)) {
                section.setHorizontalAlignment(HorizontalAlignment.CENTER);
            }
            for (final TextWidget text : header.collectChildren(TextWidget.class)) {
                text.setFontWeight(FontWeight.BOLD);
            }

            for(final Record record : STORE.getAllRecords()) {
                final Row row = new Row();
                table.add(row);
                Cell cell = row.getCell(0);
                Section section = new Section();
                cell.add(section);
                InputField field = new InputField();
                section.add(field);
                field.setTextModel(record.getModel(NAME));
                cell = row.getCell(1);
                section = new Section();
                cell.add(section);
                field = new InputField();
                section.add(field);
                field.setTextModel(new IntegerToStringModel(record.getModel(AGE)));
            }

            final Section buttonSection = new Section();
            root.add(buttonSection);
            final Button createRecord = new Button("Create record");
            buttonSection.add(createRecord);
            createRecord.onClick(evt -> {
                final Record record = STORE.createRecord();
                final Row row = new Row();
                table.add(row);
                Cell cell = row.getCell(0);
                Section section = new Section();
                cell.add(section);
                InputField field = new InputField();
                section.add(field);
                field.setTextModel(record.getModel(NAME));
                cell = row.getCell(1);
                section = new Section();
                cell.add(section);
                field = new InputField();
                section.add(field);
                field.setTextModel(new IntegerToStringModel(record.getModel(AGE)));
            });

            final Button save = new Button("Save");
            buttonSection.add(save);
            save.onClick(evt -> {
                STORE.save();
            });
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
