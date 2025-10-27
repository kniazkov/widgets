/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.ReadOnlyModel;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.util.Optional;

/**
 * A demo showing how to create a custom read-only model that transforms data from another model.
 *
 * <b>How to use</b>
 * <ol>
 *   <li>Run this program.</li>
 *   <li>
 *     Open your browser and go to
 *     <a href="http://localhost:8000">http://localhost:8000</a>.
 *   </li>
 *   <li>Type some text into the input field and watch it appear reversed in uppercase.</li>
 * </ol>
 */
public class CustomModel {
    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Page page = root -> {
            final Section inputSection = new Section();
            root.add(inputSection);

            inputSection.add(new TextWidget("Enter some text: "));
            InputField inputField = new InputField();
            inputSection.add(inputField);

            final Section outputSection = new Section();
            root.add(outputSection);

            outputSection.add(new TextWidget("You entered: '"));
            final TextWidget echoText = new TextWidget();
            //echoText.setFontWeight(FontWeight.BOLD);
            echoText.setTextModel(new MyModel(inputField.getTextModel()));
            outputSection.add(echoText);
            outputSection.add(new TextWidget("'"));
        };

        final Application app = new Application(page);
        final Options options = new Options();
        Server.start(app, options);
    }

    /**
     * A read-only model that mirrors another model's text,
     * transforming it to uppercase and reversed.
     */
    static class MyModel extends ReadOnlyModel<String> {
        private final Model<String> source;

        MyModel(final Model<String> source) {
            this.source = source;
            source.addListener(data -> this.notifyListeners());
        }

        @Override
        public boolean isValid() {
            return this.source.isValid();
        }

        @Override
        public String getData() {
            String text = this.source.getData();
            return new StringBuilder(text.trim().toUpperCase())
                    .reverse()
                    .toString();
        }
    }
}
