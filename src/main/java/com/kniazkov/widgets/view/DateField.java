/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.TemporalModel;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Objects;
import java.util.Optional;

/**
 * Calendar date editor; ISO transport never interprets a LocalDate as UTC midnight.
 */
public final class DateField extends TemporalField<LocalDate> {
    /**
     * Creates an empty date editor.
     */
    public DateField() {
        this(TemporalModel.dates());
    }

    /**
     * @param model borrowed date model
     */
    public DateField(final TemporalModel<LocalDate> model) {
        super(model);
    }

    /**
     * @return localized display text, empty for empty or invalid data
     */
    public String formatValue() {
        return this.getValue().map(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            .withLocale(this.getLocale())::format).orElse("");
    }

    /**
     * Explicitly resolves an instant. Gaps and offsets not valid in the zone are rejected.
     * For overlaps the caller must choose one of the two valid offsets.
     * @param time local time
     * @param zone explicit timezone
     * @param offset explicit offset valid for this local date/time in the zone
     * @return resolved instant, empty when this date is empty or invalid
     */
    public Optional<Instant> toInstant(final LocalTime time, final ZoneId zone,
            final ZoneOffset offset) {
        Objects.requireNonNull(time);
        Objects.requireNonNull(zone);
        Objects.requireNonNull(offset);
        return this.getValue().map(date -> ZonedDateTime.ofStrict(date.atTime(time), offset, zone)
            .toInstant());
    }

    @Override
    public String getType() {
        return "date field";
    }
}
