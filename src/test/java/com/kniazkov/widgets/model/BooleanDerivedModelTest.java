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
 * Tests for boolean models derived from other models.
 */
public final class BooleanDerivedModelTest {
    @Test
    public void invertedModelTransformsReadsWritesValidityAndUpdates() {
        final MutableTestModel<Boolean> base = new MutableTestModel<>(true, false);
        final Model<Boolean> inverted = new InvertModel(base);
        final List<Boolean> observed = new ArrayList<>();
        final Listener<Boolean> listener = observed::add;
        inverted.addListener(listener);

        assertFalse(inverted.getData());
        assertFalse(inverted.isValid());

        assertTrue(base.setData(false));
        assertTrue(inverted.getData());
        assertTrue(inverted.setData(false));
        assertTrue(base.getData());
        assertFalse(inverted.setData(false));

        assertEquals(Arrays.asList(Boolean.TRUE, Boolean.FALSE), observed);
        inverted.removeListener(listener);
    }

    @Test
    public void conjunctionCombinesValuesAndRequiresEveryModelToBeValid() {
        final MutableTestModel<Boolean> first = new MutableTestModel<>(true);
        final MutableTestModel<Boolean> second = new MutableTestModel<>(true);
        final MutableTestModel<Boolean> third = new MutableTestModel<>(true);
        final Model<Boolean> conjunction = new ConjunctionModel(first, second, third);
        final List<Boolean> observed = new ArrayList<>();
        final Listener<Boolean> listener = observed::add;
        conjunction.addListener(listener);

        assertTrue(conjunction.getData());
        assertTrue(conjunction.isValid());
        assertTrue(second.setData(false));
        assertFalse(conjunction.getData());
        assertTrue(third.setValid(false));
        assertFalse(conjunction.isValid());
        assertFalse(conjunction.setData(true));

        assertEquals(Arrays.asList(Boolean.FALSE, Boolean.FALSE), observed);
        conjunction.removeListener(listener);
    }

    @Test
    public void emptyConjunctionUsesLogicalIdentity() {
        final Model<Boolean> conjunction = new ConjunctionModel();

        assertTrue(conjunction.getData());
        assertTrue(conjunction.isValid());
    }

    @Test
    public void disjunctionCombinesValuesAndAcceptsAnyValidModel() {
        final MutableTestModel<Boolean> first = new MutableTestModel<>(false, false);
        final MutableTestModel<Boolean> second = new MutableTestModel<>(false, false);
        final Model<Boolean> disjunction = new DisjunctionModel(first, second);
        final List<Boolean> observed = new ArrayList<>();
        final Listener<Boolean> listener = observed::add;
        disjunction.addListener(listener);

        assertFalse(disjunction.getData());
        assertFalse(disjunction.isValid());
        assertTrue(first.setValid(true));
        assertTrue(disjunction.isValid());
        assertTrue(second.setData(true));
        assertTrue(disjunction.getData());
        assertFalse(disjunction.setData(false));

        assertEquals(Arrays.asList(Boolean.FALSE, Boolean.TRUE), observed);
        disjunction.removeListener(listener);
    }

    @Test
    public void emptyDisjunctionUsesLogicalIdentity() {
        final Model<Boolean> disjunction = new DisjunctionModel();

        assertFalse(disjunction.getData());
        assertFalse(disjunction.isValid());
    }

    @Test
    public void validFlagReflectsValidityAndRemainsReadOnly() {
        final MutableTestModel<String> base = new MutableTestModel<>("value", false);
        final Model<Boolean> validFlag = base.getValidFlagModel();
        final List<Boolean> observed = new ArrayList<>();
        final Listener<Boolean> listener = observed::add;
        validFlag.addListener(listener);

        assertFalse(validFlag.getData());
        assertTrue(validFlag.isValid());
        assertFalse(validFlag.setData(true));

        assertTrue(base.setValid(true));
        assertTrue(base.setData("changed"));

        assertTrue(validFlag.getData());
        assertEquals(Arrays.asList(Boolean.TRUE, Boolean.TRUE), observed);
        validFlag.removeListener(listener);
    }

    @Test
    public void predicateNotifiesOnlyWhenDerivedValueChanges() {
        final MutableTestModel<String> base = new MutableTestModel<>("a");
        final PredicateModel<String> predicate = new PredicateModel<>(
            base,
            value -> value.length() >= 3
        );
        final List<Boolean> observed = new ArrayList<>();
        final Listener<Boolean> listener = observed::add;
        predicate.addListener(listener);

        assertFalse(predicate.getData());
        assertTrue(predicate.isValid());
        assertTrue(base.setData("abc"));
        assertTrue(base.setData("abcd"));
        assertTrue(base.setValid(false));

        assertFalse(predicate.getData());
        assertFalse(predicate.setData(true));
        assertEquals(Arrays.asList(Boolean.TRUE, Boolean.FALSE), observed);
        assertTrue(predicate.invert().getData());
        predicate.removeListener(listener);
    }
}
