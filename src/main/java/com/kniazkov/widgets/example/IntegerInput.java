/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.model.DefaultIntegerModel;
import com.kniazkov.widgets.model.IntegerToStringModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

/**
 * Demonstrates integer input with validation feedback.
 * <p>
 * The example shows how to connect an {@link InputField} to an {@link Integer} model
 * using {@link IntegerToStringModel}, and how to use {@link Model#getValidFlagModel()}
 * to visually indicate invalid input.
 * </p>
 *
 * <b>How to use</b>
 * <ol>
 *   <li>Run this program.</li>
 *   <li>Open your browser and go to
 *       <a href="http://localhost:8000">http://localhost:8000</a>.</li>
 *   <li>Type any number — invalid input will highlight the field in orange.</li>
 * </ol>
 */
public class IntegerInput {
    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Page page = root -> {
            final Model<Integer> intModel = new DefaultIntegerModel();
            final Model<String> stringModel = new IntegerToStringModel(intModel);
            final Model<Boolean> validFlagModel = stringModel.getValidFlagModel();

            final Section inputSection = new Section();
            root.add(inputSection);
            inputSection.add(new TextWidget("Enter some number: "));
            InputField inputField = new InputField();
            inputSection.add(inputField);
            inputField.setTextModel(stringModel);
            validFlagModel.addListener(flag -> inputField.setBgColor(flag ? Color.WHITE : Color.ORANGE));

            final Section outputSection = new Section();
            root.add(outputSection);
            outputSection.add(new TextWidget("Integer model value: '"));
            final TextWidget echoText = new TextWidget();
            outputSection.add(echoText);
            echoText.setFontWeight(FontWeight.BOLD);
            echoText.setTextModel(new IntegerToStringModel(intModel));
            outputSection.add(new TextWidget("'"));
        };

        final Application app = new Application(page);
        final Options options = new Options();
        Server.start(app, options);
    }
}
