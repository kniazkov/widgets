/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Checks key ownership, lazy materialization and application-driven mutations.
 */
public class TreeViewTest {
    /**
     * Shared fixture labels.
     */
    private static final TreeView.Messages LABELS = new TreeView.Messages("Load", "Empty", "Error");

    /**
     * @param key synthetic branch identity
     * @return descriptor
     */
    private static TreeView.Node branch(final String key) {
        return new TreeView.Node(key, key, true);
    }

    /**
     * Invalid provider batches leave no partial children and can be retried.
     */
    @Test
    public void rejectsDuplicatesAndCyclesAtomically() {
        assertThrows(IllegalArgumentException.class, () -> new TreeView(
            List.of(branch("root"), branch("root")), key -> null, LABELS));
        final List<CompletableFuture<List<TreeView.Node>>> calls = new ArrayList<>();
        final TreeView tree = new TreeView(List.of(branch("root")), key -> {
            final var next = new CompletableFuture<List<TreeView.Node>>();
            calls.add(next);
            return next;
        }, LABELS);
        final RootWidget root = new RootWidget(tree);
        for (final String duplicate : List.of("root", "child")) {
            tree.expand("root");
            calls.getLast().complete(List.of(branch("child"), branch(duplicate)));
            assertThrows(IllegalArgumentException.class, () -> tree.select("child"));
            tree.collapse("root");
        }
        tree.expand("root");
        calls.getLast().complete(List.of(branch("child")));
        tree.expand("child");
        calls.getLast().complete(List.of());
        assertThrows(IllegalArgumentException.class, () -> tree.move("root", "child"));
        root.closePage();
    }

    /**
     * Identity survives edits and moves; removing the selected subtree clears it.
     */
    @Test
    public void preservesSelectionUntilRemovalAndIgnoresSupersededLoads() {
        final var calls = new ArrayList<CompletableFuture<List<TreeView.Node>>>();
        final TreeView tree = new TreeView(List.of(branch("left"), branch("right")), key -> {
            final var future = new CompletableFuture<List<TreeView.Node>>();
            calls.add(future);
            return future;
        }, LABELS);
        final RootWidget root = new RootWidget(tree);
        tree.expand("left");
        tree.collapse("left");
        tree.expand("left");
        calls.getLast().complete(List.of(branch("child")));
        calls.getFirst().complete(List.of(branch("stale")));
        assertFalse(tree.accepts("select", "stale"));
        tree.select("child");
        tree.rename("child", "Updated");
        tree.expand("right");
        calls.getLast().complete(List.of());
        tree.move("child", "right");
        tree.collapse("right");
        assertEquals("child", tree.getSelectedKeyModel().getData());
        assertFalse(tree.accepts("select", "child"));
        tree.expand("right");
        assertEquals(3, calls.size());
        tree.remove("right");
        assertEquals("", tree.getSelectedKeyModel().getData());
        assertEquals(Set.of("left"), tree.getExpandedKeysModel().getData());
        assertFalse(tree.accepts("select", "child"));
        root.closePage();
    }

    /**
     * A ten-way tree of depth six has 1,111,111 potential nodes. Expanding one path
     * materializes only the six sets of direct children.
     */
    @Test
    public void loadsOnlyTheExpandedPath() {
        final AtomicInteger supplied = new AtomicInteger();
        final TreeView tree = new TreeView(List.of(branch("r")), key -> {
            supplied.addAndGet(10);
            return CompletableFuture.completedFuture(IntStream.range(0, 10)
                .mapToObj(i -> new TreeView.Node(key + i, key + i, key.length() < 6)).toList());
        }, LABELS);
        final RootWidget root = new RootWidget(tree);
        final var sandbox = WidgetSandbox.open(root);
        assertEquals(0, supplied.get());
        for (int depth = 0; depth < 6; depth++) {
            tree.expand("r" + "0".repeat(depth));
        }
        assertEquals(60, supplied.get());
        final var snapshot = sandbox.drainUpdates().stream().filter(update ->
            "set tree view".equals(update.get("action").stringValue())).reduce((a, b) -> b)
            .orElseThrow().get("snapshot").objectValueOrThrow();
        assertEquals(61, snapshot.get("rows").toJsonArray().orElseThrow().size());
        assertTrue(tree.accepts("select", "r000000"));
        root.closePage();
    }
}
