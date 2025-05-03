/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Testing the instruction propagation chain.
 */
public class InstructionPropagationTest {
    /**
     * Testing the instruction propagation chain.
     */
    @Test
    public void test() {
        final Client client = new Client();
        final RootWidget root = client.getRootWidget();
        final Paragraph paragraph = root.createParagraph();
        final TextWidget label = paragraph.createTextWidget("Enter value:");
        final InputField input = paragraph.createInputField();
        final IntegerViaStringModel model = new IntegerViaStringModel();
        input.setTextModel(model);
        model.setIntValue(13);
        final Button button = paragraph.createButton("Submit");
        final List<Instruction> updates = client.collectUpdates();
        Assert.assertFalse(updates.isEmpty());
    }
}
