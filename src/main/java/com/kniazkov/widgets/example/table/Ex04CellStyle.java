/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example.table;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.view.Cell;
import com.kniazkov.widgets.view.CellStyle;
import com.kniazkov.widgets.view.State;
import com.kniazkov.widgets.view.Table;
import com.kniazkov.widgets.view.TextWidget;
import com.kniazkov.widgets.view.TextWidgetStyle;

/**
 * Demonstrates applying cell-level styling to an entire table.
 * <p>
 * The example constructs a 10×10 multiplication table, populates the header row
 * and header column with labels, and styles the text of those headers by selecting
 * all {@link TextWidget} instances within the corresponding row and column.
 * <p>
 * Afterwards, a {@link CellStyle} is customized to define fixed cell dimensions, background color
 * on hover, and border appearance. This style is then applied to every {@link Cell} in the table,
 * demonstrating how visual configuration can be propagated across a large widget structure
 * with minimal code.
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
public class Ex04CellStyle {
    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Page page = root -> {
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
            TextWidgetStyle textStyle = TextWidget.getDefaultStyle().derive();
            textStyle.setColor(Color.BLUE);
            textStyle.setFontWeight(FontWeight.BOLD);
            for (final TextWidget widget : table.getRow(0).collectChildren(TextWidget.class)) {
                widget.setStyle(textStyle);
            }
            for (final TextWidget widget : table.getColumn(0).collectChildren(TextWidget.class)) {
                widget.setStyle(textStyle);
            }
            CellStyle cellStyle = Cell.getDefaultStyle().derive();
            cellStyle.setWidth(35);
            cellStyle.setHeight(25);
            cellStyle.setBgColor(State.HOVERED, Color.YELLOW);
            cellStyle.setBorderWidth(1);
            cellStyle.setBorderStyle(BorderStyle.SOLID);
            cellStyle.setBorderColor(State.NORMAL, Color.WHITE);
            cellStyle.setBorderColor(State.HOVERED, Color.GRAY);
            for (final Cell cell : table.collectChildren(Cell.class)) {
                cell.setStyle(cellStyle);
            }
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
