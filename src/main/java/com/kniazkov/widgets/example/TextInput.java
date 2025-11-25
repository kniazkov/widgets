/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.State;
import com.kniazkov.widgets.view.TextWidget;

/**
 * A minimal demo application that shows how to handle text input
 * and display user-entered text in real time.
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
public class TextInput {
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
            InputField inputField = new InputField();
            inputField.setFontWeight(State.ACTIVE, FontWeight.BLACK);
            inputSection.add(inputField);

            final Section outputSection = new Section();
            root.add(outputSection);
            outputSection.add(new TextWidget("You entered: '"));
            final TextWidget echoText = new TextWidget();
            echoText.setFontWeight(FontWeight.BOLD);
            echoText.setItalic(true);
            echoText.setFontSize(new FontSize("14pt"));
            outputSection.add(echoText);
            outputSection.add(new TextWidget("'"));

            inputField.onTextInput(text -> echoText.setText(text.trim()));
        };

        final Application app = new Application(page);
        final Options options = new Options();
        Server.start(app, options);
    }
}
