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
 * Tests for validated integer and real-number models.
 */
public final class ValidatedNumberModelTest {
    @Test
    public void validatesIntegerBoundaries() {
        assertFalse(new ValidatedIntegerModel(-1,
            ValidatedIntegerModel.NOT_NEGATIVE).isValid());
        assertTrue(new ValidatedIntegerModel(0,
            ValidatedIntegerModel.NOT_NEGATIVE).isValid());
        assertFalse(new ValidatedIntegerModel(0,
            ValidatedIntegerModel.POSITIVE).isValid());
        assertTrue(new ValidatedIntegerModel(1,
            ValidatedIntegerModel.POSITIVE).isValid());
    }

    @Test
    public void validatesRealNumberBoundaries() {
        assertFalse(new ValidatedRealNumberModel(-0.1,
            ValidatedRealNumberModel.NOT_NEGATIVE).isValid());
        assertTrue(new ValidatedRealNumberModel(0.0,
            ValidatedRealNumberModel.NOT_NEGATIVE).isValid());
        assertFalse(new ValidatedRealNumberModel(0.0,
            ValidatedRealNumberModel.POSITIVE).isValid());
        assertTrue(new ValidatedRealNumberModel(0.1,
            ValidatedRealNumberModel.POSITIVE).isValid());
        assertTrue(new ValidatedRealNumberModel(0.0,
            ValidatedRealNumberModel.UNIT_INTERVAL).isValid());
        assertTrue(new ValidatedRealNumberModel(1.0,
            ValidatedRealNumberModel.UNIT_INTERVAL).isValid());
        assertFalse(new ValidatedRealNumberModel(1.1,
            ValidatedRealNumberModel.UNIT_INTERVAL).isValid());
    }

    @Test
    public void usesCriterionToValidateDefaultValue() {
        assertTrue(new ValidatedIntegerModel(
            ValidatedIntegerModel.NOT_NEGATIVE).isValid());
        assertFalse(new ValidatedIntegerModel(
            ValidatedIntegerModel.POSITIVE).isValid());
        assertTrue(new ValidatedRealNumberModel(
            ValidatedRealNumberModel.UNIT_INTERVAL).isValid());
        assertFalse(new ValidatedRealNumberModel(
            ValidatedRealNumberModel.POSITIVE).isValid());
    }

    @Test
    public void derivedModelsPreserveCustomCriteria() {
        final ValidatedIntegerModel integers = new ValidatedIntegerModel(2,
            value -> value % 2 == 0);
        final ValidatedRealNumberModel reals = new ValidatedRealNumberModel(0.5,
            value -> value < 1.0);

        final Model<Integer> derivedIntegers = integers.deriveWithData(3);
        final Model<Double> derivedReals = reals.deriveWithData(2.0);

        assertNotSame(integers, derivedIntegers);
        assertNotSame(reals, derivedReals);
        assertEquals(ValidatedIntegerModel.class, derivedIntegers.getClass());
        assertEquals(ValidatedRealNumberModel.class, derivedReals.getClass());
        assertFalse(derivedIntegers.isValid());
        assertFalse(derivedReals.isValid());
    }

    @Test
    public void notifiesListenersWhenValidatedValuesChange() {
        final ValidatedIntegerModel integers = new ValidatedIntegerModel(1,
            ValidatedIntegerModel.POSITIVE);
        final ValidatedRealNumberModel reals = new ValidatedRealNumberModel(0.5,
            ValidatedRealNumberModel.UNIT_INTERVAL);
        final List<Integer> observedIntegers = new ArrayList<>();
        final List<Double> observedReals = new ArrayList<>();
        final Listener<Integer> integerListener = observedIntegers::add;
        final Listener<Double> realListener = observedReals::add;
        integers.addListener(integerListener);
        reals.addListener(realListener);

        assertTrue(integers.setData(-1));
        assertTrue(reals.setData(2.0));

        assertEquals(Arrays.asList(-1), observedIntegers);
        assertEquals(Arrays.asList(2.0), observedReals);
        assertFalse(integers.isValid());
        assertFalse(reals.isValid());
        integers.removeListener(integerListener);
        reals.removeListener(realListener);
    }
}
