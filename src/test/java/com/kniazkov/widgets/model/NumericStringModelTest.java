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
import static org.junit.Assert.assertTrue;

/**
 * Tests for numeric-to-string model adapters.
 */
public final class NumericStringModelTest {
    /**
     * Verifies the integerAdapterSynchronizesInBothDirections behavior.
     */
    @Test
    public void integerAdapterSynchronizesInBothDirections() {
        final IntegerModel base = new IntegerModel(7);
        final IntegerToStringModel adapter = new IntegerToStringModel(base);
        final List<String> observed = new ArrayList<>();
        final Listener<String> listener = observed::add;
        adapter.addListener(listener);

        assertEquals("7", adapter.getData());
        assertTrue(adapter.isValid());

        assertTrue(adapter.setData("0042"));
        assertEquals(Integer.valueOf(42), base.getData());
        assertEquals("0042", adapter.getData());
        assertFalse(adapter.setData("0042"));

        assertTrue(base.setData(9));
        assertEquals("9", adapter.getData());
        assertEquals(Arrays.asList("0042", "9"), observed);
        adapter.removeListener(listener);
    }

    /**
     * Verifies the integerAdapterRecoversFromInvalidTextWhenBaseChanges behavior.
     */
    @Test
    public void integerAdapterRecoversFromInvalidTextWhenBaseChanges() {
        final IntegerModel base = new IntegerModel(7);
        final IntegerToStringModel adapter = new IntegerToStringModel(base);
        final List<String> observed = new ArrayList<>();
        final Listener<String> listener = observed::add;
        adapter.addListener(listener);

        assertTrue(adapter.setData("not an integer"));
        assertFalse(adapter.isValid());
        assertEquals(Integer.valueOf(7), base.getData());

        assertTrue(base.setData(8));
        assertTrue(adapter.isValid());
        assertEquals("8", adapter.getData());
        assertEquals(Arrays.asList("not an integer", "8"), observed);
        adapter.removeListener(listener);
    }

    /**
     * Verifies the integerAdapterRelaysBaseValidityChangesWithoutDataChanges behavior.
     */
    @Test
    public void integerAdapterRelaysBaseValidityChangesWithoutDataChanges() {
        final MutableTestModel<Integer> base = new MutableTestModel<>(7, false);
        final IntegerToStringModel adapter = new IntegerToStringModel(base);
        final List<String> observed = new ArrayList<>();
        final Listener<String> listener = observed::add;
        adapter.addListener(listener);

        assertFalse(adapter.isValid());
        assertTrue(base.setValid(true));

        assertTrue(adapter.isValid());
        assertEquals(Arrays.asList("7"), observed);
        adapter.removeListener(listener);
    }

    /**
     * Verifies the realAdapterSynchronizesInBothDirections behavior.
     */
    @Test
    public void realAdapterSynchronizesInBothDirections() {
        final RealNumberModel base = new RealNumberModel(1.25);
        final RealToStringModel adapter = new RealToStringModel(base);
        final List<String> observed = new ArrayList<>();
        final Listener<String> listener = observed::add;
        adapter.addListener(listener);

        assertEquals("1.25", adapter.getData());
        assertTrue(adapter.isValid());

        assertTrue(adapter.setData("01.500"));
        assertEquals(Double.valueOf(1.5), base.getData());
        assertEquals("01.500", adapter.getData());
        assertFalse(adapter.setData("01.500"));

        assertTrue(base.setData(2.75));
        assertEquals("2.75", adapter.getData());
        assertEquals(Arrays.asList("01.500", "2.75"), observed);
        adapter.removeListener(listener);
    }

    /**
     * Verifies the realAdapterRecoversFromInvalidTextWhenBaseChanges behavior.
     */
    @Test
    public void realAdapterRecoversFromInvalidTextWhenBaseChanges() {
        final RealNumberModel base = new RealNumberModel(1.25);
        final RealToStringModel adapter = new RealToStringModel(base);
        final List<String> observed = new ArrayList<>();
        final Listener<String> listener = observed::add;
        adapter.addListener(listener);

        assertTrue(adapter.setData("not a real number"));
        assertFalse(adapter.isValid());
        assertEquals(Double.valueOf(1.25), base.getData());

        assertTrue(base.setData(2.5));
        assertTrue(adapter.isValid());
        assertEquals("2.5", adapter.getData());
        assertEquals(Arrays.asList("not a real number", "2.5"), observed);
        adapter.removeListener(listener);
    }

    /**
     * Verifies the realAdapterRelaysBaseValidityChangesWithoutDataChanges behavior.
     */
    @Test
    public void realAdapterRelaysBaseValidityChangesWithoutDataChanges() {
        final MutableTestModel<Double> base = new MutableTestModel<>(1.25, false);
        final RealToStringModel adapter = new RealToStringModel(base);
        final List<String> observed = new ArrayList<>();
        final Listener<String> listener = observed::add;
        adapter.addListener(listener);

        assertFalse(adapter.isValid());
        assertTrue(base.setValid(true));

        assertTrue(adapter.isValid());
        assertEquals(Arrays.asList("1.25"), observed);
        adapter.removeListener(listener);
    }

    /**
     * Verifies the adaptersDerivePlainStringModels behavior.
     */
    @Test
    public void adaptersDerivePlainStringModels() {
        final Model<String> integerDerived = new IntegerToStringModel(
            new IntegerModel()
        ).deriveWithData("12");
        final Model<String> realDerived = new RealToStringModel(
            new RealNumberModel()
        ).deriveWithData("1.5");

        assertTrue(integerDerived instanceof StringModel);
        assertEquals("12", integerDerived.getData());
        assertTrue(realDerived instanceof StringModel);
        assertEquals("1.5", realDerived.getData());
    }
}
