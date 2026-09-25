/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.model.BooleanModel;
import com.kniazkov.widgets.model.StringModel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Exercises composite bindings, caption actions, disabled styling and radio membership.
 */
public final class LabeledChoiceTest {
    /**
     * Model identity, replacement and bidirectional updates survive composition.
     */
    @Test
    public void delegatesModelsWithoutCopyingTheirData() {
        final StringModel text = new StringModel("News");
        final BooleanModel checked = new BooleanModel(true);
        final CheckBoxWithText box = new CheckBoxWithText(text, checked);
        assertSame(text, box.getTextModel());
        assertSame(text, box.getTextWidget().getTextModel());
        assertSame(checked, box.getCheckBox().getCheckedStateModel());
        text.setData("Updates");
        assertEquals("Updates", box.getText());
        box.uncheck();
        assertFalse(checked.getData());
        box.getCheckBox().check();
        assertTrue(box.isChecked());
        final BooleanModel replacement = new BooleanModel(false);
        box.setCheckedStateModel(replacement);
        box.check();
        assertTrue(replacement.getData());
        final StringModel renamed = new StringModel("Offers");
        box.setTextModel(renamed);
        box.setText("Special offers");
        assertEquals("Special offers", renamed.getData());
        assertEquals("Updates", text.getData());
    }

    /**
     * Disabled changes affect both children and do not destroy configured normal colors.
     */
    @Test
    public void sharesDisabledStateAndPreservesCaptionStyling() {
        final Color defaultColor = TextWidget.getDefaultStyle().getColor();
        for (final LabeledChoice<?> choice : new LabeledChoice<?>[] {
            new CheckBoxWithText("News"), new RadioButtonWithText("Delivery")
        }) {
            assertEquals(defaultColor, choice.getTextWidget().getColor());
            final BooleanModel disabled = new BooleanModel(true);
            choice.setDisabledStateModel(disabled);
            assertSame(disabled, choice.control().getDisabledStateModel());
            assertSame(disabled, choice.getTextWidget().getModel(State.ANY, Property.DISABLED));
            choice.getTextWidget().setColor(State.NORMAL, Color.RED);
            assertEquals(Color.GRAY, choice.getTextWidget().getColor(State.DISABLED));
            choice.getTextWidget().getController(Event.CLICK).handleEvent(null);
            assertFalse(choice.isChecked());
            disabled.setData(false);
            choice.getTextWidget().getController(Event.CLICK).handleEvent(null);
            assertTrue(choice.isChecked());
            assertEquals(Color.RED, choice.getTextWidget().getColor());
            final BooleanModel replacement = new BooleanModel(true);
            choice.setDisabledStateModel(replacement);
            disabled.setData(false);
            assertTrue(choice.isDisabled());
            choice.enable();
            assertFalse(replacement.getData());
            choice.control().disable();
            assertTrue(choice.getTextWidget().getModel(State.ANY, Property.DISABLED).getData());
        }
        assertEquals(defaultColor, TextWidget.getDefaultStyle().getColor());
    }

    /**
     * Caption clicks toggle checkboxes, while radio captions obey group exclusivity.
     */
    @Test
    public void captionActionsMatchTheirSelectionControls() {
        final CheckBoxWithText checkbox = new CheckBoxWithText("News");
        checkbox.getTextWidget().getController(Event.CLICK).handleEvent(null);
        assertTrue(checkbox.isChecked());
        checkbox.getTextWidget().getController(Event.CLICK).handleEvent(null);
        assertFalse(checkbox.isChecked());
        final RadioButtonWithText first = new RadioButtonWithText("First");
        final RadioButtonWithText second = new RadioButtonWithText("Second");
        try (RadioGroup group = new RadioGroup(first.getRadioButton(), second.getRadioButton())) {
            first.check();
            second.getTextWidget().getController(Event.CLICK).handleEvent(null);
            assertFalse(first.isChecked());
            assertTrue(second.isChecked());
            second.getTextWidget().getController(Event.CLICK).handleEvent(null);
            assertTrue(second.isChecked());
            final BooleanModel replacement = new BooleanModel(false);
            first.setCheckedStateModel(replacement);
            replacement.setData(true);
            assertTrue(first.isChecked());
            assertFalse(second.isChecked());
            assertEquals(2, group.getButtons().size());
        }
    }
}
