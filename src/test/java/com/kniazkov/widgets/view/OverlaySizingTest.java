/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.BooleanModel;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the opt-in reactive sizing mode without changing existing stacks.
 */
public class OverlaySizingTest {
    /**
     * Defaults stay compatible and rebinding detaches old models.
     */
    @Test public void bindsSizingMode() {
        final OverlayStack stack = new OverlayStack(new TextWidget("Price"));
        assertFalse(stack.isFitToFirstChild());
        final BooleanModel mode = new BooleanModel(true);
        stack.setFitToFirstChildModel(mode);
        assertTrue(stack.isFitToFirstChild());
        mode.setData(false);
        assertFalse(stack.isFitToFirstChild());
        stack.setFitToFirstChildModel(new BooleanModel(true));
        mode.setData(true);
        mode.setData(false);
        assertTrue(stack.isFitToFirstChild());
    }
}
