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
import com.kniazkov.widgets.model.SingleThreadModel;
import com.kniazkov.widgets.view.PasswordInput;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

/**
 * A minimal demo application that shows how to use {@link PasswordInput}
 * with a custom password validation model.
 *
 * <p>The example binds a {@link PasswordModel} to the password field and
 * displays the entered value below the field in real time.
 *
 * <b>How to use</b>
 * <ol>
 *   <li>Run this program.</li>
 *   <li>
 *     Open your browser and go to
 *     <a href="http://localhost:8000">http://localhost:8000</a>.
 *   </li>
 *   <li>Type a password into the input field and watch it appear below.</li>
 * </ol>
 */
public class PasswordField {
    /**
     * Creates the example.
     */
    public PasswordField() {
    }

    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Page page = (root, parameters) -> {
            Section section = new Section();
            root.add(section);
            section.add(new TextWidget("Enter password:"));
            final PasswordInput field = new PasswordInput();
            section.add(field);
            field.setWidth("150px");
            final Model<String> model = new PasswordModel();
            field.setTextModel(model);

            section = new Section();
            root.add(section);
            section.add(new TextWidget("You entered: '"));
            final TextWidget echo = new TextWidget();
            echo.setFontWeight(FontWeight.BOLD);
            section.add(echo);
            section.add(new TextWidget("'"));

            field.onTextInput(text -> echo.setText(text.trim()));
        };

        final Application app = new Application(page);
        final Options options = new Options.Builder().build();
        Server.start(app, options);
    }

    /**
     * A single-threaded text model that stores and validates password data.
     *
     * <p>The password is valid when it is at least six characters long,
     * contains only Latin letters and digits, and includes at least one
     * uppercase letter, one lowercase letter, and one digit.
     */
    static final class PasswordModel extends SingleThreadModel<String> {
        /**
         * Stored password value.
         */
        String password;

        /**
         * Creates a new password model with an empty password.
         */
        public PasswordModel() {
            this.password = "";
        }

        /**
         * Creates a new password model with the specified initial password.
         *
         * @param data the initial password value
         */
        public PasswordModel(final String data) {
            this.password  = data;
        }

        /**
         * Checks whether the stored password satisfies the validation rules.
         *
         * @return {@code true} if the password is valid; {@code false} otherwise
         */
        @Override
        public boolean isValid() {
            if (this.password.length() < 6) {
                return false;
            }

            boolean hasUppercase = false;
            boolean hasLowercase = false;
            boolean hasDigit = false;

            for (int i = 0; i < this.password.length(); i++) {
                char ch = this.password.charAt(i);

                if (ch >= 'A' && ch <= 'Z') {
                    hasUppercase = true;
                } else if (ch >= 'a' && ch <= 'z') {
                    hasLowercase = true;
                } else if (ch >= '0' && ch <= '9') {
                    hasDigit = true;
                } else {
                    return false;
                }
            }

            return hasUppercase && hasLowercase && hasDigit;
        }

        @Override
        public String getData() {
            return this.password;
        }

        @Override
        public boolean setData(final String data) {
            if (this.password.equals(data)) {
                return false;
            }
            this.password = data;
            this.notifyListeners(data);
            return true;
        }

        @Override
        public Model<String> deriveWithData(final String data) {
            return new PasswordModel(data);
        }
    }
}
