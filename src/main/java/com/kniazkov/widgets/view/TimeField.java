/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.TemporalModel;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * Local wall-clock time at minute precision, with no date, offset or implicit timezone.
 */
public final class TimeField extends TemporalField<LocalTime> {
    /**
     * Creates an empty time editor.
     */
    public TimeField() {
        this(TemporalModel.times());
    }

    /**
     * @param model borrowed time model
     */
    public TimeField(final TemporalModel<LocalTime> model) {
        super(model);
    }

    /**
     * @return localized display text, empty for empty or invalid data
     */
    public String formatValue() {
        return this.getValue().map(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(this.getLocale())::format).orElse("");
    }

    @Override
    public String getType() {
        return "time field";
    }
}
