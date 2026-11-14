/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Visual layout for direct {@link FormField} children, measured in its own container.
 * Alone this is presentation only; use it in a {@link Form} to add validation and submission.
 */
public final class FormLayout extends Panel {
    /**
     * Creates a form layout that aligns fields at the default breakpoint.
     */
    public FormLayout() {
    }

    /**
     * Sets the minimum available content width for two columns.
     * @param pixels breakpoint in CSS pixels (240–2048), inclusive
     */
    public void setBreakpoint(final int pixels) {
        if (pixels < 240 || pixels > 2048) {
            throw new IllegalArgumentException("Invalid form layout breakpoint");
        }
        this.getModel(State.ANY, Property.FORM_BREAKPOINT).setData(pixels);
    }

    /**
     * Changes vertical field spacing and the horizontal label/control gap.
     * @param pixels gap in CSS pixels (0–128)
     */
    public void setGap(final int pixels) {
        LayoutSizes.gap(this, pixels);
    }

    @Override
    public String getType() {
        return "form layout";
    }
}
