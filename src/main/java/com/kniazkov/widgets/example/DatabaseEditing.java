/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.db.ConflictException;
import com.kniazkov.widgets.db.DataRecord;
import com.kniazkov.widgets.db.Database;
import com.kniazkov.widgets.db.Draft;
import com.kniazkov.widgets.db.Field;
import com.kniazkov.widgets.db.Schema;
import com.kniazkov.widgets.db.Store;
import com.kniazkov.widgets.db.ValueType;
import com.kniazkov.widgets.model.IntegerToStringModel;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.RootWidget;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

/**
 * Demonstrates isolated multi-field editing with an atomic commit.
 */
public final class DatabaseEditing {
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
     * Example database.
     */
    private static final Database DATABASE = Database.builder()
        .store("employees", Schema.of(NAME, AGE))
        .build();

    /**
     * Employee store.
     */
    private static final Store EMPLOYEES = DATABASE.getStore("employees");

    /**
     * Record shared by every open page.
     */
    private static final DataRecord EMPLOYEE = createEmployee();

    /**
     * Utility class.
     */
    private DatabaseEditing() {
    }

    /**
     * Starts the example.
     *
     * @param args program arguments
     */
    public static void main(final String[] args) {
        final Page page = (root, parameters) -> {
            addTitle(root, "Canonical record shared by all pages");
            addCanonicalRecord(root);

            addTitle(root, "Private editing draft");
            addEditingForm(root);

            root.add(
                new Section(
                    new TextWidget(
                        "Open this page in two browsers: each browser gets its own draft."
                    )
                )
            );
        };

        Server.start(new Application(page), new Options.Builder().build());
    }

    /**
     * Creates the initial employee.
     *
     * @return committed employee record
     */
    private static DataRecord createEmployee() {
        final Draft draft = EMPLOYEES.createDraft();
        draft.model(NAME).setData("Alice");
        draft.model(AGE).setData(34);
        return draft.commit();
    }

    /**
     * Adds a title to the page.
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
     * Adds widgets bound to the canonical record.
     *
     * @param root page root
     */
    private static void addCanonicalRecord(final RootWidget root) {
        final TextWidget name = new TextWidget();
        name.setTextModel(EMPLOYEE.model(NAME));
        root.add(new Section(new TextWidget("Name: "), name));

        final TextWidget age = new TextWidget();
        age.setTextModel(new IntegerToStringModel(EMPLOYEE.model(AGE)));
        root.add(new Section(new TextWidget("Age: "), age));
    }

    /**
     * Adds a form bound to one isolated draft.
     *
     * @param root page root
     */
    private static void addEditingForm(final RootWidget root) {
        final Draft draft = EMPLOYEE.edit();

        final InputField name = new InputField();
        name.setTextModel(draft.model(NAME));
        root.add(new Section(new TextWidget("Name: "), name));

        final InputField age = new InputField();
        age.setTextModel(new IntegerToStringModel(draft.model(AGE)));
        root.add(new Section(new TextWidget("Age: "), age));

        final Button save = new Button("Save");
        final Button cancel = new Button("Cancel");
        final TextWidget status = new TextWidget("The draft has not been committed.");
        root.add(new Section(save, cancel, status));

        save.onClick(event -> {
            try {
                draft.commit();
                status.setText("Saved atomically. Every bound widget is now updated.");
                disableForm(name, age, save, cancel);
            } catch (final ConflictException exception) {
                status.setText("Conflict: the record was changed by another draft.");
                save.disable();
            }
        });
        cancel.onClick(event -> {
            draft.cancel();
            status.setText("Cancelled. The canonical record was not changed.");
            disableForm(name, age, save, cancel);
        });
    }

    /**
     * Disables a completed form.
     *
     * @param name name field
     * @param age age field
     * @param save save button
     * @param cancel cancel button
     */
    private static void disableForm(
        final InputField name,
        final InputField age,
        final Button save,
        final Button cancel
    ) {
        name.disable();
        age.disable();
        save.disable();
        cancel.disable();
    }
}
