package com.kniazkov.widgets;

/**
 * A container that holds only {@link InlineWidget} instances.
 * <p>
 *     Intended for inline layout, where widgets are placed next to each other,
 *     typically in a horizontal flow (e.g., left to right). The exact position of each widget
 *     may depend on the width or content of the previous one — for example,
 *     in flowing or wrapping layouts.
 * </p>
 *
 * <p>
 *     This container exposes factory methods for inline widgets, such as {@link TextWidget},
 *     {@link InputField}, and {@link Button}.
 * </p>
 */
public interface InlineContainer extends TypedContainer<InlineWidget> {
    /**
     * Creates and adds a new {@link TextWidget} with the given text.
     *
     * @param text Initial text
     * @return The created {@link TextWidget}
     */
    TextWidget createTextWidget(String text);

    /**
     * Creates and adds a new {@link TextWidget} with empty text.
     *
     * @return The created {@link TextWidget}
     */
    default TextWidget createTextWidget() {
        return this.createTextWidget("");
    }

    /**
     * Creates and adds a new {@link InputField} widget.
     *
     * @return The created {@link InputField}
     */
    InputField createInputField();

    /**
     * Creates and adds a new {@link Button} widget.
     *
     * @return The created {@link Button}
     */
    Button createButton();

    /**
     * Creates and adds a new {@link Button} widget with the given label text.
     *
     * @param text The text to display inside the button
     * @return The created {@link Button}
     */
    default Button createButton(final String text) {
        final Button button = this.createButton();
        button.setText(text);
        return button;
    }
}
