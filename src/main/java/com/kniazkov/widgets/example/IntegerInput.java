/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.*;

/**
 * A web application that demonstrates the processing of integer values entered into a text field.
 * In this example, a special model is used for parsing integers.
 * How to use:
 *   1. Run the program;
 *   2. Open your browser and type "<a href="http://localhost:8000">...</a>" in the address bar.
 */
public class IntegerInput {
    /**
     * Starting point.
     * @param args Program arguments
     */
    public static void main(String[] args) {
        final Page page = root -> {
            final Paragraph line = new Paragraph();
            root.appendChild(line);

            final InputField input = new InputField();
            line.appendChild(input);
            final IntegerModel inputData = new IntegerModel();
            input.setTextModel(inputData);

            line.appendChild(new TextWidget(" x 2 = "));

            final TextWidget output = new TextWidget("0");
            line.appendChild(output);

            input.onTextInput(data -> {
                if (inputData.isValid()) {
                    output.setText(String.valueOf(inputData.getIntValue() * 2));
                } else {
                    output.setText("?");
                }
            });
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
