/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Style definition for {@link SortableSection}, including its reactive gesture settings.
 */
public class SortableSectionStyle extends SectionStyle implements HasAnimationDuration {
    /**
     * Global default style with animationDuration set to 250.
     */
    public static final SortableSectionStyle DEFAULT = new SortableSectionStyle();

    /**
     * Creates the default style with the base widget's layout and appearance.
     */
    private SortableSectionStyle() {
        super(Section.getDefaultStyle());
        this.setAnimationDuration(250);
    }

    /**
     * Creates a style whose models inherit reactively from the specified parent.
     * @param parent parent style
     */
    public SortableSectionStyle(final SortableSectionStyle parent) {
        super(parent);
    }

    @Override
    public SortableSectionStyle derive() {
        return new SortableSectionStyle(this);
    }
}
