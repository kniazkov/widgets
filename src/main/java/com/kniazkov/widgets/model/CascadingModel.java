/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;

/**
 * A cascading reactive model that delegates to a base {@link Model} until locally overridden.
 * <p>
 * This implementation introduces a <em>lazy fork</em> mechanism that allows localized modifications
 * without affecting the shared base model:
 * <ul>
 *   <li>Initially, all reads and validity checks are delegated directly to the base model.</li>
 *   <li>When {@link #setData(Object)} is invoked for the first time, this model "forks" — a new
 *       local model is created by calling {@link Model#deriveWithData(Object)} on the base,
 *       initialized with the provided data.</li>
 *   <li>After the fork, all reads and writes are performed on this new local model, and the base
 *       model remains untouched.</li>
 * </ul>
 * <p>
 * This mechanism enables hierarchical or cascading configuration layers: for example, a shared
 * style definition or configuration can act as the base model, while individual views
 * or components locally override values without breaking linkage for other dependents still
 * observing the base.
 * <p>
 * Like its superclass {@link SingleThreadModel}, this class is <strong>not thread-safe</strong>
 * and should be used only from a single logical thread or event loop.
 *
 * @param <T> the type of the data managed by this model
 */
public final class CascadingModel<T> extends SingleThreadModel<T> implements Listener<T> {

    /**
     * The currently active model — initially the shared base model, and later
     * a locally created model once {@link #setData(Object)} is called.
     * All read and write operations are delegated to this reference.
     */
    private Model<T> model;

    /**
     * A flag indicating whether this model has already been locally overridden.
     * {@code false} means this instance still delegates directly to the base model.
     * {@code true} means a local model has been created and is now active.
     */
    private boolean flag;

    /**
     * Creates a new cascading model that initially delegates all operations
     * to the specified base model.
     *
     * @param base the base model to delegate to until overridden
     */
    public CascadingModel(final Model<T> base) {
        this.model = base;
        this.flag = false;
        base.addListener(this);
    }

    @Override
    public boolean isValid() {
        return this.model.isValid();
    }

    @Override
    public T getData() {
        return this.model.getData();
    }

    @Override
    public boolean setData(final T data) {
        if (this.flag) {
            return this.model.setData(data);
        }
        this.model.removeListener(this);
        this.model = this.model.deriveWithData(data);
        this.model.addListener(this);
        this.notifyListeners(data);
        this.flag = true;
        return true;
    }

    @Override
    public Model<T> deriveWithData(final T data) {
        return this.model.deriveWithData(data);
    }

    @Override
    public void accept(final T data) {
        this.notifyListeners();
    }
}
