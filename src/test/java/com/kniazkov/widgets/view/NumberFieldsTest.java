/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.NumberModel;
import com.kniazkov.widgets.model.BooleanModel;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.model.NumberValue;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

/**
 * Exact numbers, explicit invalid drafts, reactive grids and owned quantity form controls.
 */
public final class NumberFieldsTest {
    /**
     * Large decimals survive round trips and malformed payloads never retain old typed data.
     */
    @Test
    public void precisionAndConversion() {
        final NumberField field = new NumberField();
        final BigDecimal large = new BigDecimal("90071992547409931234567890.01");
        field.setValue(large);
        assertEquals(large, field.getValue().orElseThrow());
        for (final String raw : new String[] {"-", ".", "-.", "1.", "1e9", "NaN", "Infinity",
                "+1", " 1", "1,000", "１２", "1\u0000", "--1"}) {
            field.setText(raw);
            assertEquals(NumberModel.Status.CONVERSION, field.getValueModel().status());
            assertTrue(field.getValue().isEmpty());
            assertEquals(raw, field.getText());
        }
        field.setText("9".repeat(257));
        assertEquals(NumberModel.Status.PRECISION, field.getValueModel().status());
        field.setText("0." + "0".repeat(64) + "1");
        assertEquals(NumberModel.Status.PRECISION, field.getValueModel().status());
        field.setText("9".repeat(513));
        assertEquals(NumberModel.Status.PRECISION, field.getValueModel().status());
        assertThrows(IllegalArgumentException.class,
            () -> field.setValue(new BigDecimal("1E+1000000")));
        field.clear();
        assertTrue(field.getValueModel().isValid());
        assertEquals(NumberModel.Status.EMPTY, field.getValueModel().status());
    }

    /**
     * Locale conversion is explicit and preserves scale; grouping is not silently stripped.
     */
    @Test
    public void localeAndDrafts() {
        final NumberField field = new NumberField();
        field.setLocale(Locale.forLanguageTag("ru"));
        field.setText("12,50");
        assertEquals(new BigDecimal("12.50"), field.getValue().orElseThrow());
        field.setLocale(Locale.US);
        assertEquals("12.50", field.getText());
        field.setText("12.");
        assertThrows(IllegalStateException.class,
            () -> field.setLocale(Locale.forLanguageTag("ru")));
        assertEquals("12.", field.getText());
        field.clear();
        field.setLocale(Locale.forLanguageTag("ru"));
        for (final String value : new String[] {"1.25", "1 000,25", "1\u00a0000,25", ",", "-,"}) {
            field.setText(value);
            assertEquals(NumberModel.Status.CONVERSION, field.getValueModel().status());
        }
        assertThrows(NullPointerException.class, () -> field.setValue(null));
    }

    /**
     * Inclusive bounds, origin-relative steps and atomic invalid configurations never clamp.
     */
    @Test
    public void exactGridAndReactiveBounds() {
        final NumberModel model = NumberModel.decimals();
        model.configure(NumberValue.of(new BigDecimal("0.05")),
            NumberValue.of(new BigDecimal("0.36")), new BigDecimal("0.10"));
        model.setData(NumberValue.of(new BigDecimal("0.05")));
        assertFalse(model.move(-1));
        assertTrue(model.move(1));
        assertEquals(new BigDecimal("0.15"), model.value().orElseThrow());
        model.move(1);
        model.move(1);
        assertFalse(model.move(1));
        assertEquals(new BigDecimal("0.35"), model.value().orElseThrow());
        model.textModel().setData("0.36");
        assertEquals(NumberModel.Status.STEP, model.status());
        model.textModel().setData("0.04");
        assertEquals(NumberModel.Status.BOUNDS, model.status());
        model.textModel().setData("0.35");
        model.configure(NumberValue.of(new BigDecimal("0.05")),
            NumberValue.of(new BigDecimal("0.25")), new BigDecimal("0.10"));
        assertEquals("0.35", model.textModel().getData());
        assertEquals(NumberModel.Status.BOUNDS, model.status());
        assertFalse(model.move(-1));
        assertThrows(IllegalArgumentException.class,
            () -> model.configure(NumberValue.empty(), NumberValue.empty(), BigDecimal.ZERO));
        assertEquals(NumberModel.Status.BOUNDS, model.status());
        model.textModel().setData("");
        assertTrue(model.move(1));
        assertEquals(new BigDecimal("0.05"), model.value().orElseThrow());
    }

    /**
     * Integer arithmetic does not overflow a long, and copied constraints are independent.
     */
    @Test
    public void integerAndSharedModel() {
        final NumberModel model = NumberModel.integers();
        final NumberField first = new NumberField(model);
        final NumberField mirror = new NumberField(model);
        first.setText("9223372036854775807");
        assertTrue(model.move(1));
        assertEquals("9223372036854775808", mirror.getText());
        first.setText("1.0");
        assertFalse(model.isValid());
        assertThrows(IllegalArgumentException.class,
            () -> first.setValue(new BigDecimal("1.0")));
        first.setValue(BigDecimal.TEN);
        final var copy = model.deriveWithData(NumberValue.of(BigDecimal.ONE));
        assertEquals(BigDecimal.ONE, copy.getData().value().orElseThrow());
        assertEquals(BigDecimal.TEN, mirror.getValue().orElseThrow());
        first.setText("9".repeat(256));
        assertFalse(model.move(1));
        assertThrows(IllegalArgumentException.class, () -> model.move(0));
    }

    /**
     * Quantity is a normal FormField with parse errors distinct from bounds/step failures.
     */
    @Test
    public void formAndLifecycle() {
        final QuantityField quantity = new QuantityField("count", "Count");
        quantity.setRequired(true);
        quantity.getEditor().setConversionMessage("Not a number");
        quantity.setConstraintMessages("Required", "Outside bounds");
        final Form form = new Form("Numbers", "Save");
        form.addField(quantity);
        final RootWidget root = new RootWidget(form);
        final var sandbox = WidgetSandbox.open(root);
        final AtomicInteger calls = new AtomicInteger();
        form.onSubmit(values -> {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture(Form.Result.success());
        });
        form.submit();
        assertEquals("Required", quantity.getError());
        quantity.getEditor().setText("-");
        form.submit();
        assertEquals("Not a number", quantity.getError());
        assertEquals(0, calls.get());
        quantity.getEditor().setValue(BigDecimal.ONE);
        form.submit();
        assertEquals(1, calls.get());
        quantity.getEditor().getValueModel().configure(NumberValue.of(BigDecimal.TEN),
            NumberValue.empty(), BigDecimal.ONE);
        form.submit();
        assertEquals("Outside bounds", quantity.getError());
        assertEquals(1, calls.get());
        quantity.dispose();
        sandbox.clearUpdates();
        quantity.getEditor().getValueModel().configure(NumberValue.empty(), NumberValue.empty(),
            BigDecimal.ONE);
        assertTrue(sandbox.drainUpdates().isEmpty());
        root.closePage();
    }
    /**
     * Replaced disabled models and queued clicks cannot bypass read-only or updated limits.
     */
    @Test
    public void quantityActionsRecheckCurrentState() {
        final QuantityField field = new QuantityField("quantity", "Count");
        final RootWidget root = new RootWidget(field);
        final Button plus = root.collectChildren(Button.class).stream()
            .filter(button -> button.getAccessibleNameModel().getData().equals("Increase Count"))
            .findFirst().orElseThrow();
        final NumberField editor = field.getEditor();
        editor.setValue(BigDecimal.ONE);
        final BooleanModel disabled = new BooleanModel(true);
        editor.setDisabledStateModel(disabled);
        assertTrue(plus.isDisabled());
        plus.getController(Event.CLICK).handleEvent(null);
        assertEquals(BigDecimal.ONE, editor.getValue().orElseThrow());
        disabled.setData(false);
        assertFalse(plus.isDisabled());
        plus.getController(Event.CLICK).handleEvent(null);
        assertEquals(new BigDecimal("2"), editor.getValue().orElseThrow());
        editor.setReadOnly(true);
        assertTrue(plus.isDisabled());
        plus.getController(Event.CLICK).handleEvent(null);
        assertEquals(new BigDecimal("2"), editor.getValue().orElseThrow());
        editor.setReadOnly(false);
        editor.getValueModel().configure(NumberValue.empty(), NumberValue.of(new BigDecimal("2")),
            BigDecimal.ONE);
        assertTrue(plus.isDisabled());
        plus.getController(Event.CLICK).handleEvent(null);
        assertEquals(new BigDecimal("2"), editor.getValue().orElseThrow());
        final JsonObject wrongType = new JsonObject();
        wrongType.addBoolean("text", true);
        editor.handleEvent(Event.TEXT_INPUT, wrongType);
        assertEquals(new BigDecimal("2"), editor.getValue().orElseThrow());
        final JsonObject oversized = new JsonObject();
        oversized.addString("text", "9".repeat(513));
        editor.handleEvent(Event.TEXT_INPUT, oversized);
        assertEquals(new BigDecimal("2"), editor.getValue().orElseThrow());
        root.closePage();
    }

}
