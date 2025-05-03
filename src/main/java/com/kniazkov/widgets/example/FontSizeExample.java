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
 * A web application that demonstrates the visual differences between font sizes.
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
public class FontSizeExample {
    /**
     * Starting point.
     *
     * @param args Program arguments
     */
    public static void main(String[] args) {
        final Page page = root -> {
            final Paragraph paragraph = root.createParagraph();
            for (int size = 4; size <= 48; size += 2) {
                final TextWidget widget = paragraph.createTextWidget("A");
                widget.setFontSize(String.format("%dpt", size));
            }
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
