/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.BoxShadow;
import com.kniazkov.widgets.common.BoxSizing;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.Cursor;
import com.kniazkov.widgets.images.SvgImageSource;
import java.util.Set;

/**
 * Style definition for {@link RadioButton}.
 */
public final class RadioButtonStyle extends Style implements HasColor, HasBgColor,
        HasAbsoluteWidth, HasAbsoluteHeight, HasMargin, HasSelectableImage, HasBoxShadow,
        HasCursor, HasTransition, HasBoxSizing, HasOpacity {
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
     * Image displayed by a selected radio button.
     */
    private static final String SELECTED_SVG =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\" "
            + "viewBox=\"0 0 24 24\">"
            + "<circle cx=\"12\" cy=\"12\" r=\"10.5\" fill=\"white\" "
            + "stroke=\"black\" stroke-width=\"1.5\"/>"
            + "<circle cx=\"12\" cy=\"12\" r=\"5\" fill=\"black\"/>"
            + "</svg>";

    /**
     * Image displayed by an unselected radio button.
     */
    private static final String UNSELECTED_SVG =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\" "
            + "viewBox=\"0 0 24 24\">"
            + "<circle cx=\"12\" cy=\"12\" r=\"10.5\" fill=\"none\" "
            + "stroke=\"black\" stroke-width=\"1.5\"/>"
            + "</svg>";

    /**
     * Global default radio-button style.
     */
    public static final RadioButtonStyle DEFAULT = new RadioButtonStyle();

    /**
     * Creates the default radio-button style.
     */
    private RadioButtonStyle() {
        this.setBoxShadow(BoxShadow.NONE);
        this.setBoxShadow(
            State.HOVERED,
            new BoxShadow(0, 0, 0, 3, new Color(37, 99, 235, 38))
        );
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
                return SELECTED_SVG;
            }
        });
        this.setUnselectedImageSource(new SvgImageSource() {
            @Override
            protected String getSvg() {
                return UNSELECTED_SVG;
            }
        });
    }

    /**
     * Creates a style inheriting from another radio-button style.
     *
     * @param parent parent style
     */
    public RadioButtonStyle(final RadioButtonStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public RadioButtonStyle derive() {
        return new RadioButtonStyle(this);
    }
}
