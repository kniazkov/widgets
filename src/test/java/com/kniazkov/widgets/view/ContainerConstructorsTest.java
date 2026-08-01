/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests declarative container construction and the general-purpose {@link Panel}.
 */
public final class ContainerConstructorsTest {
    @Test
    public void buildsNestedWidgetTreeDeclaratively() {
        final TextWidget title = new TextWidget("Title");
        final TextWidget buttonText = new TextWidget("Save");
        final Button button = new Button(buttonText);
        final Section header = new Section(title, button);
        final Panel panel = new Panel(header);
        final Cell cell = new Cell(new Section(new TextWidget("Value")));
        final Row row = new Row(cell);
        final Table table = new Table(row);

        final RootWidget root = new RootWidget(panel, table);

        assertEquals(2, root.getChildCount());
        assertSame(panel, root.getChild(0));
        assertSame(table, root.getChild(1));
        assertSame(root, panel.getParent().get());
        assertSame(panel, header.getParent().get());
        assertSame(header, title.getParent().get());
        assertSame(button, buttonText.getParent().get());
        assertSame(table, row.getParent().get());
        assertSame(row, cell.getParent().get());
    }

    @Test
    public void supportsStyleAndChildrenConstructors() {
        final Section section = new Section(
            Section.getDefaultStyle().derive(),
            new TextWidget("section")
        );
        final InlineBlock inlineBlock = new InlineBlock(
            InlineBlock.getDefaultStyle().derive(),
            section
        );
        final Panel panel = new Panel(
            Panel.getDefaultStyle().derive(),
            new Section(new TextWidget("panel"))
        );
        final Cell cell = new Cell(
            Cell.getDefaultStyle().derive(),
            panel
        );
        final Row row = new Row(Row.getDefaultStyle().derive(), cell);
        final Table table = new Table(Table.getDefaultStyle().derive(), row);

        assertSame(section, inlineBlock.getChild(0));
        assertSame(panel, cell.getChild(0));
        assertSame(cell, row.getChild(0));
        assertSame(row, table.getChild(0));
    }

    @Test
    public void addsIterableChildrenInOrder() {
        final Section first = new Section(new TextWidget("first"));
        final Section second = new Section(new TextWidget("second"));
        final Panel panel = new Panel();

        panel.addAll(Arrays.asList(first, second));

        assertEquals(2, panel.getChildCount());
        assertSame(first, panel.getChild(0));
        assertSame(second, panel.getChild(1));
        assertSame(panel, first.getParent().get());
        assertSame(panel, second.getParent().get());
    }

    @Test
    public void createsButtonWithArbitraryInlineChild() {
        final ActiveText child = new ActiveText();

        final Button button = new Button(Button.getDefaultStyle().derive(), child);

        assertSame(child, button.getChild());
        assertSame(button, child.getParent().get());
    }

    @Test
    public void panelIsAStyledBlockContainer() {
        final Panel panel = new Panel();

        assertTrue(panel instanceof BlockWidget);
        assertTrue(panel instanceof BlockContainer);
        assertTrue(panel instanceof HasBgColor);
        assertTrue(panel instanceof HasBorder);
        assertEquals("panel", panel.getType());
    }

    @Test
    public void constructorDoesNotDispatchToOverriddenAdd() {
        final TrackingSection section = new TrackingSection(new TextWidget("initial"));

        assertFalse(section.wasAddCalled());

        section.add(new TextWidget("later"));

        assertTrue(section.wasAddCalled());
    }

    /** Section that records calls to its overridable add method. */
    private static final class TrackingSection extends Section {
        /** Whether add was called. */
        private boolean addCalled;

        /**
         * Creates a section with an initial child.
         *
         * @param child initial child
         */
        private TrackingSection(final InlineWidget<?> child) {
            super(child);
        }

        @Override
        public void add(final InlineWidget<?> widget) {
            this.addCalled = true;
            super.add(widget);
        }

        /**
         * Returns whether add was called.
         *
         * @return call flag
         */
        private boolean wasAddCalled() {
            return this.addCalled;
        }
    }
}
