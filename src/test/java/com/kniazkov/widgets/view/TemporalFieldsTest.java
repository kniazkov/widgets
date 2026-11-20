/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.TemporalModel;
import com.kniazkov.widgets.model.TemporalValue;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

/**
 * Local temporal values, invalid drafts, range ownership and explicit instant conversion.
 */
public final class TemporalFieldsTest {
    /**
     * Strict parsing rejects normalized calendar mistakes and never retains stale typed values.
     */
    @Test
    public void dateParsingAndEmptyAreDistinct() {
        final DateField date = new DateField();
        assertTrue(date.getValueModel().isValid());
        assertTrue(date.getValue().isEmpty());
        date.setText("2028-02-29");
        assertEquals(LocalDate.of(2028, 2, 29), date.getValue().orElseThrow());
        for (final String invalid : new String[] {"2027-02-29", "2028-04-31", "2028-2-1",
                "0000-01-01", "2028-01-01T00:00:00Z", "?", " "}) {
            date.setText(invalid);
            assertFalse(date.getValueModel().isValid());
            assertTrue(date.getValue().isEmpty());
            assertEquals(invalid, date.getText());
        }
        date.clear();
        assertTrue(date.getValueModel().isValid());
        assertTrue(date.getValue().isEmpty());
        assertThrows(IllegalArgumentException.class,
            () -> date.setValue(LocalDate.of(10000, 1, 1)));
    }

    /**
     * Inclusive bounds revalidate a draft and publish validity without silently clamping it.
     */
    @Test
    public void reactiveBoundsAndModelSharing() {
        final var model = TemporalModel.dates();
        final DateField date = new DateField(model);
        final DateField mirror = new DateField(model);
        final LocalDate min = LocalDate.of(2028, 2, 1);
        final LocalDate max = LocalDate.of(2028, 2, 29);
        date.setBounds(min, max);
        date.setValue(min);
        assertEquals(min, mirror.getValue().orElseThrow());
        date.setValue(max);
        assertEquals(max.toString(), mirror.getText());
        date.setText("2028-03-01");
        assertFalse(model.isValid());
        model.setBounds(TemporalValue.empty(), TemporalValue.empty());
        assertTrue(model.isValid());
        assertEquals("2028-03-01", date.getText());
        assertThrows(IllegalArgumentException.class, () -> date.setBounds(max, min));
        assertEquals("", model.minimumText());
        final var independent = model.deriveWithData(TemporalValue.of(min));
        assertEquals(min, independent.getData().value().orElseThrow());
        assertEquals("2028-03-01", date.getText());
    }

    /**
     * Wall-clock input is strict, minute-precision and never wraps an overnight range.
     */
    @Test
    public void timeBoundsAndPrecision() {
        final TimeField time = new TimeField();
        time.setBounds(LocalTime.of(9, 0), LocalTime.of(18, 0));
        for (final String value : new String[] {"09:00", "18:00"}) {
            time.setText(value);
            assertTrue(time.getValueModel().isValid());
        }
        for (final String value : new String[] {"08:59", "18:01", "24:00", "12:60",
                "9:00", "12:00:00", "12:00Z", "?"}) {
            time.setText(value);
            assertFalse(time.getValueModel().isValid());
            assertTrue(time.getValue().isEmpty());
        }
        assertThrows(IllegalArgumentException.class,
            () -> time.setValue(LocalTime.of(12, 0, 1)));
        assertThrows(IllegalArgumentException.class,
            () -> time.setBounds(LocalTime.of(18, 0), LocalTime.of(9, 0)));
        time.clear();
        assertTrue(time.getValueModel().isValid());
    }

    /**
     * Locale affects explicit presentation only; DST gaps and overlap offsets stay explicit.
     */
    @Test
    public void localeAndStrictInstantConversion() {
        final DateField date = new DateField();
        date.setValue(LocalDate.of(2028, 2, 29));
        date.setLocale(Locale.forLanguageTag("ru"));
        assertEquals("29.02.2028", date.formatValue());
        date.setLocale(Locale.US);
        assertEquals("2/29/28", date.formatValue());
        assertEquals("2028-02-29", date.getText());
        final ZoneId berlin = ZoneId.of("Europe/Berlin");
        date.setValue(LocalDate.of(2026, 3, 29));
        assertThrows(DateTimeException.class,
            () -> date.toInstant(LocalTime.of(2, 30), berlin, ZoneOffset.ofHours(1)));
        date.setValue(LocalDate.of(2026, 10, 25));
        final var first = date.toInstant(LocalTime.of(2, 30), berlin, ZoneOffset.ofHours(2));
        final var last = date.toInstant(LocalTime.of(2, 30), berlin, ZoneOffset.ofHours(1));
        assertEquals(3600,
            last.orElseThrow().getEpochSecond() - first.orElseThrow().getEpochSecond());
        assertThrows(DateTimeException.class,
            () -> date.toInstant(LocalTime.of(2, 30), berlin, ZoneOffset.UTC));
        date.clear();
        assertTrue(date.toInstant(LocalTime.NOON, berlin, ZoneOffset.ofHours(1)).isEmpty());
    }

    /**
     * Date ranges integrate with Form constraints and do not call the application for bad ranges.
     */
    @Test
    public void rangeFormValidationAndOwnership() {
        assertThrows(NullPointerException.class, () -> new DateRange(null, "Start", "End"));
        final DateRange range = new DateRange("period", "Start", "End");
        final Form form = new Form("Dates", "Save");
        form.addDateRange(range);
        final RootWidget root = new RootWidget(form);
        final AtomicInteger calls = new AtomicInteger();
        form.onSubmit(values -> {
            calls.incrementAndGet();
            assertTrue(values.containsKey("period.start"));
            return CompletableFuture.completedFuture(Form.Result.success());
        });
        form.submit();
        assertEquals(1, calls.get());
        range.getStart().setText("2028-02-29");
        form.submit();
        assertEquals(1, calls.get());
        assertFalse(range.getStartField().getError().isEmpty());
        assertFalse(range.getValueModel().isValid());
        range.getEnd().setText("2028-02-28");
        form.submit();
        assertEquals(1, calls.get());
        range.getEnd().setText("2028-02-29");
        form.submit();
        assertEquals(2, calls.get());
        assertTrue(range.getValueModel().isValid());
        assertEquals(range.getValue().orElseThrow().start(), range.getValue().orElseThrow().end());
        assertTrue(range.getStartField().getError().isEmpty());
        assertThrows(IllegalArgumentException.class,
            () -> form.addField(new FormField("period.start", "Duplicate")));
        range.getStartField().remove();
        assertTrue(range.getValue().isEmpty());
        assertFalse(range.isValidRange());
        root.closePage();
    }

    /**
     * Required fields reject emptiness, invalid raw input and new bounds before application code.
     */
    @Test
    public void dateInsideFormAndDisposal() {
        final DateField date = new DateField();
        final FormField row = new FormField("date", "Date", date);
        row.setRequired(true);
        final Form form = new Form("Dates", "Save");
        form.addField(row);
        final RootWidget root = new RootWidget(form);
        final var sandbox = WidgetSandbox.open(root);
        final AtomicInteger calls = new AtomicInteger();
        form.onSubmit(values -> {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture(Form.Result.success());
        });
        form.submit();
        date.setText("2027-02-29");
        form.submit();
        assertEquals(0, calls.get());
        date.setValue(LocalDate.of(2028, 2, 29));
        form.submit();
        assertEquals(1, calls.get());
        date.dispose();
        sandbox.clearUpdates();
        date.getValueModel().setBounds(TemporalValue.empty(), TemporalValue.empty());
        assertTrue(sandbox.drainUpdates().isEmpty());
        root.closePage();
    }
}
