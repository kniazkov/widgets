/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.model.Binding;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.TemporalModel;
import com.kniazkov.widgets.model.TemporalValue;
import com.kniazkov.widgets.protocol.SetTemporalInput;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Native temporal editor with typed data, canonical text and server-side constraints.
 * @param <T> local calendar value type
 */
public abstract class TemporalField<T extends Comparable<? super T>> extends InputField {
    /**
     * Borrowed model; its raw adapter must not be replaced independently.
     */
    private final TemporalModel<T> valueModel;
    /**
     * Native locale hint; actual picker formatting depends on the browser and OS.
     */
    private Locale locale = Locale.ENGLISH;
    /**
     * Suppresses duplicate configuration on ordinary edits.
     */
    private String published = "";

    /**
     * @param model typed data model
     */
    @SuppressWarnings("this-escape")
    protected TemporalField(final TemporalModel<T> model) {
        this.valueModel = Objects.requireNonNull(model);
        super.setTextModel(model.textModel());
        final Binding<TemporalValue<T>> binding = new Binding<>(model, value -> this.publish());
        this.onRetire(binding::close);
    }

    /**
     * @return typed borrowed model; mutations must use the owning page context
     */
    public final TemporalModel<T> getValueModel() {
        return this.valueModel;
    }

    /**
     * @return current value, empty for empty or invalid input; check model validity separately
     */
    public final Optional<T> getValue() {
        return this.valueModel.value();
    }

    /**
     * @param value non-null value within inclusive bounds
     */
    public final void setValue(final T value) {
        this.requireUsable();
        this.valueModel.setData(TemporalValue.of(value));
    }

    /**
     * Clears the value without substituting a default date or time.
     */
    public final void clear() {
        this.requireUsable();
        this.valueModel.setData(TemporalValue.empty());
    }

    /**
     * @param minimum inclusive lower bound
     * @param maximum inclusive upper bound
     */
    public final void setBounds(final T minimum, final T maximum) {
        this.requireUsable();
        this.valueModel.setBounds(TemporalValue.of(minimum), TemporalValue.of(maximum));
    }

    /**
     * @param locale language hint for native input; browser preferences may take precedence
     */
    public final void setLocale(final Locale locale) {
        this.requireUsable();
        if (Objects.requireNonNull(locale).getLanguage().isEmpty()) {
            throw new IllegalArgumentException("A language is required");
        }
        this.locale = locale;
        this.publish();
    }

    /**
     * @return configured locale hint
     */
    public final Locale getLocale() {
        return this.locale;
    }

    @Override
    public final void setTextModel(final Model<String> model) {
        if (model != this.valueModel.textModel()) {
            throw new IllegalArgumentException("Use the typed temporal model");
        }
        super.setTextModel(model);
    }

    /**
     * Publishes native limits without changing the canonical value or input draft.
     */
    private void publish() {
        final JsonObject data = new JsonObject();
        data.addString("min", this.valueModel.minimumText());
        data.addString("max", this.valueModel.maximumText());
        data.addString("lang", this.locale.toLanguageTag());
        final String key = data.toString();
        if (!key.equals(this.published)) {
            this.published = key;
            this.pushUpdate(new SetTemporalInput(this.getId(), data));
        }
    }
}
