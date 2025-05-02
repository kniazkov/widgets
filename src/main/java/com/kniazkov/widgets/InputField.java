/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonObject;
import java.util.Optional;

/**
 * A widget representing a single-line text input field.
 * <p>
 *     Internally, the text content is managed using a {@link Model}, allowing for reactive
 *     behavior, validation, and data binding. Controllers can be assigned to handle input
 *     and click events.
 * </p>
 */
public final class InputField extends InlineWidget implements HasTextInput, Clickable {
    /**
     * Text model binding — handles model and listener for text changes.
     */
    private final ModelBinding<String> text;

    /**
     * Controller invoked when the user modifies the text.
     */
    private TypedController<String> textInputCtrl;

    /**
     * Controller invoked when the field is clicked.
     */
    private Controller clickCtrl;

    /**
     * Constructs a new input field with default model and no-op controllers.
     *
     * @param client the owning client
     * @param parent the container this widget belongs to
     */
    InputField(final Client client, final Container parent) {
        super(client, parent);
        this.text = new ModelBinding<>(
            new DefaultStringModel(),
            new TextModelListener(this)
        );
        this.textInputCtrl = data -> { }; // no-op
        this.clickCtrl = StubController.INSTANCE;
    }

    @Override
    public boolean accept(final WidgetVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    String getType() {
        return "input field";
    }

    /**
     * Handles input and click events from the client.
     * <p>
     * Recognized event types:
     * <ul>
     *     <li>{@code "text input"} — updates the model and invokes the input controller</li>
     *     <li>{@code "click"} — invokes the click controller</li>
     * </ul>
     *
     * @param type event type
     * @param data optional event data (e.g., new text)
     */
    @Override
    void handleEvent(final String type, final Optional<JsonObject> data) {
        if (type.equals("text input") && data.isPresent()) {
            final JsonElement element = data.get().getElement("text");
            if (element.isString()) {
                final String text = element.getStringValue();
                this.text.getModel().setData(text);
                this.textInputCtrl.handleEvent(text);
            }
        } else if (type.equals("click")) {
            this.clickCtrl.handleEvent();
        }
    }

    @Override
    public Model<String> getTextModel() {
        return this.text.getModel();
    }

    @Override
    public void setTextModel(final Model<String> model) {
        this.text.setModel(model);
    }

    @Override
    public void onTextInput(TypedController<String> ctrl) {
        this.textInputCtrl = ctrl;
    }

    @Override
    public void onClick(final Controller ctrl) {
        this.clickCtrl = ctrl;
    }
}
