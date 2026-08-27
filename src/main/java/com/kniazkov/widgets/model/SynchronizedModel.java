/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe reactive wrapper for another {@link Model} instance.
 * <p>
 * This wrapper synchronizes access to an underlying (potentially non-thread-safe)
 * model using a {@link ReentrantLock} and maintains its own independent listener registry
 * backed by a {@link WeakHashMap}. Listeners are automatically removed when they
 * are garbage-collected, preventing memory leaks.
 *
 * @param <T> the type of the data managed by this model
 */
public final class SynchronizedModel<T> implements Model<T>, Listener<T> {
    /**
     * The wrapped base model that provides the actual data and validation logic.
     * All access is guarded by {@link #lock}.
     */
    private Model<T> base;

    /**
     * A reentrant synchronization lock used to serialize and coordinate access
     * to the underlying model’s state and listener registry.
     */
    private final ReentrantLock lock;

    /**
     * A registry of listeners that observe changes from this wrapper.
     */
    private final Map<Listener<T>, Object> listeners;

    /**
     * Listener registered with the current base model. A separate listener is created for each
     * base so that late notifications from a previously wrapped model can be discarded.
     */
    private Listener<T> baseListener;

    /**
     * Number of base-model calls whose callbacks must be deferred until the lock is released.
     */
    private int deferralDepth;

    /**
     * Notifications produced synchronously by the base while a wrapper operation holds the lock.
     */
    private final List<Notification<T>> deferredNotifications;

    /**
     * Creates a new synchronized wrapper for the specified base model.
     *
     * @param base the model to wrap
     */
    public SynchronizedModel(final Model<T> base) {
        this.base = Objects.requireNonNull(base, "base");
        this.lock = new ReentrantLock();
        this.listeners = new WeakHashMap<>();
        this.deferredNotifications = new ArrayList<>();
        this.baseListener = this.createBaseListener(this.base);
        this.base.addListener(this.baseListener);
    }

    @Override
    public boolean isValid() {
        this.lock.lock();
        try {
            return this.base.isValid();
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public T getData() {
        this.lock.lock();
        try {
            return this.base.getData();
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public boolean setData(final T data) {
        final List<Notification<T>> notifications = new ArrayList<>();
        this.lock.lock();
        try {
            this.deferralDepth++;
            return this.base.setData(data);
        } finally {
            this.deferralDepth--;
            this.drainDeferredNotifications(notifications);
            this.lock.unlock();
            this.dispatch(notifications);
        }
    }

    @Override
    public void addListener(final Listener<T> listener) {
        this.lock.lock();
        try {
            this.listeners.put(listener, Boolean.TRUE);
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public void removeListener(final Listener<T> listener) {
        this.lock.lock();
        try {
            this.listeners.remove(listener);
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public void notifyListeners() {
        final List<Notification<T>> notifications = new ArrayList<>(1);
        this.lock.lock();
        try {
            this.enqueueNotification(this.base.getData(), notifications);
        } finally {
            this.lock.unlock();
        }
        this.dispatch(notifications);
    }

    @Override
    public Model<T> deriveWithData(final T data) {
        final Model<T> derived;
        this.lock.lock();
        try {
            derived = this.base.deriveWithData(data);
        } finally {
            this.lock.unlock();
        }
        return new SynchronizedModel<>(derived);
    }

    @Override
    public SynchronizedModel<T> asSynchronized() {
        /*
         * Returns this instance itself, since it is already a thread-safe model
         */
        return this;
    }

    /**
     * Returns the currently wrapped base model.
     *
     * @return the current underlying {@link Model} instance
     */
    public Model<T> getBase() {
        this.lock.lock();
        try {
            return this.base;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Replaces the underlying base model with a new one.
     * <p>
     * This method safely detaches the internal listener from the previous base model,
     * attaches it to the new one, and immediately notifies all listeners of this wrapper
     * with the new model’s current data value.
     * <p>
     * If the specified model is the same as the current one, no action is taken.
     *
     * @param model the new base {@link Model} to wrap
     */
    public void setBase(final Model<T> model) {
        Objects.requireNonNull(model, "model");
        final List<Notification<T>> notifications = new ArrayList<>();
        this.lock.lock();
        try {
            if (this.base == model) {
                return;
            }
            this.deferralDepth++;
            try {
                this.base.removeListener(this.baseListener);
                this.base = model;
                this.baseListener = this.createBaseListener(model);
                this.base.addListener(this.baseListener);
                this.enqueueNotification(model.getData(), notifications);
            } finally {
                this.deferralDepth--;
            }
        } finally {
            this.drainDeferredNotifications(notifications);
            this.lock.unlock();
            this.dispatch(notifications);
        }
    }

    /**
     * Notifies all currently alive listeners with the specified data.
     *
     * @param data the data value to broadcast to listeners
     */
    private void notifyListeners(final T data) {
        final List<Notification<T>> notifications = new ArrayList<>(1);
        this.lock.lock();
        try {
            this.enqueueNotification(data, notifications);
        } finally {
            this.lock.unlock();
        }
        this.dispatch(notifications);
    }

    @Override
    public void accept(final T data) {
        this.notifyListeners(data);
    }

    /**
     * Creates a listener tied to one particular base model.
     *
     * @param source base model that owns the listener
     * @return listener forwarding only updates from that model while it remains current
     */
    private Listener<T> createBaseListener(final Model<T> source) {
        return data -> this.accept(source, data);
    }

    /**
     * Accepts an update from a particular base model and ignores callbacks that arrive after the
     * wrapper has switched to another base.
     *
     * @param source model that emitted the update
     * @param data emitted data
     */
    private void accept(final Model<T> source, final T data) {
        final List<Notification<T>> notifications = new ArrayList<>(1);
        this.lock.lock();
        try {
            if (this.base != source) {
                return;
            }
            this.enqueueNotification(data, notifications);
        } finally {
            this.lock.unlock();
        }
        this.dispatch(notifications);
    }

    /**
     * Takes a listener snapshot and either queues it for later or makes it ready for dispatch.
     * The wrapper lock must be held by the current thread.
     *
     * @param data data to send
     * @param notifications collection receiving notifications that can be dispatched immediately
     */
    private void enqueueNotification(
        final T data,
        final List<Notification<T>> notifications
    ) {
        final Notification<T> notification = new Notification<>(
            data,
            new ArrayList<>(this.listeners.keySet())
        );
        if (this.deferralDepth > 0) {
            this.deferredNotifications.add(notification);
        } else {
            notifications.add(notification);
        }
    }

    /**
     * Moves deferred notifications into the provided collection once the outermost base call has
     * completed. The wrapper lock must be held by the current thread.
     *
     * @param notifications destination collection
     */
    private void drainDeferredNotifications(final List<Notification<T>> notifications) {
        if (this.deferralDepth == 0 && !this.deferredNotifications.isEmpty()) {
            notifications.addAll(this.deferredNotifications);
            this.deferredNotifications.clear();
        }
    }

    /**
     * Invokes listener callbacks without holding the wrapper lock.
     *
     * @param notifications notifications to deliver
     */
    private void dispatch(final List<Notification<T>> notifications) {
        for (final Notification<T> notification : notifications) {
            for (final Listener<T> listener : notification.listeners) {
                listener.accept(notification.data);
            }
        }
    }

    /**
     * Immutable listener snapshot paired with the value emitted by the base model.
     *
     * @param <T> data type
     */
    private static final class Notification<T> {
        /**
         * Data value to deliver.
         */
        private final T data;

        /**
         * Listener snapshot captured when the update occurred.
         */
        private final List<Listener<T>> listeners;

        /**
         * Creates a notification.
         *
         * @param data data value
         * @param listeners listener snapshot
         */
        private Notification(final T data, final List<Listener<T>> listeners) {
            this.data = data;
            this.listeners = listeners;
        }
    }
}
