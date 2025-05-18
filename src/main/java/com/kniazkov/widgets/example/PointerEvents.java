/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.Application;
import com.kniazkov.widgets.Button;
import com.kniazkov.widgets.Options;
import com.kniazkov.widgets.Page;
import com.kniazkov.widgets.Paragraph;
import com.kniazkov.widgets.Server;
import com.kniazkov.widgets.TextWidget;

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
public class PointerEvents {

    /**
     * Starting point.
     *
     * @param args Program arguments
     */
    public static void main(String[] args) {
        // Defines the structure and logic of the page
        final Page page = root -> {
            // First paragraph: contains the button
            final Paragraph paragraph = root.createParagraph();
            final Button button = paragraph.createButton();
            button.setWidth(200);
            final TextWidget text = button.createText("Click me");

            button.onClick((data) -> {
                text.setText("Clicked");
            });

            button.onPointerEnter((data) -> {
                text.setText("Mouse over");
            });

            button.onPointerLeave((data) -> {
                text.setText("Mouse out");
            });
        };

        // Create the application and start the server
        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
