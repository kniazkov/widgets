/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.InputMode;
import com.kniazkov.widgets.model.InputModeModel;
import com.kniazkov.widgets.model.StringModel;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Hints are reactive properties, independent of the entered text.
 */
public final class InputHintsTest {
    /**
     * Checks reactive hints and model rebinding.
     */
    @Test public void inheritsAndRebindsHintModels() {
        final InputFieldStyle style = InputField.getDefaultStyle().derive();
        style.setPlaceholder("0.00");
        style.setInputMode(InputMode.DECIMAL);
        final InputField field = new InputField(style, "12.50");
        assertEquals("0.00", field.getPlaceholder());
        assertEquals(InputMode.DECIMAL, field.getInputMode());
        final var sandbox = WidgetSandbox.open(field);
        final StringModel placeholder = new StringModel("Amount");
        final InputModeModel mode = new InputModeModel(InputMode.NUMERIC);
        field.setPlaceholderModel(placeholder);
        field.setInputModeModel(mode);
        sandbox.clearUpdates();
        placeholder.setData("Enter amount");
        mode.setData(InputMode.DECIMAL);
        assertEquals(2, sandbox.drainUpdates().size());
        assertEquals("12.50", field.getText());
        field.setPlaceholderModel(new StringModel("New"));
        field.setInputModeModel(new InputModeModel());
        sandbox.clearUpdates();
        placeholder.setData("Old");
        mode.setData(InputMode.NONE);
        assertEquals(0, sandbox.drainUpdates().size());
        assertEquals("", new SuggestionField().getPlaceholder());
        assertEquals(InputMode.TEXT, new SuggestionField().getInputMode());
    }
}
