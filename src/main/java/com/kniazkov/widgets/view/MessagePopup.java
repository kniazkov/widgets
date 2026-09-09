/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.HorizontalAlignment;
import java.util.Objects;

/**
 * A ready-to-use modal message containing text and one or two action buttons.
 */
public class MessagePopup extends ModalPopup {
    /**
     * Creates a message with one action button.
     *
     * @param message message text
     * @param button action button
     */
    public MessagePopup(final String message, final Button button) {
        this(message, new Button[] {Objects.requireNonNull(button, "button")});
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
        this(message, new Button[] {
            Objects.requireNonNull(firstButton, "firstButton"),
            Objects.requireNonNull(secondButton, "secondButton")
        });
    }

    /**
     * Creates a message from a validated button set.
     *
     * @param message message text
     * @param buttons one or two action buttons
     */
    private MessagePopup(final String message, final Button[] buttons) {
        super(messageStyle());
        final Section messageSection = new Section(new TextWidget(
            Objects.requireNonNull(message, "message")
        ));
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
