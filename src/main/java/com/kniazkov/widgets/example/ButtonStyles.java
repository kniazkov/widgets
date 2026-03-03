/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.model.IntegerModel;
import com.kniazkov.widgets.model.IntegerToStringModel;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

/**
 * Demonstrates ....
 *
 * <b>How to use</b>
 * <ol>
 *   <li>Run the program.</li>
 *   <li>
 *     Open your browser and navigate to
 *     <a href="http://localhost:8000">http://localhost:8000</a>.
 *   </li>
 * </ol>
 */
public class ButtonStyles {

    /**
     * Application entry point.
     *
     * @param args program arguments (unused)
     */
    public static void main(final String[] args) {
        final Page page = (root, parameters) -> {
            Section section = new Section();
            root.add(section);
            final Button button = new Button();
            section.add(button);
            button.setWidth("100px");
            button.setHeight("100px");
            final IntegerModel model = new IntegerModel();
            final TextWidget widget = new TextWidget();
            widget.setTextModel(new IntegerToStringModel(model));
            button.setWidget(widget);
            button.onClick(data -> model.setData(model.getData() + 1));

            section = new Section();
            root.add(section);
            final Button enable = new Button("Enable");
            section.add(enable);
            enable.onClick(data -> button.enable());
            final Button disable = new Button("Disable");
            section.add(disable);
            disable.onClick(data -> button.disable());
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
