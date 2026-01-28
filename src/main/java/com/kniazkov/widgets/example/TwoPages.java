/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.PageSettings;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.view.RootWidget;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.util.Map;

/**
 * Demonstrates how to create a web application with multiple pages.
 *
 * <p>
 * This example registers:
 * <ul>
 *     <li>a main (index) page at <code>/</code>,</li>
 *     <li>an additional page at <code>/about</code>.</li>
 * </ul>
 *
 * <p>
 * Each page is implemented as its own {@link Page} class. The {@link Application}
 * routes incoming requests based on the URL path.
 *
 * <b>How to use</b>
 * <ol>
 *   <li>Compile and run the program;</li>
 *   <li>
 *     Open your browser and navigate to:<br>
 *     <a href="http://localhost:8000">http://localhost:8000</a><br>
 *     You will see the main (index) page.
 *   </li>
 *   <li>
 *     Then open:<br>
 *     <a href="http://localhost:8000/about">http://localhost:8000/about</a><br>
 *     This loads the second page ("about").
 *   </li>
 * </ol>
 *
 * <p>
 * This example shows the basic usage of {@link Application#addPage(String, Page)}
 * for registering additional routes.
 */
public class TwoPages {
    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Application application = new Application(new IndexPage());
        application.addPage("about", new AboutPage());
        final Options options = new Options();
        Server.start(application, options);
    }

    private static final class IndexPage implements Page {
        @Override
        public void create(final RootWidget root, final PageSettings settings) {
            final Section section = new Section();
            root.add(section);

            final TextWidget textWidget = new TextWidget("hello from the main page!");
            section.add(textWidget);
        }
    }

    private static final class AboutPage implements Page {
        @Override
        public void create(final RootWidget root, final PageSettings settings) {
            final Section section = new Section();
            root.add(section);

            final TextWidget textWidget = new TextWidget("hello from \"about\" page!");
            section.add(textWidget);
        }
    }
}
