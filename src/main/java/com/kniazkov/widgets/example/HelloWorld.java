/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

/**
 * A minimal example of a web application built using the widget-based framework.
 * <p>
 * This example demonstrates how to define a simple {@link Page} that displays
 * a single {@link TextWidget} inside a {@link Section}. It serves as the
 * canonical “Hello, World!” example for applications built on this framework.
 * </p>
 *
 * <h3>How to use</h3>
 * <ol>
 *   <li>Run the program;</li>
 *   <li>
 *     Open your browser and go to
 *     <a href="http://localhost:8000">http://localhost:8000</a>.
 *   </li>
 * </ol>
 */
public class HelloWorld {
    public static void main(String[] args) {
        final Page page = root -> {
            final Section section = new Section();
            root.add(section);

            final TextWidget textWidget = new TextWidget("it works!");
            section.add(textWidget);
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
