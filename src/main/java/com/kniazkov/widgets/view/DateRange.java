/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Binding;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.SingleThreadModel;
import com.kniazkov.widgets.model.TemporalValue;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Optional inclusive date interval with two labelled editors and server cross-field validation.
 * Both empty is valid; one empty, invalid input or start after end is invalid.
 */
public final class DateRange extends Panel {
    /**
     * Immutable interval; both endpoints are included.
     * @param start first included date
     * @param end last included date
     */
    public record Interval(LocalDate start, LocalDate end) {
        /**
         * Rejects missing or reversed endpoints.
         */
        public Interval {
            Objects.requireNonNull(start);
            Objects.requireNonNull(end);
            if (start.isAfter(end)) {
                throw new IllegalArgumentException("Reversed date range");
            }
        }
    }

    /**
     * First calendar editor.
     */
    private final DateField start = new DateField();
    /**
     * Last calendar editor.
     */
    private final DateField end = new DateField();
    /**
     * First labelled form field.
     */
    private final FormField startField;
    /**
     * Last labelled form field.
     */
    private final FormField endField;
    /**
     * Localized cross-field failure.
     */
    private String errorMessage = "Enter both dates with start on or before end.";
    /**
     * Read-only aggregate, exposing no stale interval after invalid edits.
     */
    private final Model<TemporalValue<Interval>> model = new SingleThreadModel<>() {
        @Override
        public boolean isValid() {
            return DateRange.this.isValidRange();
        }
        @Override
        public TemporalValue<Interval> getData() {
            return DateRange.this.getValue().map(TemporalValue::of).orElseGet(TemporalValue::empty);
        }
        @Override
        public boolean setData(final TemporalValue<Interval> value) {
            return false;
        }
        @Override
        public Model<TemporalValue<Interval>> deriveWithData(final TemporalValue<Interval> value) {
            throw new UnsupportedOperationException("Date range is an owned aggregate");
        }
    };

    /**
     * @param name stable prefix for the two submitted fields
     * @param startCaption visible first label
     * @param endCaption visible last label
     */
    @SuppressWarnings("this-escape")
    public DateRange(final String name, final String startCaption, final String endCaption) {
        Objects.requireNonNull(name);
        this.startField = new FormField(name + ".start", startCaption, this.start);
        this.endField = new FormField(name + ".end", endCaption, this.end);
        this.add(this.startField);
        this.add(this.endField);
        final Binding<TemporalValue<LocalDate>> first = new Binding<>(this.start.getValueModel(),
            value -> this.model.notifyListeners());
        final Binding<TemporalValue<LocalDate>> last = new Binding<>(this.end.getValueModel(),
            value -> this.model.notifyListeners());
        this.onRetire(first::close);
        this.onRetire(last::close);
    }

    /**
     * @return first editor and label/constraint configuration
     */
    public FormField getStartField() {
        return this.startField;
    }

    /**
     * @return last editor and label/constraint configuration
     */
    public FormField getEndField() {
        return this.endField;
    }

    /**
     * @return first date editor
     */
    public DateField getStart() {
        return this.start;
    }

    /**
     * @return last date editor
     */
    public DateField getEnd() {
        return this.end;
    }

    /**
     * @return read-only reactive interval and validity
     */
    public Model<TemporalValue<Interval>> getValueModel() {
        return this.model;
    }

    /**
     * @return valid inclusive interval, empty for empty or invalid data
     */
    public Optional<Interval> getValue() {
        if (this.fields().size() != 2) {
            return Optional.empty();
        }
        return this.start.getValue().flatMap(first -> this.end.getValue()
            .filter(last -> !first.isAfter(last)).map(last -> new Interval(first, last)));
    }

    /**
     * @return whether both endpoints are empty or form a valid inclusive interval
     */
    public boolean isValidRange() {
        return this.fields().size() == 2 && ((this.start.getText().isEmpty()
            && this.end.getText().isEmpty()) || this.getValue().isPresent());
    }

    /**
     * @param locale native language hint and formatting locale for both endpoints
     */
    public void setLocale(final Locale locale) {
        this.start.setLocale(locale);
        this.end.setLocale(locale);
    }

    /**
     * @param minimum inclusive minimum for both dates
     * @param maximum inclusive maximum for both dates
     */
    public void setBounds(final LocalDate minimum, final LocalDate maximum) {
        this.start.setBounds(minimum, maximum);
        this.end.setBounds(minimum, maximum);
    }

    /**
     * @param message localized cross-field error displayed beside both endpoints
     */
    public void setErrorMessage(final String message) {
        if (Objects.requireNonNull(message).isBlank()) {
            throw new IllegalArgumentException("Empty date range error");
        }
        this.errorMessage = message;
    }

    /**
     * @return owned endpoint fields, excluding moved or disposed children
     */
    List<FormField> fields() {
        return List.of(this.startField, this.endField).stream()
            .filter(field -> field.getParent().orElse(null) == this && field.hasEditor()
                && !field.isRetired()).toList();
    }

    /**
     * Checks the same immutable submit snapshot as other form constraints.
     * Disabled or detached endpoints do not participate.
     * @param values participating canonical texts
     * @return cross-field errors, or empty when absent, valid or individually invalid
     */
    Map<String, String> errors(final Map<String, String> values) {
        final String first = values.get(this.startField.getName());
        final String last = values.get(this.endField.getName());
        if (first == null || last == null || (first.isEmpty() && last.isEmpty())) {
            return Map.of();
        }
        if (first.isEmpty() || last.isEmpty()) {
            return Map.of(this.startField.getName(), this.errorMessage,
                this.endField.getName(), this.errorMessage);
        }
        if (this.start.getValueModel().isValid() && this.end.getValueModel().isValid()
                && LocalDate.parse(first).isAfter(LocalDate.parse(last))) {
            return Map.of(this.startField.getName(), this.errorMessage,
                this.endField.getName(), this.errorMessage);
        }
        return Map.of();
    }

    @Override
    public String getType() {
        return "date range";
    }
}
