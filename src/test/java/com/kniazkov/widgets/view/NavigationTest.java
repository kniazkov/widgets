/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

/**
 * Stable keys, lazy retention, unavailable selections and ownership retirement.
 */
public final class NavigationTest {
    /**
     * Reordering preserves selection and drafts; removal disposes rather than detaches.
     */
    @Test
    public void tabsRetainAndDisposeContent() {
        final Tabs tabs = new Tabs();
        final AtomicInteger created = new AtomicInteger();
        final InputField editor = new InputField("draft");
        final Section content = new Section(editor);
        tabs.addTab("first", "First", () -> content);
        tabs.addTab("second", "Second", () -> {
            created.incrementAndGet();
            return new Panel();
        });
        final WidgetSandbox<Tabs> sandbox = WidgetSandbox.open(tabs);
        assertEquals(0, created.get());
        assertEquals("first", tabs.getSelectedKeyModel().getData());
        assertFalse(tabs.getSelectedKeyModel().setData("missing"));
        assertFalse(tabs.getSelectedKeyModel().setData(""));
        assertTrue(tabs.getSelectedKeyModel().setData("second"));
        assertFalse(content.isInteractionEnabled());
        tabs.reorder(List.of("second", "first"));
        assertEquals("second", tabs.getSelectedKeyModel().getData());
        assertTrue(tabs.getSelectedKeyModel().setData("first"));
        assertEquals("draft", editor.getText());
        tabs.getSelectedKeyModel().setData("second");
        assertEquals(1, created.get());
        tabs.removeItem("second");
        assertEquals("first", tabs.getSelectedKeyModel().getData());
        tabs.replaceContent("first", Panel::new);
        assertTrue(content.isRetired());
        assertTrue(editor.isRetired());
        assertTrue(sandbox.drainUpdates().stream().anyMatch(update ->
            update.toString().contains("dispose widget")));
        tabs.dispose();
        assertFalse(tabs.getSelectedKeyModel().setData("first"));
        assertThrows(IllegalStateException.class, () -> tabs.addTab("third", "Third", Panel::new));
    }

    /**
     * Hidden and disabled headers cannot be selected, including through application models.
     */
    @Test
    public void unavailableTabsAndInvalidReorder() {
        final Tabs tabs = new Tabs();
        tabs.addTab("a", "A", Panel::new);
        tabs.addTab("b", "B", Panel::new);
        tabs.setItemVisible("a", false);
        assertEquals("b", tabs.getSelectedKeyModel().getData());
        assertFalse(tabs.getSelectedKeyModel().setData("a"));
        tabs.setItemEnabled("b", false);
        assertEquals("", tabs.getSelectedKeyModel().getData());
        tabs.setItemVisible("a", true);
        assertEquals("a", tabs.getSelectedKeyModel().getData());
        assertThrows(IllegalArgumentException.class, () -> tabs.reorder(List.of("a", "a")));
        assertThrows(IllegalArgumentException.class, () -> tabs.addTab("a", "Again", Panel::new));
        assertThrows(IllegalArgumentException.class, () -> tabs.removeItem("missing"));
        assertEquals(List.of("a", "b"), tabs.getKeys());
        assertThrows(UnsupportedOperationException.class, () -> tabs.getKeys().clear());
        tabs.dispose();
    }

    /**
     * Single and multiple expansion use immutable snapshots and defer collapsed replacement.
     */
    @Test
    public void accordionExpansionAndReplacement() {
        for (final boolean multiple : List.of(false, true)) {
            final Accordion accordion = new Accordion(multiple);
            final AtomicInteger created = new AtomicInteger();
            accordion.addSection("a", "A", Panel::new);
            accordion.addSection("b", "B", Panel::new);
            assertEquals(Set.of(), accordion.getExpandedKeysModel().getData());
            assertEquals(multiple, accordion.getExpandedKeysModel().setData(Set.of("a", "b")));
            accordion.getExpandedKeysModel().setData(Set.of("a"));
            accordion.replaceContent("b", () -> {
                created.incrementAndGet();
                return new Panel();
            });
            assertEquals(0, created.get());
            accordion.getExpandedKeysModel().setData(Set.of("b"));
            assertEquals(1, created.get());
            accordion.setItemEnabled("b", false);
            assertEquals(Set.of(), accordion.getExpandedKeysModel().getData());
            assertFalse(accordion.getExpandedKeysModel().setData(Set.of("b")));
            assertThrows(UnsupportedOperationException.class,
                () -> accordion.getExpandedKeysModel().getData().add("a"));
            accordion.dispose();
        }
    }

    /**
     * Failed content creation or attachment cannot discard the existing active subtree.
     */
    @Test
    public void replacementFailuresKeepContent() {
        final Tabs tabs = new Tabs();
        final Panel original = new Panel();
        tabs.addTab("a", "A", () -> original);
        assertThrows(IllegalArgumentException.class,
            () -> tabs.replaceContent("a", () -> original));
        assertFalse(original.isRetired());
        assertThrows(IllegalStateException.class, () -> tabs.replaceContent("a", () -> {
            throw new IllegalStateException("factory failed");
        }));
        assertFalse(original.isRetired());
        assertEquals("a", tabs.getSelectedKeyModel().getData());
        tabs.dispose();
    }

    /**
     * A failing lazy fallback must leave the active key and its availability untouched.
     */
    @Test
    public void failedFallbackIsAtomic() {
        final Tabs tabs = new Tabs();
        tabs.addTab("a", "A", Panel::new);
        tabs.addTab("b", "B", () -> {
            throw new IllegalStateException("lazy factory failed");
        });
        assertThrows(IllegalStateException.class, () -> tabs.removeItem("a"));
        assertThrows(IllegalStateException.class, () -> tabs.setItemVisible("a", false));
        assertThrows(IllegalStateException.class, () -> tabs.setItemEnabled("a", false));
        assertEquals(List.of("a", "b"), tabs.getKeys());
        assertEquals("a", tabs.getSelectedKeyModel().getData());
        tabs.removeItem("b");
        tabs.setItemVisible("a", false);
        assertEquals("", tabs.getSelectedKeyModel().getData());
        tabs.dispose();
    }

    /**
     * Ancestors are links, current page is plain text, invalid path writes are atomic.
     */
    @Test
    public void breadcrumbPathOwnership() {
        final Breadcrumbs path = new Breadcrumbs("Path", List.of(
            new Breadcrumbs.Item("Home", "/"), new Breadcrumbs.Item("Here", "")));
        final Section ancestor = (Section) path.getChild(0);
        assertTrue(ancestor.getChild(0) instanceof Link);
        assertTrue(((Section) path.getChild(1)).getChild(0) instanceof TextWidget);
        assertThrows(IllegalArgumentException.class, () -> path.setItems(List.of(
            new Breadcrumbs.Item("Missing URL", ""), new Breadcrumbs.Item("Here", ""))));
        assertEquals(2, path.getItems().size());
        path.setItems(List.of());
        assertTrue(ancestor.isRetired());
        assertEquals(0, path.getChildCount());
        path.dispose();
    }
}
