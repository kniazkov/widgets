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
import com.kniazkov.widgets.images.ImageSource;
import com.kniazkov.widgets.common.Offset;
import com.kniazkov.widgets.common.VerticalAlignment;
import com.kniazkov.widgets.common.WidgetSize;
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
import com.kniazkov.widgets.model.SynchronizedModel;
import com.kniazkov.widgets.model.VerticalAlignmentModel;
import com.kniazkov.widgets.model.WidgetSizeModel;

/**
 * Represents a typed declarative property that can be bound to a widget through a {@link Model}.
 * <p>
 * Each property defines:
 * <ul>
 *   <li>a unique canonical name,</li>
 *   <li>a Java value type,</li>
 *   <li>a default model instance,</li>
 *   <li>a strategy for converting data to JSON for UI updates.</li>
 * </ul>
 * <p>
 * Properties act as the backbone of the framework's reactive model-binding system.
 * They allow widgets to synchronize their state with models and automatically emit updates
 * when the underlying data changes.
 *
 * @param <T> the value type associated with this property
 */
public abstract class Property<T> {
    /**
     * Returns the canonical name of this property.
     * The name serves as a unique textual identifier used for debugging, serialization, and UI
     * protocol updates. It typically corresponds to the property’s JSON key or declarative
     * attribute name (e.g. {@code "text"}, {@code "color"}, {@code "width"}).
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
     * Default models are used when initializing styles or widgets that have not been explicitly
     * provided with a model for a given property.
     *
     * @return a new {@link Model} instance providing a default value
     */
    public abstract Model<T> createDefaultModel();

    /**
     * Converts a data value of this property to its corresponding JSON representation for
     * serialization and client updates.
     *
     * @param data the value to convert
     * @return the JSON representation of the given value
     */
    public abstract JsonElement convertData(T data);

    @Override
    public String toString() {
        return this.getName();
    }

    /**
     * Creates a new {@link Binding} between this property’s {@link Model}
     * and the specified {@link Widget}.
     * This method defines how a widget observes updates for this property. A corresponding listener
     * is automatically attached so that whenever the model’s data changes, the widget emits
     * an appropriate update.
     *
     * @param state the widget state (e.g. {@code NORMAL}, {@code HOVER})
     * @param model the data model providing this property’s value
     * @param widget the widget that should listen for model changes
     * @return a new binding connecting the model to the widget
     * @throws IllegalArgumentException if the model type is incompatible
     */
    public Binding<T> bindModel(State state, Model<?> model, Widget widget) {
        return new Binding<>(
            this.cast(model),
            new Widget.PropertyListener<>(widget, state, this)
        );
    }

    /**
     * Rebinds the given {@link Binding} to a new {@link Model} instance of the same property type.
     * This allows updating the data source of an existing binding without recreating or reattaching
     * listeners. The new model must be compatible with this property’s expected value type;
     * otherwise, an {@link IllegalArgumentException} is thrown.
     *
     * @param binding the existing binding to update
     * @param model the new model to attach to the binding
     * @throws IllegalArgumentException if the binding or model type is incompatible
     */
    public void rebindModel(Binding<?> binding, Model<?> model) {
        this.cast(binding).setModel(this.cast(model));
    }

    /**
     * Safely casts a generic {@link Binding} to a typed {@link Binding} corresponding
     * to this property.
     *
     * @param binding the binding to cast
     * @return the typed binding
     * @throws IllegalArgumentException if the binding’s model type is incompatible
     */
    public Binding<T> cast(final Binding<?> binding) {
        final Object data = binding.getModel().getData();
        if (data != null && !this.getValueClass().isInstance(data)) {
            throw new IllegalArgumentException(
                "Binding type mismatch for property '" + getName() + "': " +
                data.getClass().getName() + " cannot be cast to " + getValueClass().getName()
            );
        }
        @SuppressWarnings("unchecked")
        final Binding<T> typed = (Binding<T>) binding;
        return typed;
    }

    /**
     * Safely casts a generic {@link Model} to a typed {@link Model} corresponding
     * to this property.
     *
     * @param model the model to cast
     * @return the typed model
     * @throws IllegalArgumentException if the model’s data type is incompatible
     */
    public Model<T> cast(final Model<?> model) {
        final Object data = model.getData();
        if (data != null && !this.getValueClass().isInstance(data)) {
            throw new IllegalArgumentException(
                "Model type mismatch for property '" + getName() + "': " +
                data.getClass().getName() + " cannot be cast to " + getValueClass().getName()
            );
        }
        @SuppressWarnings("unchecked")
        Model<T> typed = (Model<T>) model;
        return typed;
    }

    /**
     * Safely casts a generic {@link SynchronizedModel} to a typed {@link SynchronizedModel}
     * corresponding to this property.
     *
     * @param model the synchronized model to cast
     * @return the typed synchronized model
     * @throws IllegalArgumentException if the model’s data type is incompatible
     */
    public SynchronizedModel<T> cast(final SynchronizedModel<?> model) {
        final Object data = model.getData();
        if (data != null && !getValueClass().isInstance(data)) {
            throw new IllegalArgumentException(
                "Synchronized model type mismatch for property '" + getName() + "': " +
                data.getClass().getName() + " cannot be cast to " + getValueClass().getName()
            );
        }
        @SuppressWarnings("unchecked")
        SynchronizedModel<T> typed = (SynchronizedModel<T>) model;
        return typed;
    }

    /**
     * Property indicating whether the widget's current state is valid.
     */
    public static final Property<Boolean> VALID = new Property<Boolean>() {
        @Override
        public String getName() {
            return "valid";
        }

        @Override
        public Class<Boolean> getValueClass() {
            return Boolean.class;
        }

        @Override
        public Model<Boolean> createDefaultModel() {
            return new BooleanModel();
        }

        @Override
        public JsonElement convertData(final Boolean data) {
            return JsonBoolean.getInstance(data);
        }
    };

    /**
     * Property controlling whether the widget is disabled and non-interactive.
     */
    public static final Property<Boolean> DISABLED = new Property<Boolean>() {
        @Override
        public String getName() {
            return "disabled";
        }

        @Override
        public Class<Boolean> getValueClass() {
            return Boolean.class;
        }

        @Override
        public Model<Boolean> createDefaultModel() {
            return new BooleanModel();
        }

        @Override
        public JsonElement convertData(final Boolean data) {
            return JsonBoolean.getInstance(data);
        }
    };

    /**
     * Property controlling whether the widget is hidden.
     */
    public static final Property<Boolean> HIDDEN = new Property<Boolean>() {
        @Override
        public String getName() {
            return "hidden";
        }

        @Override
        public Class<Boolean> getValueClass() {
            return Boolean.class;
        }

        @Override
        public Model<Boolean> createDefaultModel() {
            return new BooleanModel();
        }

        @Override
        public JsonElement convertData(final Boolean data) {
            return JsonBoolean.getInstance(data);
        }
    };

    /**
     * Property representing the widget's textual content.
     */
    public static final Property<String> TEXT = new Property<String>() {
        @Override
        public String getName() {
            return "text";
        }

        @Override
        public Class<String> getValueClass() {
            return String.class;
        }

        @Override
        public Model<String> createDefaultModel() {
            return new StringModel();
        }

        @Override
        public JsonElement convertData(final String data) {
            return new JsonString(data);
        }
    };

    /**
     * Property specifying the widget’s foreground (text) color.
     */
    public static final Property<Color> COLOR = new Property<Color>() {
        @Override
        public String getName() {
            return "color";
        }

        @Override
        public Class<Color> getValueClass() {
            return Color.class;
        }

        @Override
        public Model<Color> createDefaultModel() {
            return new ColorModel(Color.BLACK);
        }

        @Override
        public JsonElement convertData(final Color data) {
            return data.toJsonObject();
        }
    };

    /**
     * Property specifying the widget’s background color.
     */
    public static final Property<Color> BG_COLOR = new Property<Color>() {
        @Override
        public String getName() {
            return "bg color";
        }

        @Override
        public Class<Color> getValueClass() {
            return Color.class;
        }

        @Override
        public Model<Color> createDefaultModel() {
            return new ColorModel(Color.WHITE);
        }

        @Override
        public JsonElement convertData(final Color data) {
            return data.toJsonObject();
        }
    };

    /**
     * Property defining the widget’s font family.
     */
    public static final Property<FontFace> FONT_FACE = new Property<FontFace>() {
        @Override
        public String getName() {
            return "font face";
        }

        @Override
        public Class<FontFace> getValueClass() {
            return FontFace.class;
        }

        @Override
        public Model<FontFace> createDefaultModel() {
            return new FontFaceModel();
        }

        @Override
        public JsonElement convertData(final FontFace data) {
            return new JsonString(data.getName());
        }
    };

    /**
     * Property defining the widget’s font size.
     */
    public static final Property<FontSize> FONT_SIZE = new Property<FontSize>() {
        @Override
        public String getName() {
            return "font size";
        }

        @Override
        public Class<FontSize> getValueClass() {
            return FontSize.class;
        }

        @Override
        public Model<FontSize> createDefaultModel() {
            return new FontSizeModel();
        }

        @Override
        public JsonElement convertData(final FontSize data) {
            return new JsonString(data.getCSSCode());
        }
    };

    /**
     * Property controlling the widget’s font weight (thickness).
     */
    public static final Property<FontWeight> FONT_WEIGHT = new Property<FontWeight>() {
        @Override
        public String getName() {
            return "font weight";
        }

        @Override
        public Class<FontWeight> getValueClass() {
            return FontWeight.class;
        }

        @Override
        public Model<FontWeight> createDefaultModel() {
            return new FontWeightModel();
        }

        @Override
        public JsonElement convertData(final FontWeight data) {
            return new JsonNumber(data.getWeight());
        }
    };

    /**
     * Property indicating whether the widget’s text is rendered in italic style.
     */
    public static final Property<Boolean> ITALIC = new Property<Boolean>() {
        @Override
        public String getName() {
            return "italic";
        }

        @Override
        public Class<Boolean> getValueClass() {
            return Boolean.class;
        }

        @Override
        public Model<Boolean> createDefaultModel() {
            return new BooleanModel();
        }

        @Override
        public JsonElement convertData(final Boolean data) {
            return JsonBoolean.getInstance(data);
        }
    };

    /**
     * Property defining the widget’s width in CSS units.
     */
    public static final Property<WidgetSize> WIDTH = new Property<WidgetSize>() {
        @Override
        public String getName() {
            return "width";
        }

        @Override
        public Class<WidgetSize> getValueClass() {
            return WidgetSize.class;
        }

        @Override
        public Model<WidgetSize> createDefaultModel() {
            return new WidgetSizeModel();
        }

        @Override
        public JsonElement convertData(final WidgetSize data) {
            return new JsonString(data.getCSSCode());
        }
    };

    /**
     * Property defining the widget’s absolute width in CSS units.
     */
    public static final Property<AbsoluteSize> ABSOLUTE_WIDTH = new Property<AbsoluteSize>() {
        @Override
        public String getName() {
            return "width";
        }

        @Override
        public Class<AbsoluteSize> getValueClass() {
            return AbsoluteSize.class;
        }

        @Override
        public Model<AbsoluteSize> createDefaultModel() {
            return new AbsoluteSizeModel();
        }

        @Override
        public JsonElement convertData(final AbsoluteSize data) {
            return new JsonString(data.getCSSCode());
        }
    };

    /**
     * Property defining the widget’s height in CSS units.
     */
    public static final Property<WidgetSize> HEIGHT = new Property<WidgetSize>() {
        @Override
        public String getName() {
            return "height";
        }

        @Override
        public Class<WidgetSize> getValueClass() {
            return WidgetSize.class;
        }

        @Override
        public Model<WidgetSize> createDefaultModel() {
            return new WidgetSizeModel();
        }

        @Override
        public JsonElement convertData(final WidgetSize data) {
            return new JsonString(data.getCSSCode());
        }
    };

    /**
     * Property defining the widget’s absolute height in CSS units.
     */
    public static final Property<AbsoluteSize> ABSOLUTE_HEIGHT = new Property<AbsoluteSize>() {
        @Override
        public String getName() {
            return "height";
        }

        @Override
        public Class<AbsoluteSize> getValueClass() {
            return AbsoluteSize.class;
        }

        @Override
        public Model<AbsoluteSize> createDefaultModel() {
            return new AbsoluteSizeModel();
        }

        @Override
        public JsonElement convertData(final AbsoluteSize data) {
            return new JsonString(data.getCSSCode());
        }
    };

    /**
     * Property specifying the widget’s outer spacing (CSS margin).
     */
    public static final Property<Offset> MARGIN = new Property<Offset>() {
        @Override
        public String getName() {
            return "margin";
        }

        @Override
        public Class<Offset> getValueClass() {
            return Offset.class;
        }

        @Override
        public Model<Offset> createDefaultModel() {
            return new OffsetModel();
        }

        @Override
        public JsonElement convertData(final Offset data) {
            return data.toJsonObject();
        }
    };

    /**
     * Property specifying the widget’s inner spacing (CSS padding).
     */
    public static final Property<Offset> PADDING = new Property<Offset>() {
        @Override
        public String getName() {
            return "padding";
        }

        @Override
        public Class<Offset> getValueClass() {
            return Offset.class;
        }

        @Override
        public Model<Offset> createDefaultModel() {
            return new OffsetModel();
        }

        @Override
        public JsonElement convertData(final Offset data) {
            return data.toJsonObject();
        }
    };

    /**
     * Property specifying the color of the widget’s border.
     */
    public static final Property<Color> BORDER_COLOR = new Property<Color>() {
        @Override
        public String getName() {
            return "border color";
        }

        @Override
        public Class<Color> getValueClass() {
            return Color.class;
        }

        @Override
        public Model<Color> createDefaultModel() {
            return new ColorModel(Color.BLACK);
        }

        @Override
        public JsonElement convertData(final Color data) {
            return data.toJsonObject();
        }
    };

    /**
     * Property defining the visual style of the widget’s border (e.g., solid, dashed).
     */
    public static final Property<BorderStyle> BORDER_STYLE = new Property<BorderStyle>() {
        @Override
        public String getName() {
            return "border style";
        }

        @Override
        public Class<BorderStyle> getValueClass() {
            return BorderStyle.class;
        }

        @Override
        public Model<BorderStyle> createDefaultModel() {
            return new BorderStyleModel(BorderStyle.NONE);
        }

        @Override
        public JsonElement convertData(final BorderStyle data) {
            return new JsonString(data.getCSSCode());
        }
    };

    /**
     * Property representing the thickness of the widget’s border.
     */
    public static final Property<AbsoluteSize> BORDER_WIDTH = new Property<AbsoluteSize>() {
        @Override
        public String getName() {
            return "border width";
        }

        @Override
        public Class<AbsoluteSize> getValueClass() {
            return AbsoluteSize.class;
        }

        @Override
        public Model<AbsoluteSize> createDefaultModel() {
            return new AbsoluteSizeModel("0px");
        }

        @Override
        public JsonElement convertData(final AbsoluteSize data) {
            return new JsonString(data.getCSSCode());
        }
    };

    /**
     * Property defining the corner radius applied to the widget’s border.
     */
    public static final Property<AbsoluteSize> BORDER_RADIUS = new Property<AbsoluteSize>() {
        @Override
        public String getName() {
            return "border radius";
        }

        @Override
        public Class<AbsoluteSize> getValueClass() {
            return AbsoluteSize.class;
        }

        @Override
        public Model<AbsoluteSize> createDefaultModel() {
            return new AbsoluteSizeModel("0px");
        }

        @Override
        public JsonElement convertData(final AbsoluteSize data) {
            return new JsonString(data.getCSSCode());
        }
    };

    /**
     * Property storing the image source used by image widgets.
     */
    public static final Property<ImageSource> IMAGE_SOURCE = new Property<ImageSource>() {
        @Override
        public String getName() {
            return "source";
        }

        @Override
        public Class<ImageSource> getValueClass() {
            return ImageSource.class;
        }

        @Override
        public Model<ImageSource> createDefaultModel() {
            return new ImageSourceModel();
        }

        @Override
        public JsonElement convertData(final ImageSource data) {
            return new JsonString(data.toString());
        }
    };

    /**
     * Property storing the horizontal alignment.
     */
    public static final Property<HorizontalAlignment> HORIZONTAL_ALIGNMENT =
            new Property<HorizontalAlignment>() {
        @Override
        public String getName() {
            return "horz alignment";
        }

        @Override
        public Class<HorizontalAlignment> getValueClass() {
            return HorizontalAlignment.class;
        }

        @Override
        public Model<HorizontalAlignment> createDefaultModel() {
            return new HorizontalAlignmentModel();
        }

        @Override
        public JsonElement convertData(final HorizontalAlignment data) {
            return new JsonString(data.getCSSCode());
        }
    };

    /**
     * Property storing the vertical alignment.
     */
    public static final Property<VerticalAlignment> VERTICAL_ALIGNMENT =
            new Property<VerticalAlignment>() {
        @Override
        public String getName() {
            return "vert alignment";
        }

        @Override
        public Class<VerticalAlignment> getValueClass() {
            return VerticalAlignment.class;
        }

        @Override
        public Model<VerticalAlignment> createDefaultModel() {
            return new VerticalAlignmentModel();
        }

        @Override
        public JsonElement convertData(final VerticalAlignment data) {
            return new JsonString(data.getCSSCode());
        }
    };

    /**
     * A property that indicates the space between table cells.
     */
    public static final Property<AbsoluteSize> CELL_SPACING = new Property<AbsoluteSize>() {
        @Override
        public String getName() {
            return "cell spacing";
        }

        @Override
        public Class<AbsoluteSize> getValueClass() {
            return AbsoluteSize.class;
        }

        @Override
        public Model<AbsoluteSize> createDefaultModel() {
            return new AbsoluteSizeModel("0px");
        }

        @Override
        public JsonElement convertData(final AbsoluteSize data) {
            return new JsonString(data.getCSSCode());
        }
    };

    /**
     * Property controlling whether the widget (checkbox) is checked.
     */
    public static final Property<Boolean> CHECKED = new Property<Boolean>() {
        @Override
        public String getName() {
            return "checked";
        }

        @Override
        public Class<Boolean> getValueClass() {
            return Boolean.class;
        }

        @Override
        public Model<Boolean> createDefaultModel() {
            return new BooleanModel();
        }

        @Override
        public JsonElement convertData(final Boolean data) {
            return JsonBoolean.getInstance(data);
        }
    };

    /**
     * Property controlling whether a widget accepts multiple selections.
     */
    public static final Property<Boolean> MULTIPLE_INPUT = new Property<Boolean>() {
        @Override
        public String getName() {
            return "multiple input";
        }

        @Override
        public Class<Boolean> getValueClass() {
            return Boolean.class;
        }

        @Override
        public Model<Boolean> createDefaultModel() {
            return new BooleanModel();
        }

        @Override
        public JsonElement convertData(final Boolean data) {
            return JsonBoolean.getInstance(data);
        }
    };

    /**
     * Property specifying the accepted file types for a file input widget.
     * <p>
     * The value is a comma-separated list of file extensions or MIME types that the widget should
     * accept (e.g., ".pdf,.docx" or "image/*,audio/*"). This property controls the browser's file
     * selection filter.
     */
    public static final Property<String> ACCEPTED_FILES = new Property<String>() {
        @Override
        public String getName() {
            return "accepted files";
        }

        @Override
        public Class<String> getValueClass() {
            return String.class;
        }

        @Override
        public Model<String> createDefaultModel() {
            return new StringModel();
        }

        @Override
        public JsonElement convertData(final String data) {
            return new JsonString(data);
        }
    };
}
