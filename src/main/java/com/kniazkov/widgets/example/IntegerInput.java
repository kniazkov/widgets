/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.*;

/**
 * A web application that demonstrates the processing of integer values entered into a text field.
 * <p>
 *     In this example, an {@link IntegerViaStringModel} is used to parse and validate user input.
 *     When a valid number is entered, it is multiplied by 2 and the result is displayed next
 *     to the input.
 * </p>
 * <p>
 *     How to use:
 * <ol>
 *     <li>Run the program;</li>
 *     <li>
 *         Open your browser and navigate to
 *         <a href="http://localhost:8000">http://localhost:8000</a>.
 *     </li>
 * </ol>
 */
public class IntegerInput {

    /**
     * Starting point.
     *
     * @param args Program arguments
     */
    public static void main(String[] args) {
        // Defines the page layout and logic
        final Page page = root -> {
            // Create a horizontal paragraph container
            final Paragraph line = root.createParagraph();

            // Create an input field and place it inside the paragraph
            final InputField input = line.createInputField();

            // Create an IntegerModel that parses string input into integers
            final IntegerViaStringModel inputData = new IntegerViaStringModel();

            // Bind the input field to the IntegerModel
            input.setTextModel(inputData);

            // Add a text label next to the input field
            line.createTextWidget(" x 2 = ");

            // Create a text widget for displaying the computed result
            final TextWidget output = line.createTextWidget();

            // Set up a controller to react to user input
            input.onTextInput(data -> {
                // If the input is a valid integer, compute and show the result
                if (inputData.isValid()) {
                    output.setText(String.valueOf(inputData.getIntValue() * 2));
                } else {
                    // If invalid, show a placeholder
                    output.setText("?");
                }
            });

            // Set initial value (optional): triggers output immediately
            input.setText("0");
        };

        // Create an application instance with the page
        final Application application = new Application(page);

        // Default server options (e.g., port 8000)
        final Options options = new Options();

        // Start the web server
        Server.start(application, options);
    }
}
