/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.Application;
import com.kniazkov.widgets.Button;
import com.kniazkov.widgets.InputField;
import com.kniazkov.widgets.Model;
import com.kniazkov.widgets.Options;
import com.kniazkov.widgets.Page;
import com.kniazkov.widgets.Paragraph;
import com.kniazkov.widgets.Server;
import com.kniazkov.widgets.StyleSet;
import com.kniazkov.widgets.TextWidget;
import com.kniazkov.widgets.TextWidgetStyle;

/**
 * A web application that demonstrates dynamic text style changes.
 * <p>
 *     This example shows how font faces can be managed and applied in different ways:
 * </p>
 * <ul>
 *     <li>Using the default font style);</li>
 *     <li>Setting the font directly on a widget instance;</li>
 *     <li>Applying a forked style derived from a shared {@link StyleSet};</li>
 *     <li>Changing fonts dynamically using user input and a {@link Model}.</li>
 * </ul>
 *
 * <p>
 *     How to use:
 * </p>
 * <ol>
 *     <li>Run the program;</li>
 *     <li>Open your browser and go to
 *         <a href="http://localhost:8000">http://localhost:8000</a>.
 *     </li>
 * </ol>
 */
public class FontFaceExample {
    /**
     * Starting point.
     *
     * @param args Program arguments
     */
    public static void main(String[] args) {
        final Page page = root -> {
            // First paragraph uses default text style from the root (nothing overridden)
            final Paragraph p0 = root.createParagraph();
            p0.createTextWidget("First line (default font).");

            // Second paragraph: font is set directly on the widget
            final Paragraph p1 = root.createParagraph();
            final TextWidget t1 = p1.createTextWidget("Second line (font set directly).");
            t1.setFontFace("monospace");

            // Third paragraph: style is forked from the default, then modified and applied
            final Paragraph p2 = root.createParagraph();
            final TextWidget t2 = p2.createTextWidget("Third line (font set via forked style).");
            final StyleSet defaultStyles = root.getDefaultStyles();
            final TextWidgetStyle style = defaultStyles.getDefaultTextWidgetStyle().fork();
            style.setFontFace("sans-serif");
            style.apply(t2);

            // Fourth paragraph: input field to enter font name, buttons to apply changes
            final Paragraph p3 = root.createParagraph();
            p3.createTextWidget("Enter font name:");
            final InputField input = p3.createInputField();
            final Model<String> fontNameModel = input.getTextModel();

            // Button that updates the default style (affects any widget using the original style)
            final Button b0 = p3.createButton("Change default font");
            b0.onClick(() -> {
                defaultStyles.getDefaultTextWidgetStyle().setFontFace(fontNameModel.getData());
            });

            // Button that updates the forked style (affects only widgets that applied it)
            final Button b1 = p3.createButton("Change font in forked style");
            b1.onClick(() -> {
                style.setFontFace(fontNameModel.getData());
            });
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
