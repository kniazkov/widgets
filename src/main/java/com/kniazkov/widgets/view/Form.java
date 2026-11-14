/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Accessible page-owned form with one validation and submit path for R44 Enter and its button.
 */
public final class Form extends Panel {
    /**
     * Result of application validation or submission.
     * @param fieldErrors errors by stable field name
     * @param formError form-level error, or an empty string
     */
    public record Result(Map<String, String> fieldErrors, String formError) {
        /**
         * Stores a copy without retaining application-mutable state.
         */
        public Result {
            fieldErrors = Map.copyOf(fieldErrors);
            Objects.requireNonNull(formError);
            if (fieldErrors.values().stream().anyMatch(String::isBlank)) {
                throw new IllegalArgumentException("Empty field error");
            }
        }

        /**
         * @return successful validation or submission
         */
        public static Result success() {
            return new Result(Map.of(), "");
        }

        /**
         * @return whether no errors are present
         */
        public boolean isSuccess() {
            return this.fieldErrors.isEmpty() && this.formError.isEmpty();
        }
    }

    /**
     * Text announced by the status region.
     * @param busy progress label
     * @param success success label
     * @param invalid field error summary
     * @param unavailable generic failure without internal error details
     */
    public record Messages(String busy, String success, String invalid, String unavailable) {
        /**
         * Checks all visible and announced labels are present.
         */
        public Messages {
            Objects.requireNonNull(busy);
            Objects.requireNonNull(success);
            Objects.requireNonNull(invalid);
            Objects.requireNonNull(unavailable);
        }
    }

    /**
     * Default status text.
     */
    private static final Messages DEFAULT = new Messages(
        "Submitting…", "Submitted.", "Check the highlighted fields.",
        "Submission unavailable. Please try again."
    );

    /**
     * Current registered fields in visual order.
     */
    private final Panel fields = new Panel();
    /**
     * Button route is identical to the R44 Enter route.
     */
    private final Button submitButton;
    /**
     * Help and summary text always remain separate from input values.
     */
    private final TextWidget status = new TextWidget();
    /**
     * User-facing status labels.
     */
    private Messages messages = DEFAULT;
    /**
     * Application-wide constraints across an immutable snapshot.
     */
    private Function<Map<String, String>, Result> validator = values -> Result.success();
    /**
     * Application submission, expected to return promptly.
     */
    private Function<Map<String, String>, CompletionStage<Result>> submission =
        values -> CompletableFuture.completedFuture(Result.success());
    /**
     * In-flight snapshot; null means idle.
     */
    private Map<String, String> pending;
    /**
     * Protects even a synchronous validator or handler against recursive submission.
     */
    private boolean submitting;
    /**
     * Identifies the active asynchronous attempt.
     */
    private long attempt;

    /**
     * Creates a named form with a single native activation button.
     * @param name accessible form name, shown by the application separately as needed
     * @param submitCaption button text
     */
    @SuppressWarnings("this-escape")
    public Form(final String name, final String submitCaption) {
        this.setAccessibleName(Objects.requireNonNull(name));
        this.submitButton = new Button(Objects.requireNonNull(submitCaption));
        this.submitButton.onClick(event -> this.submit());
        this.status.getModel(State.ANY, Property.FORM_STATUS).setData(true);
        final TextFlow actions = new TextFlow();
        actions.add(this.submitButton);
        final TextFlow feedback = new TextFlow();
        feedback.add(this.status);
        this.add(this.fields);
        this.add(actions);
        this.add(feedback);
        this.onRetire(() -> {
            this.pending = null;
            this.submitting = false;
        });
    }

    /**
     * Registers the field and appends it in validation/focus order.
     * @param field field to register
     */
    public void addField(final FormField field) {
        Objects.requireNonNull(field);
        if (!field.hasEditor() || this.fieldList().stream().anyMatch(
                candidate -> candidate.getName().equals(field.getName()))) {
            throw new IllegalArgumentException("Invalid editor or duplicate form field name");
        }
        this.fields.add(field);
    }

    /**
     * Removes the field from this form without disposing it.
     * @param field field to remove
     */
    public void removeField(final FormField field) {
        this.fields.remove(field);
    }

    /**
     * Replaces the synchronous validation procedure. It must not wait for background work.
     * @param handler validation of immutable values keyed by field name
     */
    public void setValidator(final Function<Map<String, String>, Result> handler) {
        this.validator = Objects.requireNonNull(handler);
    }

    /**
     * Sets the asynchronous application action. Callbacks run in the page context;
     * return a completion stage promptly rather than blocking the page thread.
     * @param handler dispatcher for the immutable validated values
     */
    public void onSubmit(final Function<Map<String, String>, CompletionStage<Result>> handler) {
        this.submission = Objects.requireNonNull(handler);
    }

    /**
     * Sets application-language progress and result labels.
     * @param labels visible messages
     */
    public void setMessages(final Messages labels) {
        this.messages = Objects.requireNonNull(labels);
    }

    /**
     * @return whether the form is validating or awaiting an application result
     */
    public boolean isSubmitting() {
        return this.submitting;
    }

    /**
     * @return current general feedback, not the values sent for validation
     */
    public String getStatus() {
        return this.status.getText();
    }

    /**
     * Attempts validation and submission once; detached and retired forms cannot submit.
     */
    public void submit() {
        this.getRootWidget().ifPresent(root -> root.execute(() -> this.start(root)));
    }

    /**
     * Verifies the field is in this form's private field container.
     * @param field candidate field
     * @return current ownership
     */
    boolean owns(final FormField field) {
        return this.fields.getParent().orElse(null) == this
            && field.getParent().orElse(null) == this.fields;
    }

    /**
     * @return form fields in display order
     */
    private List<FormField> fieldList() {
        return IntStream.range(0, this.fields.getChildCount())
            .mapToObj(this.fields::getChild).filter(FormField.class::isInstance)
            .map(FormField.class::cast).filter(this::owns).toList();
    }

    /**
     * Captures current participating models once for validation and submission.
     * @return source-independent, immutable snapshot
     */
    private Map<String, String> snapshot() {
        final Map<String, String> values = new LinkedHashMap<>();
        this.fieldList().stream().filter(FormField::participates).forEach(
            field -> values.put(field.getName(), field.getEditor().getText())
        );
        return Collections.unmodifiableMap(values);
    }

    /**
     * Runs field and application constraints before invoking asynchronous submission.
     * @param values snapshot to check
     * @return validation result
     */
    private Result validate(final Map<String, String> values) {
        final Map<String, String> errors = new LinkedHashMap<>();
        for (final FormField field : this.fieldList()) {
            if (values.containsKey(field.getName())) {
                field.markTouched();
                final String error = field.constraintError(values.get(field.getName()));
                if (!error.isEmpty()) {
                    errors.put(field.getName(), error);
                }
            }
        }
        return errors.isEmpty() ? this.validator.apply(values) : new Result(errors, "");
    }

    /**
     * Serializes an attempt in the owning page.
     * @param root live page
     */
    private void start(final RootWidget root) {
        if (this.submitting || this.isRetired() || this.getRootWidget().orElse(null) != root
                || !this.isInteractionEnabled()) {
            return;
        }
        this.submitting = true;
        try {
            final Map<String, String> values = this.snapshot();
            final Result validation = Objects.requireNonNull(this.validate(values));
            if (!validation.isSuccess()) {
                this.submitting = false;
                this.showResult(validation, this.messages.invalid(), true);
                return;
            }
            this.pending = values;
            final long id = ++this.attempt;
            this.showResult(Result.success(), this.messages.busy(), false);
            this.setBusy(true);
            final WeakReference<Form> owner = new WeakReference<>(this);
            final WeakReference<RootWidget> page = new WeakReference<>(root);
            this.submission.apply(values).whenComplete((result, error) -> {
                final RootWidget context = page.get();
                final Form form = owner.get();
                if (context != null && form != null) {
                    context.execute(() -> form.finish(context, id, result, error != null));
                }
            });
        } catch (final RuntimeException failure) {
            if (!this.isRetired()) {
                this.pending = null;
                this.setBusy(false);
                this.showResult(Result.success(), this.messages.unavailable(), false);
            }
        }
    }

    /**
     * Completes an attempt only on its original page, without replacing the current values.
     * @param root live page
     * @param id attempt identity
     * @param result application result
     * @param failed exceptional completion
     */
    private void finish(final RootWidget root, final long id, final Result result,
            final boolean failed) {
        if (this.isRetired() || !this.belongsTo(root) || this.attempt != id
                || this.pending == null) {
            return;
        }
        final Map<String, String> submitted = this.pending;
        this.pending = null;
        this.setBusy(false);
        if (failed || result == null) {
            this.showResult(Result.success(), this.messages.unavailable(), false);
        } else if (result.isSuccess()) {
            if (submitted.equals(this.snapshot())) {
                this.fieldList().stream().filter(FormField::participates)
                    .forEach(FormField::markPristine);
            }
            this.showResult(result, this.messages.success(), false);
        } else if (submitted.equals(this.snapshot())) {
            this.showResult(result, this.messages.invalid(), this.getRootWidget().isPresent());
        } else {
            final String summary = result.formError().isEmpty()
                ? this.messages.invalid() : result.formError();
            this.showResult(Result.success(), summary, false);
        }
    }

    /**
     * Mirrors the server guard to the browser button and accessibility state.
     * @param busy whether a valid request is pending
     */
    private void setBusy(final boolean busy) {
        this.submitting = busy;
        this.getModel(State.ANY, Property.FORM_BUSY).setData(busy);
        if (!this.submitButton.isRetired()) {
            this.submitButton.setDisabledFlag(busy);
        }
    }

    /**
     * Routes messages to their fields and optionally focuses the first one in display order.
     * @param result validation or submission result
     * @param fallback status when the application supplies no general error
     * @param focus whether to request focus on the first live invalid editor
     */
    private void showResult(final Result result, final String fallback, final boolean focus) {
        this.status.setText(result.formError().isEmpty() ? fallback : result.formError());
        InputField first = null;
        for (final FormField field : this.fieldList()) {
            if (!field.hasEditor() || field.getEditor().isRetired()) {
                continue;
            }
            final String message = field.participates()
                ? result.fieldErrors().getOrDefault(field.getName(), "") : "";
            field.showError(message);
            if (first == null && !message.isEmpty()) {
                first = field.getEditor();
            }
        }
        if (focus && first != null) {
            first.focusFormEditor();
        }
    }

    @Override
    public String getType() {
        return "form";
    }
}
