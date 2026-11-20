/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.InputMode;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.NumberModel;
import com.kniazkov.widgets.model.NumberValue;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Exact decimal or integer text editor. No browser Number conversion or native rounding occurs.
 * Locale is model-owned; sharing a model also shares constraints and locale.
 */
public final class NumberField extends InputField {
    /**
     * Borrowed typed model with an owned raw-text adapter.
     */
    private final NumberModel valueModel;

    /**
     * Optional owner hook for replacing interaction-state models, absent until composition.
     */
    private Runnable stateModelsChanged;

    /**
     * @param listener owner hook, released with the editor
     */
    void onStateModelsChanged(final Runnable listener) {
        this.stateModelsChanged = Objects.requireNonNull(listener);
        this.onRetire(() -> this.stateModelsChanged = null);
    }

    @Override
    public <T> void setModel(final State state, final Property<T> property, final Model<T> model) {
        super.setModel(state, property, model);
        if (this.stateModelsChanged != null
                && (property == Property.DISABLED || property == Property.READ_ONLY)) {
            this.stateModelsChanged.run();
        }
    }

    /**
     * Localized parse error, distinct from bounds and step errors owned by FormField.
     */
    private String conversionMessage = "Enter a valid number.";

    /**
     * @param message nonblank localized malformed-number message
     */
    public void setConversionMessage(final String message) {
        if (Objects.requireNonNull(message).isBlank()) {
            throw new IllegalArgumentException("A conversion error message is required");
        }
        this.conversionMessage = message;
    }

    /**
     * @return localized malformed-number message
     */
    public String getConversionMessage() {
        return this.conversionMessage;
    }

    /**
     * Creates an empty decimal editor, step 0.01 and US decimal separator.
     */
    public NumberField() {
        this(NumberModel.decimals());
    }

    /**
     * @param model decimal or integer model
     */
    @SuppressWarnings("this-escape")
    public NumberField(final NumberModel model) {
        this.valueModel = Objects.requireNonNull(model);
        super.setTextModel(model.textModel());
        this.setInputMode(model.isInteger() ? InputMode.NUMERIC : InputMode.DECIMAL);
        this.setFormMaxLength(512);
        this.setColor(State.INVALID, Color.fromString("#991b1b"));
    }

    /**
     * @return shared exact model, including explicit validation status
     */
    public NumberModel getValueModel() {
        return this.valueModel;
    }

    /**
     * @return exact valid number; empty for empty or invalid input
     */
    public Optional<BigDecimal> getValue() {
        return this.valueModel.value();
    }

    /**
     * @param value exact number satisfying the model constraints
     */
    public void setValue(final BigDecimal value) {
        this.requireUsable();
        this.valueModel.setData(NumberValue.of(value));
    }

    /**
     * Clears the editor without introducing a zero.
     */
    public void clear() {
        this.requireUsable();
        this.valueModel.setData(NumberValue.empty());
    }

    /**
     * @param locale decimal separator locale; invalid drafts must first be corrected
     */
    public void setLocale(final Locale locale) {
        this.requireUsable();
        this.valueModel.setLocale(locale);
    }

    @Override
    public void setTextModel(final Model<String> model) {
        if (model != this.valueModel.textModel()) {
            throw new IllegalArgumentException("Use the typed numeric model");
        }
        super.setTextModel(model);
    }

    @Override
    public String getType() {
        return "number field";
    }
}
