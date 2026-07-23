/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import java.util.Arrays;
import java.util.List;

/**
 * A boolean model that represents the logical disjunction (<i>OR</i>) of multiple
 * underlying boolean models.
 * <p>
 * The value of this model is {@code true} if <b>at least one</b> wrapped model currently
 * holds {@code true}. Likewise, its validity reflects whether at least one underlying
 * model is valid.
 * <p>
 * The model is fully reactive: whenever any of the underlying models changes its data or validity,
 * this wrapper notifies its listeners. This allows combining several independent conditions into
 * a single aggregate flag, useful in UI logic (e.g. requiring at least one contact method to be
 * provided).
 * </p>
 */
public class DisjunctionModel extends ReadOnlyModel<Boolean> {

    /**
     * The list of underlying boolean models whose values and validity
     * are combined using logical OR.
     */
    private final List<Model<Boolean>> list;

    /**
     * A listener attached to every underlying model to propagate any update
     * (data or validity change) to this wrapper's listeners. It must be stored in a field
     * so it remains strongly referenced and is not accidentally removed by garbage collection.
     */
    private final Listener<Boolean> forwarder;

    /**
     * Creates a new disjunction model that computes the logical OR of the
     * specified boolean-based models.
     *
     * @param base the models whose boolean values are combined
     */
    public DisjunctionModel(final List<Model<Boolean>> base) {
        this.list = base;
        this.forwarder = data -> this.notifyListeners();

        for (final Model<Boolean> model : this.list) {
            model.addListener(this.forwarder);
        }
    }

    /**
     * Creates a new disjunction model that computes the logical OR of the
     * specified boolean-based models.
     *
     * @param base the models whose boolean values are combined
     */
    @SafeVarargs
    public DisjunctionModel(final Model<Boolean>... base) {
        this(Arrays.asList(base));
    }

    @Override
    public boolean isValid() {
        boolean value = false;
        for (final Model<Boolean> model : this.list) {
            if (model.isValid()) {
                value = true;
                break;
            }
        }
        return value;
    }

    @Override
    public Boolean getData() {
        boolean value = false;
        for (final Model<Boolean> model : this.list) {
            if (model.getData()) {
                value = true;
                break;
            }
        }
        return value;
    }

    /**
     * Returns a model that represents the logical negation of this disjunction.
     *
     * @return a model exposing the negated disjunction value
     */
    public Model<Boolean> invert() {
        return new InvertModel(this);
    }
}
