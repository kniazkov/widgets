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
    public static final ButtonStyle PRIMARY = createTintedStyle(
        DefaultTheme.BUTTON_PRIMARY,
        DefaultTheme.BUTTON_PRIMARY_HOVER,
        DefaultTheme.BUTTON_PRIMARY_ACTIVE,
        DefaultTheme.BORDER_STRONG,
        DefaultTheme.TEXT,
        DefaultTheme.TEXT
    );

    /**
     * Ready-to-use style for destructive actions.
     */
    public static final ButtonStyle DANGER = createTintedStyle(
        DefaultTheme.BUTTON_DANGER,
        DefaultTheme.BUTTON_DANGER_HOVER,
        DefaultTheme.BUTTON_DANGER_ACTIVE,
        DefaultTheme.DANGER_ACTIVE,
        DefaultTheme.DANGER,
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
     * Creates a softly tinted action style.
     *
     * @param normal normal background color
     * @param hovered hovered background color
     * @param active pressed background color
     * @param border normal border color
     * @param activeBorder hovered and pressed border color
     * @param text text color
     * @return tinted button style
     */
    private static ButtonStyle createTintedStyle(
        final Color normal,
        final Color hovered,
        final Color active,
        final Color border,
        final Color activeBorder,
        final Color text
    ) {
        final ButtonStyle style = DEFAULT.derive();
        style.setBgColor(State.NORMAL, normal);
        style.setBgColor(State.HOVERED, hovered);
        style.setBgColor(State.FOCUSED, normal);
        style.setBgColor(State.ACTIVE, active);
        style.setBgColor(State.DISABLED, DefaultTheme.SURFACE_MUTED);

        style.setBorderColor(State.NORMAL, border);
        style.setBorderColor(State.HOVERED, activeBorder);
        style.setBorderColor(State.FOCUSED, DefaultTheme.PRIMARY);
        style.setBorderColor(State.ACTIVE, activeBorder);
        style.setBorderColor(State.DISABLED, DefaultTheme.BORDER);

        style.setBoxShadow(State.NORMAL,
            new BoxShadow(0, 2, 5, new Color(15, 23, 42, 20)));
        style.setBoxShadow(State.HOVERED,
            new BoxShadow(0, 4, 10, new Color(15, 23, 42, 32)));
        style.setBoxShadow(State.ACTIVE,
            new BoxShadow(0, 1, 3, new Color(15, 23, 42, 28)));

        style.textStyle.setColor(text);
        style.textStyle.setFontSize("14px");
        style.textStyle.setFontWeight(FontWeight.SEMIBOLD);
        return style;
    }
}
