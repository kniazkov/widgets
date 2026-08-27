/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link CascadingModel}.
 */
public final class CascadingModelTest {
    /**
     * Verifies the followsBaseAndRelaysUpdatesBeforeFirstWrite behavior.
     */
    @Test
    public void followsBaseAndRelaysUpdatesBeforeFirstWrite() {
        final StringModel base = new StringModel("base");
        final Model<String> cascading = base.asCascading();
        final List<String> observed = new ArrayList<>();
        final Listener<String> listener = observed::add;
        cascading.addListener(listener);

        assertEquals("base", cascading.getData());
        assertTrue(base.setData("shared"));

        assertEquals("shared", cascading.getData());
        assertEquals(Arrays.asList("shared"), observed);
        cascading.removeListener(listener);
    }

    /**
     * Verifies the firstWriteForksWithoutChangingBase behavior.
     */
    @Test
    public void firstWriteForksWithoutChangingBase() {
        final StringModel base = new StringModel("base");
        final Model<String> cascading = base.asCascading();
        final List<String> observed = new ArrayList<>();
        final Listener<String> listener = observed::add;
        cascading.addListener(listener);

        assertTrue(cascading.setData("local"));
        assertEquals("base", base.getData());
        assertEquals("local", cascading.getData());

        assertTrue(base.setData("new base"));
        assertEquals("local", cascading.getData());
        assertTrue(cascading.setData("new local"));
        assertFalse(cascading.setData("new local"));

        assertEquals(Arrays.asList("local", "new local"), observed);
        cascading.removeListener(listener);
    }

    /**
     * Verifies the delegatesValidityToActiveModel behavior.
     */
    @Test
    public void delegatesValidityToActiveModel() {
        final MutableTestModel<String> base = new MutableTestModel<>("base", false);
        final Model<String> cascading = base.asCascading();

        assertFalse(cascading.isValid());
        assertTrue(base.setValid(true));
        assertTrue(cascading.isValid());

        assertTrue(cascading.setData("local"));
        assertTrue(base.setValid(false));
        assertTrue(cascading.isValid());
    }

    /**
     * Verifies the derivesIndependentModelFromActiveModel behavior.
     */
    @Test
    public void derivesIndependentModelFromActiveModel() {
        final StringModel base = new StringModel("base");
        final Model<String> cascading = base.asCascading();

        final Model<String> derivedBeforeFork = cascading.deriveWithData("first");
        assertNotSame(cascading, derivedBeforeFork);
        assertTrue(derivedBeforeFork instanceof StringModel);
        assertEquals("first", derivedBeforeFork.getData());

        assertTrue(cascading.setData("local"));
        final Model<String> derivedAfterFork = cascading.deriveWithData("second");
        assertTrue(derivedAfterFork instanceof StringModel);
        assertEquals("second", derivedAfterFork.getData());
    }
}
