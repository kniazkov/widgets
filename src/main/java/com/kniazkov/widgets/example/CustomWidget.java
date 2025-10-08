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
import com.kniazkov.widgets.controller.TypedController;
import com.kniazkov.widgets.model.IntegerToStringModel;
import com.kniazkov.widgets.model.ModelBinding;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.HasTextInput;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

/**
 * A sample web application demonstrating a custom composite widget built from basic components.
 * <p>
 * The {@code MyWidget} class combines an {@link InputField} and a {@link Button}
 * into a reusable component that allows the user to enter text and confirm it.
 * When the button is clicked, the entered text is sent to the controller via
 * {@link HasTextInput#onTextInput(TypedController)}.
 * </p>
 *
 * <p>
 * In this example, the entered text is displayed in bold below the widget,
 * converted to uppercase. The additional section appears dynamically after the
 * first user interaction.
 * </p>
 *
 * <h3>How to use</h3>
 * <ol>
 *   <li>Run the program;</li>
 *   <li>
 *     Open your browser and go to
 *     <a href="http://localhost:8000">http://localhost:8000</a>;
 *   </li>
 *   <li>Type any text into the input field and click the button —
 *       the text will appear below in uppercase.</li>
 * </ol>
 */
public class CustomWidget {
    public static void main(String[] args) {
        final Page page = root -> {
            final MyWidget customWidget = new MyWidget("Enter text", "Ok");
            root.add(customWidget);

            final Section section = new Section();
            section.add(new TextWidget("You entered: "));
            final TextWidget textWidget = new TextWidget();
            section.add(textWidget);
            textWidget.setFontWeight(FontWeight.BOLD);

            customWidget.onTextInput(data -> {
                if (!section.getParent().isPresent()) {
                    root.add(section);
                }
                textWidget.setText(data.toUpperCase());
            });
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }

    /**
     * A simple composite widget consisting of a text caption, an input field, and a button.
     * <p>
     * This widget implements {@link HasTextInput}, allowing clients to register a
     * {@link TypedController TypedController&lt;String&gt;} that reacts to user input.
     * When the button is clicked, the text currently entered in the input field
     * is sent to the controller.
     * </p>
     *
     * <p>
     * The layout is horizontal:
     * <ul>
     *   <li>A caption label on the left;</li>
     *   <li>An input field in the center;</li>
     *   <li>A confirmation button on the right.</li>
     * </ul>
     * </p>
     *
     * <p>
     * The input field background is highlighted in {@link Color#YELLOW} to
     * emphasize interactivity.
     * </p>
     */
    static class MyWidget extends Section implements HasTextInput {
        private final InputField field;

        private TypedController<String> ctrl = TypedController.stub();

        MyWidget(final String caption, final String buttonText) {
            this.add(new TextWidget(caption + ": "));

            this.field = new InputField();
            this.add(field);
            this.field.setBgColor(Color.YELLOW);

            final Button button = new Button(buttonText);
            this.add(button);
            button.onClick(data -> ctrl.handleEvent(field.getText()));
        }

        @Override
        public ModelBinding<String> getTextModelBinding() {
            return this.field.getTextModelBinding();
        }

        @Override
        public void onTextInput(TypedController<String> ctrl) {
            this.ctrl = ctrl;
        }
    }
}
