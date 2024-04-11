/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.*;
import org.jetbrains.annotations.NotNull;

/**
 * A web application that contains a button that can be clicked by the user,
 * as well as a counter of such clicks. The second counter shows the number of button clicks
 * on all pages (instances) of the web application.
 * Here is a demonstration of updating data that is in another, inactive window,
 * without having to manually refresh the web page.
 * How to use:
 *   1. Run the program;
 *   2. Open your browser and type "<a href="http://localhost:8000">...</a>" in the address bar;
 *   3. Open a second tab at the same address.
 */
public class ClickCounterExt {
    /**
     * Starting point.
     * @param args Program arguments
     */
    public static void main(String[] args) {
        final Page page = new Page() {
            final IntegerModel globalCounterModel = new IntegerModel();

            @Override
            public void create(@NotNull RootWidget root) {
                final Button button = new Button();
                button.setChild(new TextWidget("Click me"));

                final Paragraph p0 = new Paragraph();
                root.appendChild(p0);
                p0.appendChild(button);

                final TextWidget localCounter = new TextWidget();
                final IntegerModel localCounterModel = new IntegerModel();
                localCounter.setTextModel(localCounterModel);
                final TextWidget globalCounter = new TextWidget();
                globalCounter.setTextModel(globalCounterModel);

                final Paragraph p1 = new Paragraph();
                root.appendChild(p1);
                p1.appendChild(new TextWidget("Click counter: "))
                    .appendChild(localCounter)
                    .appendChild(new TextWidget(" ("))
                    .appendChild(globalCounter)
                    .appendChild(new TextWidget(")"));

                button.onClick(() -> {
                    localCounterModel.setIntValue(localCounterModel.getIntValue() + 1);
                    globalCounterModel.setIntValue(globalCounterModel.getIntValue() + 1);
                });
            }
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
