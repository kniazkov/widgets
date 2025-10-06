/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.Application;
import com.kniazkov.widgets.Button;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.Options;
import com.kniazkov.widgets.Page;
import com.kniazkov.widgets.Paragraph;
import com.kniazkov.widgets.Server;
import com.kniazkov.widgets.TextWidget;

/**
 * A web application that demonstrates text color changes.
 * <p>
 *     When the user clicks one of the color buttons, the text color updates accordingly.
 * </p>
 *
 * <p>
 * How to use:
 * <ol>
 *     <li>Run the program;</li>
 *     <li>
 *         Open your browser and go to
 *         <a href="http://localhost:8000">http://localhost:8000</a>.
 *     </li>
 * </ol>
 */
public class TextColor {

    /**
     * Starting point.
     *
     * @param args Program arguments
     */
    public static void main(String[] args) {
        // Defines the structure and behavior of the page
        final Page page = root -> {
            // Create a horizontal paragraph to hold all widgets
            final Paragraph paragraph = root.createParagraph();

            // Create a text widget with default color
            final TextWidget text =
                paragraph.createTextWidget("Click on some button and the color changes:");

            // Create a red button and set its click handler to change the text color
            final Button red = paragraph.createButton("red");
            red.setBackgroundColor(Color.RED);
            red.onClick((data) -> text.setColor(Color.RED));

            // Create a green button with the same behavior
            final Button green = paragraph.createButton("green");
            green.onClick((data) -> text.setColor(Color.GREEN));

            // Create a blue button
            final Button blue = paragraph.createButton("blue");
            blue.onClick((data) -> text.setColor(Color.BLUE));
        };

        // Create the application and start the server
        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
