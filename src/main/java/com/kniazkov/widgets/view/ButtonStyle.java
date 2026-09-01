/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.BoxShadow;
import com.kniazkov.widgets.common.BoxSizing;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.Cursor;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.common.Outline;
import java.util.Set;

/**
 * Style definition for {@link Button}.
 */
public class ButtonStyle extends Style implements HasBgColor, HasBorder, HasAbsoluteWidth,
        HasAbsoluteHeight, HasMargin, HasPadding, HasHiddenState, HasBoxShadow, HasOutline,
        HasCursor, HasTransition, HasBoxSizing {
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(
        State.NORMAL,
        State.HOVERED,
        State.FOCUSED,
        State.ACTIVE,
        State.DISABLED
    );

    /**
     * The global default button style.
     */
    public static final ButtonStyle DEFAULT = new ButtonStyle();

    /**
     * Ready-to-use style for the main action on a page or form.
     */
    public static final ButtonStyle PRIMARY = createColoredStyle(
        DefaultTheme.PRIMARY,
        DefaultTheme.PRIMARY_HOVER,
        DefaultTheme.PRIMARY_ACTIVE
    );

    /**
     * Ready-to-use style for destructive actions.
     */
    public static final ButtonStyle DANGER = createColoredStyle(
        DefaultTheme.DANGER,
        DefaultTheme.DANGER_HOVER,
        DefaultTheme.DANGER_ACTIVE
    );

    /**
     * Style applied to text children created through the button API.
     */
    private final TextWidgetStyle textStyle;

    /**
     * Creates the default button style.
     */
    private ButtonStyle() {
        this.setBoxShadow(BoxShadow.NONE);
        this.setBoxShadow(State.HOVERED,
            new BoxShadow(0, 3, 8, new Color(15, 23, 42, 32)));
        this.setBoxShadow(State.FOCUSED, DefaultTheme.FOCUS_SHADOW);
        this.setOutline(Outline.NONE);
        this.setOutline(State.FOCUSED, DefaultTheme.FOCUS_OUTLINE);
        this.setCursor(Cursor.POINTER);
        this.setCursor(State.DISABLED, Cursor.NOT_ALLOWED);
        this.setTransition(DefaultTheme.TRANSITION);
        this.setBoxSizing(BoxSizing.BORDER_BOX);
        this.setBgColor(State.NORMAL, Color.WHITE);
        this.setBgColor(State.HOVERED, DefaultTheme.SURFACE_MUTED);
        this.setBgColor(State.FOCUSED, Color.WHITE);
        this.setBgColor(State.ACTIVE, DefaultTheme.SURFACE_DISABLED);
        this.setBgColor(State.DISABLED, DefaultTheme.SURFACE_MUTED);

        this.setBorderColor(State.NORMAL, DefaultTheme.BORDER_STRONG);
        this.setBorderColor(State.HOVERED, DefaultTheme.TEXT);
        this.setBorderColor(State.FOCUSED, DefaultTheme.PRIMARY);
        this.setBorderColor(State.ACTIVE, DefaultTheme.BORDER_STRONG);
        this.setBorderColor(State.DISABLED, DefaultTheme.BORDER);

        this.setBorderStyle(BorderStyle.SOLID);

        this.setBorderWidth(1);
        this.setBorderRadius(8);

        this.setWidth(AbsoluteSize.UNDEFINED);
        this.setHeight(40);
        this.setMargin(2);
        this.setPadding(16, 8);
        this.textStyle = TextWidget.getDefaultStyle().derive();
    }

    /**
     * Creates a new button style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public ButtonStyle(final ButtonStyle parent) {
        super(parent);
        this.textStyle = parent.textStyle.derive();
    }

    /**
     * Returns the style used for text children created through the button API.
     *
     * @return default text style associated with this button style
     */
    public TextWidgetStyle getDefaultTextStyle() {
        return this.textStyle;
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public ButtonStyle derive() {
        return new ButtonStyle(this);
    }

    /**
     * Creates a ready-to-use colored action style.
     *
     * @param normal normal background and border color
     * @param hovered hovered background and border color
     * @param active pressed background and border color
     * @return colored button style
     */
    private static ButtonStyle createColoredStyle(final Color normal, final Color hovered,
                                                  final Color active) {
        final ButtonStyle style = DEFAULT.derive();
        style.setBgColor(State.NORMAL, normal);
        style.setBgColor(State.HOVERED, hovered);
        style.setBgColor(State.FOCUSED, normal);
        style.setBgColor(State.ACTIVE, active);
        style.setBgColor(State.DISABLED, DefaultTheme.MUTED);

        style.setBorderColor(State.NORMAL, normal);
        style.setBorderColor(State.HOVERED, hovered);
        style.setBorderColor(State.FOCUSED, normal);
        style.setBorderColor(State.ACTIVE, active);
        style.setBorderColor(State.DISABLED, DefaultTheme.MUTED);

        style.setBoxShadow(State.NORMAL,
            new BoxShadow(0, 2, 5, new Color(15, 23, 42, 28)));
        style.setBoxShadow(State.HOVERED,
            new BoxShadow(0, 5, 12, new Color(15, 23, 42, 40)));
        style.setBoxShadow(State.ACTIVE,
            new BoxShadow(0, 1, 3, new Color(15, 23, 42, 35)));

        style.textStyle.setColor(Color.WHITE);
        style.textStyle.setFontSize("14px");
        style.textStyle.setFontWeight(FontWeight.SEMIBOLD);
        return style;
    }
}
