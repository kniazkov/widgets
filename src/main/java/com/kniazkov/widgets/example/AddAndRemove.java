/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Section;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates dynamic creation and removal of widgets.
 *
 * <b>How to use</b>
 * <ol>
 *   <li>Run this program.</li>
 *   <li>
 *     Open your browser and go to
 *     <a href="http://localhost:8000">http://localhost:8000</a>.
 *   </li>
 *   <li>Click "Add more..." to create buttons. Click any button to remove it.</li>
 * </ol>
 */
public class AddAndRemove {

    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Page page = root -> {
            final Section section = new Section();
            root.add(section);

            final Button addButton = new Button("Add more...");
            section.add(addButton);

            final AtomicInteger counter = new AtomicInteger();

            addButton.onClick(evt0 -> {
                final Button dynamicButton = new Button(String.valueOf(counter.getAndIncrement()));
                section.add(dynamicButton);

                dynamicButton.onClick(evt1 -> dynamicButton.remove());
            });
        };

        final Application app = new Application(page);
        final Options options = new Options();
        Server.start(app, options);
    }
}
