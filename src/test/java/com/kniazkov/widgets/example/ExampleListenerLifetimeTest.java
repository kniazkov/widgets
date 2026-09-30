/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.model.BooleanModel;
import com.kniazkov.widgets.model.StringModel;
import com.kniazkov.widgets.model.ListenerLifetimeTest;
import com.kniazkov.widgets.view.TextWidget;
import java.lang.ref.Reference;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Examples must remain reactive after garbage collection, too.
 */
public class ExampleListenerLifetimeTest {
    /**
     * Verifies the transformed model is retained by its view.
     * @throws Exception if collection is interrupted
     */
    @Test
    public void customModelKeepsNotifyingItsView() throws Exception {
        final StringModel source = new StringModel("first");
        final TextWidget text = new TextWidget();
        text.setTextModel(new CustomModel.MyModel(source));
        ListenerLifetimeTest.collect();
        source.setData("second");
        assertEquals("DNOCES", text.getText());
        Reference.reachabilityFence(text);
    }

    /**
     * Verifies the caption receives checkbox state changes after collection.
     * @throws Exception if collection is interrupted
     */
    @Test
    public void checkboxCaptionKeepsItsListener() throws Exception {
        final BooleanModel source = new BooleanModel(false);
        final CheckBoxes.CheckCaption caption = new CheckBoxes.CheckCaption(source);
        ListenerLifetimeTest.collect();
        source.setData(true);
        assertEquals("Checked", caption.getText());
        source.setData(false);
        assertEquals("Unchecked", caption.getText());
        Reference.reachabilityFence(caption);
    }
}
