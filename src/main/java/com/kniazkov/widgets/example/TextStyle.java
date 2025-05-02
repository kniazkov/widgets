/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.Application;
import com.kniazkov.widgets.Options;
import com.kniazkov.widgets.Page;
import com.kniazkov.widgets.Paragraph;
import com.kniazkov.widgets.Server;
import com.kniazkov.widgets.TextWidget;

/**
 * A web application that demonstrates text style changes.
 *
 * <p>
 * How to use:
 * <ol>
 *     <li>Run the program;</li>
 *     <li>
 *         Open your browser and go to
 *         <a href="http://localhost:8000">http://localhost:8000</a>.
 *     </li>
 * </ol>
 */
public class TextStyle {
    /**
     * Starting point.
     *
     * @param args Program arguments
     */
    public static void main(String[] args) {
        final Page page = root -> {
            final Paragraph paragraph = root.createParagraph();
            final TextWidget textWidget = paragraph.createTextWidget("It works.");
            textWidget.setFontFace("Times New Roman");
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
