/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

/**
 * Tests atomic inline groups and ordinary widget tree operations.
 */
public final class NoWrapTest {
    /**
     * Inline groups support nesting, reactive children and removal.
     */
    @Test
    public void keepsInlineChildrenInTheWidgetTree() {
        final TextWidget prefix = new TextWidget("our ");
        final Link link = new Link("policy", "/policy");
        final NoWrap nested = new NoWrap(link);
        final NoWrap group = new NoWrap(NoWrap.getDefaultStyle().derive(), prefix, nested);
        final Section section = new Section(group);
        assertSame(section, group.getParent().orElseThrow());
        assertSame(group, nested.getParent().orElseThrow());
        assertSame(nested, link.getParent().orElseThrow());
        assertEquals("no-wrap", group.getType());
        assertEquals(2, group.getChildCount());
        prefix.setText("updated ");
        assertEquals("updated ", ((TextWidget) group.getChild(0)).getText());
        group.remove(prefix);
        assertFalse(prefix.getParent().isPresent());
        assertSame(nested, group.getChild(0));
        group.add(prefix);
        assertSame(prefix, group.getChild(1));
        assertSame(group, prefix.getParent().orElseThrow());
    }
}
