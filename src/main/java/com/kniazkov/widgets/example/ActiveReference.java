/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.view.ActiveText;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

/**
 * A demonstration of the {@link ActiveText} widget with clickable text functionality.
 * <p>
 * This example creates an interactive text element that, when clicked,
 * redirects the user to an external URL. It showcases how to make text
 * responsive to user interactions and handle click events.
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
public class ActiveReference {
    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Page page = (root, parameters) -> {
            final Section section = new Section();
            root.add(section);

            final ActiveText textWidget = new ActiveText("click me");
            section.add(textWidget);
            textWidget.onClick(evt -> root.goToPage("https://google.com"));
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
