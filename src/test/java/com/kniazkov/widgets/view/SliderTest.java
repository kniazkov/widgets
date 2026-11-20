/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.model.SliderModel;
import java.math.BigDecimal;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Decimal grid invariants, event validation and sharing between controls.
 */
public class SliderTest {
    /**
     * @param min lower bound
     * @param max upper bound
     * @param step increment
     * @param value initial value
     * @return exact model
     */
    private static SliderModel model(final String min, final String max, final String step,
            final String value) {
        return new SliderModel(new BigDecimal(min), new BigDecimal(max), new BigDecimal(step),
            new BigDecimal(value));
    }

    /**
     * Incomplete grids, oversized ranges and unrepresentable selections fail atomically.
     */
    @Test
    public void rejectsInvalidRangesAndValues() {
        for (final String step : List.of("0", "-1", "0.3", "0.0000001")) {
            assertThrows(IllegalArgumentException.class, () -> model("0", "1", step, "0"));
        }
        assertThrows(IllegalArgumentException.class, () -> model("1", "1", "1", "1"));
        assertThrows(IllegalArgumentException.class, () -> model("2", "1", "1", "1"));
        assertThrows(IllegalArgumentException.class, () -> model("0", "1", "0.25", "0.4"));
        final SliderModel grid = model("-1.25", "1.25", "0.25", "-1.25");
        assertThrows(IllegalArgumentException.class, () -> grid.setData(new BigDecimal("-1.50")));
        assertThrows(IllegalArgumentException.class, () -> grid.select(11));
        assertEquals(0, grid.index());
        grid.select(10);
        assertEquals(new BigDecimal("1.25"), grid.getData());
        assertFalse(grid.setData(new BigDecimal("1.250")));
    }

    /**
     * Exact fractional ticks beyond the JavaScript safe-integer limit and server validation.
     */
    @Test
    public void acceptsOnlyValidTicksOnActiveSliders() {
        final SliderModel model = model("9007199254740993.01", "9007199254740993.03", "0.01",
            "9007199254740993.02");
        final Slider slider = new Slider(model, "Exact", "mm");
        final JsonObject event = new JsonObject();
        event.addNumber("index", 2);
        assertTrue(Event.SLIDER_COMMIT.rejectionReason(slider, event).isEmpty());
        slider.handleEvent("slider commit", event);
        assertEquals(new BigDecimal("9007199254740993.03"), model.getData());
        event.addNumber("index", 1.25);
        assertTrue(Event.SLIDER_COMMIT.rejectionReason(slider, event).isPresent());
        event.addString("index", "1");
        assertTrue(Event.SLIDER_COMMIT.rejectionReason(slider, event).isPresent());
        event.addNumber("index", 3);
        assertTrue(Event.SLIDER_COMMIT.rejectionReason(slider, event).isPresent());
        event.addNumber("index", -1);
        assertTrue(Event.SLIDER_COMMIT.rejectionReason(slider, event).isPresent());
        event.addNumber("index", 1);
        slider.disable();
        assertTrue(Event.SLIDER_COMMIT.rejectionReason(slider, event).isPresent());
        assertTrue(Event.SLIDER_COMMIT.rejectionReason(new Panel(), event).isPresent());
    }
}
