/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.images.SvgImageSource;

import java.util.Set;

/**
 * Style definition for {@link CheckBox}.
 */
public class CheckBoxStyle extends Style implements HasColor, HasBgColor, HasAbsoluteWidth, HasAbsoluteHeight,
        HasSelectableImage
{
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(
            State.NORMAL,
            State.HOVERED,
            State.ACTIVE,
            State.DISABLED
    );

    /**
     * Default image for checked checkbox.
     */
    private static final String CHECKED_SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"64\" height=\"64\" viewBox=\"0 0 64 64\" fill=\"none\"><rect x=\"10\" y=\"10\" width=\"44\" height=\"44\" rx=\"6\" fill=\"white\" stroke=\"black\" stroke-width=\"4\"/><path d=\"M18 34 L28 44 L46 22\" stroke=\"black\" stroke-width=\"4\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/></svg>";

    /**
     * Default image for unchecked checkbox.
     */
    private static final String UNCHECKED_SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"64\" height=\"64\" viewBox=\"0 0 64 64\" fill=\"none\"><rect x=\"10\" y=\"10\" width=\"44\" height=\"44\" rx=\"6\" fill=\"white\" stroke=\"black\" stroke-width=\"4\"/></svg>";

    /**
     * The global default check box style.
     */
    public static final CheckBoxStyle DEFAULT = new CheckBoxStyle();

    /**
     * Creates the default text style.
     */
    private CheckBoxStyle() {
        this.setColor(State.NORMAL, Color.BLACK);
        this.setColor(State.HOVERED, Color.BLACK);
        this.setColor(State.ACTIVE, Color.BLACK);
        this.setColor(State.DISABLED, Color.GRAY);

        this.setBgColor(State.NORMAL, Color.WHITE);
        this.setBgColor(State.HOVERED, new Color(224, 224, 224));
        this.setBgColor(State.ACTIVE, new Color(255, 255, 230));
        this.setBgColor(State.DISABLED, Color.LIGHT_GRAY);

        this.setWidth(24);
        this.setHeight(24);

        this.setSelectedImageSource(new SvgImageSource() {
            @Override
            protected String getSvg() {
                return CHECKED_SVG;
            }
        });
        this.setUnselectedImageSource(new SvgImageSource() {
            @Override
            protected String getSvg() {
                return UNCHECKED_SVG;
            }
        });
    }

    /**
     * Creates a new text style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public CheckBoxStyle(final CheckBoxStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public CheckBoxStyle derive() {
        return new CheckBoxStyle(this);
    }
}
