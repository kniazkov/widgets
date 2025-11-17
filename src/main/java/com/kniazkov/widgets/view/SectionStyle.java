/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.HorizontalAlignment;

/**
 * Style definition for {@link Section}.
 */
public class SectionStyle extends Style implements HasHorizontalAlignment {
    /**
     * The global default section style.
     */
    public static final SectionStyle DEFAULT = new SectionStyle();

    /**
     * Creates the default section style.
     */
    private SectionStyle() {
        this.setHorizontalAlignment(HorizontalAlignment.LEFT);
    }

    /**
     * Creates a new section style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public SectionStyle(final SectionStyle parent) {
        super(parent);
    }

    @Override
    public SectionStyle derive() {
        return new SectionStyle(this);
    }
}
