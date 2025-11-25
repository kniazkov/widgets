/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.model.EmailModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

/**
 * A minimal demo application that shows an email input field with live validation.
 *
 * <p>
 * The page contains a text field bound to an {@code EmailModel}. As the user types,
 * the field is dynamically validated: if the value is not a valid email address,
 * the input widget is highlighted accordingly. The currently entered value is also
 * displayed below the field.
 * </p>
 *
 * <b>How to use</b>
 * <ol>
 *   <li>Run this program.</li>
 *   <li>
 *     Open your browser and go to
 *     <a href="http://localhost:8000">http://localhost:8000</a>.
 *   </li>
 *   <li>Type an email address into the input field and observe validation feedback.</li>
 *   <li>Watch the entered value appear in the text below.</li>
 * </ol>
 */
public class EmailInput {
    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Page page = (root, parameters) -> {
            Section section = new Section();
            root.add(section);
            section.add(new TextWidget("Enter email: "));
            final InputField field = new InputField();
            section.add(field);
            field.setWidth("150px");
            final Model<String> model = new EmailModel();
            field.setTextModel(model);

            section = new Section();
            root.add(section);
            section.add(new TextWidget("You entered: '"));
            final TextWidget echo = new TextWidget();
            echo.setFontWeight(FontWeight.BOLD);
            section.add(echo);
            section.add(new TextWidget("'"));

            field.onTextInput(text -> echo.setText(text.trim()));
        };

        final Application app = new Application(page);
        final Options options = new Options();
        Server.start(app, options);
    }
}
