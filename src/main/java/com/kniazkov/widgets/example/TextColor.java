/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.Application;
import com.kniazkov.widgets.Button;
import com.kniazkov.widgets.Color;
import com.kniazkov.widgets.Options;
import com.kniazkov.widgets.Page;
import com.kniazkov.widgets.Paragraph;
import com.kniazkov.widgets.Server;
import com.kniazkov.widgets.TextWidget;

/**
 * A web application that demonstrates text color changes.
 * How to use:
 *   1. Run the program;
 *   2. Open your browser and type "<a href="http://localhost:8000">...</a>" in the address bar.
 */
public class TextColor {
    /**
     * Starting point.
     * @param args Program arguments
     */
    public static void main(String[] args) {
        final Page page = root -> {
            final Paragraph paragraph = new Paragraph();
            root.appendChild(paragraph);

            final TextWidget text = new TextWidget("Click on some button and the color changes:");
            paragraph.appendChild(text);

            final Button red = new Button("red");
            paragraph.appendChild(red);
            red.onClick(() -> text.setColor(Color.RED));

            final Button green = new Button("green");
            paragraph.appendChild(green);
            green.onClick(() -> text.setColor(Color.GREEN));

            final Button blue = new Button("blue");
            paragraph.appendChild(blue);
            blue.onClick(() -> text.setColor(Color.BLUE));
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
