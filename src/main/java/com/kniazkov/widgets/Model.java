/*
 * Copyright (c) 2025 Ivan Kniazkov
 */

package com.kniazkov.widgets;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public abstract class Model<T> {
    private Prototype<T> prototype;
    private final List<WeakReference<Listener<T>>> listeners;

    protected Model() {
        this.listeners = new LinkedList<>();
    }

    protected abstract Model<T> createInstance();

    public abstract boolean isValid();

    protected abstract Optional<T> readData();

    protected abstract T getDefaultData();

    protected abstract boolean writeData(T data);

    public Model<T> fork() {
        final Model<T> fork = this.createInstance();
        final Listener<T> listener = fork::notifyListeners;
        fork.prototype = new Prototype<>();
        fork.prototype.model = this;
        fork.prototype.listener = listener;
        this.addListener(listener);
        return fork;
    }

    public T getData() {
        if (this.isValid()) {
            Optional<T> data = this.readData();
            if (data.isPresent()) {
                return data.get();
            }
        }
        if (this.prototype != null) {
            return this.prototype.model.getData();
        }
        return this.getDefaultData();
    }

    public void setData(final T newData) {
        final Optional<T> oldData = this.readData();
        if (oldData.isPresent()) {
            if (!oldData.get().equals(newData)) {
                if (writeData(newData)) {
                    this.notifyListeners(newData);
                }
            }
        } else if (this.prototype != null) {
            if (writeData(newData)) {
                if (!this.prototype.model.getData().equals(newData)) {
                    this.notifyListeners(newData);
                }
            }
            this.prototype.model.removeListener(this.prototype.listener);
            this.prototype = null;
        } else {
            if (!getDefaultData().equals(newData)) {
                if (writeData(newData)) {
                    this.notifyListeners(newData);
                }
            }
        }
    }

    public void addListener(final Listener<T> listener) {
        this.listeners.add(new WeakReference<>(listener));
    }

    public void removeListener(final Listener<T> listener) {
        final Iterator<WeakReference<Listener<T>>> iterator = this.listeners.iterator();
        while (iterator.hasNext()) {
            final WeakReference<Listener<T>> reference = iterator.next();
            final Listener<T> existing = reference.get();
            if (existing == listener) {
                iterator.remove();
                break;
            } else if (existing == null) {
                iterator.remove();
            }
        }
    }

    public void notifyListeners() {
        this.notifyListeners(this.getData());
    }

    protected void notifyListeners(final T data) {
        final Iterator<WeakReference<Listener<T>>> iterator = this.listeners.iterator();
        while (iterator.hasNext()) {
            final WeakReference<Listener<T>> reference = iterator.next();
            final Listener<T> listener = reference.get();
            if (listener == null) {
                iterator.remove();
            } else {
                listener.dataChanged(data);
            }
        }
    }

    protected void detach() {
        if (this.prototype != null) {
            this.prototype.model.removeListener(this.prototype.listener);
            this.prototype = null;
        }
    }

    private static class Prototype<T> {
        Model<T> model;
        Listener<T> listener;
    }
}
