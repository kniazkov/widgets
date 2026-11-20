/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Single-use modal form with a typed result and serialized asynchronous confirmation.
 * User dismissal is blocked while busy; disposal always ends the result and cancels pending work.
 * @param <T> non-null application result of confirmation
 */
public final class Dialog<T> extends ModalPopup {
    /**
     * Distinguishes explicit cancellation from closing without confirmation.
     */
    public enum Reason {
        /**
         * The application action succeeded.
         */
        CONFIRMED,
        /**
         * The user activated the cancellation action.
         */
        CANCELLED,
        /**
         * Close, Escape, outside click, removal or page disposal.
         */
        CLOSED
    }

    /**
     * Immutable completion; only a confirmed result contains a value.
     * @param <T> result type
     */
    public static final class Result<T> {
        /**
         * Terminal decision.
         */
        private final Reason reason;
        /**
         * Successful application value, absent for cancel/close.
         */
        private final T value;

        /**
         * Creates one internally validated terminal result.
         * @param reason decision
         * @param value successful value or absent internal state
         */
        private Result(final Reason reason, final T value) {
            this.reason = Objects.requireNonNull(reason);
            if ((reason == Reason.CONFIRMED) != (value != null)) {
                throw new IllegalArgumentException("Only confirmation has a value");
            }
            this.value = value;
        }

        /**
         * @return terminal decision
         */
        public Reason reason() {
            return this.reason;
        }

        /**
         * @return successful value, empty for cancel/close
         */
        public Optional<T> value() {
            return Optional.ofNullable(this.value);
        }
    }

    /**
     * Application-localized captions and safe feedback, never exception details.
     * @param confirm confirmation caption
     * @param cancel cancellation caption
     * @param close close caption
     * @param busy in-flight status
     * @param error safe retryable failure
     * @param invalid form validation summary
     */
    public record Messages(String confirm, String cancel, String close, String busy,
            String error, String invalid) {
        /**
         * Requires meaningful visible and accessible labels.
         */
        public Messages {
            for (final String text : new String[] {confirm, cancel, close, busy, error, invalid}) {
                if (Objects.requireNonNull(text).isBlank()) {
                    throw new IllegalArgumentException("Empty dialog label");
                }
            }
        }
    }

    /**
     * Completion controlled exclusively by the dialog.
     */
    private final CompletableFuture<Result<T>> result = new CompletableFuture<>();
    /**
     * Optional application content before the form.
     */
    private final Panel content = new Panel();
    /**
     * Existing validation, keyboard submit and duplicate suppression implementation.
     */
    private final Form form;
    /**
     * Localized feedback.
     */
    private final Messages messages;
    /**
     * Application action; released on terminal completion.
     */
    private Function<Map<String, String>, CompletionStage<T>> action;
    /**
     * Explicit cancel action.
     */
    private final Button cancelButton;
    /**
     * Explicit close action.
     */
    private final Button closeButton;
    /**
     * Holds one operation indicator at a time.
     */
    private final Panel feedback = new Panel();
    /**
     * Current pending operation, or null when idle.
     */
    private CompletableFuture<T> pending;
    /**
     * Guards recursive callbacks as well as duplicate browser events.
     */
    private boolean busy;
    /**
     * Policy restored after a failed attempt.
     */
    private boolean previousEscape;
    /**
     * Policy restored after a failed attempt.
     */
    private boolean previousOutside;
    /**
     * Generation of an asynchronous attempt.
     */
    private long attempt;

    /**
     * Creates an unattached dialog. Return from the action promptly; do IO on another executor.
     * @param title visible and accessible title
     * @param messages localized labels
     * @param action validated immutable form snapshot to asynchronous application result
     */
    @SuppressWarnings("this-escape")
    public Dialog(final String title, final Messages messages,
            final Function<Map<String, String>, CompletionStage<T>> action) {
        if (Objects.requireNonNull(title).isBlank()) {
            throw new IllegalArgumentException("Empty dialog title");
        }
        this.messages = Objects.requireNonNull(messages);
        this.action = Objects.requireNonNull(action);
        this.setAccessibleName(title);
        this.setCloseOnEscape(true);
        this.setWidth(560);
        final TextWidget heading = new TextWidget(title);
        heading.setFontSize("22px");
        heading.setHeadingLevel(HeadingLevel.H2);
        this.add(new Section(heading));
        this.add(this.content);
        this.form = new Form(title, messages.confirm());
        this.form.setMessages(new Form.Messages("", "", messages.invalid(), ""));
        this.form.onSubmit(this::start);
        this.add(this.form);
        this.add(this.feedback);
        this.cancelButton = new Button(messages.cancel());
        this.closeButton = new Button(messages.close());
        this.cancelButton.onClick(event -> this.cancel());
        this.closeButton.onClick(event -> this.close());
        this.add(new Section(this.cancelButton, this.closeButton));
        this.onRetire(() -> this.resolve(Reason.CLOSED, null));
    }

    /**
     * Creates a simple boolean confirmation using the same modal/form implementation.
     * @param title title
     * @param message plain-text explanation
     * @param messages labels
     * @return new unattached dialog
     */
    public static Dialog<Boolean> confirmation(final String title, final String message,
            final Messages messages) {
        final Dialog<Boolean> dialog = new Dialog<>(title, messages,
            values -> CompletableFuture.completedFuture(true));
        dialog.content.add(new Section(new TextWidget(Objects.requireNonNull(message))));
        return dialog;
    }

    /**
     * @return owned container for explanatory text or nested-dialog launchers
     */
    public Panel getContent() {
        return this.content;
    }

    /**
     * Adds a field to the existing Form validation and Enter path.
     * @param field owned form field
     */
    public void addField(final FormField field) {
        this.requireUsable();
        this.form.addField(field);
    }

    /**
     * Configures application validation before the asynchronous action.
     * @param validator immutable snapshot to field/general errors
     */
    public void setValidator(final Function<Map<String, String>, Form.Result> validator) {
        this.requireUsable();
        this.form.setValidator(validator);
    }

    /**
     * @return read-only completion stage; mutating a derived future cannot finish this dialog
     */
    public CompletionStage<Result<T>> result() {
        return this.result.minimalCompletionStage();
    }

    /**
     * @return whether an application action is in flight
     */
    public boolean isBusy() {
        return this.busy;
    }

    /**
     * Confirms through the same validation path as Enter and the form button.
     */
    public void confirm() {
        if (!this.result.isDone()) {
            this.form.submit();
        }
    }

    /**
     * @return false when busy or already completed
     */
    public boolean cancel() {
        return this.end(Reason.CANCELLED);
    }

    /**
     * @return false when busy or already completed
     */
    public boolean close() {
        return this.end(Reason.CLOSED);
    }

    @Override
    public void dismiss() {
        this.close();
    }

    @Override
    public void remove() {
        super.remove();
        this.resolve(Reason.CLOSED, null);
    }

    @Override
    public String getType() {
        return "dialog";
    }

    /**
     * Serializes an explicit user decision in the owning page.
     * @param reason decision
     * @return whether this call completed the dialog
     */
    private boolean end(final Reason reason) {
        final boolean[] ended = {false};
        this.getRootWidget().ifPresent(root -> root.execute(() -> {
            if (!this.busy && !this.result.isDone()) {
                this.resolve(reason, null);
                this.dispose();
                ended[0] = true;
            }
        }));
        return ended[0];
    }

    /**
     * Starts work only after Form has acquired its submit guard and validated the snapshot.
     * @param values immutable validated fields
     * @return form completion bridge
     */
    private CompletionStage<Form.Result> start(final Map<String, String> values) {
        final CompletableFuture<Form.Result> bridge = new CompletableFuture<>();
        if (this.busy || this.result.isDone()) {
            bridge.completeExceptionally(new IllegalStateException("Dialog is not idle"));
            return bridge;
        }
        final RootWidget root = this.getRootWidget().orElseThrow();
        this.busy = true;
        this.previousEscape = this.isCloseOnEscape();
        this.previousOutside = this.isCloseOnOutsideClick();
        this.setCloseOnEscape(false);
        this.setCloseOnOutsideClick(false);
        this.cancelButton.disable();
        this.closeButton.disable();
        if (this.feedback.getChildCount() != 0) {
            this.feedback.getChild(0).dispose();
        }
        this.feedback.add(new Busy(this.messages.busy()));
        final long id = ++this.attempt;
        final WeakReference<Dialog<T>> owner = new WeakReference<>(this);
        final WeakReference<RootWidget> page = new WeakReference<>(root);
        try {
            final CompletableFuture<T> work = Objects.requireNonNull(this.action.apply(values))
                .toCompletableFuture();
            if (this.result.isDone()) {
                work.cancel(true);
                bridge.cancel(false);
                return bridge;
            }
            this.pending = work;
            work.whenComplete((value, failure) -> {
                final Dialog<T> dialog = owner.get();
                final RootWidget context = page.get();
                if (dialog != null && context != null) {
                    context.execute(() -> dialog.finish(context, id, value, failure, bridge));
                }
            });
        } catch (final RuntimeException failure) {
            this.finish(root, id, null, failure, bridge);
        }
        return bridge;
    }

    /**
     * Applies one attempt on the original page; stale and retired completions have no UI effect.
     * @param root original page
     * @param id attempt generation
     * @param value non-null successful value
     * @param failure application failure, never exposed to the browser
     * @param bridge completion for Form
     */
    private void finish(final RootWidget root, final long id, final T value,
            final Throwable failure, final CompletableFuture<Form.Result> bridge) {
        if (this.isRetired() || this.result.isDone() || this.attempt != id
                || this.getRootWidget().orElse(null) != root) {
            bridge.cancel(false);
            return;
        }
        this.pending = null;
        this.busy = false;
        if (failure != null || value == null) {
            this.setCloseOnEscape(this.previousEscape);
            this.setCloseOnOutsideClick(this.previousOutside);
            this.cancelButton.enable();
            this.closeButton.enable();
            ((Busy) this.feedback.getChild(0)).finish(this.messages.error(),
                    Feedback.Severity.ERROR);
            bridge.completeExceptionally(new IllegalStateException("Dialog action failed"));
        } else {
            bridge.complete(Form.Result.success());
            this.resolve(Reason.CONFIRMED, value);
            this.dispose();
        }
    }

    /**
     * Releases action references and completes observers exactly once, also during page teardown.
     * @param reason terminal decision
     * @param value successful value, if any
     */
    private void resolve(final Reason reason, final T value) {
        if (this.result.isDone()) {
            return;
        }
        ++this.attempt;
        this.busy = false;
        this.action = null;
        final CompletableFuture<T> work = this.pending;
        this.pending = null;
        this.result.complete(new Result<>(reason, value));
        if (work != null) {
            work.cancel(true);
        }
    }
}
