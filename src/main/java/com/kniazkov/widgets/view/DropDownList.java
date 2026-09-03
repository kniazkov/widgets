/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.controller.HandlesFocusEvents;
import com.kniazkov.widgets.controller.HandlesPointerEvents;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.StringModel;
import com.kniazkov.widgets.protocol.SetDropDownOption;
import com.kniazkov.widgets.protocol.SetDropDownOptions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A native drop-down list with a fixed number of reactive text options.
 *
 * <p>Each position is created once and remains stable for the lifetime of the widget. Its text
 * comes from a {@link Model} and can therefore change reactively, while the selected value is the
 * position itself. Index {@code -1} means that no option is selected.</p>
 *
 * <p>This widget deliberately does not contain arbitrary child widgets. Browser-native option
 * elements provide correct keyboard navigation and accessibility; a popup containing arbitrary
 * widgets belongs to a separate composite control.</p>
 */
public final class DropDownList extends InlineWidget<DropDownListStyle>
        implements HasSelectedIndex, HasDisabledState, HasStyledText, HasColor, HasBgColor,
        HasBorder, HasAbsoluteWidth, HasAbsoluteHeight, HasMargin, HasPadding, HasBoxShadow,
        HasOutline, HasCursor, HasTransition, HasBoxSizing, HandlesFocusEvents,
        HandlesPointerEvents {
    /**
     * Fixed ordered option bindings.
     */
    private final List<OptionBinding> options;

    /**
     * Returns the global default style.
     *
     * @return default style
     */
    public static DropDownListStyle getDefaultStyle() {
        return DropDownListStyle.DEFAULT;
    }

    /**
     * Creates an empty drop-down list.
     */
    public DropDownList() {
        this(getDefaultStyle(), List.<Model<String>>of());
    }

    /**
     * Creates fixed options whose texts are stored in newly created string models.
     *
     * @param options ordered option texts
     */
    public DropDownList(final String... options) {
        this(getDefaultStyle(), toModels(List.of(options)));
    }

    /**
     * Creates fixed options whose texts are stored in newly created string models.
     *
     * @param options ordered option texts
     */
    public DropDownList(final Collection<String> options) {
        this(getDefaultStyle(), toModels(options));
    }

    /**
     * Creates a list from an ordered collection of option models.
     *
     * @param options ordered option models
     */
    public DropDownList(final Iterable<Model<String>> options) {
        this(getDefaultStyle(), copyModels(options));
    }

    /**
     * Creates fixed string options with an explicit style.
     *
     * @param style widget style
     * @param options ordered option texts
     */
    public DropDownList(
        final DropDownListStyle style,
        final Collection<String> options
    ) {
        this(style, toModels(options));
    }

    /**
     * Creates reactive fixed options with an explicit style.
     *
     * @param style widget style
     * @param optionModels ordered option models
     */
    @SuppressWarnings("this-escape")
    public DropDownList(
        final DropDownListStyle style,
        final Iterable<Model<String>> optionModels
    ) {
        super(style);
        final List<Model<String>> models = copyModels(optionModels);
        final List<String> texts = new ArrayList<>(models.size());
        for (final Model<String> model : models) {
            texts.add(model.getData());
        }
        this.pushUpdate(new SetDropDownOptions(this.getId(), texts));
        final List<OptionBinding> bindings = new ArrayList<>(models.size());
        for (int index = 0; index < models.size(); index++) {
            bindings.add(new OptionBinding(index, models.get(index)));
        }
        this.options = List.copyOf(bindings);
        this.getSelectedIndexModel();
    }

    /**
     * Returns the fixed number of options.
     *
     * @return option count
     */
    public int getOptionCount() {
        return this.options.size();
    }

    /**
     * Returns the model bound to an option position.
     *
     * @param index option position
     * @return visible-text model
     * @throws IndexOutOfBoundsException when the position does not exist
     */
    public Model<String> getOptionModel(final int index) {
        return this.options.get(index).getModel();
    }

    /**
     * Returns an immutable snapshot of the option models in their fixed order.
     *
     * @return option models
     */
    public List<Model<String>> getOptionModels() {
        final List<Model<String>> result = new ArrayList<>(this.options.size());
        for (final OptionBinding option : this.options) {
            result.add(option.getModel());
        }
        return List.copyOf(result);
    }

    /**
     * Replaces the text model at an existing position without changing option count or indices.
     *
     * @param index option position
     * @param model replacement visible-text model
     * @throws IndexOutOfBoundsException when the position does not exist
     */
    public void setOptionModel(final int index, final Model<String> model) {
        this.options.get(index).setModel(Objects.requireNonNull(model, "model"));
    }

    /**
     * Returns the current visible text at an option position.
     *
     * @param index option position
     * @return visible text
     */
    public String getOptionText(final int index) {
        return this.getOptionModel(index).getData();
    }

    /**
     * Changes visible text through the model bound to an existing position.
     *
     * @param index option position
     * @param text new visible text
     */
    public void setOptionText(final int index, final String text) {
        this.getOptionModel(index).setData(Objects.requireNonNull(text, "text"));
    }

    @Override
    public void setSelectedIndexModel(final Model<Integer> model) {
        final Model<Integer> replacement = Objects.requireNonNull(model, "model");
        this.validateSelectedIndex(replacement.getData());
        HasSelectedIndex.super.setSelectedIndexModel(replacement);
    }

    @Override
    public void setSelectedIndex(final int index) {
        this.validateSelectedIndex(index);
        HasSelectedIndex.super.setSelectedIndex(index);
    }

    @Override
    public String getType() {
        return "drop down list";
    }

    /**
     * Rejects values that cannot refer to this widget's stable option array.
     *
     * @param index selected position
     */
    private void validateSelectedIndex(final int index) {
        if (index < -1 || index >= this.options.size()) {
            throw new IllegalArgumentException("Invalid drop-down option index: " + index);
        }
    }

    /**
     * Converts simple strings to independent reactive models.
     *
     * @param options option texts
     * @return newly created models
     */
    private static List<Model<String>> toModels(final Collection<String> options) {
        final List<Model<String>> result = new ArrayList<>(options.size());
        for (final String option : options) {
            result.add(new StringModel(Objects.requireNonNull(option, "option")));
        }
        return result;
    }

    /**
     * Copies and validates model references from an arbitrary iterable.
     *
     * @param options option models
     * @return validated model snapshot
     */
    private static List<Model<String>> copyModels(final Iterable<Model<String>> options) {
        final List<Model<String>> result = new ArrayList<>();
        for (final Model<String> model : Objects.requireNonNull(options, "options")) {
            result.add(Objects.requireNonNull(model, "option model"));
        }
        return result;
    }

    /**
     * Keeps one stable option position bound to a replaceable text model.
     */
    private final class OptionBinding {
        /**
         * Stable option position.
         */
        private final int index;

        /**
         * Strong listener reference required by weak-listener models.
         */
        private final Listener<String> listener;

        /**
         * Currently bound text model.
         */
        private Model<String> model;

        /**
         * Binds one fixed position without sending a redundant initial update.
         *
         * @param index option position
         * @param model initial text model
         */
        OptionBinding(final int index, final Model<String> model) {
            this.index = index;
            this.listener = text -> DropDownList.this.pushUpdate(
                new SetDropDownOption(DropDownList.this.getId(), this.index, text)
            );
            this.model = model;
            this.model.addListener(this.listener);
        }

        /**
         * Returns the current text model.
         *
         * @return text model
         */
        Model<String> getModel() {
            return this.model;
        }

        /**
         * Rebinds this position and immediately publishes the replacement's current text.
         *
         * @param replacement replacement model
         */
        void setModel(final Model<String> replacement) {
            if (this.model != replacement) {
                this.model.removeListener(this.listener);
                this.model = replacement;
                this.model.addListener(this.listener);
                this.listener.accept(replacement.getData());
            }
        }
    }
}
