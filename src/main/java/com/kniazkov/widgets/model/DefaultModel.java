/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.common.InlineWidgetSize;

/**
 * Default in-memory implementation of {@link Model}.
 * This class provides a simple and reliable data model suitable for most use cases
 * where the model only needs to store data in memory and does not require
 * validation, persistence, or complex synchronization logic.
 *
 * @param <T> the type of the data managed by this model
 */
public abstract class DefaultModel<T> extends SingleThreadModel<T> {
    /**
     * The internal data storage.
     */
    private T data;

    /**
     * Creates a new default model instance.
     */
    protected DefaultModel() {
        this.data = this.getDefaultData();
    }

    /**
     * Creates a new default model initialized with the specified value.
     * This value immediately becomes the current and default data of the model.
     *
     * @param data the initial value to store in the model
     */
    protected DefaultModel(T data) {
        this.data = data;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public T getData() {
        return this.data;
    }

    @Override
    public boolean setData(final T data) {
        if (this.data.equals(data)) {
            return false;
        }
        this.data = data;
        this.notifyListeners(data);
        return true;
    }

    /**
     * Provides the default value for model data. The “no data” model is filled with this data.
     * Implementations must ensure that this method never returns {@code null}.
     *
     * @return the default data value
     */
    protected abstract T getDefaultData();

    /**
     * Creates an appropriate {@link Model} instance for the given data object.
     * <p>
     * This factory inspects the runtime type of the provided {@code data}
     * and returns a corresponding {@link DefaultModel} subclass:
     * <ul>
     *   <li>{@link String} → {@link StringModel}</li>
     *   <li>{@link Integer} → {@link IntegerModel}</li>
     *   <li>{@link Boolean} → {@link BooleanModel}</li>
     *   <li>{@link Color} → {@link ColorModel}</li>
     *   <li>{@link FontFace} → {@link FontFaceModel}</li>
     *   <li>{@link FontSize} → {@link FontSizeModel}</li>
     *   <li>{@link FontWeight} → {@link FontWeightModel}</li>
     *   <li>{@link InlineWidgetSize} → {@link InlineWidgetSizeModel}</li>
     * </ul>
     * <p>
     *
     * @param data the initial data value for the model (must not be {@code null})
     * @return a new {@link Model} instance suitable for the given data type
     * @throws IllegalArgumentException if no matching model implementation exists
     */
    public static Model<?> create(final Object data) {
        if (data instanceof String) {
            return new StringModel((String) data);
        }
        if (data instanceof Integer) {
            return new IntegerModel((Integer) data);
        }
        if (data instanceof Boolean) {
            return new BooleanModel((Boolean) data);
        }
        if (data instanceof Color) {
            return new ColorModel((Color) data);
        }
        if (data instanceof FontFace) {
            return new FontFaceModel((FontFace) data);
        }
        if (data instanceof FontSize) {
            return new FontSizeModel((FontSize) data);
        }
        if (data instanceof FontWeight) {
            return new FontWeightModel((FontWeight) data);
        }
        if (data instanceof InlineWidgetSize) {
            return new InlineWidgetSizeModel((InlineWidgetSize) data);
        }
        throw new IllegalArgumentException(
            "Unsupported data type for DefaultModel: " + data.getClass().getName()
        );
    }

    /**
     * Creates a {@link StringModel} for the given string data.
     *
     * @param data the initial string value
     * @return a new {@link StringModel} initialized with the given value
     */
    public static Model<String> create(final String data) {
        return new StringModel(data);
    }

    /**
     * Creates an {@link IntegerModel} for the given integer data.
     *
     * @param data the initial integer value
     * @return a new {@link IntegerModel} initialized with the given value
     */
    public static Model<Integer> create(final Integer data) {
        return new IntegerModel(data);
    }

    /**
     * Creates a {@link BooleanModel} for the given boolean data.
     *
     * @param data the initial boolean value
     * @return a new {@link BooleanModel} initialized with the given value
     */
    public static Model<Boolean> create(final Boolean data) {
        return new BooleanModel(data);
    }

    /**
     * Creates a {@link ColorModel} for the given {@link Color}.
     *
     * @param data the initial color value
     * @return a new {@link ColorModel} initialized with the given value
     */
    public static Model<Color> create(final Color data) {
        return new ColorModel(data);
    }

    /**
     * Creates a {@link FontFaceModel} for the given {@link FontFace}.
     *
     * @param data the initial font face value
     * @return a new {@link FontFaceModel} initialized with the given value
     */
    public static Model<FontFace> create(final FontFace data) {
        return new FontFaceModel(data);
    }

    /**
     * Creates a {@link FontSizeModel} for the given {@link FontSize}.
     *
     * @param data the initial font size value
     * @return a new {@link FontSizeModel} initialized with the given value
     */
    public static Model<FontSize> create(final FontSize data) {
        return new FontSizeModel(data);
    }

    /**
     * Creates a {@link FontWeightModel} for the given {@link FontWeight}.
     *
     * @param data the initial font weight value
     * @return a new {@link FontWeightModel} initialized with the given value
     */
    public static Model<FontWeight> create(final FontWeight data) {
        return new FontWeightModel(data);
    }

    /**
     * Creates an {@link InlineWidgetSizeModel} for the given {@link InlineWidgetSize}.
     *
     * @param data the initial inline widget size value
     * @return a new {@link InlineWidgetSizeModel} initialized with the given value
     */
    public static Model<InlineWidgetSize> create(final InlineWidgetSize data) {
        return new InlineWidgetSizeModel(data);
    }
}
