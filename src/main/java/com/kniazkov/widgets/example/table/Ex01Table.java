/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example.table;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.view.Cell;
import com.kniazkov.widgets.view.Row;
import com.kniazkov.widgets.view.Table;

/**
 * Demonstrates generating a simple multiplication table using table widgets.
 * <p>
 * The program creates a 10×10 {@link Table}, fills each {@link Cell} with the
 * product of its row and column indices, and displays the result as plain text
 * without any additional styling.
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
public class Ex01Table {
    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Page page = (root, parameters) -> {
            final Table table = new Table();
            root.add(table);
            for (int i = 1; i <= 10; i++) {
                final Row row = new Row();
                table.add(row);
                for (int j = 1; j <= 10; j++ ) {
                    final Cell cell = new Cell();
                    row.add(cell);
                    cell.createText(String.valueOf(i * j));
                }
            }
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
