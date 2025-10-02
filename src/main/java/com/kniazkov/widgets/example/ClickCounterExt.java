/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.*;

/**
 * A web application that contains a button and two counters:
 * <ul>
 *     <li>One counter displays the number of clicks made on the current page (local counter);</li>
 *     <li>
 *         The second counter displays the total number of clicks made across all open pages
 *         (global counter).
 *     </li>
 * </ul>
 *
 * <p>
 *     This example demonstrates how to share a model across multiple UI instances and
 *     automatically propagate updates between windows/tabs, without needing manual refresh.
 * </p>
 *
 * <p>
 * How to use:
 * <ol>
 *     <li>Run the program;</li>
 *     <li>
 *         Open your browser and go to
 *         <a href="http://localhost:8000">http://localhost:8000</a>;
 *     </li>
 *     <li>
 *         Open the same address in a second tab or window — place them side by side
 *         to observe live sync.
 *     </li>
 * </ol>
 */
public class ClickCounterExt {
    /**
     * Starting point.
     *
     * @param args Program arguments
     */
    public static void main(String[] args) {
        // Define the page logic. One global model is shared across all page instances.
        final Page page = new Page() {
            // Shared model: will be reused across all pages (shared by reference)
            final IntegerViaStringModel globalCounterModel = new IntegerViaStringModel();

            @Override
            public void create(RootWidget root) {
                // Paragraph containing the button
                final Paragraph p0 = root.createParagraph();
                final Button button = p0.createButton("Click me");

                // Paragraph containing the counters
                final Paragraph p1 = root.createParagraph();
                p1.createTextWidget("Click counter: ");

                // Local counter: private to this page
                final TextWidget localCounter = p1.createTextWidget();
                final IntegerViaStringModel localCounterModel = new IntegerViaStringModel();
                localCounter.setTextModel(localCounterModel);

                // Separator
                p1.createTextWidget("(");

                // Global counter: shared across all pages
                final TextWidget globalCounter = p1.createTextWidget();
                globalCounter.setTextModel(globalCounterModel);

                p1.createTextWidget(")");

                // On button click: increment both counters
                button.onClick((data) -> {
                    localCounterModel.setIntValue(localCounterModel.getIntValue() + 1);
                    globalCounterModel.setIntValue(globalCounterModel.getIntValue() + 1);
                });
            }
        };

        // Create the application and start the server
        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
