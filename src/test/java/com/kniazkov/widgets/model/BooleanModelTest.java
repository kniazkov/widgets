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
 * Tests for {@link BooleanModel}.
 */
public final class BooleanModelTest {
    /**
     * Verifies the usesFalseAsDefaultValue behavior.
     */
    @Test
    public void usesFalseAsDefaultValue() {
        final BooleanModel model = new BooleanModel();

        assertEquals(Boolean.FALSE, model.getData());
        assertTrue(model.isValid());
    }

    /**
     * Verifies the usesProvidedInitialValue behavior.
     */
    @Test
    public void usesProvidedInitialValue() {
        final BooleanModel model = new BooleanModel(true);

        assertEquals(Boolean.TRUE, model.getData());
    }

    /**
     * Verifies the notifiesListenerOnlyWhenValueChanges behavior.
     */
    @Test
    public void notifiesListenerOnlyWhenValueChanges() {
        final BooleanModel model = new BooleanModel();
        final List<Boolean> observedValues = new ArrayList<>();
        final Listener<Boolean> listener = observedValues::add;
        model.addListener(listener);

        assertTrue(model.setData(true));
        assertFalse(model.setData(true));

        assertEquals(Arrays.asList(Boolean.TRUE), observedValues);
        model.removeListener(listener);
    }

    /**
     * Verifies the stopsNotifyingRemovedListener behavior.
     */
    @Test
    public void stopsNotifyingRemovedListener() {
        final BooleanModel model = new BooleanModel();
        final List<Boolean> observedValues = new ArrayList<>();
        final Listener<Boolean> listener = observedValues::add;
        model.addListener(listener);

        model.removeListener(listener);
        assertTrue(model.setData(true));

        assertTrue(observedValues.isEmpty());
    }

    /**
     * Verifies the derivesIndependentModelWithSpecifiedValue behavior.
     */
    @Test
    public void derivesIndependentModelWithSpecifiedValue() {
        final BooleanModel model = new BooleanModel();

        final Model<Boolean> derived = model.deriveWithData(true);

        assertNotSame(model, derived);
        assertEquals(Boolean.FALSE, model.getData());
        assertEquals(Boolean.TRUE, derived.getData());
    }

    /**
     * Verifies the createsReactiveInvertedModel behavior.
     */
    @Test
    public void createsReactiveInvertedModel() {
        final BooleanModel model = new BooleanModel();
        final Model<Boolean> inverted = model.invert();

        assertEquals(Boolean.TRUE, inverted.getData());
        assertTrue(model.setData(true));
        assertEquals(Boolean.FALSE, inverted.getData());
        assertTrue(inverted.setData(true));
        assertEquals(Boolean.FALSE, model.getData());
    }

    /**
     * Verifies the acceptsOnlyBooleanObjects behavior.
     */
    @Test
    public void acceptsOnlyBooleanObjects() {
        final BooleanModel model = new BooleanModel();

        assertFalse(model.setObject("true"));
        assertEquals(Boolean.FALSE, model.getData());
        assertTrue(model.setObject(Boolean.TRUE));
        assertEquals(Boolean.TRUE, model.getData());
    }
}
