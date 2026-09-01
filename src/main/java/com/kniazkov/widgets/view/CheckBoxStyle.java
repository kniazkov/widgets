/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.BoxShadow;
import com.kniazkov.widgets.common.BoxSizing;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.Cursor;
import com.kniazkov.widgets.images.SvgImageSource;

import java.util.Set;

/**
 * Style definition for {@link CheckBox}.
 */
public class CheckBoxStyle extends Style implements HasColor, HasBgColor, HasAbsoluteWidth,
        HasAbsoluteHeight, HasMargin, HasSelectableImage, HasBoxShadow, HasCursor, HasTransition,
        HasBoxSizing, HasOpacity {
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
    private static final String CHECKED_SVG =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\" "
            + "viewBox=\"0 0 24 24\">"
            + "<rect x=\"1.5\" y=\"1.5\" width=\"21\" height=\"21\" rx=\"6\" "
            + "fill=\"white\" stroke=\"black\" stroke-width=\"1.5\"/>"
            + "<path d=\"M6 12.5l4 4L18 8\" fill=\"none\" stroke=\"white\" "
            + "stroke-width=\"2.4\" stroke-linecap=\"round\" "
            + "stroke-linejoin=\"round\"/>"
            + "</svg>";

    /**
     * Default image for unchecked checkbox.
     */
    private static final String UNCHECKED_SVG =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\" "
            + "viewBox=\"0 0 24 24\">"
            + "<rect x=\"1.5\" y=\"1.5\" width=\"21\" height=\"21\" rx=\"6\" "
            + "fill=\"none\" stroke=\"black\" stroke-width=\"1.5\"/>"
            + "</svg>";

    /**
     * The global default check box style.
     */
    public static final CheckBoxStyle DEFAULT = new CheckBoxStyle();

    /**
     * Creates the default text style.
     */
    private CheckBoxStyle() {
        this.setBoxShadow(BoxShadow.NONE);
        this.setBoxShadow(State.HOVERED,
            new BoxShadow(0, 0, 0, 3, new Color(37, 99, 235, 38)));
        this.setCursor(Cursor.POINTER);
        this.setCursor(State.DISABLED, Cursor.NOT_ALLOWED);
        this.setTransition(DefaultTheme.TRANSITION);
        this.setBoxSizing(BoxSizing.BORDER_BOX);
        this.setColor(State.NORMAL, DefaultTheme.PRIMARY_HOVER);
        this.setColor(State.HOVERED, DefaultTheme.PRIMARY_ACTIVE);
        this.setColor(State.ACTIVE, DefaultTheme.TEXT);
        this.setColor(State.DISABLED, DefaultTheme.MUTED);

        this.setBgColor(State.NORMAL, DefaultTheme.PRIMARY);
        this.setBgColor(State.HOVERED, DefaultTheme.PRIMARY_HOVER);
        this.setBgColor(State.ACTIVE, DefaultTheme.PRIMARY_ACTIVE);
        this.setBgColor(State.DISABLED, DefaultTheme.BORDER);

        this.setOpacity(State.NORMAL, 1.0);
        this.setOpacity(State.HOVERED, 1.0);
        this.setOpacity(State.ACTIVE, 1.0);
        this.setOpacity(State.DISABLED, 0.75);

        this.setWidth(24);
        this.setHeight(24);
        this.setMargin(2, 1);

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
