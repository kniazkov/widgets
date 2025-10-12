package com.kniazkov.widgets.model;

/**
 * A read-only version of {@link Model}.
 * This model exposes the same data retrieval API but ignores any attempts to modify its value.
 * It is typically used when a model represents static or externally managed data.
 *
 * @param <T> the type of data managed by this model
 */
public abstract class ReadOnlyModel<T> extends Model<T>  {
    @Override
    protected boolean writeData(T data) {
        return false;
    }

    @Override
    public boolean setData(T data) {
        return false;
    }
}
