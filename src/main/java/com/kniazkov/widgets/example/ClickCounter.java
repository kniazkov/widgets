/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.Application;
import com.kniazkov.widgets.Button;
import com.kniazkov.widgets.IntegerModel;
import com.kniazkov.widgets.Options;
import com.kniazkov.widgets.Page;
import com.kniazkov.widgets.Paragraph;
import com.kniazkov.widgets.RootWidget;
import com.kniazkov.widgets.Server;
import com.kniazkov.widgets.TextWidget;
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
                Paragraph firstLine = new Paragraph();
                root.appendChild(firstLine);

                Button button = new Button();
                firstLine.appendChild(button);
                button.setChild(new TextWidget("Click me"));

                Paragraph secondLine = new Paragraph();
                root.appendChild(secondLine);
                secondLine.appendChild(new TextWidget("Click counter: "));

                TextWidget counter = new TextWidget();
                secondLine.appendChild(counter);
                IntegerModel model = new IntegerModel();
                counter.setTextModel(model);
            }
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
