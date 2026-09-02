/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.model.Model;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Coordinates checked-state models of radio buttons so that at most one is selected.
 *
 * <p>Selecting a button, either from the browser or directly through its model, clears all other
 * buttons in the group. Clearing the selected button programmatically is allowed and leaves the
 * group without a selection.</p>
 */
public final class RadioGroup implements AutoCloseable {
    /**
     * Model subscription associated with one button.
     */
    private static final class Membership {
        private Model<Boolean> model;
        private final Listener<Boolean> listener;

        /**
         * Creates a model subscription descriptor.
         *
         * @param model observed model
         * @param listener registered listener
         */
        Membership(final Model<Boolean> model, final Listener<Boolean> listener) {
            this.model = model;
            this.listener = listener;
        }
    }

    /**
     * Buttons in insertion order and their model subscriptions.
     */
    private final Map<RadioButton, Membership> buttons = new LinkedHashMap<>();

    /**
     * Prevents notifications caused by clearing peers from recursively restarting selection.
     */
    private boolean updating;

    /**
     * Creates an empty group.
     */
    public RadioGroup() {
    }

    /**
     * Creates a group and adds the specified buttons in order.
     *
     * <p>If several buttons are initially selected, the last one wins.</p>
     *
     * @param buttons buttons to add
     */
    public RadioGroup(final RadioButton... buttons) {
        for (final RadioButton button : buttons) {
            this.add(button);
        }
    }

    /**
     * Adds a button and starts observing its checked-state model.
     *
     * @param button button to add
     * @throws IllegalArgumentException when another button uses the same model
     */
    public void add(final RadioButton button) {
        final RadioButton item = Objects.requireNonNull(button, "button");
        if (this.buttons.containsKey(item)) {
            return;
        }
        final Model<Boolean> model = item.getCheckedStateModel();
        this.requireUniqueModel(item, model);
        final Listener<Boolean> listener = selected -> this.changed(item, selected);
        this.buttons.put(item, new Membership(model, listener));
        item.attach(this);
        model.addListener(listener);
        if (model.getData()) {
            this.changed(item, true);
        }
    }

    /**
     * Removes a button and stops observing it without changing its checked state.
     *
     * @param button button to remove
     * @return whether the button belonged to this group
     */
    public boolean remove(final RadioButton button) {
        final Membership membership = this.buttons.remove(button);
        if (membership == null) {
            return false;
        }
        membership.model.removeListener(membership.listener);
        button.detach(this);
        return true;
    }

    /**
     * Returns a stable snapshot of buttons in insertion order.
     *
     * @return immutable button list
     */
    public List<RadioButton> getButtons() {
        return List.copyOf(this.buttons.keySet());
    }

    /**
     * Unsubscribes from every button and empties the group.
     */
    @Override
    public void close() {
        for (final RadioButton button : new ArrayList<>(this.buttons.keySet())) {
            this.remove(button);
        }
    }

    /**
     * Validates a checked-model replacement before the button applies it.
     *
     * @param button button replacing its model
     * @param model replacement model
     */
    void validateModelReplacement(
        final RadioButton button,
        final Model<Boolean> model
    ) {
        this.requireUniqueModel(button, model);
    }

    /**
     * Moves this group's listener from the old model to the replacement.
     *
     * @param button button whose model changed
     * @param model replacement model
     */
    void replaceModel(final RadioButton button, final Model<Boolean> model) {
        final Membership membership = this.buttons.get(button);
        if (membership == null || membership.model == model) {
            return;
        }
        membership.model.removeListener(membership.listener);
        membership.model = model;
        membership.model.addListener(membership.listener);
        if (model.getData()) {
            this.changed(button, true);
        }
    }

    /**
     * Clears all peers after a button becomes selected.
     */
    private void changed(final RadioButton selected, final Boolean checked) {
        if (!checked || this.updating) {
            return;
        }
        this.updating = true;
        try {
            for (final RadioButton button : this.buttons.keySet()) {
                if (button != selected && button.isChecked()) {
                    button.uncheck();
                }
            }
        } finally {
            this.updating = false;
        }
    }

    /**
     * Rejects sharing one model between two buttons because it makes mutual exclusion impossible.
     */
    private void requireUniqueModel(
        final RadioButton button,
        final Model<Boolean> model
    ) {
        for (final Map.Entry<RadioButton, Membership> entry : this.buttons.entrySet()) {
            if (entry.getKey() != button && entry.getValue().model == model) {
                throw new IllegalArgumentException(
                    "Radio buttons in one group must use different checked-state models"
                );
            }
        }
    }
}
