/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.HorizontalAlignment;
import java.util.Objects;

/**
 * A ready-to-use modal message containing text and one or two action buttons.
 */
public final class MessagePopup extends ModalPopup {
    /**
     * Text widget that displays the message.
     */
    private final TextWidget textWidget;

    /**
     * Creates a message with one action button.
     *
     * @param message message text
     * @param button action button
     */
    public MessagePopup(final String message, final Button button) {
        this(new TextWidget(Objects.requireNonNull(message, "message")), button);
    }

    /**
     * Creates a message with two action buttons.
     *
     * @param message message text
     * @param firstButton first action button
     * @param secondButton second action button
     */
    public MessagePopup(final String message, final Button firstButton,
            final Button secondButton) {
        this(
            new TextWidget(Objects.requireNonNull(message, "message")),
            firstButton,
            secondButton
        );
    }

    /**
     * Creates a message from a text widget and one action button.
     * The same text widget remains available through {@link #getTextWidget()}.
     *
     * @param textWidget message text widget
     * @param button action button
     */
    public MessagePopup(final TextWidget textWidget, final Button button) {
        this(textWidget, new Button[] {Objects.requireNonNull(button, "button")});
    }

    /**
     * Creates a message from a text widget and two action buttons.
     * The same text widget remains available through {@link #getTextWidget()}.
     *
     * @param textWidget message text widget
     * @param firstButton first action button
     * @param secondButton second action button
     */
    public MessagePopup(final TextWidget textWidget, final Button firstButton,
            final Button secondButton) {
        this(textWidget, new Button[] {
            Objects.requireNonNull(firstButton, "firstButton"),
            Objects.requireNonNull(secondButton, "secondButton")
        });
    }

    /**
     * Creates a message from a validated button set.
     *
     * @param textWidget message text widget
     * @param buttons one or two action buttons
     */
    private MessagePopup(final TextWidget textWidget, final Button[] buttons) {
        super(messageStyle());
        this.textWidget = Objects.requireNonNull(textWidget, "textWidget");
        final Section messageSection = new Section(this.textWidget);
        messageSection.setMargin(0, 0, 0, 16);
        this.add(messageSection);

        final Section buttonSection = new Section(buttons[0]);
        buttonSection.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        if (buttons.length == 2) {
            buttonSection.add(buttons[1]);
        }
        this.add(buttonSection);
    }

    /**
     * Returns the text widget displayed by this message popup.
     * Its style or text model may be changed after construction.
     *
     * @return message text widget
     */
    public TextWidget getTextWidget() {
        return this.textWidget;
    }

    /**
     * Creates the default message-window style.
     *
     * @return message style
     */
    private static ModalPopupStyle messageStyle() {
        final ModalPopupStyle style = ModalPopup.getDefaultStyle().derive();
        style.setWidth(400);
        return style;
    }
}
