/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonString;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Binding;
import com.kniazkov.widgets.model.ColorModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.StringModel;

/**
 * Defines standard property keys used by widgets.
 * Each key represents a distinct bindable visual or behavioral property.
 */
public enum Property {
    /**
     * Text content of the widget.
     * */
    TEXT {
        @Override
        public String getName() {
            return "text";
        }

        @Override
        public Model<?> createDefaultModel() {
            return new StringModel();
        }

        @Override
        public <T> JsonElement convertData(final T data) {
            return new JsonString((String)data);
        }
    },

    /**
     * Foreground (text) color.
     */
    COLOR {
        @Override
        public String getName() {
            return "color";
        }

        @Override
        public Model<?> createDefaultModel() {
            return new ColorModel(Color.BLACK);
        }

        @Override
        public <T> JsonElement convertData(final T data) {
            return ((Color)data).toJsonObject();
        }
    },

    /** Background color. */
    BG_COLOR {
        @Override
        public String getName() {
            return "bg color";
        }

        @Override
        public Model<?> createDefaultModel() {
            return new ColorModel(Color.WHITE);
        }

        @Override
        public <T> JsonElement convertData(final T data) {
            return ((Color)data).toJsonObject();
        }
    };

    @Override
    public String toString() {
        return this.getName();
    }

    /**
     * Returns the canonical name of this property.
     * The name is a unique textual identifier used for debugging, serialization,
     * or mapping between property keys and their visual or behavioral meanings.
     *
     * @return the unique, human-readable name of this property
     */
    public abstract String getName();

    /**
     * Creates a default {@link Model} instance appropriate for this property.
     * Each property defines a model type that represents its expected data domain.
     * For example, {@code TEXT} may return a {@link StringModel}, while {@code COLOR} could return
     * a {@code Model<Color>}. Default models are used when initializing styles or widgets that
     * do not yet have an explicitly provided model for a given property.
     */
    public abstract Model<?> createDefaultModel();

    /**
     * Converts the specified property value into its JSON representation.
     * This method defines how the raw data of this property type should be serialized
     * when preparing updates to be sent to a client or frontend.
     *
     * @param <T> the compile-time type of the data being serialized
     * @param data the property data to convert (must not be {@code null})
     * @return a {@link JsonElement} representing this property’s value
     */
    public abstract <T> JsonElement convertData(T data);

    /**
     * Creates a new reactive {@link Binding} between the specified {@link Model}
     * and this property of a {@link Widget}.
     * @param state the logical widget state to which this binding applies
     * @param model the reactive {@link Model} supplying data for this property
     * @param widget the {@link Widget} instance that consumes model updates
     * @return a new {@link Binding} connecting the model and widget
     */
    public <T> Binding<?> bindModel(State state, Model<T> model, Widget widget) {
        return new Binding<>(
            model,
            new Widget.PropertyListener<>(widget, state, this)
        );
    }
}
