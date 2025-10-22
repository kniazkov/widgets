/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.model.IntegerModel;
import com.kniazkov.widgets.model.IntegerToStringModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.SynchronizedModel;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

/**
 * A web application demonstrating shared (global) and local state across multiple clients.
 * <p>
 * This example extends the basic click counter by introducing two counters:
 * a <b>global counter</b> shared by all connected clients, and a <b>local counter</b>
 * specific to each browser session.
 * Clicking the button increases both counters, and changes to the global counter
 * are instantly reflected across all open sessions.
 * </p>
 *
 * <b>How to use</b>
 * <ol>
 *   <li>Run the program;</li>
 *   <li>
 *     Open your browser and go to
 *     <a href="http://localhost:8000">http://localhost:8000</a>;
 *   </li>
 *   <li>Open the same URL in two tabs or browsers;</li>
 *   <li>Click the button — observe that the <b>global counter</b> updates everywhere,
 *       while the <b>local counter</b> only changes in the tab you clicked.</li>
 * </ol>
 */
public class ClickCounterExt {
    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Model<Integer> globalModel = new IntegerModel().asSynchronized();

        final Page page = root -> {
            System.gc();
            final Model<Integer> localModel = new IntegerModel();

            // --- Shared button section ---
            Section section = new Section();
            root.add(section);

            final Button button = new Button("Click me");
            section.add(button);

            button.onClick(data -> {
                globalModel.setData(globalModel.getData() + 1);
                localModel.setData(localModel.getData() + 1);
            });

            // --- Global counter section ---
            section = new Section();
            root.add(section);
            section.add(new TextWidget("Global counter: "));
            final TextWidget globalCounter = new TextWidget();
            section.add(globalCounter);
            globalCounter.setTextModel(new IntegerToStringModel(globalModel));
            globalCounter.setFontWeight(FontWeight.BOLD);

            // --- Local counter section ---
            section = new Section();
            root.add(section);
            section.add(new TextWidget("Local counter: "));
            final TextWidget localCounter = new TextWidget();
            section.add(localCounter);
            localCounter.setTextModel(new IntegerToStringModel(localModel));
            localCounter.setFontWeight(FontWeight.BOLD);
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
