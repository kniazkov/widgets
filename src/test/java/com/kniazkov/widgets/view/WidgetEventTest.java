/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.controller.HandlesPointerEvents;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests synthetic browser events inside a widget sandbox.
 */
public final class WidgetEventTest {
    /**
     * Verifies the editableTextWidgetsApplyEventBeforeController behavior.
     */
    @Test
    public void editableTextWidgetsApplyEventBeforeController() {
        final List<Widget<?>> widgets = Arrays.<Widget<?>>asList(
            new InputField("before"),
            new PasswordInput("before"),
            new TextArea("before")
        );

        for (final Widget<?> widget : widgets) {
            final HasTextInput input = (HasTextInput) widget;
            final AtomicReference<String> modelValueSeenByController = new AtomicReference<>();
            input.onTextInput(value -> modelValueSeenByController.set(input.getText()));
            final WidgetSandbox<?> sandbox = WidgetSandbox.open(widget);
            sandbox.clearUpdates();
            final JsonObject data = new JsonObject();
            data.addString("text", "after");

            sandbox.fire(Event.TEXT_INPUT, data);

            assertEquals("after", input.getText());
            assertEquals("after", modelValueSeenByController.get());
            assertEquals(1, WidgetSandbox.findUpdates(
                sandbox.drainUpdates(), "set text", widget
            ).size());
        }
    }

    /**
     * Verifies the checkboxAppliesEventBeforeController behavior.
     */
    @Test
    public void checkboxAppliesEventBeforeController() {
        final CheckBox checkBox = new CheckBox();
        final AtomicReference<Boolean> modelValueSeenByController = new AtomicReference<>();
        checkBox.setController(
            Event.CHECK,
            value -> modelValueSeenByController.set(checkBox.isChecked())
        );
        final WidgetSandbox<CheckBox> sandbox = WidgetSandbox.open(checkBox);
        sandbox.clearUpdates();
        final JsonObject data = new JsonObject();
        data.addBoolean("state", true);

        sandbox.fire(Event.CHECK, data);

        assertTrue(checkBox.isChecked());
        assertEquals(Boolean.TRUE, modelValueSeenByController.get());
        assertEquals(1, WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "set checked", checkBox
        ).size());
    }

    /**
     * Verifies the allPointerWidgetsSubscribeAndHandleClicks behavior.
     */
    @Test
    public void allPointerWidgetsSubscribeAndHandleClicks() {
        final List<Widget<?>> widgets = Arrays.<Widget<?>>asList(
            new Panel(),
            new InlineBlock(),
            new Row(),
            new Cell(),
            new ActiveText(),
            new ActiveImage("active.png"),
            new Button(),
            new FileLoader(),
            new CheckBox(),
            new InputField(),
            new PasswordInput(),
            new TextArea()
        );

        for (final Widget<?> widget : widgets) {
            final AtomicInteger calls = new AtomicInteger();
            final WidgetSandbox<?> sandbox = WidgetSandbox.open(widget);
            sandbox.clearUpdates();

            ((HandlesPointerEvents) widget).onClick(event -> calls.incrementAndGet());

            final List<JsonObject> subscription = WidgetSandbox.findUpdates(
                sandbox.drainUpdates(), "subscribe", widget
            );
            assertEquals("subscription count for " + widget.getType(), 1, subscription.size());
            assertEquals("click", subscription.get(0).get("event").getStringValue());

            sandbox.fire(Event.CLICK, new JsonObject());

            assertEquals("click count for " + widget.getType(), 1, calls.get());
        }
    }

    /**
     * File selection must be visible before any binary chunk arrives.
     */
    @Test
    public void fileLoaderReportsSelectionAtZeroPercent() {
        final FileLoader loader = new FileLoader();
        final AtomicReference<UploadingFile> selected = new AtomicReference<>();
        loader.onSelect(selected::set);
        final WidgetSandbox<FileLoader> sandbox = WidgetSandbox.open(loader);
        sandbox.clearUpdates();
        final int chunkSize = new Options.Builder().build().getChunkSize();
        final JsonObject data = new JsonObject();
        data.addNumber("fileId", 7);
        data.addString("name", "data.bin");
        data.addString("type", "application/octet-stream");
        data.addNumber("size", chunkSize + 1);
        data.addNumber("totalChunks", 2);

        sandbox.fire(Event.UPLOAD, data);

        assertEquals("data.bin", selected.get().getName());
        assertEquals("application/octet-stream", selected.get().getType());
        assertEquals(chunkSize + 1, selected.get().getSize());
        assertEquals(Integer.valueOf(0), selected.get().getLoadingPercentageModel().getData());
        assertTrue(sandbox.drainUpdates().isEmpty());
        assertSame(loader, sandbox.getSubject());
    }
}
