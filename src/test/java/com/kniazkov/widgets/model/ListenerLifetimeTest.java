/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * Regression coverage for live derived models backed by weak subscriptions.
 */
public class ListenerLifetimeTest {
    /**
     * Verifies notifications through each supported listener ownership pattern.
     * @throws Exception if collection is interrupted
     */
    @Test
    public void selfListenersAndOwnedBindingsSurviveCollection() throws Exception {
        final IntegerModel integer = new IntegerModel(1);
        assertUpdates(new IntegerToStringModel(integer), () -> integer.setData(2), "2");
        final RealNumberModel real = new RealNumberModel(1.0);
        final var realText = new RealToStringModel(real);
        assertUpdates(realText, () -> real.setData(2.5), "2.5");
        final BooleanModel flag = new BooleanModel(false);
        assertUpdates(new InvertModel(flag), () -> flag.setData(true), false);
        final BooleanModel other = new BooleanModel(false);
        assertUpdates(new ConjunctionModel(flag, other), () -> other.setData(true), true);
        assertUpdates(new DisjunctionModel(flag, other), () -> {
            flag.setData(false);
            other.setData(false);
        }, false);
        final StringModel text = new StringModel("first");
        assertUpdates(new PredicateModel<>(text, value -> value.length() > 5),
            () -> text.setData("second"), true);
        final var required = new NotEmptyStringModel("value");
        assertUpdates(required.getValidFlagModel(), () -> required.setData(""), false);
        assertUpdates(new CascadingModel<>(text), () -> text.setData("third"), "third");
        assertUpdates(text.asSynchronized(), () -> text.setData("fourth"), "fourth");
        final List<String> values = new ArrayList<>();
        final Binding<String> binding = new Binding<>(text, values::add);
        collect();
        text.setData("fifth");
        assertEquals("fifth", values.getLast());
        Reference.reachabilityFence(binding);
    }

    /**
     * Keeps the consumer alive while collecting unrelated listener objects.
     * @param model derived model
     * @param change source mutation
     * @param expected expected notification
     * @param <T> value type
     * @throws Exception if collection is interrupted
     */
    private static <T> void assertUpdates(final Model<T> model, final Runnable change,
        final T expected) throws Exception {
        final List<T> values = new ArrayList<>();
        final Listener<T> listener = values::add;
        model.addListener(listener);
        collect();
        change.run();
        assertFalse("Missing notification after GC: " + model.getClass(), values.isEmpty());
        assertEquals(expected, values.getLast());
        Reference.reachabilityFence(model);
        Reference.reachabilityFence(listener);
    }

    /**
     * Ensures collection happened instead of assuming System.gc is synchronous.
     */
    public static void collect() throws InterruptedException {
        final WeakReference<Object> probe = new WeakReference<>(new Object());
        for (int attempt = 0; probe.get() != null && attempt < 50; attempt++) {
            System.gc();
            Thread.sleep(10);
        }
        assertNull("GC did not collect the unowned probe", probe.get());
    }
}
