/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Binding;
import com.kniazkov.widgets.model.NumberModel;
import com.kniazkov.widgets.model.NumberValue;
import java.util.Objects;

/**
 * Labelled quantity editor with exact server-side decrement and increment actions.
 * Register directly with Form.addField; limits express numeric constraints, not business rights.
 * A physical click or keyboard activation changes one step; holding never auto-repeats.
 */
public final class QuantityField extends FormField {
    /**
     * Decrement action follows the editor's current model and interaction state.
     */
    private final Button decrease = new Button("−");
    /**
     * Increment action follows the editor's current model and interaction state.
     */
    private final Button increase = new Button("+");

    /**
     * @param name stable form field name
     * @param caption visible editor label
     */
    public QuantityField(final String name, final String caption) {
        this(name, caption, NumberModel.integers());
    }

    /**
     * @param name stable form field name
     * @param caption visible editor label
     * @param model borrowed exact numeric model
     */
    @SuppressWarnings("this-escape")
    public QuantityField(final String name, final String caption, final NumberModel model) {
        super(name, caption, new NumberField(Objects.requireNonNull(model)));
        this.setActionNames("Decrease " + caption, "Increase " + caption);
        this.addEditorAction(this.decrease);
        this.addEditorAction(this.increase);
        this.decrease.onClick(event -> this.move(-1));
        this.increase.onClick(event -> this.move(1));
        final Binding<NumberValue> values = new Binding<>(model, value -> this.refresh());
        final Binding<Boolean> disabled = new Binding<>(
            this.getEditor().getModel(State.ANY, Property.DISABLED), value -> this.refresh());
        final Binding<Boolean> readOnly = new Binding<>(
            this.getEditor().getModel(State.ANY, Property.READ_ONLY), value -> this.refresh());
        this.getEditor().onStateModelsChanged(() -> {
            disabled.setModel(this.getEditor().getModel(State.ANY, Property.DISABLED));
            readOnly.setModel(this.getEditor().getModel(State.ANY, Property.READ_ONLY));
        });
        this.onRetire(values::close);
        this.onRetire(disabled::close);
        this.onRetire(readOnly::close);
    }

    @Override
    public NumberField getEditor() {
        return (NumberField) super.getEditor();
    }

    /**
     * @param decrement nonblank accessible action name
     * @param increment nonblank accessible action name
     */
    public void setActionNames(final String decrement, final String increment) {
        if (Objects.requireNonNull(decrement).isBlank()
                || Objects.requireNonNull(increment).isBlank()) {
            throw new IllegalArgumentException("Quantity actions require accessible names");
        }
        this.decrease.setAccessibleName(decrement);
        this.increase.setAccessibleName(increment);
    }

    /**
     * Rechecks live ownership, read-only state and constraints when a queued action arrives.
     * @param direction exactly -1 or 1
     */
    private void move(final int direction) {
        final NumberField editor = this.getEditor();
        if (this.hasEditor() && !editor.isRetired() && editor.isInteractionEnabled()
                && !editor.isReadOnly()) {
            editor.getValueModel().move(direction);
        }
    }

    /**
     * Recomputes both buttons even when a constraint changes without changing the raw draft.
     */
    private void refresh() {
        final NumberField editor = this.getEditor();
        final boolean blocked = editor.isDisabled() || editor.isReadOnly() || !this.hasEditor();
        this.decrease.setDisabledFlag(blocked || editor.getValueModel().next(-1).isEmpty());
        this.increase.setDisabledFlag(blocked || editor.getValueModel().next(1).isEmpty());
    }

    @Override
    public String getType() {
        return "quantity field";
    }
}
