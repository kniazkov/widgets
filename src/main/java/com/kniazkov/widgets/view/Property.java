/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.common.InlineWidgetSize;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.model.Binding;
import com.kniazkov.widgets.model.BooleanModel;
import com.kniazkov.widgets.model.ColorModel;
import com.kniazkov.widgets.model.FontFaceModel;
import com.kniazkov.widgets.model.FontSizeModel;
import com.kniazkov.widgets.model.FontWeightModel;
import com.kniazkov.widgets.model.InlineWidgetSizeModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.StringModel;

/**
 * Defines standard property keys used by widgets.
 * Each key represents a distinct bindable visual or behavioral property.
 */
public enum Property {
    /** Text content of the widget. */
    TEXT {
        @Override
        public String getName() {
            return "text";
        }

        @Override
        public Binding<?> bindModel(final State state, final Model<?> model, final Widget widget) {
            return new Binding<>(
                castModel(model, String.class),
                new HasText.TextModelListener(widget)
            );
        }

        @Override
        public Model<?> createDefaultModel() {
            return new StringModel();
        }
    },

    /** Foreground (text) color. */
    COLOR {
        @Override
        public String getName() {
            return "color";
        }

        @Override
        public Binding<?> bindModel(final State state, final Model<?> model, final Widget widget) {
            return new Binding<>(
                castModel(model, Color.class),
                new HasColor.ColorModelListener(widget)
            );
        }

        @Override
        public Model<?> createDefaultModel() {
            return new ColorModel(Color.BLACK);
        }
    },

    /** Background color. */
    BG_COLOR {
        @Override
        public String getName() {
            return "bg color";
        }

        @Override
        public Binding<?> bindModel(final State state, final Model<?> model, final Widget widget) {
            return new Binding<>(
                castModel(model, Color.class),
                new HasBgColor.BgColorModelListener(widget, state)
            );
        }

        @Override
        public Model<?> createDefaultModel() {
            return new ColorModel(Color.WHITE);
        }
    },

    /** Font face (typeface family). */
    FONT_FACE {
        @Override
        public String getName() {
            return "font face";
        }

        @Override
        public Binding<?> bindModel(final State state, final Model<?> model, final Widget widget) {
            return new Binding<>(
                castModel(model, FontFace.class),
                new HasStyledText.FontFaceModelListener(widget)
            );
        }

        @Override
        public Model<?> createDefaultModel() {
            return new FontFaceModel();
        }
    },

    /** Font size. */
    FONT_SIZE {
        @Override
        public String getName() {
            return "font size";
        }

        @Override
        public Binding<?> bindModel(final State state, final Model<?> model, final Widget widget) {
            return new Binding<>(
                castModel(model, FontSize.class),
                new HasStyledText.FontSizeModelListener(widget)
            );
        }

        @Override
        public Model<?> createDefaultModel() {
            return new FontSizeModel();
        }
    },

    /** Font weight (normal, bold, etc.). */
    FONT_WEIGHT {
        @Override
        public String getName() {
            return "font weight";
        }

        @Override
        public Binding<?> bindModel(final State state, final Model<?> model, final Widget widget) {
            return new Binding<>(
                castModel(model, FontWeight.class),
                new HasStyledText.FontWeightModelListener(widget)
            );
        }

        @Override
        public Model<?> createDefaultModel() {
            return new FontWeightModel();
        }
    },

    /** Whether the font is italic. */
    ITALIC {
        @Override
        public String getName() {
            return "italic";
        }

        @Override
        public Binding<?> bindModel(final State state, final Model<?> model, final Widget widget) {
            return new Binding<>(
                castModel(model, Boolean.class),
                new HasStyledText.ItalicModelListener(widget)
            );
        }

        @Override
        public Model<?> createDefaultModel() {
            return new BooleanModel();
        }
    },

    /** Width of the widget. */
    WIDTH {
        @Override
        public String getName() {
            return "width";
        }

        @Override
        public Binding<?> bindModel(final State state, final Model<?> model, final Widget widget) {
            return new Binding<>(
                castModel(model, InlineWidgetSize.class),
                new HasWidth.WidthModelListener<>(widget)
            );
        }

        @Override
        public Model<?> createDefaultModel() {
            return new InlineWidgetSizeModel();
        }
    },

    /** Height of the widget. */
    HEIGHT {
        @Override
        public String getName() {
            return "height";
        }

        @Override
        public Binding<?> bindModel(final State state, final Model<?> model, final Widget widget) {
            return new Binding<>(
                castModel(model, InlineWidgetSize.class),
                new HasHeight.HeightModelListener<>(widget)
            );
        }

        @Override
        public Model<?> createDefaultModel() {
            return new InlineWidgetSizeModel();
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
     * Creates a new {@link Binding} between the specified {@link Model} and the given
     * {@link Widget}.
     * <p>
     * This method defines how a widget reacts to changes in the model’s data for a particular
     * property. Each property type knows how to construct an appropriate {@link Listener}
     * that updates the widget whenever the model changes.
     *
     * @param state the current logical state of the widget
     * @param model the reactive model providing data for this property
     * @param widget the widget to which the listener should be attached
     * @return a new {@link Binding} connecting the model and widget
     * @throws IllegalArgumentException if the model’s data type is incompatible
     */
    public abstract Binding<?> bindModel(State state, Model<?> model, Widget widget);

    /**
     * Creates a default {@link Model} instance appropriate for this property.
     * Each property defines a model type that represents its expected data domain.
     * For example, {@code TEXT} may return a {@link StringModel}, while {@code COLOR} could return
     * a {@code Model<Color>}. Default models are used when initializing styles or widgets that
     * do not yet have an explicitly provided model for a given property.
     */
    public abstract Model<?> createDefaultModel();

    /**
     * Safely casts a generic {@link Model} to a specific typed model, verifying that its current
     * data value is compatible with the expected {@code type}.
     *
     * @param model the model to cast (must not be {@code null})
     * @param type the expected data type of the model
     * @param <T> the target type of the model’s data
     * @return the same model instance, safely cast to {@code Model<T>}
     * @throws IllegalArgumentException if the model’s data is incompatible with {@code type}
     */
    @SuppressWarnings("unchecked")
    protected static <T> Model<T> castModel(final Model<?> model, final Class<T> type) {
        final Object data = model.getData();
        if (!type.isInstance(data)) {
            throw new IllegalArgumentException(
                "Incompatible model type: expected " + type.getName() +
                ", but was " + data.getClass().getName()
            );
        }
        return (Model<T>) model;
    }
}
