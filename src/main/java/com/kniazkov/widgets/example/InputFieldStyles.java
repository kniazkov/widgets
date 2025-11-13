/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import com.kniazkov.widgets.view.TextWidgetStyle;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Demonstration of ...
 * <h3>How to use</h3>
 * <ol>
 *   <li>Run the program.</li>
 *   <li>
 *     Open your browser and go to
 *     <a href="http://localhost:8000">http://localhost:8000</a>.
 *   </li>
 * </ol>
 */
public class InputFieldStyles {

    /**
     * Application entry point.
     *
     * @param args program arguments (unused)
     */
    public static void main(final String[] args) {
        final Page page = root -> {
            Section section = new Section();
            root.add(section);
            section.add(new TextWidget("Enter some text: "));
            InputField field = new InputField();
            section.add(field);

            section = new Section();
            root.add(section);
            final Button makeValid = new Button("Make valid");
            section.add(makeValid);
            makeValid.onClick(data -> field.setValidState(true));
            final Button makeInvalid = new Button("Make invalid");
            section.add(makeInvalid);
            makeInvalid.onClick(data -> field.setValidState(false));
            final Button enable = new Button("Enable");
            section.add(enable);
            enable.onClick(data -> field.enable());
            final Button disable = new Button("Disable");
            section.add(disable);
            disable.onClick(data -> field.disable());
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
