/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Color;
import java.util.UUID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests for model factories and specialized value constructors.
 */
public final class ModelFactoryTest {
    @Test
    public void createsDefaultModelsForSupportedRuntimeTypes() {
        assertModel(StringModel.class, "value");
        assertModel(IntegerModel.class, 42);
        assertModel(RealNumberModel.class, 1.5);
        assertModel(BooleanModel.class, true);
        assertModel(ColorModel.class, Color.RED);

        final UUID uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        assertModel(UuidModel.class, uuid);
    }

    private static void assertModel(
        final Class<? extends Model> expectedType,
        final Object value
    ) {
        final Model<?> model = DefaultModel.create(value);

        assertEquals(expectedType, model.getClass());
        assertEquals(value, model.getData());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnsupportedRuntimeType() {
        DefaultModel.create(1L);
    }

    @Test
    public void createsIndependentReadOnlyModels() {
        final Model<String> model = ReadOnlyModel.create("first");

        final Model<String> derived = model.deriveWithData("second");

        assertTrue(model.isValid());
        assertEquals("first", model.getData());
        assertFalse(model.setData("changed"));
        assertEquals("first", model.getData());
        assertNotSame(model, derived);
        assertEquals("second", derived.getData());
        assertFalse(derived.setData("changed"));
    }

    @Test
    public void parsesSpecializedSizeConstructors() {
        assertEquals("10px", new AbsoluteSizeModel("10px").getData().getCSSCode());
        assertEquals("16px", new FontSizeModel("12pt").getData().getCSSCode());
        assertEquals("50.0%", new WidgetSizeModel("50%").getData().getCSSCode());
    }
}
