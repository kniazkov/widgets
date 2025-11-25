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
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import com.kniazkov.widgets.view.TextWidgetStyle;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Demonstration of reactive text widget styles and cascading color updates.
 * <p>
 * This example application showcases the use of {@link TextWidget} and
 * {@link TextWidgetStyle} in combination with reactive {@link Model models}
 * and cascading inheritance between styles.
 * <p>
 * It creates several {@link TextWidget} instances that share and override color models
 * to demonstrate both local and global style propagation. The UI also includes buttons that modify
 * style data at runtime, immediately updating all affected widgets.
 *
 * <b>How to use</b>
 * <ol>
 *   <li>Run the program.</li>
 *   <li>
 *     Open your browser and go to
 *     <a href="http://localhost:8000">http://localhost:8000</a>.
 *   </li>
 * </ol>
 *
 * <b>Demonstrated features</b>
 * <ul>
 *   <li>Creation of widgets using the default style.</li>
 *   <li>Derivation of new styles via {@link TextWidgetStyle#derive()}.</li>
 *   <li>Reactive updates to shared color models across multiple widgets.</li>
 *   <li>Global style mutation and its effect across all connected sessions.</li>
 * </ul>
 *
 * <b>Important note</b>
 * <p>
 * The <b>"Change default style"</b> button modifies the static default style returned by
 * {@link TextWidget#getDefaultStyle()}. Since this style is shared globally across all pages
 * and browser sessions, the color change will be visible to <b>all users</b>, including new pages
 * opened later. To limit updates to a single page, use a derived style created with
 * {@link TextWidgetStyle#derive()} instead.
 */
public class TextWidgetStyles {

    /**
     * Application entry point.
     *
     * @param args program arguments (unused)
     */
    public static void main(final String[] args) {
        final Page page = (root, parameters) -> {
            // Section 1: text with default style
            Section section = new Section();
            root.add(section);
            TextWidget widget = new TextWidget("Default style");
            section.add(widget);

            // Section 2: derived color style
            section = new Section();
            root.add(section);
            final TextWidgetStyle firstStyle = TextWidget.getDefaultStyle().derive();
            firstStyle.setColor(Color.RED);
            widget = new TextWidget(firstStyle, "Colored text");
            section.add(widget);
            section.add(new TextWidget(" "));
            widget = new TextWidget(firstStyle, "Blue text");
            widget.setColor(Color.BLUE);
            section.add(widget);
            section.add(new TextWidget(" "));
            final TextWidgetStyle secondStyle = TextWidget.getDefaultStyle().derive();
            secondStyle.setColor(Color.VIOLET);
            widget = new TextWidget("Violet text");
            widget.setStyle(secondStyle);
            section.add(widget);

            // Section 3: interactive style changes
            section = new Section();
            root.add(section);

            // Button 1: modifies the global default style
            Button button = new Button("Change default style");
            section.add(button);
            button.onClick(data -> {
                TextWidget.getDefaultStyle().setColor(Color.ORANGE);
            });

            // Button 2: toggles a local derived style between red and green
            final AtomicBoolean flag = new AtomicBoolean();
            button = new Button("Color toggle");
            section.add(button);
            button.onClick(data -> {
                if (flag.get()) {
                    firstStyle.setColor(Color.RED);
                    flag.set(false);
                } else {
                    firstStyle.setColor(Color.GREEN);
                    flag.set(true);
                }
            });
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
