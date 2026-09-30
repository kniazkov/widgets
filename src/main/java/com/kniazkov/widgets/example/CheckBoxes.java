/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.view.CheckBox;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.model.Model;
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
     * A caption retained by the widget tree also owns its subscription.
     */
    static final class CheckCaption extends TextWidget implements Listener<Boolean> {
        /**
         * Creates the caption and listens to the checkbox model.
         * @param source checkbox state
         */
        CheckCaption(final Model<Boolean> source) {
            super("Check me");
            source.addListener(this);
        }

        @Override
        public void accept(final Boolean checked) {
            this.setText(checked ? "Checked" : "Unchecked");
        }
    }

    /**
     * Creates the example.
     */
    public CheckBoxes() {
    }

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
            section.add(new CheckCaption(first.getCheckedStateModel()));
            section = new Section();
            root.add(section);
            final CheckBox second = new CheckBox();
            section.add(second);
            second.disable();
            section.add(new TextWidget("Disabled"));
        };

        final Application application = new Application(page);
        final Options options = new Options.Builder().build();
        Server.start(application, options);
    }
}
