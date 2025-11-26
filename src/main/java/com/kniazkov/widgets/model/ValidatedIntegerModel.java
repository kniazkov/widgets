/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * A validated integer model that applies a user-defined validation criterion
 * to determine whether the current value is considered valid.
 */
public final class ValidatedIntegerModel extends DefaultModel<Integer> {
    /**
     * A functional interface defining an integer validation rule.
     */
    public interface Criterion {
        /**
         * Evaluates the validity of a value.
         *
         * @param value the integer value to validate
         * @return {@code true} if the value is valid according to the rule
         */
        boolean isValid(int value);
    }

    /**
     * A predefined criterion that accepts only values greater than or equal to zero.
     */
    public static final Criterion NOT_NEGATIVE = value -> value >= 0;

    /**
     * A predefined criterion that accepts only strictly positive values (greater than zero).
     */
    public static final Criterion POSITIVE = value -> value > 0;

    /**
     * The validation rule used to check whether the current value is considered valid.
     */
    private final Criterion criterion;

    /**
     * Creates a new validated integer model initialized with {@code 0}
     * and using the specified validation criterion.
     *
     * @param criterion the validation rule that determines whether the value is valid
     */
    public ValidatedIntegerModel(final Criterion criterion) {
        this.criterion = criterion;
    }

    /**
     * Creates a new validated integer model initialized with the specified value
     * and using the given validation criterion.
     *
     * @param data the initial integer value
     * @param criterion the validation rule that determines whether the value is valid
     */
    public ValidatedIntegerModel(final Integer data, final Criterion criterion) {
        super(data);
        this.criterion = criterion;
    }

    @Override
    public boolean isValid() {
        return this.criterion.isValid(this.getData());
    }

    @Override
    public Integer getDefaultData() {
        return 0;
    }

    @Override
    public Model<Integer> deriveWithData(final Integer data) {
        return new ValidatedIntegerModel(data, this.criterion);
    }
}
