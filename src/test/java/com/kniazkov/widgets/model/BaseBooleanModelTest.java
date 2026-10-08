/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.view.CheckBox;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Verifies fluent boolean composition and adaptation of arbitrary models.
 */
public class BaseBooleanModelTest {
    /**
     * Exercises all truth-table combinations with several operands.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void combinesMultipleLiveOperands() {
        final BooleanModel a = new BooleanModel();
        final BooleanModel b = new BooleanModel();
        final BooleanModel c = new BooleanModel();
        final BaseBooleanModel and = a.and(b, c);
        final BaseBooleanModel or = a.or(b, c);
        final BaseBooleanModel chain = a.and(b, c).invert().or(a);
        final AtomicInteger notifications = new AtomicInteger();
        final Listener<Boolean> listener = value -> notifications.incrementAndGet();
        chain.addListener(listener);
        for (int bits = 0; bits < 8; bits++) {
            final boolean x = (bits & 1) != 0;
            final boolean y = (bits & 2) != 0;
            final boolean z = (bits & 4) != 0;
            a.setData(x);
            b.setData(y);
            c.setData(z);
            assertEquals(x && y && z, and.getData());
            assertEquals(x || y || z, or.getData());
            assertEquals(!(x && y && z) || x, chain.getData());
        }
        assertTrue(notifications.get() > 0);
        assertFalse(and.setData(false));
        assertFalse(or.setData(false));
        assertSame(a, a.and());
        assertSame(a, a.or());
        assertSame(a, BaseBooleanModel.of(a));
    }

    /**
     * Validity-derived chains remain connected across garbage collection.
     */
    @Test
    public void validityChainSurvivesCollection() {
        final NotEmptyStringModel text = new NotEmptyStringModel();
        final BaseBooleanModel disabled = text.getValidFlagModel().invert();
        final AtomicInteger updates = new AtomicInteger();
        final Listener<Boolean> listener = value -> updates.incrementAndGet();
        disabled.addListener(listener);
        System.gc();
        text.setData("filled");
        assertFalse(disabled.getData());
        assertEquals(1, updates.get());
        text.setData("");
        assertTrue(disabled.getData());
        assertEquals(2, updates.get());
    }

    /**
     * Adapters preserve writes, listeners, removal and read-only behavior.
     */
    @Test
    public void adaptsGenericModelsWithoutCopying() {
        final Model<Boolean> source = new BooleanModel().asSynchronized();
        final BaseBooleanModel adapted = BaseBooleanModel.of(source);
        final AtomicInteger updates = new AtomicInteger();
        final Listener<Boolean> listener = value -> updates.incrementAndGet();
        adapted.addListener(listener);
        adapted.invert().setData(false);
        assertTrue(source.getData());
        assertEquals(1, updates.get());
        source.setData(false);
        assertFalse(adapted.getData());
        assertEquals(2, updates.get());
        adapted.removeListener(listener);
        source.setData(true);
        assertEquals(2, updates.get());
        assertFalse(BaseBooleanModel.of(ReadOnlyModel.create(true)).setData(false));
        final CheckBox checkbox = new CheckBox();
        checkbox.setCheckedStateModel(source);
        checkbox.getCheckedStateModel().invert().setData(true);
        assertFalse(source.getData());
        assertFalse(checkbox.isChecked());
    }
}
