/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.Controller;
import com.kniazkov.widgets.controller.HandlesFocusEvents;
import com.kniazkov.widgets.controller.HandlesPointerEvents;
import com.kniazkov.widgets.controller.KeyboardEvent;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.protocol.SetKeyboardShortcuts;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An editable text input field widget.
 */
public class InputField extends InlineWidget<InputFieldStyle> implements HasTextInput,
        HasPlaceholder,
        HasInputMode,
        HasStyledText, HasColor, HasBgColor, HasBorder, HasAbsoluteWidth, HasAbsoluteHeight,
        HasMargin, HasPadding, HasInvalidState, HasDisabledState, HasHorizontalAlignment,
        HandlesFocusEvents, HandlesPointerEvents, HasBoxShadow, HasOutline, HasCursor,
        HasTransition, HasBoxSizing {
    /**
     * Active, explicitly registered key handlers.
     */
    private final Map<KeyboardEvent, KeyBinding> keyBindings = new LinkedHashMap<>();

    /**

     * Whether local editing and keyboard actions are currently read-only.

     */
    private boolean readOnly;

    /**
     * A server controller and its synchronous browser default policy.
     * @param controller callback
     * @param preventDefault whether to prevent the native action and stop propagation
     */
    private record KeyBinding(Controller<KeyboardEvent> controller, boolean preventDefault) {
    }

    /**
     * Returns the default style instance used by input fields.
     *
     * @return the singleton default {@link TextWidgetStyle} instance
     */
    public static InputFieldStyle getDefaultStyle() {
        return InputFieldStyle.DEFAULT;
    }

    /**
     * Creates a new input field with empty text.
     */
    public InputField() {
        this("");
    }

    /**
     * Creates a new input field with the given initial text.
     *
     * @param text the initial text to display in the input field
     */
    public InputField(final String text) {
        this(getDefaultStyle(), text);
    }

    /**
     * Creates a new input field with the specified style and initial text.
     *
     * @param style the style to apply to this widget
     * @param text the initial text to display
     */
    @SuppressWarnings("this-escape")
    public InputField(final InputFieldStyle style, final String text) {
        /*
         * Construction initializes the inherited text binding before publication.
         */
        super(style);
        this.setText(text);
        this.onRetire(this.keyBindings::clear);
    }

    @Override
    public String getType() {
        return "input field";
    }

    /**
     * Sets a new text model for this input field.
     * <p>
     * In addition to assigning the provided {@link Model} to {@link Property#TEXT},
     * this method also synchronizes the input field's validity state with the
     * validity of the underlying text data.
     * <p>
     * The supplied text model exposes its own validity flag via {@link Model#getValidFlagModel()}.
     * That flag is used to configure the field's {@code valid-state} model, ensuring that:
     * <ul>
     *   <li>If the text model contains invalid data, the widget immediately enters
     *       the invalid visual state.</li>
     *   <li>Subsequent validity changes in the text model automatically propagate
     *       to the widget's invalid-state flag.</li>
     * </ul>
     *
     * @param model the text model to assign
     */
    @Override
    public void setTextModel(Model<String> model) {
        this.setModel(State.ANY, Property.TEXT, model);
        this.setModel(State.ANY, Property.VALID, model.getValidFlagModel());
    }

    /**
     * Registers a logical Enter action on a single-line editor. Replaces an existing callback.
     * @param controller handler receiving the normalized press, or null to unsubscribe
     */
    public final void onEnter(final Controller<KeyboardEvent> controller) {
        this.onKeyDown(new KeyboardEvent.Shortcut(KeyboardEvent.Key.ENTER), true, controller);
    }

    /**
     * Registers an exact key press subscription.
     * @param shortcut named key and exact modifiers
     * @param preventDefault cancel native behavior and propagation on the client
     * @param controller handler, or null to unsubscribe
     */
    public final void onKeyDown(final KeyboardEvent.Shortcut shortcut, final boolean preventDefault,
            final Controller<KeyboardEvent> controller) {
        this.setKeyBinding(shortcut, KeyboardEvent.Phase.DOWN, preventDefault, controller);
    }

    /**
     * Registers an exact key release notification without affecting the native key press.
     * @param shortcut named key and exact modifiers
     * @param controller handler, or null to unsubscribe
     */
    public final void onKeyUp(final KeyboardEvent.Shortcut shortcut,
            final Controller<KeyboardEvent> controller) {
        this.setKeyBinding(shortcut, KeyboardEvent.Phase.UP, false, controller);
    }

    /**
     * Applies one binding and publishes the small complete table, without field data.
     * @param shortcut normalized shortcut
     * @param phase normalized event phase
     * @param preventDefault local cancellation policy
     * @param controller handler, or null to remove
     */
    private synchronized void setKeyBinding(final KeyboardEvent.Shortcut shortcut,
            final KeyboardEvent.Phase phase, final boolean preventDefault,
            final Controller<KeyboardEvent> controller) {
        this.requireUsable();
        Objects.requireNonNull(shortcut);
        final KeyboardEvent key = new KeyboardEvent(shortcut, phase);
        if (controller == null) {
            if (this.keyBindings.remove(key) == null) {
                return;
            }
        } else {
            if (this instanceof TextArea && phase == KeyboardEvent.Phase.DOWN
                    && shortcut.equals(new KeyboardEvent.Shortcut(KeyboardEvent.Key.ENTER))) {
                throw new IllegalArgumentException("Use a modified Enter in a multiline editor");
            }
            if (this.keyBindings.size() >= 32 && !this.keyBindings.containsKey(key)) {
                throw new IllegalArgumentException("Too many keyboard subscriptions");
            }
            this.keyBindings.put(key, new KeyBinding(controller, preventDefault));
        }
        this.pushUpdate(new SetKeyboardShortcuts(this.getId(), this.keyBindings.entrySet()
            .stream().map(entry -> entry.getKey().signature() + ":"
                + (entry.getValue().preventDefault() ? "1" : "0")).toList()));
    }

    /**
     * Checks the live server subscription; called by browser event validation.
     * @param event normalized press or release
     * @return whether a callback is registered and active
     */
    public final synchronized boolean acceptsKeyboardEvent(final KeyboardEvent event) {
        return !this.isRetired() && !this.readOnly && this.getRootWidget().isPresent()
            && this.isInteractionEnabled() && this.keyBindings.containsKey(event);
    }

    /**
     * Invokes a currently subscribed controller after protocol validation.
     * @param event normalized press or release
     */
    public final void dispatchKeyboardEvent(final KeyboardEvent event) {
        final KeyBinding binding;
        synchronized (this) {
            if (!this.acceptsKeyboardEvent(event)) {
                return;
            }
            binding = this.keyBindings.get(event);
        }
        binding.controller().handleEvent(event);
    }

    /**
     * Sets native read-only editing; also disables server keyboard actions for the editor.
     * @param value whether the field is read-only
     */
    public final synchronized void setReadOnly(final boolean value) {
        this.requireUsable();
        if (this.readOnly != value) {
            this.readOnly = value;
            this.getModel(State.ANY, Property.READ_ONLY).setData(value);
        }
    }

    /**
     * @return whether the field is read-only
     */
    public final synchronized boolean isReadOnly() {
        return this.readOnly;
    }
}
