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
 * A web application that demonstrates....
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
public class ItalicExample {
    /**
     * Starting point.
     *
     * @param args Program arguments
     */
    public static void main(String[] args) {
        final Page page = root -> {
            final Paragraph p0 = root.createParagraph();
            p0.createTextWidget("Default");

            final Paragraph p1 = root.createParagraph();
            final TextWidget t1 = p1.createTextWidget("Italic");
            t1.setItalic(true);

            final Paragraph p2 = root.createParagraph();
            final TextWidget t2 = p2.createTextWidget("Bold and italic");
            t2.setFontWeight(FontWeight.BOLD);
            t2.setItalic(true);
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
