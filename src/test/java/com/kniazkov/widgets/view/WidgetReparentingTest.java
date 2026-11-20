/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Runs the ownership contract for each writable container family.
 */
@RunWith(Parameterized.class)
public class WidgetReparentingTest {
    /**
     * Setup for the container under test.
     */
    private final Family family;

    /**
     * Selects one container family.
     *
     * @param family container adapter
     */
    public WidgetReparentingTest(final Family family) {
        this.family = family;
    }

    /**
     * Enumerates structural owners, including single-slot replacements.
     *
     * @return parameter values
     */
    @Parameterized.Parameters(name = "{0}")
    public static List<Family> families() {
        return List.of(
            new Family("root", RootWidget::new, Section::new, RootWidget::add, false),
            new Family("panel", Panel::new, Section::new, Panel::add, false),
            new Family("popup", Popup::new, Section::new, Popup::add, false),
            new Family("modal", ModalPopup::new, Section::new, ModalPopup::add, false),
            new Family("cell", Cell::new, Panel::new, Cell::add, false),
            new Family("row", Row::new, Cell::new, Row::add, false),
            new Family("table", Table::new, Row::new, Table::add, false),
            new Family("section", Section::new, TextWidget::new, Section::add, false),
            new Family("inline block", InlineBlock::new, Section::new, InlineBlock::add, false),
            new Family("nowrap", NoWrap::new, TextWidget::new, NoWrap::add, false),
            new Family("overlay", OverlayStack::new, (Supplier<Widget<?>>) TextWidget::new,
                OverlayStack::add, false),
            new Family("sortable", SortableSection::new, TextWidget::new,
                SortableSection::add, true),
            new Family("button", () -> new Button("empty"), TextWidget::new, Button::put, true),
            new Family("margin", () -> new MarginDecorator(new TextWidget()), TextWidget::new,
                MarginDecorator::put, true),
            new Family("zoom", () -> new ZoomDecorator(new TextWidget()), TextWidget::new,
                ZoomDecorator::put, true)
        );
    }

    /**
     * A successful move changes both owners; subsequent operations cannot lose the child.
     */
    @Test
    public void movesAndDetachesExactlyOnce() {
        final Container original = this.family.owner.get();
        final Container destination = this.family.owner.get();
        final Widget<?> child = this.family.child.get();
        this.family.add.accept(original, child);
        /*
         * Root instances have not been exported, so changing owners is still safe.
         */
        this.family.add.accept(destination, child);
        assertFalse(original.collectChildren(Widget.class).contains(child));
        assertSame(destination, child.getParent().orElseThrow());
        assertTrue(destination.collectChildren(Widget.class).contains(child));

        original.remove(child);
        assertSame(destination, child.getParent().orElseThrow());
        child.remove();
        assertTrue(child.getParent().isEmpty());
        assertFalse(destination.collectChildren(Widget.class).contains(child));
        this.family.add.accept(destination, child);
        destination.removeAll();
        assertTrue(child.getParent().isEmpty());
    }

    /**
     * Null and duplicates cannot replace, duplicate, or detach a present child.
     */
    @Test
    public void rejectsOrIgnoresInvalidAddBeforeMutation() {
        final Container destination = this.family.owner.get();
        final Widget<?> child = this.family.child.get();
        this.family.add.accept(destination, child);
        assertThrows(NullPointerException.class, () -> this.family.add.accept(destination, null));
        if (this.family.duplicateNoop) {
            this.family.add.accept(destination, child);
        } else {
            assertThrows(IllegalArgumentException.class,
                () -> this.family.add.accept(destination, child));
        }
        assertSame(destination, child.getParent().orElseThrow());
        assertEquals(1, destination.getChildCount());
        assertSame(child, destination.getChild(0));
        child.dispose();
        assertThrows(IllegalArgumentException.class,
            () -> this.family.add.accept(destination, child));
    }

    /**
     * Invalid types leave both the destination and candidate unmodified.
     */
    @Test
    public void rejectsIncompatibleChildType() {
        final Container destination = this.family.owner.get();
        final RootWidget wrong = new RootWidget();
        final int count = destination.getChildCount();
        if (destination instanceof OverlayStack) {
            assertThrows(IllegalArgumentException.class,
                () -> this.family.add.accept(destination, wrong));
        } else {
            assertThrows(ClassCastException.class,
                () -> this.family.add.accept(destination, wrong));
        }
        assertEquals(count, destination.getChildCount());
        assertTrue(wrong.getParent().isEmpty());
    }

    /**
     * A single-child decorator replaces its old child without detaching the new one.
     */
    @Test
    public void replacesOldSlotOnlyInSingleChildHosts() {
        final Container destination = this.family.owner.get();
        final Widget<?> old = this.family.child.get();
        final Widget<?> replacement = this.family.child.get();
        this.family.add.accept(destination, old);
        this.family.add.accept(destination, replacement);
        if (destination instanceof Decorator) {
            assertTrue(old.getParent().isEmpty());
            assertEquals(1, destination.getChildCount());
            assertSame(replacement, destination.getChild(0));
        } else {
            assertSame(destination, old.getParent().orElseThrow());
            assertEquals(2, destination.getChildCount());
        }
        assertSame(destination, replacement.getParent().orElseThrow());
    }

    /**
     * Adds a typed bridge to the generic parameter runner.
     */
    private static final class Family {
        /**
         * Name used in the test report.
         */
        private final String name;
        /**
         * Produces an empty or default-filled container.
         */
        private final Supplier<Container> owner;
        /**
         * Produces compatible children.
         */
        private final Supplier<Widget<?>> child;
        /**
         * Routes to the family's typed mutation.
         */
        private final BiConsumer<Container, Widget<?>> add;
        /**
         * Whether adding the same widget is already a no-op.
         */
        private final boolean duplicateNoop;

        /**
         * Adapts each family's typed mutation to the parameterized harness.
         *
         * @param name display name
         * @param owner container factory
         * @param child compatible child factory
         * @param add typed mutation
         * @param duplicateNoop whether duplicate requests are ignored
         * @param <C> container type
         * @param <W> child type
         */
        @SuppressWarnings("unchecked")
        private <C extends Container, W extends Widget<?>> Family(final String name,
                final Supplier<C> owner, final Supplier<W> child, final BiConsumer<C, W> add,
                final boolean duplicateNoop) {
            this.name = name;
            this.owner = owner::get;
            this.child = child::get;
            this.add = (container, widget) -> add.accept((C) container, (W) widget);
            this.duplicateNoop = duplicateNoop;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
