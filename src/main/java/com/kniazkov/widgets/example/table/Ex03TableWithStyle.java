/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example.table;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.view.Table;
import com.kniazkov.widgets.view.TextWidget;
import com.kniazkov.widgets.view.TextWidgetStyle;

/**
 * Demonstrates applying styles to an entire row and column of a dynamically
 * constructed multiplication table.
 * <p>
 * The example builds a 10×10 table, fills the first row and first column
 * with labels, then computes the multiplication values for the remaining cells.
 * <p>
 * After the table is populated, the example retrieves the header row and
 * header column using {@link Table#getRow(int)} and {@link Table#getColumn(int)},
 * collects all {@link TextWidget} instances within them, and applies a custom
 * text style (blue, bold) to each of those widgets.
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
public class Ex03TableWithStyle {
    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Page page = (root, parameters) -> {
            final Table table = new Table();
            root.add(table);
            for (int j = 1; j <= 10; j++ ) {
                table.getCell(0, j).setText(String.valueOf(j));
            }
            for (int i = 1; i <= 10; i++) {
                table.getCell(i, 0).setText(String.valueOf(i));
                for (int j = 1; j <= 10; j++ ) {
                    table.getCell(i, j).setText(String.valueOf(i * j));
                }
            }
            TextWidgetStyle style = TextWidget.getDefaultStyle().derive();
            style.setColor(Color.BLUE);
            style.setFontWeight(FontWeight.BOLD);
            for (final TextWidget widget : table.getRow(0).collectChildren(TextWidget.class)) {
                widget.setStyle(style);
            }
            for (final TextWidget widget : table.getColumn(0).collectChildren(TextWidget.class)) {
                widget.setStyle(style);
            }
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
