/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.Cursor;
import com.kniazkov.widgets.model.Model;
import java.util.Objects;
import java.util.Set;

/**
 * Inline composition of a selection control and an interactive caption.
 * Text, checked and disabled models are delegated to the contained widgets.
 * Use the composite setters when replacing models; changing their data is shared
 * with the contained widgets. Caption clicks obey the current disabled model.
 *
 * @param <C> selection control type
 */
public abstract class LabeledChoice<C extends InlineWidget<?> & HasCheckedState
        & HasDisabledState> extends InlineBlock implements HasText, HasCheckedState,
        HasDisabledState {
    /**
     * Wrapped selection control.
     */
    private final C control;
    /**
     * Caption with disabled-state styling.
     */
    private final Caption caption = new Caption();
    /**
     * Row controlling spacing and alignment.
     */
    private final Section section = new Section();

    /**
     * Builds the composition without copying the supplied models.
     * @param control selection control
     * @param text caption model
     * @param checked selection model
     * @param radio whether clicking the caption only selects, never clears
     */
    @SuppressWarnings("this-escape")
    protected LabeledChoice(final C control, final Model<String> text,
            final Model<Boolean> checked, final boolean radio) {
        this.control = Objects.requireNonNull(control, "control");
        this.setTextModel(text);
        this.setCheckedStateModel(checked);
        this.caption.setDisabledStateModel(control.getDisabledStateModel());
        final MarginDecorator label = new MarginDecorator(this.caption);
        label.setLeftMargin(6);
        this.section.setMiddleAlignment();
        this.section.add(control);
        this.section.add(label);
        this.add(this.section);
        this.caption.onClick(event -> {
            if (!this.isDisabled()) {
                this.setCheckedFlag(radio || !this.isChecked());
            }
        });
    }

    /**
     * Returns the original selection control for further customization.
     * @return contained control
     */
    protected final C control() {
        return this.control;
    }

    /**
     * Returns the caption, including its font, color and pointer-event settings.
     * Disabled color is a separate state and does not overwrite normal colors.
     * @return interactive caption
     */
    public final ActiveText getTextWidget() {
        return this.caption;
    }

    /**
     * Returns the row for adjusting spacing, padding and alignment.
     * @return inner section
     */
    public final Section getSection() {
        return this.section;
    }

    @Override
    public final Model<String> getTextModel() {
        return this.caption.getTextModel();
    }

    @Override
    public final void setTextModel(final Model<String> model) {
        this.caption.setTextModel(Objects.requireNonNull(model, "model"));
    }

    @Override
    public final Model<Boolean> getCheckedStateModel() {
        return this.control.getCheckedStateModel();
    }

    @Override
    public final void setCheckedStateModel(final Model<Boolean> model) {
        this.control.setCheckedStateModel(Objects.requireNonNull(model, "model"));
    }

    @Override
    public final Model<Boolean> getDisabledStateModel() {
        return this.control.getDisabledStateModel();
    }

    @Override
    public final void setDisabledStateModel(final Model<Boolean> model) {
        final Model<Boolean> replacement = Objects.requireNonNull(model, "model");
        this.control.setDisabledStateModel(replacement);
        this.caption.setDisabledStateModel(replacement);
    }

    /**
     * Active text that participates in disabled-state rendering.
     */
    private static final class Caption extends ActiveText implements HasDisabledState {
        /**
         * Creates a caption with ordinary text typography and gray disabled text.
         */
        Caption() {
            super(new CaptionStyle(), "");
        }
    }

    /**
     * Caption typography follows TextWidget defaults instead of link colors.
     */
    private static final class CaptionStyle extends ActiveTextStyle {
        /**
         * Creates the default caption style.
         */
        CaptionStyle() {
            super(ActiveText.getDefaultStyle());
            final TextWidgetStyle text = TextWidget.getDefaultStyle();
            for (final State state : getSupportedStates()) {
                this.copyTextModel(text, state, Property.FONT_FACE);
                this.copyTextModel(text, state, Property.FONT_SIZE);
                this.copyTextModel(text, state, Property.FONT_WEIGHT);
                this.copyTextModel(text, state, Property.ITALIC);
                this.copyTextModel(text, state, Property.TEXT_DECORATION);
                this.copyTextModel(text, state, Property.COLOR);
            }
            this.setColor(State.DISABLED, Color.GRAY);
            this.setCursor(State.DISABLED, Cursor.DEFAULT);
        }

        /**
         * Follows a text default while isolating per-state overrides.
         * @param text source style
         * @param state target state
         * @param property copied property
         * @param <T> property value type
         */
        private <T> void copyTextModel(final TextWidgetStyle text, final State state,
                final Property<T> property) {
            this.setModel(state, property, text.getModel(State.NORMAL, property).asCascading());
        }

        /**
         * Derives caption styling without losing disabled-state support.
         * @param parent parent style
         */
        CaptionStyle(final CaptionStyle parent) {
            super(parent);
        }

        @Override
        public Set<State> getSupportedStates() {
            return State.setOf(State.NORMAL, State.HOVERED, State.ACTIVE, State.DISABLED);
        }

        @Override
        public CaptionStyle derive() {
            return new CaptionStyle(this);
        }
    }
}
