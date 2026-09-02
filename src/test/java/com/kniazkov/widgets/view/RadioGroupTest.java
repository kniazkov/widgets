/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.BooleanModel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests radio-button mutual exclusion and model subscriptions.
 */
public final class RadioGroupTest {
    /**
     * Selecting a button through its model must clear the previous selection.
     */
    @Test
    public void keepsAtMostOneButtonSelected() {
        final RadioButton first = new RadioButton();
        final RadioButton second = new RadioButton();
        final RadioButton third = new RadioButton();
        try (RadioGroup group = new RadioGroup(first, second, third)) {
            first.check();
            assertTrue(first.isChecked());

            second.getCheckedStateModel().setData(true);
            assertFalse(first.isChecked());
            assertTrue(second.isChecked());
            assertFalse(third.isChecked());

            second.uncheck();
            assertFalse(first.isChecked());
            assertFalse(second.isChecked());
            assertFalse(third.isChecked());
            assertEquals(3, group.getButtons().size());
        }
    }

    /**
     * When initially selected buttons are added, the last one must win.
     */
    @Test
    public void lastInitiallySelectedButtonWins() {
        final RadioButton first = new RadioButton();
        final RadioButton second = new RadioButton();
        first.check();
        second.check();

        try (RadioGroup ignored = new RadioGroup(first, second)) {
            assertFalse(first.isChecked());
            assertTrue(second.isChecked());
        }
    }

    /**
     * Replacing a button model must move the group subscription to the new model.
     */
    @Test
    public void followsCheckedStateModelReplacement() {
        final RadioButton first = new RadioButton();
        final RadioButton second = new RadioButton();
        first.check();
        try (RadioGroup ignored = new RadioGroup(first, second)) {
            final BooleanModel replacement = new BooleanModel(true);

            second.setCheckedStateModel(replacement);

            assertFalse(first.isChecked());
            assertTrue(second.isChecked());
            replacement.setData(false);
            assertFalse(second.isChecked());
        }
    }

    /**
     * Removed buttons must no longer affect their former group.
     */
    @Test
    public void unsubscribesRemovedButtons() {
        final RadioButton first = new RadioButton();
        final RadioButton second = new RadioButton();
        try (RadioGroup group = new RadioGroup(first, second)) {
            assertTrue(group.remove(second));
            first.check();
            second.check();

            assertTrue(first.isChecked());
            assertTrue(second.isChecked());
            assertFalse(group.remove(second));
        }
    }

    /**
     * Sharing one model would make mutual exclusion impossible and must be rejected.
     */
    @Test
    public void rejectsSharedCheckedStateModel() {
        final BooleanModel shared = new BooleanModel(false);
        final RadioButton first = new RadioButton(shared);
        final RadioButton second = new RadioButton(shared);
        try (RadioGroup group = new RadioGroup(first)) {
            assertThrows(IllegalArgumentException.class, () -> group.add(second));
        }
    }
}
