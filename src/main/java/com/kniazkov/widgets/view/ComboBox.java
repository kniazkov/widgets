/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.Event;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Editable search with explicit strict-choice or free-text policy.
 * @param <T> application value type
 */
public final class ComboBox<T> extends TypedChoice<T> {
    /**
     * A provider must return promptly; a new search cancels its outstanding future.
     * @param <E> value type
     */
    @FunctionalInterface
    public interface Provider<E> {
        /**
         * @param query browser text
         * @param cursor empty on the first page; then provider-issued cursor
         * @return cancellable result future
         */
        CompletableFuture<Page<E>> search(String query, String cursor);
    }

    /**
     * Provider page. More pages append options; option keys must be unique across pages.
     * @param options options for this page
     * @param nextCursor provider-issued cursor, or empty at end
     * @param <E> payload type
     */
    public record Page<E>(List<ChoiceOption<E>> options, String nextCursor) {
        /**
         * Freezes provider results.
         */
        public Page {
            options = List.copyOf(options);
            Objects.requireNonNull(nextCursor);
        }
    }

    /**
     * Whether separate application-owned free text is accepted.
     */
    private final boolean freeTextAllowed;
    /**
     * Provider, null for locally filtered static options.
     */
    private Provider<T> provider;
    /**
     * Latest query; never becomes a typed value.
     */
    private String query = "";
    /**
     * Uncommitted free text in explicit free-text mode.
     */
    private String freeText = "";
    /**
     * Status sent to the client.
     */
    private String status = "ready";
    /**
     * Opaque next-page cursor.
     */
    private String cursor = "";
    /**
     * Last accepted browser query serial.
     */
    private int serial;
    /**
     * Current attempt; identity guards against cancelled/superseded completions.
     */
    private CompletableFuture<Page<T>> pending;
    /**
     * Latest displayed search result (no filtered-out selected option).
     */
    private List<ChoiceOption<T>> shown = List.of();

    /**
     * @param options static choices
     * @param freeTextAllowed whether unselected text is a valid separate input
     */
    @SuppressWarnings("this-escape")
    public ComboBox(final List<ChoiceOption<T>> options, final boolean freeTextAllowed) {
        super(false, options);
        this.freeTextAllowed = freeTextAllowed;
        this.subscribeToEvent(Event.CHOICE_SEARCH);
        this.onRetire(this::cancelPending);
        this.publishChoices();
    }

    @Override
    public String getType() {
        return "typed combo box";
    }

    /**
     * Enables async search, or switches to the current static snapshot with {@code null}.
     * @param provider async provider
     */
    public void setProvider(final Provider<T> provider) {
        this.requireUsable();
        this.cancelPending();
        this.provider = provider;
        this.resetSearch();
        this.publishChoices();
    }

    @Override
    public void setOptions(final List<ChoiceOption<T>> values) {
        this.cancelPending();
        this.resetSearch();
        super.setOptions(values);
    }

    /**
     * @return last raw text entered, or empty in strict mode or after choosing a key
     */
    public String getFreeText() {
        return this.freeText;
    }

    @Override
    public void choose(final List<String> keys) {
        this.freeText = "";
        this.cancelPending();
        this.resetSearch();
        super.choose(keys);
        this.publishChoices();
    }

    @Override
    public void clear() {
        this.choose(List.of());
    }

    /**
     * Processes search input, popup cancellation, or continuation of the current query.
     * @param action admitted command
     * @param text query text or empty
     * @param version client input serial
     */
    public void search(final String action, final String text, final int version) {
        if ("more".equals(action)) {
            if (version == this.serial && !this.cursor.isEmpty() && this.pending == null) {
                this.request(this.cursor);
            }
            return;
        }
        if (version <= this.serial) {
            return;
        }
        this.serial = version;
        this.cancelPending();
        this.resetSearch();
        if ("query".equals(action)) {
            this.query = text;
            if (this.freeTextAllowed) {
                this.freeText = text;
                super.clear();
            }
            if (this.provider != null) {
                this.request("");
                return;
            }
        }
        this.publishChoices();
    }

    @Override
    protected void selectionChanged(final List<String> keys) {
        if (!keys.isEmpty()) {
            this.freeText = "";
        }
        super.selectionChanged(keys);
    }

    @Override
    protected List<ChoiceOption<T>> visibleOptions() {
        if (this.provider != null) {
            return this.shown;
        }
        final String needle = this.query.toLowerCase(Locale.ROOT);
        return this.getOptions().stream()
            .filter(option -> option.label().toLowerCase(Locale.ROOT).contains(needle)).toList();
    }

    @Override
    protected void addChoiceState(final JsonObject json) {
        json.addNumber("serial", this.serial);
        json.addString("status", this.status);
        json.addBoolean("more", !this.cursor.isEmpty());
        json.addString("freeText", this.freeText);
    }

    /**
     * Cancels the previous future before resetting its identity.
     */
    private void cancelPending() {
        final CompletableFuture<Page<T>> previous = this.pending;
        this.pending = null;
        if (previous != null) {
            previous.cancel(true);
        }
    }

    /**
     * Closes the provider result and leaves any committed key intact.
     */
    private void resetSearch() {
        this.query = "";
        this.cursor = "";
        this.status = "ready";
        this.shown = List.of();
    }

    /**
     * Invokes the provider and publishes progress without blocking the root.
     * @param next requested provider cursor
     */
    private void request(final String next) {
        final RootWidget root = this.getRootWidget().orElse(null);
        if (root == null) {
            return;
        }
        this.status = "loading";
        this.publishChoices();
        try {
            final CompletableFuture<Page<T>> future = Objects.requireNonNull(
                this.provider.search(this.query, next));
            this.pending = future;
            final WeakReference<RootWidget> owner = new WeakReference<>(root);
            final WeakReference<ComboBox<T>> box = new WeakReference<>(this);
            future.whenComplete((page, failure) -> {
                final RootWidget context = owner.get();
                final ComboBox<T> widget = box.get();
                if (context != null && widget != null) {
                    context.execute(() -> widget.finish(context, future, page, !next.isEmpty()));
                }
            });
        } catch (final RuntimeException failure) {
            this.reportError();
        }
    }

    /**
     * Publishes only the live request on its original page.
     * @param root original page
     * @param future current future
     * @param page result, null on provider failure
     * @param append append next page rather than replace
     */
    private void finish(final RootWidget root, final CompletableFuture<Page<T>> future,
            final Page<T> page, final boolean append) {
        if (this.pending != future) {
            return;
        }
        this.pending = null;
        if (this.isRetired() || this.getRootWidget().orElse(null) != root) {
            return;
        }
        if (page == null) {
            this.reportError();
            return;
        }
        try {
            final List<ChoiceOption<T>> updated = new ArrayList<>(append ? this.shown : List.of());
            updated.addAll(page.options());
            this.status = "ready";
            this.cursor = page.nextCursor();
            this.shown = List.copyOf(updated);
            this.replaceOptions(updated, true);
        } catch (final IllegalArgumentException failure) {
            this.shown = List.of();
            this.reportError();
        }
    }

    /**
     * Publishes a localized-by-client generic error without internal exception text.
     */
    private void reportError() {
        this.cursor = "";
        this.status = "error";
        this.publishChoices();
    }
}
