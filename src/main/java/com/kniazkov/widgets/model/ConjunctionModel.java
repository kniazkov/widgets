/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import java.util.Arrays;
import java.util.List;

/**
 * A boolean model that represents the logical conjunction (<i>AND</i>) of multiple
 * underlying boolean models.
 * <p>
 * The value of this model is {@code true} only if <b>all</b> wrapped models currently hold
 * {@code true}. Likewise, its validity reflects whether all underlying models are valid.
 * <p>
 * The model is fully reactive: whenever any of the underlying models changes its data or validity,
 * this wrapper notifies its listeners. This allows combining several independent conditions into
 * a single aggregate flag, useful in UI logic (e.g. enabling a button only when all fields
 * are valid).
 * </p>
 */
public class ConjunctionModel extends ReadOnlyModel<Boolean> {

    /**
     * The list of underlying boolean models whose values and validity
     * are combined using logical AND.
     */
    private final List<Model<Boolean>> list;

    /**
     * A listener attached to every underlying model to propagate any update
     * (data or validity change) to this wrapper’s listeners. It must be stored in a field
     * so it remains strongly referenced and is not accidentally removed by garbage collection.
     */
    private final Listener<Boolean> forwarder;

    /**
     * Creates a new conjunction model that computes the logical AND of the
     * specified boolean-based models.
     *
     * @param base the models whose boolean values are combined
     */
    public ConjunctionModel(final List<Model<Boolean>> base) {
        this.list = base;
        this.forwarder = data -> this.notifyListeners();

        for (final Model<Boolean> model : this.list) {
            model.addListener(this.forwarder);
        }
    }

    /**
     * Creates a new conjunction model that computes the logical AND of the
     * specified boolean-based models.
     *
     * @param base the models whose boolean values are combined
     */
    public ConjunctionModel(final Model<Boolean>... base) {
        this(Arrays.asList(base));
    }

    @Override
    public boolean isValid() {
        boolean value = true;
        for (final Model<Boolean> model : this.list) {
            if (!model.isValid()) {
                value = false;
                break;
            }
        }
        return value;
    }

    @Override
    public Boolean getData() {
        boolean value = true;
        for (final Model<Boolean> model : this.list) {
            if (!model.getData()) {
                value = false;
                break;
            }
        }
        return value;
    }

    /**
     * Returns a model that represents the logical negation of this conjunction.
     *
     * @return a model exposing the negated conjunction value
     */
    public Model<Boolean> invert() {
        return new InvertModel(this);
    }
}
