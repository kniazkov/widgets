/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Tests hierarchy and decorator changes through the widget protocol. */
public final class WidgetContainerTest {
    @Test
    public void everyMultiChildContainerEmitsAppendAndRemoveUpdates() {
        final RootWidget root = new RootWidget();
        assertContainerMutation(root, new Section(), child -> root.add((BlockWidget<?>) child));

        final Panel panel = new Panel();
        assertContainerMutation(panel, new Section(), child -> panel.add((BlockWidget<?>) child));

        final InlineBlock inlineBlock = new InlineBlock();
        assertContainerMutation(
            inlineBlock, new Section(), child -> inlineBlock.add((BlockWidget<?>) child)
        );

        final Section section = new Section();
        assertContainerMutation(
            section, new TextWidget(), child -> section.add((InlineWidget<?>) child)
        );

        final Table table = new Table();
        assertContainerMutation(table, new Row(), child -> table.add((Row) child));

        final Row row = new Row();
        assertContainerMutation(row, new Cell(), child -> row.add((Cell) child));

        final Cell cell = new Cell();
        assertContainerMutation(cell, new Section(), child -> cell.add((BlockWidget<?>) child));
    }

    @Test
    public void removingDecoratorChildInstallsDetachedPlaceholder() {
        final TextWidget buttonChild = new TextWidget("button");
        final Button button = new Button(buttonChild);
        final WidgetSandbox<Button> buttonSandbox = WidgetSandbox.open(button);
        buttonSandbox.clearUpdates();

        button.remove(buttonChild);

        assertNotSame(buttonChild, button.getChild());
        assertFalse(buttonChild.getParent().isPresent());
        assertSame(button, button.getChild().getParent().get());
        assertEquals(1, WidgetSandbox.findUpdates(
            buttonSandbox.drainUpdates(), "set child", button.getChild()
        ).size());

        final TextWidget decoratedChild = new TextWidget("decorated");
        final MarginDecorator decorator = new MarginDecorator(decoratedChild);
        final WidgetSandbox<MarginDecorator> decoratorSandbox = WidgetSandbox.open(decorator);
        decoratorSandbox.clearUpdates();

        decorator.remove(decoratedChild);

        assertNotSame(decoratedChild, decorator.getChild());
        assertFalse(decoratedChild.getParent().isPresent());
        assertSame(decorator, decorator.getChild().getParent().get());
        assertEquals(1, WidgetSandbox.findUpdates(
            decoratorSandbox.drainUpdates(), "set child", decorator.getChild()
        ).size());
    }

    @Test
    public void rootEmitsNavigationAndResetCommands() {
        final RootWidget root = new RootWidget();
        final WidgetSandbox<RootWidget> sandbox = WidgetSandbox.open(root);
        sandbox.clearUpdates();

        root.goToPage("/next");
        root.remove();

        final List<JsonObject> updates = sandbox.drainUpdates();
        final List<JsonObject> navigation = WidgetSandbox.findUpdates(updates, "go to page");
        assertEquals(1, navigation.size());
        assertEquals("/next", navigation.get(0).get("href").getStringValue());
        assertEquals(1, WidgetSandbox.findUpdates(updates, "reset").size());
    }

    /** Operation that adds a child to a typed container. */
    private interface AddChild {
        /**
         * Adds the child.
         *
         * @param child child widget
         */
        void apply(Widget<?> child);
    }

    /**
     * Verifies the common add/remove contract for a container implementation.
     *
     * @param container container under test
     * @param child legal child for the container
     * @param add typed add operation
     */
    private static void assertContainerMutation(final Widget<?> container, final Widget<?> child,
            final AddChild add) {
        final WidgetSandbox<?> sandbox = WidgetSandbox.open(container);
        sandbox.clearUpdates();

        add.apply(child);

        assertSame(container, child.getParent().get());
        List<JsonObject> updates = WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "append child", child
        );
        assertEquals("append count for " + container.getType(), 1, updates.size());
        assertEquals(container.getId().toString(),
            updates.get(0).get("container").getStringValue());

        ((Container) container).remove(child);

        assertFalse(child.getParent().isPresent());
        updates = WidgetSandbox.findUpdates(sandbox.drainUpdates(), "remove child", child);
        assertEquals("remove count for " + container.getType(), 1, updates.size());
        assertEquals(container.getId().toString(),
            updates.get(0).get("container").getStringValue());
        assertTrue(((Container) container).getChildCount() == 0);
    }
}
