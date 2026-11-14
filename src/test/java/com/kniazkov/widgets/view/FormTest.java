/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.model.NotEmptyStringModel;
import com.kniazkov.widgets.model.StringModel;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * Page-owned form validation and asynchronous submission behavior.
 */
public class FormTest {
    /**
     * Checks the field model and immutable cross-field validation snapshot.
     */
    @Test
    public void validatesRequiredModelAndCrossFieldRulesBeforeDispatch() {
        final Form form = new Form("Samples", "Apply");
        final FormField first = new FormField("first", "First");
        final FormField second = new FormField("second", "Confirmation");
        final NotEmptyStringModel text = new NotEmptyStringModel();
        first.getEditor().setTextModel(text);
        first.setRequired(true);
        first.setMaxLength(8);
        form.addField(first);
        form.addField(second);
        final RootWidget root = new RootWidget(form);
        final AtomicInteger sent = new AtomicInteger();
        form.setValidator(values -> values.get("first").equals(values.get("second"))
            ? Form.Result.success() : new Form.Result(Map.of("second", "Different"), ""));
        form.onSubmit(values -> {
            assertEquals(Map.of("first", "blue", "second", "blue"), values);
            assertThrows(UnsupportedOperationException.class, () -> values.put("first", "red"));
            sent.incrementAndGet();
            return CompletableFuture.completedFuture(Form.Result.success());
        });
        form.submit();
        assertEquals("This field is required.", first.getError());
        assertTrue(first.isTouched());
        text.setData("too much text");
        assertTrue(first.isDirty());
        form.submit();
        assertEquals("Check the value of this field.", first.getError());
        text.setData("blue");
        form.submit();
        assertEquals("", first.getError());
        assertEquals("Different", second.getError());
        assertEquals(0, sent.get());
        second.getEditor().setText("blue");
        form.submit();
        assertEquals(1, sent.get());
        assertFalse(first.isDirty());
        assertFalse(first.isTouched());
        root.closePage();
    }

    /**
     * Checks independent blur handling and read-only versus disabled participation.
     */
    @Test
    public void tracksTouchWithoutReplacingBlurAndIncludesReadOnlyButNotDisabled() {
        final Form form = new Form("Samples", "Apply");
        final FormField fixed = new FormField("fixed", "Reference", new InputField("DEMO"));
        final FormField optional = new FormField("optional", "Optional");
        final AtomicInteger blur = new AtomicInteger();
        final StringModel text = new StringModel("");
        optional.getEditor().setTextModel(text);
        optional.getEditor().onBlur(event -> blur.incrementAndGet());
        fixed.setReadOnly(true);
        form.addField(fixed);
        form.addField(optional);
        final RootWidget root = new RootWidget(form);
        assertFalse(optional.isTouched());
        optional.getEditor().handleEvent(Event.BLUR, new JsonObject());
        assertEquals(1, blur.get());
        assertTrue(optional.isTouched());
        text.setData("new");
        assertTrue(optional.isDirty());
        optional.getEditor().setDisabledFlag(true);
        assertThrows(IllegalArgumentException.class,
            () -> form.addField(new FormField("fixed", "Duplicate")));
        form.onSubmit(values -> {
            assertEquals(Map.of("fixed", "DEMO"), values);
            return CompletableFuture.completedFuture(Form.Result.success());
        });
        form.submit();
        assertEquals("Submitted.", form.getStatus());
        assertTrue(fixed.getEditor().isReadOnly());
        root.closePage();
    }

    /**
     * Rejects attempts while busy and keeps edits newer than an asynchronous result.
     */
    @Test
    public void serializesIndependentAttemptsAndDoesNotApplyStaleErrorsOrResetInput() {
        final Form form = new Form("Samples", "Apply");
        final FormField field = new FormField("title", "Name");
        form.addField(field);
        final RootWidget root = new RootWidget(form);
        final CompletableFuture<Form.Result> first = new CompletableFuture<>();
        final CompletableFuture<Form.Result> second = new CompletableFuture<>();
        final AtomicInteger calls = new AtomicInteger();
        form.onSubmit(values -> calls.incrementAndGet() == 1 ? first : second);
        field.getEditor().setText("paper");
        form.submit();
        assertTrue(form.isSubmitting());
        form.submit();
        assertEquals(1, calls.get());
        field.getEditor().setText("blue");
        first.complete(new Form.Result(Map.of("title", "Unavailable"), "Try again"));
        assertEquals("Try again", form.getStatus());
        assertEquals("", field.getError());
        assertEquals("blue", field.getEditor().getText());
        assertTrue(field.isDirty());
        form.submit();
        assertEquals(2, calls.get());
        second.complete(new Form.Result(Map.of("title", "Unavailable"), ""));
        assertFalse(form.isSubmitting());
        assertEquals("Unavailable", field.getError());
        assertEquals("blue", field.getEditor().getText());
        root.closePage();
    }

    /**
     * Handles exceptional completion and ignores a response after page disposal.
     */
    @Test
    public void failureAndRetirementDoNotLeakExceptionsOrRepeatTheServiceCall() {
        final Form form = new Form("Samples", "Apply");
        final FormField field = new FormField("title", "Name");
        form.addField(field);
        final RootWidget root = new RootWidget(form);
        form.onSubmit(values -> CompletableFuture.failedFuture(new IllegalStateException()));
        form.submit();
        assertFalse(form.isSubmitting());
        assertEquals("Submission unavailable. Please try again.", form.getStatus());
        final CompletableFuture<Form.Result> pending = new CompletableFuture<>();
        form.onSubmit(values -> pending);
        form.submit();
        assertTrue(form.isSubmitting());
        root.closePage();
        pending.complete(Form.Result.success());
        assertFalse(form.isSubmitting());
    }
}
