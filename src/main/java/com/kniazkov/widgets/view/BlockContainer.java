/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

/**
 * An extension of the {@link BlockContainer} interface that provides convenience methods
 * for creating and managing inline widgets.
 * <p>
 * This interface simplifies the common pattern where a block container holds only a single
 * inline widget (e.g., {@code <td><p><input.../></p></td>}) by providing helper methods
 * to create the most frequently used inline widgets without manual widget hierarchy setup.
 */
public interface BlockContainer extends TypedContainer<BlockWidget> {

    /**
     * Creates a new section widget, removes all existing children from this container,
     * and adds the newly created section as the sole child.
     *
     * @return the newly created and added {@link Section} widget
     */
    default Section createSection() {
        this.removeAll();
        final Section section = new Section();
        this.add(section);
        return section;
    }

    /**
     * Creates a text widget with the specified text content within a new section.
     * Removes all existing children and sets up the widget hierarchy automatically.
     *
     * @param text the text content for the widget
     * @return the newly created {@link TextWidget}
     */
    default TextWidget createText(final String text) {
        final Section section = this.createSection();
        final TextWidget widget = new TextWidget(text);
        section.add(widget);
        return widget;
    }

    /**
     * Creates a text widget bound to the specified model within a new section.
     * Removes all existing children and sets up the widget hierarchy automatically.
     *
     * @param model the model providing text content for the widget
     * @return the newly created {@link TextWidget}
     */
    default TextWidget createText(final Model<String> model) {
        final Section section = this.createSection();
        final TextWidget widget = new TextWidget();
        widget.setTextModel(model);
        section.add(widget);
        return widget;
    }

    /**
     * Creates an input field widget within a new section.
     * Removes all existing children and sets up the widget hierarchy automatically.
     *
     * @return the newly created {@link InputField}
     */
    default InputField createInputField() {
        final Section section = this.createSection();
        final InputField field = new InputField();
        section.add(field);
        return field;
    }

    /**
     * Creates an input field widget bound to the specified model within a new section.
     * Removes all existing children and sets up the widget hierarchy automatically.
     *
     * @param model the model providing text content for the input field
     * @return the newly created {@link InputField}
     */
    default InputField createInputField(final Model<String> model) {
        final InputField field = this.createInputField();
        field.setTextModel(model);
        return field;
    }
}
