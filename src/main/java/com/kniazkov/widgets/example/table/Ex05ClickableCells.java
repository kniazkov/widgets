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
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.view.Cell;
import com.kniazkov.widgets.view.CellStyle;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.InputFieldStyle;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.State;
import com.kniazkov.widgets.view.Table;
import com.kniazkov.widgets.view.TextWidget;
import com.kniazkov.widgets.view.TextWidgetStyle;
import com.kniazkov.widgets.view.Widget;

/**
 * Demonstrates ....
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
public class Ex05ClickableCells {
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
            final CellStyle cellStyle = Cell.getDefaultStyle().derive();
            cellStyle.setWidth(35);
            cellStyle.setHeight(25);
            cellStyle.setBgColor(State.HOVERED, InputField.getDefaultStyle().getBgColor(State.HOVERED));
            cellStyle.setBorderWidth(1);
            final InputFieldStyle inputStyle = InputField.getDefaultStyle().derive();
            inputStyle.setWidth(35);
            inputStyle.setHeight(25);
            inputStyle.setMargin(0);
            for (final Cell cell : table.collectChildren(Cell.class)) {
                cell.setStyle(cellStyle);
                cell.onClick(data -> {
                    Widget widget = cell.getChild(0);
                    if (!(widget instanceof Section)) {
                        return;
                    }
                    final Section section = (Section)widget;
                    widget = section.getChild(0);
                    if (!(widget instanceof TextWidget)) {
                        return;
                    }
                    final TextWidget textWidget = (TextWidget)widget;
                    Model<String> model = textWidget.getTextModel();
                    final InputField field = new InputField();
                    field.setTextModel(model);
                    field.setStyle(inputStyle);
                    section.removeAll();
                    section.add(field);
                });
            }
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
