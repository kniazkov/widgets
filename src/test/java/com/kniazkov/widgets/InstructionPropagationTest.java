package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
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
        final Paragraph paragraph = new Paragraph();
        root.appendChild(paragraph);
        final TextWidget label = new TextWidget("Enter value:");
        paragraph.appendChild(label);
        final InputField input = new InputField();
        paragraph.appendChild(input);
        final IntegerModel model = new IntegerModel();
        input.setTextModel(model);
        model.setIntValue(13);
        final Button button = new Button("Submit");
        paragraph.appendChild(button);
        final List<Instruction> updates = client.collectUpdates();
        Assert.assertFalse(updates.isEmpty());
    }
}
