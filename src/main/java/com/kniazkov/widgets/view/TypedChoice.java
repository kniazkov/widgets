/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.controller.Controller;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.model.DefaultModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.protocol.SetChoiceState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared state for the typed choice family.
 * @param <T> application value type
 */
public abstract class TypedChoice<T> extends InlineWidget<DropDownListStyle>
        implements HasDisabledState, HasInvalidState {
    /**
     * Available options in display order.
     */
    private Map<String, ChoiceOption<T>> options = new LinkedHashMap<>();

    /**
     * Stable selected keys as an immutable reactive list.
     */
    private final Model<List<String>> selection = new SelectionModel(List.of());

    /**
     * Held strongly for this widget's lifetime because Model keeps weak listeners.
     */
    private final Listener<List<String>> change = this::selectionChanged;

    /**
     * Whether the widget allows multiple selected keys.
     */
    private final boolean multiple;

    /**
     * Application-localized hint.
     */
    private String placeholder = "";

    /**
     * @param multiple multiple-selection mode
     * @param options initial ordered options
     */
    @SuppressWarnings("this-escape")
    protected TypedChoice(final boolean multiple, final List<ChoiceOption<T>> options) {
        super(DropDownListStyle.DEFAULT);
        this.multiple = multiple;
        this.options = index(options);
        this.selection.addListener(this.change);
        this.subscribeToEvent(Event.CHOOSE_KEYS);
        this.onRetire(() -> this.selection.removeListener(this.change));
    }

    /**
     * Replaces the model in one snapshot; preserves enabled keys that still exist.
     * @param values replacement options in display order
     */
    public void setOptions(final List<ChoiceOption<T>> values) {
        this.replaceOptions(values, false);
    }

    /**
     * @param values replacement options
     * @param retainSelected whether a filtered view may keep keys outside its result page
     */
    protected final void replaceOptions(final List<ChoiceOption<T>> values,
            final boolean retainSelected) {
        this.requireUsable();
        final Map<String, ChoiceOption<T>> replacement = index(values);
        if (retainSelected) {
            for (final String key : this.selection.getData()) {
                replacement.putIfAbsent(key, this.options.get(key));
            }
        }
        if (replacement.size() > 512) {
            throw new IllegalArgumentException("A choice snapshot cannot exceed 512 options");
        }
        this.options = replacement;
        final List<String> retained = this.selection.getData().stream()
            .filter(this::enabledKey).toList();
        if (!this.selection.setData(retained)) {
            this.publishChoices();
        }
    }

    /**
     * @return current ordered option snapshot
     */
    public final List<ChoiceOption<T>> getOptions() {
        return List.copyOf(this.options.values());
    }

    /**
     * @return reactive selection keys, validated on writes
     */
    public final Model<List<String>> getSelectionModel() {
        return this.selection;
    }

    /**
     * @return current typed values in selection order
     */
    public final List<T> getSelectedValues() {
        return this.selection.getData().stream().map(this.options::get)
            .map(ChoiceOption::value).toList();
    }

    /**
     * @param text localized placeholder; independent of visual option labels
     */
    public final void setPlaceholder(final String text) {
        this.requireUsable();
        this.placeholder = Objects.requireNonNull(text);
        this.publishChoices();
    }

    /**
     * Installs a browser-driven change callback. Programmatic writes use the model listeners.
     * @param controller callback receiving the newly accepted immutable key list
     */
    public final void onChoose(final Controller<List<String>> controller) {
        Objects.requireNonNull(controller);
        this.setController(Event.CHOOSE_KEYS,
            ignored -> controller.handleEvent(this.getSelectionModel().getData()));
    }

    /**
     * Clears all current keys.
     */
    public void clear() {
        this.selection.setData(List.of());
    }

    /**
     * @param keys proposed snapshot
     * @return whether all keys are distinct and selectable
     */
    public final boolean acceptsKeys(final List<String> keys) {
        return !this.isRetired() && (this.multiple || keys.size() <= 1)
            && keys.size() <= this.options.size()
            && keys.stream().distinct().count() == keys.size()
            && keys.stream().allMatch(this::enabledKey);
    }

    /**
     * Applies an admitted browser selection.
     * @param keys stable keys
     */
    public void choose(final List<String> keys) {
        this.selection.setData(keys);
    }

    /**
     * @return options shown in the popup, excluding retained off-page values as applicable
     */
    protected List<ChoiceOption<T>> visibleOptions() {
        return this.getOptions();
    }

    /**
     * Called on actual model selection changes before publication.
     * @param keys the new selection
     */
    protected void selectionChanged(final List<String> keys) {
        this.publishChoices();
    }

    /**
     * Publishes a consistent UI snapshot.
     */
    protected final void publishChoices() {
        final JsonObject json = new JsonObject();
        json.addString("placeholder", this.placeholder);
        json.addString("selectedLabel", this.selection.getData().isEmpty()
            ? "" : this.options.get(this.selection.getData().getFirst()).label());
        final JsonArray keys = json.createArray("keys");
        this.selection.getData().forEach(key -> keys.addString(key));
        final JsonArray values = json.createArray("options");
        for (final ChoiceOption<T> option : this.visibleOptions()) {
            final JsonObject entry = new JsonObject();
            entry.addString("key", option.key());
            entry.addString("label", option.label());
            entry.addBoolean("disabled", option.disabled());
            values.addElement(entry);
        }
        this.addChoiceState(json);
        this.pushUpdate(new SetChoiceState(this.getId(), json));
    }

    /**
     * @param json outgoing snapshot to extend
     */
    protected void addChoiceState(final JsonObject json) {
        /*
         * Static controls need no search state.
         */
    }

    /**
     * @param key stable key
     * @return whether the current option is enabled
     */
    private boolean enabledKey(final String key) {
        final ChoiceOption<T> option = this.options.get(key);
        return option != null && !option.disabled();
    }

    /**
     * @param options ordered snapshot
     * @param <E> option value type
     * @return unique keys in display order
     */
    private static <E> Map<String, ChoiceOption<E>> index(final List<ChoiceOption<E>> options) {
        if (options.size() > 512) {
            throw new IllegalArgumentException("A choice snapshot cannot exceed 512 options");
        }
        final Map<String, ChoiceOption<E>> result = new LinkedHashMap<>();
        for (final ChoiceOption<E> option : new ArrayList<>(options)) {
            if (result.putIfAbsent(option.key(), option) != null) {
                throw new IllegalArgumentException("Duplicate choice key");
            }
        }
        return result;
    }
    /**
     * Validates application and browser writes with the current choices.
     */
    private final class SelectionModel extends DefaultModel<List<String>> {
        /**
         * @param keys initial immutable keys
         */
        SelectionModel(final List<String> keys) {
            super(List.copyOf(keys));
        }

        @Override
        public List<String> getDefaultData() {
            return List.of();
        }

        @Override
        public Model<List<String>> deriveWithData(final List<String> keys) {
            if (!TypedChoice.this.acceptsKeys(keys)) {
                throw new IllegalArgumentException("Invalid derived selection");
            }
            return new SelectionModel(keys);
        }

        @Override
        public boolean setData(final List<String> keys) {
            final List<String> copy = List.copyOf(keys);
            if (!TypedChoice.this.acceptsKeys(copy)) {
                throw new IllegalArgumentException("Unknown, disabled or duplicate selection key");
            }
            return super.setData(copy);
        }
    }
}
