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
public final class InputField extends InlineWidget implements HasTextInput, HasStyledText,
        HasWidth<InlineWidgetSize>, Clickable {
    /**
     * Text model binding — handles model and listener for text changes.
     */
    private final ModelBinding<String> text;

    /**
     * Font face model binding — handles model and listener for font face changes.
     */
    private final ModelBinding<FontFace> fontFace;

    /**
     * Font size model binding — handles model and listener for font size changes.
     */
    private final ModelBinding<FontSize> fontSize;

    /**
     * Font weight model binding — handles model and listener for font weight changes.
     */
    private final ModelBinding<FontWeight> fontWeight;

    /**
     * Italic model binding — handles model and listener for italic changes.
     */
    private final ModelBinding<Boolean> italic;

    /**
     * Width model binding — handles model and listener for width changes.
     */
    private final ModelBinding<InlineWidgetSize> width;

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
        final StyleSet styles = client.getRootWidget().getDefaultStyles();
        this.fontFace = new ModelBinding<>(
            styles.getDefaultInputFieldStyle().getFontFaceModel().fork(),
            new FontFaceModelListener(this)
        );
        this.fontSize = new ModelBinding<>(
            styles.getDefaultInputFieldStyle().getFontSizeModel().fork(),
            new FontSizeModelListener(this)
        );
        this.fontWeight = new ModelBinding<>(
            styles.getDefaultInputFieldStyle().getFontWeightModel().fork(),
            new FontWeightModelListener(this)
        );
        this.italic = new ModelBinding<>(
            styles.getDefaultInputFieldStyle().getItalicModel().fork(),
            new ItalicModelListener(this)
        );
        this.width = new ModelBinding<>(
            new DefaultInlineWidgetSizeModel(),
            new InlineWidgetSizeListener(this, "width")
        );
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
    public Model<FontFace> getFontFaceModel() {
        return this.fontFace.getModel();
    }

    @Override
    public void setFontFaceModel(final Model<FontFace> model) {
        this.fontFace.setModel(model);
    }

    @Override
    public Model<FontSize> getFontSizeModel() {
        return this.fontSize.getModel();
    }

    @Override
    public void setFontSizeModel(final Model<FontSize> model) {
        this.fontSize.setModel(model);
    }

    @Override
    public Model<FontWeight> getFontWeightModel() {
        return this.fontWeight.getModel();
    }

    @Override
    public void setFontWeightModel(final Model<FontWeight> model) {
        this.fontWeight.setModel(model);
    }

    @Override
    public Model<Boolean> getItalicModel() {
        return this.italic.getModel();
    }

    @Override
    public void setItalicModel(final Model<Boolean> model) {
        this.italic.setModel(model);
    }

    @Override
    public Model<InlineWidgetSize> getWidthModel() {
        return this.width.getModel();
    }

    @Override
    public void setWidthModel(final Model<InlineWidgetSize> model) {
        this.width.setModel(model);
    }

    /**
     * Sets the width of the widget in pixels.
     * <p>
     *     This is a convenience method that wraps the given pixel value in an
     *     {@link InlineWidgetSize} and updates the associated width model.
     * </p>
     *
     * @param pixels The width in pixels (must be ≥ 0)
     */
    public void setWidth(final int pixels) {
        this.width.getModel().setData(new InlineWidgetSize(pixels));
    }

    /**
     * Sets the width of the widget using a CSS-style string.
     * <p>
     *     Accepts absolute units like {@code px}, {@code pt}, {@code cm}, etc.
     *     If no unit is provided, {@code px} is assumed. The string is parsed and
     *     converted into an {@link InlineWidgetSize} instance.
     * </p>
     *
     * @param width CSS-style width string (e.g., "12pt", "100px", "2cm")
     * @throws IllegalArgumentException If the string is invalid or contains unsupported units
     */
    public void setWidth(final String width) {
        this.width.getModel().setData(new InlineWidgetSize(width));
    }

    @Override
    public void onClick(final Controller ctrl) {
        this.clickCtrl = ctrl;
    }
}
