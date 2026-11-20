/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.model.SliderModel;
import com.kniazkov.widgets.protocol.SetSlider;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * A native range control with an exact decimal model and a visible value with units.
 * The browser previews pointer movement locally; only completed edits update the model.
 */
public final class Slider extends BlockWidget<PanelStyle> implements HasDisabledState, HasWidth {
    /**
     * Shared numeric contract.
     */
    private final SliderModel model;
    /**
     * Plain-text unit displayed after the number.
     */
    private final String unit;
    /**
     * Strong listener reference for the lifetime of this widget.
     */
    private final Listener<BigDecimal> changed = ignored -> this.publish();

    /**
     * @param model shared decimal range and selected value
     * @param name accessible control name
     * @param unit unit suffix, empty for dimensionless values
     */
    @SuppressWarnings("this-escape")
    public Slider(final SliderModel model, final String name, final String unit) {
        super(PanelStyle.DEFAULT);
        this.model = Objects.requireNonNull(model);
        this.unit = Objects.requireNonNull(unit);
        if (Objects.requireNonNull(name).isBlank()) {
            throw new IllegalArgumentException("Expected an accessible slider name");
        }
        this.setAccessibleName(name);
        this.subscribeToEvent(Event.SLIDER_COMMIT);
        this.model.addListener(this.changed);
        this.onRetire(() -> this.model.removeListener(this.changed));
        this.publish();
    }

    /**
     * @return shared validated selection
     */
    public SliderModel getValueModel() {
        return this.model;
    }

    @Override
    public String getType() {
        return "slider";
    }

    /**
     * Sends exact decimal coefficients and a bounded integer tick index.
     */
    private void publish() {
        final JsonObject data = new JsonObject();
        data.addString("base", this.model.minimum().unscaledValue().toString());
        data.addString("step", this.model.step().unscaledValue().toString());
        data.addNumber("scale", this.model.step().scale());
        data.addNumber("ticks", this.model.ticks());
        data.addNumber("index", this.model.index());
        data.addString("unit", this.unit);
        this.pushUpdate(new SetSlider(this.getId(), data));
    }
}
