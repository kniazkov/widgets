/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.HandlesPointerEvents;
import com.kniazkov.widgets.model.Model;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A radio button that can be selected by the user but cannot be cleared by a user click.
 *
 * <p>The checked-state model remains fully writable: application code may select or clear the
 * button through {@link #getCheckedStateModel()}. Use {@link RadioGroup} to make several radio
 * buttons mutually exclusive.</p>
 */
public final class RadioButton extends InlineWidget<RadioButtonStyle>
        implements HasCheckedState, HasDisabledState, HasColor, HasBgColor, HasAbsoluteWidth,
        HasAbsoluteHeight, HasMargin, HasSelectableImage, HasOpacity, HandlesPointerEvents,
        HasBoxShadow, HasCursor, HasTransition, HasBoxSizing {
    /**
     * Groups observing this button. Used to keep their model subscriptions current.
     */
    private final List<RadioGroup> groups = new ArrayList<>();

    /**
     * Returns the default style instance used by radio buttons.
     *
     * @return global default radio-button style
     */
    public static RadioButtonStyle getDefaultStyle() {
        return RadioButtonStyle.DEFAULT;
    }

    /**
     * Creates an unselected radio button.
     */
    @SuppressWarnings("this-escape")
    public RadioButton() {
        super(getDefaultStyle());
        this.uncheck();
    }

    /**
     * Creates a radio button backed by the specified checked-state model.
     *
     * @param checked checked-state model
     */
    @SuppressWarnings("this-escape")
    public RadioButton(final Model<Boolean> checked) {
        super(getDefaultStyle());
        this.setCheckedStateModel(Objects.requireNonNull(checked, "checked"));
    }

    @Override
    public void setCheckedStateModel(final Model<Boolean> model) {
        final Model<Boolean> replacement = Objects.requireNonNull(model, "model");
        for (final RadioGroup group : List.copyOf(this.groups)) {
            group.validateModelReplacement(this, replacement);
        }
        HasCheckedState.super.setCheckedStateModel(replacement);
        for (final RadioGroup group : List.copyOf(this.groups)) {
            group.replaceModel(this, replacement);
        }
    }

    /**
     * Registers a group interested in checked-model replacements.
     *
     * @param group group
     */
    void attach(final RadioGroup group) {
        this.groups.add(group);
    }

    /**
     * Unregisters a group.
     *
     * @param group group
     */
    void detach(final RadioGroup group) {
        this.groups.remove(group);
    }

    @Override
    public String getType() {
        return "radio button";
    }
}
