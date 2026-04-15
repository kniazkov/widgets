/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonBoolean;
import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonNumber;
import com.kniazkov.json.JsonString;
import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.common.HorizontalAlignment;
import com.kniazkov.widgets.common.Offset;
import com.kniazkov.widgets.common.VerticalAlignment;
import com.kniazkov.widgets.common.WidgetSize;
import com.kniazkov.widgets.images.ImageSource;
import com.kniazkov.widgets.images.SvgImageSource;
import com.kniazkov.widgets.model.AbsoluteSizeModel;
import com.kniazkov.widgets.model.Binding;
import com.kniazkov.widgets.model.BooleanModel;
import com.kniazkov.widgets.model.BorderStyleModel;
import com.kniazkov.widgets.model.ColorModel;
import com.kniazkov.widgets.model.FontFaceModel;
import com.kniazkov.widgets.model.FontSizeModel;
import com.kniazkov.widgets.model.FontWeightModel;
import com.kniazkov.widgets.model.HorizontalAlignmentModel;
import com.kniazkov.widgets.model.ImageSourceModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.OffsetModel;
import com.kniazkov.widgets.model.StringModel;
import com.kniazkov.widgets.model.SvgImageSourceModel;
import com.kniazkov.widgets.model.SynchronizedModel;
import com.kniazkov.widgets.model.ValidatedRealNumberModel;
import com.kniazkov.widgets.model.VerticalAlignmentModel;
import com.kniazkov.widgets.model.WidgetSizeModel;

/**
 * Represents a typed declarative property that can be bound to a widget through a {@link Model}.
 *
 * @param <T> the value type associated with this property
 */
public abstract class Property<T> {
    /**
     * Returns the canonical name of this property.
     *
     * @return the unique, human-readable name of this property
     */
    public abstract String getName();

    /**
     * Returns the runtime Java class representing the value type managed by this property.
     *
     * @return the class of the value type associated with this property
     */
    public abstract Class<T> getValueClass();

    /**
     * Creates a default {@link Model} instance suitable for this property.
     *
     * @return a new {@link Model} instance providing a default value
     */
    public abstract Model<T> createDefaultModel();

    /**
     * Converts a data value of this property to its corresponding JSON representation.
     *
     * @param data the value to convert
     * @return the JSON representation of the given value
     */
    public abstract JsonElement convertData(T data);

    /**
     * Returns the textual representation of this property.
     * The property name is used as its string form.
     *
     * @return the property name
     */
    @Override
    public String toString() {
        return this.getName();
    }

    /**
     * Creates a new {@link Binding} between this property’s {@link Model}
     * and the specified {@link Widget}.
     *
     * @param state the widget state
     * @param model the data model providing this property’s value
     * @param widget the widget that should listen for model changes
     * @return a new binding connecting the model to the widget
     * @throws IllegalArgumentException if the model type is incompatible
     */
    public Binding<T> bindModel(State state, Model<?> model, Widget<?> widget) {
        return new Binding<T>(
                this.cast(model),
                new Widget.PropertyListener<T>(widget, state, this)
        );
    }

    /**
     * Rebinds the given {@link Binding} to a new {@link Model} instance of the same property type.
     *
     * @param binding the existing binding to update
     * @param model the new model to attach to the binding
     * @throws IllegalArgumentException if the binding or model type is incompatible
     */
    public void rebindModel(Binding<?> binding, Model<?> model) {
        this.cast(binding).setModel(this.cast(model));
    }

    /**
     * Safely casts a generic {@link Binding} to a typed {@link Binding}.
     *
     * @param binding the binding to cast
     * @return the typed binding
     * @throws IllegalArgumentException if the binding’s model type is incompatible
     */
    public Binding<T> cast(final Binding<?> binding) {
        validateType(binding.getModel().getData(), "Binding");
        @SuppressWarnings("unchecked")
        final Binding<T> typed = (Binding<T>) binding;
        return typed;
    }

    /**
     * Safely casts a generic {@link Model} to a typed {@link Model}.
     *
     * @param model the model to cast
     * @return the typed model
     * @throws IllegalArgumentException if the model’s data type is incompatible
     */
    public Model<T> cast(final Model<?> model) {
        validateType(model.getData(), "Model");
        @SuppressWarnings("unchecked")
        final Model<T> typed = (Model<T>) model;
        return typed;
    }

    /**
     * Safely casts a generic {@link SynchronizedModel} to a typed {@link SynchronizedModel}.
     *
     * @param model the synchronized model to cast
     * @return the typed synchronized model
     * @throws IllegalArgumentException if the model’s data type is incompatible
     */
    public SynchronizedModel<T> cast(final SynchronizedModel<?> model) {
        validateType(model.getData(), "Synchronized model");
        @SuppressWarnings("unchecked")
        final SynchronizedModel<T> typed = (SynchronizedModel<T>) model;
        return typed;
    }

    /**
     * Validates that the given value is compatible with this property's declared value type.
     *
     * @param data the value to validate
     * @param sourceName the logical source name used in the error message
     * @throws IllegalArgumentException if the value is not an instance of this property's value class
     */
    private void validateType(final Object data, final String sourceName) {
        if (data != null && !this.getValueClass().isInstance(data)) {
            throw new IllegalArgumentException(
                    sourceName + " type mismatch for property '" + getName() + "': " +
                            data.getClass().getName() + " cannot be cast to " + getValueClass().getName()
            );
        }
    }

    /**
     * Creates a property instance backed by the standard internal implementation.
     *
     * @param name property name
     * @param valueClass property value class
     * @param modelFactory factory creating the default model for this property
     * @param converter converter used to serialize property values to JSON
     * @param <T> property value type
     * @return a new property instance
     */
    private static <T> Property<T> of(
            final String name,
            final Class<T> valueClass,
            final ModelFactory<T> modelFactory,
            final JsonConverter<T> converter
    ) {
        return new SimpleProperty<T>(name, valueClass, modelFactory, converter);
    }

    /**
     * Creates a boolean property with the standard {@link BooleanModel}
     * and boolean JSON serialization.
     *
     * @param name property name
     * @return a configured boolean property
     */
    private static Property<Boolean> boolProperty(final String name) {
        return of(
                name,
                Boolean.class,
                BooleanModel::new,
                JsonBoolean::getInstance
        );
    }

    /**
     * Creates a string property with the standard {@link StringModel}
     * and string JSON serialization.
     *
     * @param name property name
     * @return a configured string property
     */
    private static Property<String> stringProperty(final String name) {
        return of(
                name,
                String.class,
                StringModel::new,
                JsonString::new
        );
    }

    /**
     * Creates an absolute-size property with the standard {@link AbsoluteSizeModel}
     * and CSS code serialization.
     *
     * @param name property name
     * @return a configured absolute-size property
     */
    private static Property<AbsoluteSize> absoluteSizeProperty(final String name) {
        return of(
                name,
                AbsoluteSize.class,
                AbsoluteSizeModel::new,
                data -> new JsonString(data.getCSSCode())
        );
    }

    /**
     * Creates an absolute-size property with an explicit default value
     * and CSS code serialization.
     *
     * @param name property name
     * @param defaultValue default CSS size value
     * @return a configured absolute-size property
     */
    private static Property<AbsoluteSize> absoluteSizeProperty(
            final String name,
            final String defaultValue
    ) {
        return of(
                name,
                AbsoluteSize.class,
                () -> new AbsoluteSizeModel(defaultValue),
                data -> new JsonString(data.getCSSCode())
        );
    }

    /**
     * Creates an offset property with the standard {@link OffsetModel}
     * and JSON-object serialization.
     *
     * @param name property name
     * @return a configured offset property
     */
    private static Property<Offset> offsetProperty(final String name) {
        return of(
                name,
                Offset.class,
                OffsetModel::new,
                Offset::toJsonObject
        );
    }

    /**
     * Creates a color property with the specified default color
     * and JSON-object serialization.
     *
     * @param name property name
     * @param defaultColor default color value
     * @return a configured color property
     */
    private static Property<Color> colorProperty(final String name, final Color defaultColor) {
        return of(
                name,
                Color.class,
                () -> new ColorModel(defaultColor),
                Color::toJsonObject
        );
    }

    /**
     * Creates a widget-size property with the standard {@link WidgetSizeModel}
     * and CSS code serialization.
     *
     * @param name property name
     * @return a configured widget-size property
     */
    private static Property<WidgetSize> widgetSizeProperty(final String name) {
        return of(
                name,
                WidgetSize.class,
                WidgetSizeModel::new,
                data -> new JsonString(data.getCSSCode())
        );
    }

    /**
     * Creates an SVG image source property with standard SVG image serialization.
     *
     * @param name property name
     * @return a configured SVG image source property
     */
    private static Property<SvgImageSource> svgImageSourceProperty(final String name) {
        return of(
                name,
                SvgImageSource.class,
                SvgImageSourceModel::new,
                data -> new JsonString(data.toString())
        );
    }

    /**
     * Property indicating whether the widget's current state is valid.
     */
    public static final Property<Boolean> VALID = boolProperty("valid");

    /**
     * Property controlling whether the widget is disabled and non-interactive.
     */
    public static final Property<Boolean> DISABLED = boolProperty("disabled");

    /**
     * Property controlling whether the widget is hidden.
     */
    public static final Property<Boolean> HIDDEN = boolProperty("hidden");

    /**
     * Property representing the widget's textual content.
     */
    public static final Property<String> TEXT = stringProperty("text");

    /**
     * Property specifying the widget’s foreground (text) color.
     */
    public static final Property<Color> COLOR = colorProperty("color", Color.BLACK);

    /**
     * Property specifying the widget’s background color.
     */
    public static final Property<Color> BG_COLOR = colorProperty("bg color", Color.WHITE);

    /**
     * Property specifying the widget's opacity level.
     */
    public static final Property<Double> OPACITY = of(
            "opacity",
            Double.class,
            () -> new ValidatedRealNumberModel(
                    1.0,
                    ValidatedRealNumberModel.UNIT_INTERVAL
            ),
            JsonNumber::new
    );

    /**
     * Property defining the widget’s font family.
     */
    public static final Property<FontFace> FONT_FACE = of(
            "font face",
            FontFace.class,
            FontFaceModel::new,
            data -> new JsonString(data.getName())
    );

    /**
     * Property defining the widget’s font size.
     */
    public static final Property<FontSize> FONT_SIZE = of(
            "font size",
            FontSize.class,
            FontSizeModel::new,
            data -> new JsonString(data.getCSSCode())
    );

    /**
     * Property controlling the widget’s font weight (thickness).
     */
    public static final Property<FontWeight> FONT_WEIGHT = of(
            "font weight",
            FontWeight.class,
            FontWeightModel::new,
            data -> new JsonNumber(data.getWeight())
    );

    /**
     * Property indicating whether the widget’s text is rendered in italic style.
     */
    public static final Property<Boolean> ITALIC = boolProperty("italic");

    /**
     * Property defining the widget’s width in CSS units.
     */
    public static final Property<WidgetSize> WIDTH = widgetSizeProperty("width");

    /**
     * Property defining the widget’s absolute width in CSS units.
     */
    public static final Property<AbsoluteSize> ABSOLUTE_WIDTH = absoluteSizeProperty("width");

    /**
     * Property defining the widget’s height in CSS units.
     */
    public static final Property<WidgetSize> HEIGHT = widgetSizeProperty("height");

    /**
     * Property defining the widget’s absolute height in CSS units.
     */
    public static final Property<AbsoluteSize> ABSOLUTE_HEIGHT = absoluteSizeProperty("height");

    /**
     * Property specifying the widget’s outer spacing (CSS margin).
     */
    public static final Property<Offset> MARGIN = offsetProperty("margin");

    /**
     * Property specifying the widget’s inner spacing (CSS padding).
     */
    public static final Property<Offset> PADDING = offsetProperty("padding");

    /**
     * Property specifying the color of the widget’s border.
     */
    public static final Property<Color> BORDER_COLOR = colorProperty("border color", Color.BLACK);

    /**
     * Property defining the visual style of the widget’s border.
     */
    public static final Property<BorderStyle> BORDER_STYLE = of(
            "border style",
            BorderStyle.class,
            () -> new BorderStyleModel(BorderStyle.NONE),
            data -> new JsonString(data.getCSSCode())
    );

    /**
     * Property representing the thickness of the widget’s border.
     */
    public static final Property<AbsoluteSize> BORDER_WIDTH =
            absoluteSizeProperty("border width", "0px");

    /**
     * Property defining the corner radius applied to the widget’s border.
     */
    public static final Property<AbsoluteSize> BORDER_RADIUS =
            absoluteSizeProperty("border radius", "0px");

    /**
     * Property storing the image source used by image widgets.
     */
    public static final Property<ImageSource> IMAGE_SOURCE = of(
            "source",
            ImageSource.class,
            ImageSourceModel::new,
            data -> new JsonString(data.toString())
    );

    /**
     * Property storing the image source for the selected state of a widget
     * such as a checked checkbox or a selected radio button.
     */
    public static final Property<SvgImageSource> SELECTED_IMAGE_SOURCE =
            svgImageSourceProperty("sel source");

    /**
     * Property storing the image source for the unselected state of a widget
     * such as an unchecked checkbox or an unselected radio button.
     */
    public static final Property<SvgImageSource> UNSELECTED_IMAGE_SOURCE =
            svgImageSourceProperty("unsel source");

    /**
     * Property storing the horizontal alignment.
     */
    public static final Property<HorizontalAlignment> HORIZONTAL_ALIGNMENT = of(
            "horz alignment",
            HorizontalAlignment.class,
            HorizontalAlignmentModel::new,
            data -> new JsonString(data.getCSSCode())
    );

    /**
     * Property storing the vertical alignment.
     */
    public static final Property<VerticalAlignment> VERTICAL_ALIGNMENT = of(
            "vert alignment",
            VerticalAlignment.class,
            VerticalAlignmentModel::new,
            data -> new JsonString(data.getCode())
    );

    /**
     * A property that indicates the space between table cells.
     */
    public static final Property<AbsoluteSize> CELL_SPACING =
            absoluteSizeProperty("cell spacing", "0px");

    /**
     * Property controlling whether the widget (checkbox) is checked.
     */
    public static final Property<Boolean> CHECKED = boolProperty("checked");

    /**
     * Property controlling whether a widget accepts multiple selections.
     */
    public static final Property<Boolean> MULTIPLE_INPUT = boolProperty("multiple input");

    /**
     * Property specifying the accepted file types for a file input widget.
     */
    public static final Property<String> ACCEPTED_FILES = stringProperty("accepted files");

    /**
     * Property specifying the widget’s absolute position.
     */
    public static final Property<Offset> ABSOLUTE_POSITION = offsetProperty("abs position");

    /**
     * Creates model instances for property definitions.
     *
     * @param <T> model value type
     */
    private interface ModelFactory<T> {
        /**
         * Creates a new model instance.
         *
         * @return a newly created model
         */
        Model<T> create();
    }

    /**
     * Converts property values to their JSON representation.
     *
     * @param <T> value type
     */
    private interface JsonConverter<T> {
        /**
         * Converts the specified value to JSON.
         *
         * @param data value to convert
         * @return JSON representation of the value
         */
        JsonElement convert(T data);
    }

    /**
     * Default property implementation backed by stored metadata,
     * a model factory and a JSON converter.
     *
     * @param <T> value type
     */
    private static final class SimpleProperty<T> extends Property<T> {
        /**
         * Property name.
         */
        private final String name;

        /**
         * Runtime value class.
         */
        private final Class<T> valueClass;

        /**
         * Factory creating default models.
         */
        private final ModelFactory<T> modelFactory;

        /**
         * Converter serializing values to JSON.
         */
        private final JsonConverter<T> converter;

        /**
         * Creates a property implementation with the specified components.
         *
         * @param name property name
         * @param valueClass runtime value class
         * @param modelFactory factory creating default models
         * @param converter converter serializing values to JSON
         */
        private SimpleProperty(
                final String name,
                final Class<T> valueClass,
                final ModelFactory<T> modelFactory,
                final JsonConverter<T> converter
        ) {
            this.name = name;
            this.valueClass = valueClass;
            this.modelFactory = modelFactory;
            this.converter = converter;
        }

        /**
         * Returns the property name.
         *
         * @return the property name
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * Returns the runtime class of the property value.
         *
         * @return the value class
         */
        @Override
        public Class<T> getValueClass() {
            return valueClass;
        }

        /**
         * Creates the default model for this property.
         *
         * @return a default model instance
         */
        @Override
        public Model<T> createDefaultModel() {
            return modelFactory.create();
        }

        /**
         * Converts the specified property value to JSON.
         *
         * @param data value to convert
         * @return JSON representation of the value
         */
        @Override
        public JsonElement convertData(final T data) {
            return converter.convert(data);
        }
    }
}
