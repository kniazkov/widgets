/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.e2e;

import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Cell;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.Panel;
import com.kniazkov.widgets.view.RootWidget;
import com.kniazkov.widgets.view.Row;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import com.kniazkov.widgets.view.Table;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Browser fixture for moving a live subtree without detaching it.
 */
final class WidgetReparentingPage {
    /**
     * Static fixture only.
     */
    private WidgetReparentingPage() {
    }

    /**
     * Creates a page with a movable input and source/destination markers.
     *
     * @return browser fixture
     */
    static Page create() {
        return (root, context) -> {
            final TextWidget result = new TextWidget("Actions: 0");
            final AtomicInteger count = new AtomicInteger();
            final InputField input = new InputField();
            input.setAccessibleName("Movable draft");
            final Button action = new Button("Moved action");
            action.onClick(event -> result.setText("Actions: " + count.incrementAndGet()));
            final Section moved = new Section(input, action);
            final Panel source = new Panel(new Section(new TextWidget("Source marker")), moved);
            final Panel target = new Panel(new Section(new TextWidget("Destination marker")));
            final Button move = new Button("Move to destination");
            move.onClick(event -> {
                if (moved.getParent().orElseThrow() == source) {
                    target.add(moved);
                    move.disable();
                }
            });
            final Button remove = new Button("Dispose source");
            remove.onClick(event -> {
                if (moved.getParent().orElseThrow() == target) {
                    source.dispose();
                    remove.disable();
                }
            });
            root.add(new Section(move, remove, result));
            root.add(source);
            root.add(target);
            appendTableFixture(root);
        };
    }

    /**
     * Adds a column transfer plus a deliberately rejected cycle.
     *
     * @param root fixture page
     */
    private static void appendTableFixture(final RootWidget root) {
        final TextWidget status = new TextWidget("Table ready");
        final Button move = new Button("Move table cell");
        final Button reject = new Button("Reject table cycle");
        final Cell cell = new Cell(new Section(new TextWidget("Movable cell")));
        final Table table = new Table(new Row(cell));
        final Cell outerCell = new Cell(table);
        final Table outerTable = new Table(new Row(outerCell));
        move.onClick(event -> {
            table.getColumn(1).add(cell);
            status.setText("Moved to row " + table.getChildCount() + ", column 2");
            move.disable();
        });
        reject.onClick(event -> {
            try {
                table.getColumn(1).add(outerCell);
            } catch (IllegalArgumentException ignored) {
                status.setText("Cycle rejected; rows: " + table.getChildCount());
            }
        });
        root.add(new Section(move, reject, status));
        root.add(outerTable);
    }
}
