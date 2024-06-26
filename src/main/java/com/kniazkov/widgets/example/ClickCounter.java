/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.*;

/**
 * A web application that contains a button that can be clicked by the user,
 * as well as a counter of such clicks. The principle of controller implementation
 * is demonstrated here.
 * How to use:
 *   1. Run the program;
 *   2. Open your browser and type "<a href="http://localhost:8000">...</a>" in the address bar.
 */
public class ClickCounter {
    /**
     * Starting point.
     * @param args Program arguments
     */
    public static void main(String[] args) {
        final Page page = root -> {
            final Paragraph p0 = new Paragraph();
            root.appendChild(p0);
            final Button button = new Button();
            p0.appendChild(button);
            button.setChild(new TextWidget("Click me"));

            final Paragraph p1 = new Paragraph();
            root.appendChild(p1);
            final TextWidget counter = new TextWidget();
            p1.appendChild(new TextWidget("Click counter: "))
                .appendChild(counter);
            final IntegerModel model = new IntegerModel();
            counter.setTextModel(model);

            button.onClick(() -> {
                model.setIntValue(model.getIntValue() + 1);
            });
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
