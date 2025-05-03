/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.Application;
import com.kniazkov.widgets.FontWeight;
import com.kniazkov.widgets.Options;
import com.kniazkov.widgets.Page;
import com.kniazkov.widgets.Paragraph;
import com.kniazkov.widgets.Server;
import com.kniazkov.widgets.TextWidget;

/**
 * A web application that demonstrates the visual differences between font weights.
 * <p>
 *     For each standard {@link FontWeight}, a line of text is rendered with that weight applied.
 *     This allows visually comparing how different weights appear in the UI.
 * </p>
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
public class FontWeightExample {
    /**
     * Starting point.
     *
     * @param args Program arguments
     */
    public static void main(String[] args) {
        final Page page = root -> {
            final Paragraph p0 = root.createParagraph();
            p0.createTextWidget("default");

            for (final FontWeight weight : FontWeight.values()) {
                final Paragraph par = root.createParagraph();
                final TextWidget widget = par.createTextWidget(weight.toString());
                widget.setFontWeight(weight);
            }
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
