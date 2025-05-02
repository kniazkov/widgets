/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.*;

/**
 * A web application that contains a button that can be clicked by the user,
 * along with a counter showing how many times the button has been clicked.
 * <p>
 *     This example demonstrates how to use a controller to react to user interaction
 *     and update the underlying model.
 * </p>
 *
 * <p>
 *     How to use:
 * <ol>
 *     <li>Run the program;</li>
 *     <li>
 *         Open your browser and go to <a href="http://localhost:8000">http://localhost:8000</a>.
 *     </li>
 * </ol>
 */
public class ClickCounter {

    /**
     * Starting point.
     *
     * @param args Program arguments
     */
    public static void main(String[] args) {
        // Defines the structure and logic of the page
        final Page page = root -> {
            // First paragraph: contains the button
            final Paragraph p0 = root.createParagraph();
            final Button button = p0.createButton("Click me");

            // Second paragraph: contains the label and counter
            final Paragraph p1 = root.createParagraph();
            p1.createTextWidget("Click counter: ");

            // Create a text widget to display the counter value
            final TextWidget counter = p1.createTextWidget();

            // Create a model to hold the integer counter value
            final IntegerModel model = new IntegerModel();

            // Bind the counter widget to the model
            counter.setTextModel(model);

            // Handle button clicks: increment the value stored in the model
            button.onClick(() -> {
                model.setIntValue(model.getIntValue() + 1);
            });
        };

        // Create the application and start the server
        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
