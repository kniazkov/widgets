/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.view.Image;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

/**
 * Demonstrates serving a static image using the framework.
 * <p>
 * The program starts a server and renders a page containing a single image
 * loaded from the application's static file directory (e.g., {@code house.png}
 * inside the configured {@code www} folder).
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
public class StaticImage {
    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Page page = root -> {
            root.setBgColor(Color.DARK_SLATE_GRAY);

            final Section section = new Section();
            root.add(section);

            final Image image = new Image("house.png");
            section.add(image);
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
