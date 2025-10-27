/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;
import com.kniazkov.widgets.view.Property;
import com.kniazkov.widgets.view.State;

/**
 * A universal update command that sets a specific {@link Property} of a widget
 * to a given value. This class generalizes all "set" update types (such as {@code set text},
 * {@code set color}, {@code set bg color}, etc.) into a single, type-safe form.
 * It associates a {@link Property}, a {@link State}, and a concrete value, and produces a JSON
 * representation suitable for transmission to the client.
 *
 * @param <T> the type of the property value
 */
public class SetProperty<T> extends Update {
    /**
     * The logical widget state to which this update applies.
     */
    private final State state;

    /**
     * The property to be modified.
     */
    private final Property property;

    /**
     * The new value to assign to the property.
     */
    private final T value;

    /**
     * Constructs a new property update command.
     *
     * @param widget the unique widget identifier to update
     * @param state the logical state of the widget to apply this change to
     * @param property the {@link Property} to modify
     * @param value the new value to set for the property (must match the property’s expected type)
     */
    public SetProperty(final UId widget, final State state, final Property property,
            final T value) {
        super(widget);
        this.state = state;
        this.property = property;
        this.value = value;
    }

    @Override
    public Update clone() {
        return new SetProperty<>(this.getWidgetId(), this.state, this.property, this.value);
    }

    @Override
    protected String getAction() {
        return "set " + this.property.getName();
    }

    @Override
    protected void fillJsonObject(final JsonObject obj) {
        if (this.state != State.ANY) {
            obj.addString("state", this.state.toString());
        }
        obj.addElement(this.property.getName(), this.property.convertData(this.value));
    }
}
