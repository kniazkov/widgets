/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.StringModel;
import com.kniazkov.widgets.protocol.Update;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Incremental collection preserves model and ownership transitions.
 */
public final class DirtyUpdatesTest {
    /**
     * Compare the index with the former full traversal after each structural operation.
     */
    @Test
    public void matchesTraversalForAddRemoveMoveAndReactiveModels() {
        final StringModel model = new StringModel("first");
        final TextWidget text = new TextWidget(TextWidget.getDefaultStyle(), model);
        final Section section = new Section(text);
        final Panel left = new Panel(section);
        final Panel right = new Panel();
        final RootWidget root = new RootWidget(left, right);
        drainAndCheck(root, true);
        drainAndCheck(root, false);
        model.setData("second");
        drainAndCheck(root, true);
        right.add(section);
        drainAndCheck(root, true);
        right.remove(section);
        model.setData("detached");
        drainAndCheck(root, true);
        drainAndCheck(root, false);
        left.add(section);
        final Set<Update> attached = drainAndCheck(root, true);
        assertTrue(attached.stream().anyMatch(update -> update.toString()
            .contains("detached")));
        section.dispose();
        drainAndCheck(root, true);
        model.setData("retired");
        drainAndCheck(root, false);
        root.closePage();
        drainAndCheck(root, false);
    }

    /**
     * An empty poll must not enter unchanged containers, even with a large tree.
     */
    @Test
    public void emptyPollDoesNotTraverseContainers() {
        final boolean[] forbidTraversal = {false};
        final Section section = new Section() {
            @Override
            public int getChildCount() {
                if (forbidTraversal[0]) {
                    throw new AssertionError("Empty poll traversed unchanged children");
                }
                return super.getChildCount();
            }
        };
        for (int index = 0; index < 1000; index++) {
            section.add(new TextWidget("value"));
        }
        final RootWidget root = new RootWidget(section);
        root.collectBrowserUpdates(new TreeSet<>());
        forbidTraversal[0] = true;
        final Set<Update> pending = new TreeSet<>();
        for (int index = 0; index < 100; index++) {
            root.collectBrowserUpdates(pending);
        }
        assertTrue(pending.isEmpty());
        forbidTraversal[0] = false;
        root.closePage();
    }

    /**
     * Detached changes must remain available when mounted in a different unpublished root.
     */
    @Test
    public void transfersUnpublishedDirtySubtreeBetweenRoots() {
        final Section child = new Section(new TextWidget("retained"));
        final RootWidget old = new RootWidget(child);
        final RootWidget next = new RootWidget();
        next.add(child);
        final Set<Update> oldUpdates = drainAndCheck(old, true);
        assertFalse(oldUpdates.stream().anyMatch(update -> update.toString()
            .contains("retained")));
        final Set<Update> nextUpdates = drainAndCheck(next, true);
        assertTrue(nextUpdates.stream().anyMatch(update -> update.toString()
            .contains("retained")));
    }

    /**
     * @param root page to drain
     * @param changed whether updates are expected
     * @return incremental changes
     */
    private static Set<Update> drainAndCheck(final RootWidget root, final boolean changed) {
        final Set<Update> pending = new TreeSet<>();
        root.collectBrowserUpdates(pending);
        assertEquals(changed, !pending.isEmpty());
        final Set<Update> missed = new TreeSet<>();
        for (final Widget<?> widget : root) {
            widget.getUpdates(missed);
        }
        assertTrue("Dirty index missed mounted changes", missed.isEmpty());
        return pending;
    }
}
