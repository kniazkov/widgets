/*
 * Copyright (c) 2025 Ivan Kniazkov
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
 * <p>
 *     When the user types a name, a greeting appears below. If the input is empty, the greeting
 *     is hidden.
 * </p>
 *
 * <p>
 *     How to use:
 * <ol>
 *     <li>Run the program;</li>
 *     <li>
 *         Open your browser and go to
 *         <a href="http://localhost:8000">http://localhost:8000</a>.
 *     </li>
 * </ol>
 */
public class TextInput {

    /**
     * Starting point.
     *
     * @param args Program arguments
     */
    public static void main(String[] args) {
        // Defines the structure and behavior of the page
        final Page page = root -> {
            // First paragraph: prompt and input field
            final Paragraph p0 = root.createParagraph();
            p0.createTextWidget("Enter your name: ");
            final InputField input = p0.createInputField();

            // Second paragraph: greeting output
            final Paragraph p1 = root.createParagraph();
            final TextWidget greetings = p1.createTextWidget();

            // Handle text input: show greeting if not empty
            input.onTextInput(data -> {
                if (data.isEmpty()) {
                    greetings.setText(""); // clear if empty
                } else {
                    greetings.setText("Hello, " + data.trim() + '!');
                }
            });

            // Set initial value (optional): triggers greeting immediately
            input.setText("Johnny");
        };

        // Create the application and start the server
        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
