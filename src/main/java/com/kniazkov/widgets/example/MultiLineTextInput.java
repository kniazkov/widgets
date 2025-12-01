/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextArea;
import com.kniazkov.widgets.view.TextWidget;

/**
 * A demonstration program showcasing the use of the {@link TextArea} widget.
 * <p>
 * This example creates a multi-line text input area and displays the entered text
 * in real-time below it. The text is processed to show newline characters as
 * visible symbols for demonstration purposes.
 *
 * <b>How to use</b>
 * <ol>
 *   <li>Run this program.</li>
 *   <li>
 *     Open your browser and go to
 *     <a href="http://localhost:8000">http://localhost:8000</a>.
 *   </li>
 *   <li>Type some text into the input field and watch it appear below.</li>
 * </ol>
 */
public class MultiLineTextInput {
    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Page page = (root, parameters) -> {
            final Section inputSection = new Section();
            root.add(inputSection);
            inputSection.add(new TextWidget("Enter some text: "));
            TextArea textArea = new TextArea();
            textArea.setWidth(300);
            textArea.setHeight(200);
            inputSection.add(textArea);

            final Section outputSection = new Section();
            root.add(outputSection);
            outputSection.add(new TextWidget("You entered: '"));
            final TextWidget echoText = new TextWidget();
            echoText.setFontWeight(FontWeight.BOLD);
            outputSection.add(echoText);
            outputSection.add(new TextWidget("'"));

            textArea.onTextInput(
                text -> echoText.setText(
                    text.trim().replace("\n", "§")
                )
            );
        };

        final Application app = new Application(page);
        final Options options = new Options();
        Server.start(app, options);
    }
}
