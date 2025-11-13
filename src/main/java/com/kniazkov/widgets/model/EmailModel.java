/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import java.util.regex.Pattern;

/**
 * A string model that stores an email address and validates its format.
 */
public final class EmailModel extends SingleThreadModel<String> {
    /**
     * Simple email validation pattern.
     * This is intentionally not RFC-5322-perfect, but good enough for UI validation.
     */
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * The address;
     */
    private String email;

    /**
     * Constructor.
     */
    public EmailModel() {
        this.email = "";
    }

    /**
     * Constructor.
     * @param email the address
     */
    public EmailModel(final String email) {
        this.email = email;
    }

    @Override
    public boolean isValid() {
        return EMAIL_PATTERN.matcher(this.email).matches();
    }

    @Override
    public String getData() {
        return this.email;
    }

    @Override
    public boolean setData(final String data) {
        if (this.email.equals(data)) {
            return false;
        }
        this.email = data;
        this.notifyListeners(data);
        return true;
    }

    @Override
    public Model<String> deriveWithData(final String data) {
        return new EmailModel(data);
    }
}
