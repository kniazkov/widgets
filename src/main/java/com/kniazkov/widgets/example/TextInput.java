/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.Application;
import com.kniazkov.widgets.InputField;
import com.kniazkov.widgets.Options;
import com.kniazkov.widgets.Page;
import com.kniazkov.widgets.Paragraph;
import com.kniazkov.widgets.Server;
import com.kniazkov.widgets.TextWidget;

/**
 * A web application that demonstrates the processing of text entered into a text field.
 * How to use:
 *   1. Run the program;
 *   2. Open your browser and type "<a href="http://localhost:8000">...</a>" in the address bar.
 */
public class TextInput {
    /**
     * Starting point.
     * @param args Program arguments
     */
    public static void main(String[] args) {
        final Page page = root -> {
            final Paragraph p0 = new Paragraph();
            root.appendChild(p0);
            p0.appendChild(new TextWidget("Enter your name: "));
            InputField input = new InputField();
            p0.appendChild(input);
            input.setText("Johnny");

            final Paragraph p1 = new Paragraph();
            root.appendChild(p1);
            TextWidget greetings = new TextWidget();
            p1.appendChild(greetings);

            input.onTextInput(
                data -> greetings.setText(data.isEmpty() ? "" : "Hello, " + data.trim() + '!')
            );
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
