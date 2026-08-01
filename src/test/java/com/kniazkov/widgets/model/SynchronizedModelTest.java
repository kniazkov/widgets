/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link SynchronizedModel}.
 */
public final class SynchronizedModelTest {
    @Test
    public void delegatesStateAndForwardsBaseUpdates() {
        final MutableTestModel<String> base = new MutableTestModel<>("first", false);
        final SynchronizedModel<String> model = base.asSynchronized();
        final List<String> observed = new ArrayList<>();
        final Listener<String> listener = observed::add;
        model.addListener(listener);

        assertSame(base, model.getBase());
        assertEquals("first", model.getData());
        assertFalse(model.isValid());

        assertTrue(model.setData("second"));
        assertFalse(model.setData("second"));
        assertTrue(base.setData("third"));

        assertEquals("third", model.getData());
        assertEquals(Arrays.asList("second", "third"), observed);
        model.removeListener(listener);
    }

    @Test
    public void explicitNotificationUsesCurrentValueAndHonorsRemoval() {
        final SynchronizedModel<String> model = new StringModel("value").asSynchronized();
        final List<String> observed = new ArrayList<>();
        final Listener<String> listener = observed::add;
        model.addListener(listener);

        model.notifyListeners();
        model.removeListener(listener);
        model.notifyListeners();

        assertEquals(Arrays.asList("value"), observed);
    }

    @Test
    public void switchesBaseAndDetachesFromPreviousModel() {
        final StringModel first = new StringModel("first");
        final StringModel second = new StringModel("second");
        final SynchronizedModel<String> model = first.asSynchronized();
        final List<String> observed = new ArrayList<>();
        final Listener<String> listener = observed::add;
        model.addListener(listener);

        model.setBase(second);
        assertSame(second, model.getBase());
        assertEquals("second", model.getData());

        assertTrue(first.setData("ignored"));
        assertTrue(second.setData("current"));
        model.setBase(second);

        assertEquals(Arrays.asList("second", "current"), observed);
        model.removeListener(listener);
    }

    @Test
    public void ignoresLateNotificationFromPreviousBase() {
        final DelayedNotificationModel first = new DelayedNotificationModel("first");
        final StringModel second = new StringModel("second");
        final SynchronizedModel<String> model = first.asSynchronized();
        final List<String> observed = new ArrayList<>();
        final Listener<String> listener = observed::add;
        model.addListener(listener);

        first.prepareNotification("stale");
        model.setBase(second);
        first.firePreparedNotification();

        assertEquals("second", model.getData());
        assertEquals(Arrays.asList("second"), observed);
        model.removeListener(listener);
    }

    @Test
    public void derivesAnotherSynchronizedIndependentModel() {
        final SynchronizedModel<String> model = new StringModel("base").asSynchronized();

        final Model<String> derived = model.deriveWithData("derived");

        assertNotSame(model, derived);
        assertTrue(derived instanceof SynchronizedModel);
        assertEquals("base", model.getData());
        assertEquals("derived", derived.getData());
        assertSame(derived, derived.asSynchronized());
        assertSame(model, model.asSynchronized());
    }

    @Test
    public void forwardsCallbackDataWithoutReenteringBaseModel() {
        final NonReentrantModel base = new NonReentrantModel("first");
        final SynchronizedModel<String> model = base.asSynchronized();
        final List<String> observed = new ArrayList<>();
        final Listener<String> listener = observed::add;
        model.addListener(listener);

        assertTrue(model.setData("second"));

        assertEquals("second", model.getData());
        assertEquals(Arrays.asList("second"), observed);
        model.removeListener(listener);
    }

    @Test
    public void invokesListenersWithoutHoldingWrapperLock() throws Exception {
        final SynchronizedModel<String> model = new StringModel("first").asSynchronized();
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Listener<String> listener = data -> {
            try {
                final Future<String> read = executor.submit(model::getData);
                assertEquals(data, read.get(2, TimeUnit.SECONDS));
            } catch (final Exception exception) {
                throw new AssertionError(exception);
            }
        };
        model.addListener(listener);

        try {
            assertTrue(model.setData("second"));
        } finally {
            model.removeListener(listener);
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void serializesConcurrentAccessToBaseModel() throws Exception {
        final ConcurrentAccessModel base = new ConcurrentAccessModel();
        final SynchronizedModel<Integer> model = base.asSynchronized();
        final ExecutorService executor = Executors.newFixedThreadPool(8);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<?>> tasks = new ArrayList<>();

        for (int thread = 0; thread < 8; thread++) {
            final int offset = thread * 1000;
            tasks.add(executor.submit(() -> {
                start.await();
                for (int index = 0; index < 200; index++) {
                    model.setData(offset + index);
                    model.getData();
                    model.isValid();
                }
                return null;
            }));
        }

        start.countDown();
        try {
            for (final Future<?> task : tasks) {
                task.get(20, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        assertFalse(base.hasOverlappingAccess());
    }

    /**
     * Model capable of firing a listener snapshot after listeners have been removed.
     */
    private static final class DelayedNotificationModel implements Model<String> {
        /** Current data. */
        private String data;

        /** Registered listeners. */
        private final List<Listener<String>> listeners = new ArrayList<>();

        /** Prepared listener snapshot. */
        private List<Listener<String>> prepared = new ArrayList<>();

        /**
         * Creates a model.
         *
         * @param data initial data
         */
        private DelayedNotificationModel(final String data) {
            this.data = data;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public String getData() {
            return this.data;
        }

        @Override
        public boolean setData(final String data) {
            if (this.data.equals(data)) {
                return false;
            }
            this.data = data;
            this.notifyListeners();
            return true;
        }

        @Override
        public void addListener(final Listener<String> listener) {
            this.listeners.add(listener);
        }

        @Override
        public void removeListener(final Listener<String> listener) {
            this.listeners.remove(listener);
        }

        @Override
        public void notifyListeners() {
            for (final Listener<String> listener : new ArrayList<>(this.listeners)) {
                listener.accept(this.data);
            }
        }

        @Override
        public Model<String> deriveWithData(final String data) {
            return new DelayedNotificationModel(data);
        }

        /**
         * Captures the current listeners and changes data without firing yet.
         *
         * @param data prepared data
         */
        private void prepareNotification(final String data) {
            this.data = data;
            this.prepared = new ArrayList<>(this.listeners);
        }

        /** Fires the prepared listener snapshot. */
        private void firePreparedNotification() {
            for (final Listener<String> listener : this.prepared) {
                listener.accept(this.data);
            }
        }
    }

    /**
     * Model that rejects reentrant calls while notifying listeners.
     */
    private static final class NonReentrantModel implements Model<String> {
        /** Current data. */
        private String data;

        /** Registered listeners. */
        private final List<Listener<String>> listeners = new ArrayList<>();

        /** Whether a state method is currently executing. */
        private boolean entered;

        /**
         * Creates a model.
         *
         * @param data initial data
         */
        private NonReentrantModel(final String data) {
            this.data = data;
        }

        @Override
        public boolean isValid() {
            this.enter();
            try {
                return true;
            } finally {
                this.leave();
            }
        }

        @Override
        public String getData() {
            this.enter();
            try {
                return this.data;
            } finally {
                this.leave();
            }
        }

        @Override
        public boolean setData(final String data) {
            this.enter();
            try {
                if (this.data.equals(data)) {
                    return false;
                }
                this.data = data;
                for (final Listener<String> listener : new ArrayList<>(this.listeners)) {
                    listener.accept(data);
                }
                return true;
            } finally {
                this.leave();
            }
        }

        @Override
        public void addListener(final Listener<String> listener) {
            this.listeners.add(listener);
        }

        @Override
        public void removeListener(final Listener<String> listener) {
            this.listeners.remove(listener);
        }

        @Override
        public void notifyListeners() {
            for (final Listener<String> listener : new ArrayList<>(this.listeners)) {
                listener.accept(this.data);
            }
        }

        @Override
        public Model<String> deriveWithData(final String data) {
            return new NonReentrantModel(data);
        }

        /** Enters a guarded state method. */
        private void enter() {
            if (this.entered) {
                throw new IllegalStateException("reentrant model access");
            }
            this.entered = true;
        }

        /** Leaves a guarded state method. */
        private void leave() {
            this.entered = false;
        }
    }

    /**
     * Model that records whether two threads ever enter it simultaneously.
     */
    private static final class ConcurrentAccessModel implements Model<Integer> {
        /** Current data. */
        private int data;

        /** Registered listeners. */
        private final List<Listener<Integer>> listeners = new ArrayList<>();

        /** Number of state methods currently executing. */
        private final AtomicInteger active = new AtomicInteger();

        /** Whether overlapping access was observed. */
        private final AtomicBoolean overlap = new AtomicBoolean();

        @Override
        public boolean isValid() {
            this.enter();
            try {
                return true;
            } finally {
                this.leave();
            }
        }

        @Override
        public Integer getData() {
            this.enter();
            try {
                return this.data;
            } finally {
                this.leave();
            }
        }

        @Override
        public boolean setData(final Integer data) {
            this.enter();
            try {
                if (Objects.equals(this.data, data)) {
                    return false;
                }
                this.data = data;
                for (final Listener<Integer> listener : new ArrayList<>(this.listeners)) {
                    listener.accept(data);
                }
                return true;
            } finally {
                this.leave();
            }
        }

        @Override
        public void addListener(final Listener<Integer> listener) {
            this.listeners.add(listener);
        }

        @Override
        public void removeListener(final Listener<Integer> listener) {
            this.listeners.remove(listener);
        }

        @Override
        public void notifyListeners() {
            for (final Listener<Integer> listener : new ArrayList<>(this.listeners)) {
                listener.accept(this.data);
            }
        }

        @Override
        public Model<Integer> deriveWithData(final Integer data) {
            final ConcurrentAccessModel model = new ConcurrentAccessModel();
            model.data = data;
            return model;
        }

        /** Enters a state method and makes thread overlap easier to observe. */
        private void enter() {
            if (this.active.incrementAndGet() > 1) {
                this.overlap.set(true);
            }
            Thread.yield();
        }

        /** Leaves a state method. */
        private void leave() {
            this.active.decrementAndGet();
        }

        /**
         * Returns whether overlapping state-method access occurred.
         *
         * @return overlap flag
         */
        private boolean hasOverlappingAccess() {
            return this.overlap.get();
        }
    }
}
