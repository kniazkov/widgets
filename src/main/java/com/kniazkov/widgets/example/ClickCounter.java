/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.*;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Text;

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
        final Page page = new Page() {
            int count = 0;

            @Override
            public void create(@NotNull RootWidget root) {
                final Paragraph firstLine = new Paragraph();
                root.appendChild(firstLine);

                final Paragraph secondLine = new Paragraph();
                root.appendChild(secondLine);

                final Button button = new Button();
                firstLine.appendChild(button);
                button.setChild(new TextWidget("Click me"));

                secondLine.appendChild(new TextWidget("Click counter: "));
                final TextWidget counter = new TextWidget();
                secondLine.appendChild(counter);
                final IntegerModel model = new IntegerModel();
                counter.setTextModel(model);

                button.onClick(() -> {
                    count++;
                    model.setIntValue(count);
                });
            }
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
