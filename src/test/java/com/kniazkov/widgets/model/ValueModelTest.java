/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.common.HorizontalAlignment;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.common.Offset;
import com.kniazkov.widgets.common.RelativeSize;
import com.kniazkov.widgets.common.Unit;
import com.kniazkov.widgets.common.VerticalAlignment;
import com.kniazkov.widgets.common.WidgetSize;
import com.kniazkov.widgets.images.ImageSource;
import com.kniazkov.widgets.images.SvgImageSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * Contract tests for standalone value model implementations.
 */
@RunWith(Parameterized.class)
public final class ValueModelTest {
    private final Supplier<Model<?>> defaultFactory;
    private final Function<Object, Model<?>> initializedFactory;
    private final Object defaultValue;
    private final Object initialValue;
    private final Object changedValue;
    private final boolean defaultValid;

    /**
     * Creates one parameterized test case.
     *
     * @param name model name used in the test report
     * @param defaultFactory factory for a default model
     * @param initializedFactory factory for an initialized model
     * @param defaultValue expected default value
     * @param initialValue initial non-default value
     * @param changedValue second non-default value
     * @param defaultValid expected validity of the default value
     */
    public ValueModelTest(
        final String name,
        final Supplier<Model<?>> defaultFactory,
        final Function<Object, Model<?>> initializedFactory,
        final Object defaultValue,
        final Object initialValue,
        final Object changedValue,
        final boolean defaultValid
    ) {
        this.defaultFactory = defaultFactory;
        this.initializedFactory = initializedFactory;
        this.defaultValue = defaultValue;
        this.initialValue = initialValue;
        this.changedValue = changedValue;
        this.defaultValid = defaultValid;
    }

    /**
     * Provides every standalone value model not covered by a specialized validation test.
     *
     * @return test parameters
     */
    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> parameters() {
        final FontFace serif = fontFace("serif");
        final FontFace monospace = fontFace("monospace");
        final ImageSource firstImage = ImageSource.fromHyperlink("first.png");
        final ImageSource secondImage = ImageSource.fromHyperlink("second.png");
        final SvgImageSource firstSvg = svg("<svg><circle/></svg>");
        final SvgImageSource secondSvg = svg("<svg><rect/></svg>");
        final UUID firstUuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        final UUID secondUuid = UUID.fromString("22222222-2222-2222-2222-222222222222");

        return Arrays.asList(new Object[][] {
            row("AbsoluteSizeModel", AbsoluteSizeModel::new,
                value -> new AbsoluteSizeModel((AbsoluteSize)value),
                AbsoluteSize.UNDEFINED, new AbsoluteSize(10), new AbsoluteSize(20), true),
            row("BorderStyleModel", BorderStyleModel::new,
                value -> new BorderStyleModel((BorderStyle)value),
                BorderStyle.NONE, BorderStyle.SOLID, BorderStyle.DASHED, true),
            row("ColorModel", ColorModel::new,
                value -> new ColorModel((Color)value),
                Color.BLACK, Color.RED, Color.BLUE, true),
            row("FontFaceModel", FontFaceModel::new,
                value -> new FontFaceModel((FontFace)value),
                FontFace.DEFAULT, serif, monospace, true),
            row("FontSizeModel", FontSizeModel::new,
                value -> new FontSizeModel((FontSize)value),
                FontSize.DEFAULT, new FontSize(10, Unit.PX), new FontSize(20, Unit.PX), true),
            row("FontWeightModel", FontWeightModel::new,
                value -> new FontWeightModel((FontWeight)value),
                FontWeight.NORMAL, FontWeight.BOLD, FontWeight.LIGHT, true),
            row("HorizontalAlignmentModel", HorizontalAlignmentModel::new,
                value -> new HorizontalAlignmentModel((HorizontalAlignment)value),
                HorizontalAlignment.LEFT, HorizontalAlignment.CENTER,
                HorizontalAlignment.RIGHT, true),
            row("ImageSourceModel", ImageSourceModel::new,
                value -> new ImageSourceModel((ImageSource)value),
                ImageSource.INVALID, firstImage, secondImage, true),
            row("IntegerModel", IntegerModel::new,
                value -> new IntegerModel((Integer)value), 0, 10, 20, true),
            row("OffsetModel", OffsetModel::new,
                value -> new OffsetModel((Offset)value),
                Offset.UNDEFINED, new Offset(10), new Offset(20), true),
            row("RealNumberModel", RealNumberModel::new,
                value -> new RealNumberModel((Double)value), 0.0, 1.5, 2.5, true),
            row("StringModel", StringModel::new,
                value -> new StringModel((String)value), "", "first", "second", true),
            row("SvgImageSourceModel", SvgImageSourceModel::new,
                value -> new SvgImageSourceModel((SvgImageSource)value),
                SvgImageSource.EMPTY, firstSvg, secondSvg, true),
            row("UuidModel", UuidModel::new,
                value -> new UuidModel((UUID)value),
                new UuidModel().getData(), firstUuid, secondUuid, false),
            row("VerticalAlignmentModel", VerticalAlignmentModel::new,
                value -> new VerticalAlignmentModel((VerticalAlignment)value),
                VerticalAlignment.TOP, VerticalAlignment.MIDDLE,
                VerticalAlignment.BOTTOM, true),
            row("WidgetSizeModel", WidgetSizeModel::new,
                value -> new WidgetSizeModel((WidgetSize)value),
                AbsoluteSize.UNDEFINED, new AbsoluteSize(10), new RelativeSize(50), true)
        });
    }

    private static Object[] row(
        final String name,
        final Supplier<Model<?>> defaultFactory,
        final Function<Object, Model<?>> initializedFactory,
        final Object defaultValue,
        final Object initialValue,
        final Object changedValue,
        final boolean defaultValid
    ) {
        return new Object[] {
            name,
            defaultFactory,
            initializedFactory,
            defaultValue,
            initialValue,
            changedValue,
            defaultValid
        };
    }

    private static FontFace fontFace(final String name) {
        return new FontFace() {
            @Override
            public String getName() {
                return name;
            }
        };
    }

    private static SvgImageSource svg(final String source) {
        return new SvgImageSource() {
            @Override
            protected String getSvg() {
                return source;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Model<Object> cast(final Model<?> model) {
        return (Model<Object>)model;
    }

    @Test
    public void exposesExpectedDefaultValueAndValidity() {
        final Model<?> model = this.defaultFactory.get();

        assertEquals(this.defaultValue, model.getData());
        assertEquals(this.defaultValid, model.isValid());
    }

    @Test
    public void storesProvidedInitialValue() {
        final Model<?> model = this.initializedFactory.apply(this.initialValue);

        assertEquals(this.initialValue, model.getData());
    }

    @Test
    public void notifiesListenerOnlyWhenValueChanges() {
        final Model<Object> model = cast(this.initializedFactory.apply(this.initialValue));
        final List<Object> observedValues = new ArrayList<>();
        final Listener<Object> listener = observedValues::add;
        model.addListener(listener);

        assertTrue(model.setData(this.changedValue));
        assertFalse(model.setData(this.changedValue));

        assertEquals(Collections.singletonList(this.changedValue), observedValues);
        model.removeListener(listener);
    }

    @Test
    public void derivesIndependentModelOfSameType() {
        final Model<Object> model = cast(this.initializedFactory.apply(this.initialValue));

        final Model<Object> derived = model.deriveWithData(this.changedValue);

        assertNotSame(model, derived);
        assertEquals(model.getClass(), derived.getClass());
        assertEquals(this.initialValue, model.getData());
        assertEquals(this.changedValue, derived.getData());
    }
}
