/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.view.CheckBox;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

/**
 * A demonstration program showcasing the {@link CheckBox} widget with state management.
 * <p>
 * This example creates two checkboxes: one interactive checkbox that updates a text label
 * when toggled, and one disabled checkbox that demonstrates the disabled state.
 * The example shows how to listen for state changes and control widget interactivity.
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
public class CheckBoxes {
    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Page page = (root, parameters) -> {
            Section section = new Section();
            root.add(section);

            final CheckBox first = new CheckBox();
            section.add(first);
            final TextWidget caption = new TextWidget("Check me");
            section.add(caption);
            first.getCheckedStateModel().addListener(
                checked -> caption.setText(checked ? "Checked" : "Unchecked")
            );
            section = new Section();
            root.add(section);
            final CheckBox second = new CheckBox();
            section.add(second);
            second.disable();
            section.add(new TextWidget("Disabled"));
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
