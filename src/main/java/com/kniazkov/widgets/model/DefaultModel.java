/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.common.AbsoluteSize;

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
     *   <li>{@link Double} → {@link RealNumberModel}</li>
     *   <li>{@link Boolean} → {@link BooleanModel}</li>
     *   <li>{@link Color} → {@link ColorModel}</li>
     * </ul>
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
        if (data instanceof Double) {
            return new RealNumberModel((Double) data);
        }
        if (data instanceof Boolean) {
            return new BooleanModel((Boolean) data);
        }
        if (data instanceof Color) {
            return new ColorModel((Color) data);
        }
        throw new IllegalArgumentException(
            "Unsupported data type for DefaultModel: " + data.getClass().getName()
        );
    }
}
