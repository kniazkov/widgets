/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.Application;
import com.kniazkov.widgets.Options;
import com.kniazkov.widgets.Page;
import com.kniazkov.widgets.Paragraph;
import com.kniazkov.widgets.Server;
import com.kniazkov.widgets.TextWidget;

/**
 * Web application that outputs a string to the browser.
 * How to use:
 *   1. Run the program;
 *   2. Open your browser and type "<a href="http://localhost:8000">...</a>" in the address bar.
 */
public class HelloWorld {
    /**
     * Starting point.
     * @param args Program arguments
     */
    public static void main(String[] args) {
        final Page page = root -> {
            Paragraph paragraph = new Paragraph();
            root.appendChild(paragraph);

            TextWidget text = new TextWidget();
            paragraph.appendChild(text);
            text.setText("It works.");
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
