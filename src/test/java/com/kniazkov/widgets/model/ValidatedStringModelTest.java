/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests for standalone string models with built-in validation rules.
 */
@RunWith(Parameterized.class)
public final class ValidatedStringModelTest {
    private final Supplier<Model<String>> defaultFactory;
    private final Function<String, Model<String>> initializedFactory;
    private final List<String> validValues;
    private final List<String> invalidValues;

    /**
     * Creates one validation test case.
     *
     * @param name model name used in the test report
     * @param defaultFactory factory for a default model
     * @param initializedFactory factory for an initialized model
     * @param validValues values the model must accept as valid
     * @param invalidValues values the model must reject as invalid
     */
    public ValidatedStringModelTest(
        final String name,
        final Supplier<Model<String>> defaultFactory,
        final Function<String, Model<String>> initializedFactory,
        final List<String> validValues,
        final List<String> invalidValues
    ) {
        this.defaultFactory = defaultFactory;
        this.initializedFactory = initializedFactory;
        this.validValues = validValues;
        this.invalidValues = invalidValues;
    }

    /**
     * Provides all standalone string-validation models.
     *
     * @return test parameters
     */
    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> parameters() {
        return Arrays.asList(new Object[][] {
            {
                "EmailModel",
                (Supplier<Model<String>>)EmailModel::new,
                (Function<String, Model<String>>)EmailModel::new,
                Arrays.asList("alice@example.com", "A+B@sub.example.co"),
                Arrays.asList("", "alice@", "alice@example", "alice example.com")
            },
            {
                "NotEmptyStringModel",
                (Supplier<Model<String>>)NotEmptyStringModel::new,
                (Function<String, Model<String>>)NotEmptyStringModel::new,
                Arrays.asList("value", " value "),
                Arrays.asList("", "   ")
            },
            {
                "PhoneNumberModel",
                (Supplier<Model<String>>)PhoneNumberModel::new,
                (Function<String, Model<String>>)PhoneNumberModel::new,
                Arrays.asList("+12345678", "+123456789012345"),
                Arrays.asList("+1234567", "+1234567890123456", "12345678", "+1234A678")
            },
            {
                "UsernameModel",
                (Supplier<Model<String>>)UsernameModel::new,
                (Function<String, Model<String>>)UsernameModel::new,
                Arrays.asList("alice", " alice "),
                Arrays.asList("", "   ", "alice smith")
            }
        });
    }

    @Test
    public void usesEmptyInvalidDefaultValue() {
        final Model<String> model = this.defaultFactory.get();

        assertEquals("", model.getData());
        assertFalse(model.isValid());
    }

    @Test
    public void appliesValidationRules() {
        for (final String value : this.validValues) {
            assertTrue(value, this.initializedFactory.apply(value).isValid());
        }
        for (final String value : this.invalidValues) {
            assertFalse(value, this.initializedFactory.apply(value).isValid());
        }
    }

    @Test
    public void notifiesWhenValueAndValidityChange() {
        final Model<String> model = this.initializedFactory.apply(this.validValues.get(0));
        final List<String> observedValues = new ArrayList<>();
        final Listener<String> listener = observedValues::add;
        model.addListener(listener);

        assertTrue(model.setData(this.invalidValues.get(0)));
        assertFalse(model.isValid());
        assertFalse(model.setData(this.invalidValues.get(0)));

        assertEquals(Collections.singletonList(this.invalidValues.get(0)), observedValues);
        model.removeListener(listener);
    }

    @Test
    public void derivedModelPreservesValidationRules() {
        final Model<String> model = this.initializedFactory.apply(this.validValues.get(0));

        final Model<String> derived = model.deriveWithData(this.invalidValues.get(0));

        assertNotSame(model, derived);
        assertEquals(model.getClass(), derived.getClass());
        assertTrue(model.isValid());
        assertFalse(derived.isValid());
    }
}
