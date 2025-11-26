/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * A default real-number model implementation based on {@link Double}.
 */
public final class RealNumberModel extends DefaultModel<Double> {
    /**
     * Creates a new real-number model initialized with {@code 0.0}.
     */
    public RealNumberModel() {
    }

    /**
     * Creates a new real-number model initialized with the specified value.
     *
     * @param data the initial real-number value
     */
    public RealNumberModel(final Double data) {
        super(data);
    }

    @Override
    public Double getDefaultData() {
        return 0.0;
    }

    @Override
    public Model<Double> deriveWithData(final Double data) {
        return new RealNumberModel(data);
    }
}
