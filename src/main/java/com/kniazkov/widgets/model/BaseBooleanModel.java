/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A boolean model with reactive logical operations. Operands remain live;
 * conjunction and disjunction are read-only, while inversion forwards writes.
 */
public interface BaseBooleanModel extends Model<Boolean> {
    /**
     * Returns a reactive negation of this model, forwarding inverted writes.
     * @return the negated model
     */
    default BaseBooleanModel invert() {
        return new InvertModel(this);
    }

    /**
     * Combines this model with all supplied operands using logical AND.
     * With no additional operands, returns this model unchanged.
     * @param others additional live operands
     * @return this model or a read-only conjunction
     */
    @SuppressWarnings("unchecked")
    default BaseBooleanModel and(final Model<Boolean>... others) {
        Objects.requireNonNull(others, "others");
        if (others.length == 0) {
            return this;
        }
        final List<Model<Boolean>> operands = new ArrayList<>();
        operands.add(this);
        for (final Model<Boolean> other : others) {
            operands.add(Objects.requireNonNull(other, "operand"));
        }
        return new ConjunctionModel(operands);
    }

    /**
     * Combines this model with all supplied operands using logical OR.
     * With no additional operands, returns this model unchanged.
     * @param others additional live operands
     * @return this model or a read-only disjunction
     */
    @SuppressWarnings("unchecked")
    default BaseBooleanModel or(final Model<Boolean>... others) {
        Objects.requireNonNull(others, "others");
        if (others.length == 0) {
            return this;
        }
        final List<Model<Boolean>> operands = new ArrayList<>();
        operands.add(this);
        for (final Model<Boolean> other : others) {
            operands.add(Objects.requireNonNull(other, "operand"));
        }
        return new DisjunctionModel(operands);
    }

    /**
     * Adapts an arbitrary boolean model without copying its value or listeners.
     * Existing specialized models are returned unchanged.
     * @param model source model, including database-backed models
     * @return a specialized view preserving source writes and validation
     */
    static BaseBooleanModel of(final Model<Boolean> model) {
        Objects.requireNonNull(model, "model");
        return model instanceof BaseBooleanModel specialized
            ? specialized : new BooleanModelAdapter(model);
    }
}
