/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.StringModel;
import com.kniazkov.widgets.protocol.SetSuggestions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Editable text with suggestions bound directly to application-owned string models.
 * Suggestions are filtered by case-insensitive substring in the browser. Choosing one
 * copies its current text into the field's text model, without modifying the source model.
 * Arbitrary text is allowed; the application owns the suggestion history.
 */
public class SuggestionField extends InputField implements HasSuggestionSeparator {
    /**
     * Ordered references to the application's suggestion models.
     */
    private List<Model<String>> suggestions = List.of();

    /**
     * Strong listener reference required by weak-listener models.
     */
    private final Listener<String> suggestionListener;

    /**
     * Returns the default style.
     * @return default suggestion field style
     */
    public static SuggestionFieldStyle getDefaultStyle() {
        return SuggestionFieldStyle.DEFAULT;
    }

    /**
     * Creates an empty field with no suggestions.
     */
    public SuggestionField() {
        this(getDefaultStyle(), List.<Model<String>>of());
    }

    /**
     * Creates an empty field with independent string models for convenience.
     * @param suggestions ordered suggestion texts
     */
    public SuggestionField(final String... suggestions) {
        this(List.of(suggestions));
    }

    /**
     * Creates an empty field with independent string models for convenience.
     * @param suggestions ordered suggestion texts
     */
    public SuggestionField(final Collection<String> suggestions) {
        this(getDefaultStyle(), toModels(suggestions));
    }

    /**
     * Creates an empty field bound to existing models, including database-backed models.
     * @param suggestions ordered suggestion models; references are retained, not copied as text
     */
    public SuggestionField(final Iterable<Model<String>> suggestions) {
        this(getDefaultStyle(), suggestions);
    }

    /**
     * Creates a styled field with independent string models for convenience.
     * @param style field style
     * @param suggestions ordered suggestion texts
     */
    public SuggestionField(final SuggestionFieldStyle style, final Collection<String> suggestions) {
        this(style, toModels(suggestions));
    }

    /**
     * Creates a styled field bound directly to existing suggestion models.
     * The editable text has its own model, configurable with {@link #setTextModel(Model)}.
     * @param style field style
     * @param suggestions ordered suggestion models
     */
    @SuppressWarnings("this-escape")
    public SuggestionField(
        final SuggestionFieldStyle style,
        final Iterable<Model<String>> suggestions
    ) {
        super(style, "");
        this.suggestionListener = text -> this.publishSuggestions();
        this.setSuggestionModels(suggestions);
    }

    /**
     * Returns an immutable snapshot of the model references in display order.
     * @return suggestion models
     */
    public List<Model<String>> getSuggestionModels() {
        return this.suggestions;
    }

    /**
     * Returns the existing model at a suggestion position.
     * @param index suggestion position
     * @return application-owned string model
     */
    public Model<String> getSuggestionModel(final int index) {
        return this.suggestions.get(index);
    }

    /**
     * Rebinds one suggestion, detaching the old model if no other position uses it.
     * @param index suggestion position
     * @param model replacement model
     */
    public void setSuggestionModel(final int index, final Model<String> model) {
        final List<Model<String>> updated = new ArrayList<>(this.suggestions);
        updated.set(index, Objects.requireNonNull(model, "model"));
        this.setSuggestionModels(updated);
    }

    /**
     * Replaces the ordered model references, allowing suggestions to be added or removed.
     * Changes to a supplied model immediately update the suggestions. Structural changes to
     * the caller's collection require another call to this method. Entered text is preserved.
     * @param models ordered, non-null suggestion models
     */
    public void setSuggestionModels(final Iterable<Model<String>> models) {
        final List<Model<String>> updated = new ArrayList<>();
        for (final Model<String> model : Objects.requireNonNull(models, "models")) {
            updated.add(Objects.requireNonNull(model, "suggestion model"));
        }
        for (final Model<String> model : uniqueModels(this.suggestions)) {
            model.removeListener(this.suggestionListener);
        }
        this.suggestions = List.copyOf(updated);
        for (final Model<String> model : uniqueModels(this.suggestions)) {
            model.addListener(this.suggestionListener);
        }
        this.publishSuggestions();
    }

    @Override
    public String getType() {
        return "suggestion field";
    }

    /**
     * Publishes an immutable text snapshot using the current source models.
     */
    private void publishSuggestions() {
        final List<String> texts = new ArrayList<>(this.suggestions.size());
        for (final Model<String> model : this.suggestions) {
            texts.add(model.getData());
        }
        this.pushUpdate(new SetSuggestions(this.getId(), texts));
    }

    /**
     * Subscribes only once when a model is used at multiple positions.
     * @param models ordered source models
     * @return distinct model instances, regardless of their equality implementation
     */
    private static Set<Model<String>> uniqueModels(final List<Model<String>> models) {
        final Set<Model<String>> result = Collections.newSetFromMap(new IdentityHashMap<>());
        result.addAll(models);
        return result;
    }

    /**
     * Converts convenience string arguments to independent models.
     * @param values suggestion texts
     * @return models containing those texts
     */
    private static List<Model<String>> toModels(final Collection<String> values) {
        final List<Model<String>> models = new ArrayList<>(values.size());
        for (final String value : values) {
            models.add(new StringModel(Objects.requireNonNull(value, "suggestion")));
        }
        return models;
    }
}
