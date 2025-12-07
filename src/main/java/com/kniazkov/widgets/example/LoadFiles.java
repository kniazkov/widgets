/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

/**
 * ....
 *
 * <b>How to use</b>
 * <ol>
 *   <li>Run the program;</li>
 *   <li>
 *     Open your browser and go to
 *     <a href="http://localhost:8000">http://localhost:8000</a>.
 *   </li>
 * </ol>
 */
public class LoadFiles {
    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Page page = (root, parameters) -> {
            final Section section = new Section();
            root.add(section);

            final FileLoader widget = new FileLoader("Click me");
            section.add(widget);
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
