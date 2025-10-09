/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.model.DefaultIntegerModel;
import com.kniazkov.widgets.model.IntegerToStringModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

/**
 * A simple interactive web application that demonstrates two-way data binding and event handling.
 * <p>
 * This example shows how to connect a {@link Button} with a {@link Model} using
 * a controller and automatically update a {@link TextWidget} when the model changes.
 * Each time the button is clicked, the counter value is incremented and immediately
 * reflected in the text label.
 * </p>
 *
 * <b>How to use</b>
 * <ol>
 *   <li>Run the program;</li>
 *   <li>
 *     Open your browser and go to
 *     <a href="http://localhost:8000">http://localhost:8000</a>;
 *   </li>
 *   <li>Click the button — the counter will increase with each click.</li>
 * </ol>
 */
public class ClickCounter {
    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Page page = root -> {
            final Model<Integer> model = new DefaultIntegerModel();
            final Section section = new Section();
            root.add(section);

            final Button button = new Button("Click me");
            section.add(button);

            // Increment model on click
            button.onClick(data -> model.setData(model.getData() + 1));

            // Display current counter value
            section.add(new TextWidget("Click counter: "));
            final TextWidget counter = new TextWidget();
            section.add(counter);
            counter.setTextModel(new IntegerToStringModel(model));
            counter.setFontWeight(FontWeight.BOLD);
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
