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
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.RootWidget;
import com.kniazkov.widgets.view.Row;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.Table;
import com.kniazkov.widgets.view.TextWidget;
import java.util.UUID;

/**
 * Demonstrates a relation between two stores through record identifiers.
 */
public final class DatabaseRelations {
    /**
     * Department name field.
     */
    private static final Field<String> DEPARTMENT_NAME =
        new Field<>(ValueType.NOT_EMPTY_STRING, "name");

    /**
     * Employee name field.
     */
    private static final Field<String> EMPLOYEE_NAME =
        new Field<>(ValueType.NOT_EMPTY_STRING, "name");

    /**
     * Identifier of an employee's department.
     */
    private static final Field<UUID> DEPARTMENT_ID =
        new Field<>(ValueType.IDENTIFIER, "departmentId", "departments");

    /**
     * Example database.
     */
    private static final Database DATABASE = Database.builder()
        .store("departments", Schema.of(DEPARTMENT_NAME))
        .store("employees", Schema.of(EMPLOYEE_NAME, DEPARTMENT_ID))
        .build();

    /**
     * Department store.
     */
    private static final Store DEPARTMENTS = DATABASE.getStore("departments");

    /**
     * Employee store.
     */
    private static final Store EMPLOYEES = DATABASE.getStore("employees");

    /**
     * Engineering department shared by several employees.
     */
    private static final DataRecord ENGINEERING = addDepartment("Engineering");

    /**
     * Sales department.
     */
    private static final DataRecord SALES = addDepartment("Sales");

    static {
        addEmployee("Alice", ENGINEERING.getId());
        addEmployee("Bob", SALES.getId());
        addEmployee("Carol", ENGINEERING.getId());
    }

    /**
     * Utility class.
     */
    private DatabaseRelations() {
    }

    /**
     * Starts the example.
     *
     * @param args program arguments
     */
    public static void main(final String[] args) {
        final Page page = (root, parameters) -> {
            addTitle(root, "Departments");
            root.add(
                new Section(
                    new TextWidget(
                        "Rename a department. Related employee rows update immediately."
                    )
                )
            );
            addDepartments(root);

            addTitle(root, "Employees");
            addEmployees(root);
        };

        Server.start(new Application(page), new Options.Builder().build());
    }

    /**
     * Adds a department.
     *
     * @param name department name
     * @return committed department record
     */
    private static DataRecord addDepartment(final String name) {
        final Draft draft = DEPARTMENTS.createDraft();
        draft.model(DEPARTMENT_NAME).setData(name);
        return draft.commit();
    }

    /**
     * Adds an employee referring to a department by UUID.
     *
     * @param name employee name
     * @param departmentId department identifier
     */
    private static void addEmployee(final String name, final UUID departmentId) {
        final Draft draft = EMPLOYEES.createDraft();
        draft.model(EMPLOYEE_NAME).setData(name);
        draft.model(DEPARTMENT_ID).setData(departmentId);
        draft.commit();
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
     * Adds editable department records.
     *
     * @param root page root
     */
    private static void addDepartments(final RootWidget root) {
        final Table table = new Table();
        root.add(table);
        final Row header = new Row();
        table.add(header);
        header.getCell(0).createText("Department ID");
        header.getCell(1).createText("Name");

        for (final DataRecord department : DEPARTMENTS.query(
            Query.all().orderBy(DEPARTMENT_NAME.ascending())
        ).getRecords()) {
            final Row row = new Row();
            table.add(row);
            row.getCell(0).createText(department.getId().toString());
            final InputField name = row.getCell(1).createInputField();
            name.setTextModel(department.model(DEPARTMENT_NAME));
        }
    }

    /**
     * Adds employees and resolves each department identifier.
     *
     * @param root page root
     */
    private static void addEmployees(final RootWidget root) {
        final Table table = new Table();
        root.add(table);
        final Row header = new Row();
        table.add(header);
        header.getCell(0).createText("Employee");
        header.getCell(1).createText("Department ID");
        header.getCell(2).createText("Department name");

        for (final DataRecord employee : EMPLOYEES.query(
            Query.all().orderBy(EMPLOYEE_NAME.ascending())
        ).getRecords()) {
            final Row row = new Row();
            table.add(row);
            row.getCell(0).createText(employee.model(EMPLOYEE_NAME));

            final UUID departmentId = employee.model(DEPARTMENT_ID).getData();
            row.getCell(1).createText(departmentId.toString());
            final DataRecord department = DEPARTMENTS.getRecord(departmentId);
            if (department == null) {
                row.getCell(2).createText("Missing department: " + departmentId);
            } else {
                row.getCell(2).createText(department.model(DEPARTMENT_NAME));
            }
        }
    }
}
