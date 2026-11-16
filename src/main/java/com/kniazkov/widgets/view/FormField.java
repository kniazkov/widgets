/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.controller.Event;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Text editor with a native label and optional help, error, and form constraints.
 */
public final class FormField extends Panel {
    /**
     * Bounded stable names, used in submission snapshots and error responses.
     */
    private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_.-]{0,63}");

    /**
     * Immutable field name.
     */
    private final String name;
    /**
     * Owned text editor.
     */
    private final InputField editor;
    /**
     * Container checking that the editor has not been moved.
     */
    private final TextFlow editorRow = new TextFlow();
    /**
     * Label visible next to the editor.
     */
    private final Label label;
    /**
     * Optional help text and row.
     */
    private final TextWidget help = new TextWidget();
    /**
     * Allows the empty help row to be hidden.
     */
    private final TextFlow helpRow = new TextFlow();
    /**
     * Current form-owned validation message, distinct from the editor model.
     */
    private final TextWidget error = new TextWidget();
    /**
     * Only shows nonempty messages.
     */
    private final TextFlow errorRow = new TextFlow();
    /**
     * Dirty baseline.
     */
    private String initial;
    /**
     * Whether a valid page blur or submit has marked the field touched.
     */
    private boolean touched;
    /**
     * Required constraint.
     */
    private boolean required;
    /**
     * Localizable default field-level messages.
     */
    private String requiredMessage = "This field is required.";
    /**
     * Localizable default constraint failure.
     */
    private String invalidMessage = "Check the value of this field.";

    /**
     * Creates a new single-line text field.
     * @param name stable name
     * @param caption visible label
     */
    public FormField(final String name, final String caption) {
        this(name, caption, new InputField());
    }

    /**
     * Wraps a new editor. Programmatic users configure models on {@link #getEditor()}.
     * @param name stable name
     * @param caption visible label
     * @param editor an unattached supported editor
     */
    @SuppressWarnings("this-escape")
    public FormField(final String name, final String caption, final InputField editor) {
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid form field name");
        }
        this.name = name;
        this.editor = Objects.requireNonNull(editor);
        if (editor.getParent().isPresent() || editor.isRetired()) {
            throw new IllegalArgumentException("FormField requires an available editor");
        }
        this.initial = editor.getText();
        this.label = new Label(Objects.requireNonNull(caption));
        this.setVerticalMargin(8);
        this.editor.setHorizontalMargin(0);
        this.label.setTarget(editor);
        this.editor.getModel(State.ANY, Property.FORM_NAME).setData(name);
        this.editor.setAccessibleLabel(this.label);
        this.editor.setAccessibleError(this.error);
        this.editor.subscribeToEvent(Event.BLUR);
        this.editorRow.add(editor);
        this.help.setColor(Color.fromString("#475569"));
        this.error.setColor(Color.fromString("#991b1b"));
        this.helpRow.add(this.help);
        this.errorRow.add(this.error);
        this.helpRow.hide();
        this.errorRow.hide();
        final TextFlow labelRow = new TextFlow();
        labelRow.add(this.label);
        this.add(labelRow);
        this.add(this.editorRow);
        this.add(this.helpRow);
        this.add(this.errorRow);
        if (!(editor instanceof TextArea)) {
            editor.onEnter(event -> this.submitFromEditor());
        }
    }

    /**
     * Resolves current ownership instead of retaining a previously owning form.
     */
    private void submitFromEditor() {
        if (!this.hasEditor()) {
            return;
        }
        Container parent = this.getParent().orElse(null);
        while (parent instanceof Widget<?> owner) {
            if (owner instanceof Form form) {
                if (form.owns(this)) {
                    form.submit();
                }
                return;
            }
            parent = owner.getParent().orElse(null);
        }
    }

    /**
     * @return stable name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return editor and its current reactive text model
     */
    public InputField getEditor() {
        return this.editor;
    }

    /**
     * @return visible reactive label
     */
    public Label getLabel() {
        return this.label;
    }

    /**
     * Changes the optional persistent description.
     * @param text help text, or empty to hide it
     */
    public void setHelp(final String text) {
        this.help.setText(Objects.requireNonNull(text));
        this.helpRow.setHiddenFlag(text.isEmpty());
        this.editor.setAccessibleDescriptionWidget(text.isEmpty() ? null : this.help);
    }

    /**
     * Sets labels for built-in required and editor constraints.
     * @param requiredText message for a blank required field
     * @param invalidText message for another invalid value
     */
    public void setConstraintMessages(final String requiredText, final String invalidText) {
        if (requiredText.isBlank() || invalidText.isBlank()) {
            throw new IllegalArgumentException("Empty constraint message");
        }
        this.requiredMessage = requiredText;
        this.invalidMessage = invalidText;
    }

    /**
     * Changes the browser autofill hint; this is advisory, not validation.
     * @param hint a standard HTML autocomplete token string, or empty to remove it
     */
    public void setAutocomplete(final String hint) {
        if (Objects.requireNonNull(hint).length() > 128 || !hint.matches("[a-zA-Z0-9_ -]*")) {
            throw new IllegalArgumentException("Invalid autocomplete hint");
        }
        this.editor.getModel(State.ANY, Property.AUTOCOMPLETE).setData(hint);
    }

    /**
     * Sets the native and server limit for browser edits and validation on submit.
     * @param length maximum UTF-16 code units, from 1 to the standard editor limit
     */
    public void setMaxLength(final int length) {
        if (length < 1 || length > 65536) {
            throw new IllegalArgumentException("Incorrect maximum editor length");
        }
        this.editor.setFormMaxLength(length);
    }

    /**
     * Sets the required constraint for native semantics and server form validation.
     * @param value whether blank text is disallowed
     */
    public void setRequired(final boolean value) {
        this.required = value;
        this.editor.getModel(State.ANY, Property.REQUIRED).setData(value);
    }

    /**
     * Keeps the editor in the snapshot but prevents browser editing.
     * @param value whether the editor is read-only
     */
    public void setReadOnly(final boolean value) {
        this.editor.setReadOnly(value);
    }

    /**
     * @return whether the current model differs from its baseline
     */
    public boolean isDirty() {
        return !this.editor.getText().equals(this.initial);
    }

    /**
     * @return whether the user has blurred the field or the form has validated it
     */
    public boolean isTouched() {
        return this.touched;
    }

    /**
     * @return currently displayed server form error, or an empty string
     */
    public String getError() {
        return this.error.getText();
    }

    /**
     * Establishes a new dirty/touched baseline without resetting the model value.
     */
    public void markPristine() {
        this.initial = this.editor.getText();
        this.touched = false;
    }

    /**
     * Marks touch without replacing an application's editor controller.
     */
    void markTouched() {
        this.touched = true;
    }

    /**
     * @return whether the original editor is still owned by this field
     */
    boolean hasEditor() {
        return this.editorRow.getParent().orElse(null) == this
            && this.editor.getParent().orElse(null) == this.editorRow;
    }

    /**
     * @return whether the original editor currently participates in form submission
     */
    boolean participates() {
        return this.hasEditor() && !this.editor.isRetired() && this.editor.isInteractionEnabled();
    }

    /**
     * Computes constraints before cross-field validation.
     * @param value an immutable-attempt snapshot entry
     * @return a localized constraint error or empty string
     */
    String constraintError(final String value) {
        if (this.required && value.isBlank()) {
            return this.requiredMessage;
        }
        if (value.length() > this.editor.getMaxInputLength()
                || !this.editor.getTextModel().isValid()) {
            return this.invalidMessage;
        }
        return "";
    }

    /**
     * Updates display state only, without changing input or model validity.
     * @param message error string or empty
     */
    void showError(final String message) {
        this.error.setText(message);
        this.errorRow.setHiddenFlag(message.isEmpty());
        this.editor.getModel(State.ANY, Property.FORM_ERROR).setData(!message.isEmpty());
    }

    @Override
    public String getType() {
        return "form field";
    }
}
