/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.BoxShadow;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.Outline;
import com.kniazkov.widgets.common.TimingFunction;
import com.kniazkov.widgets.common.Transition;

/**
 * Shared visual values used by the default styles of visible widgets.
 */
final class DefaultTheme {
    /**
     * Default system-oriented font stack.
     */
    static final FontFace FONT = () -> "Arial, sans-serif";

    /**
     * Main text color.
     */
    static final Color TEXT = new Color(15, 23, 42);

    /**
     * Secondary text color.
     */
    static final Color MUTED = new Color(71, 85, 105);

    /**
     * Primary accent color.
     */
    static final Color PRIMARY = new Color(37, 99, 235);

    /**
     * Primary hover color.
     */
    static final Color PRIMARY_HOVER = new Color(29, 78, 216);

    /**
     * Primary pressed color.
     */
    static final Color PRIMARY_ACTIVE = new Color(30, 64, 175);

    /**
     * Error color.
     */
    static final Color DANGER = new Color(220, 38, 38);

    /**
     * Error hover color.
     */
    static final Color DANGER_HOVER = new Color(185, 28, 28);

    /**
     * Error pressed color.
     */
    static final Color DANGER_ACTIVE = new Color(153, 27, 27);

    /**
     * Regular border color.
     */
    static final Color BORDER = new Color(203, 213, 225);

    /**
     * Strong border color.
     */
    static final Color BORDER_STRONG = new Color(100, 116, 139);

    /**
     * Subtle neutral background.
     */
    static final Color SURFACE_MUTED = new Color(241, 245, 249);

    /**
     * Disabled background.
     */
    static final Color SURFACE_DISABLED = new Color(226, 232, 240);

    /**
     * Subtle blue background.
     */
    static final Color SURFACE_BLUE = new Color(239, 246, 255);

    /**
     * Subtle red background.
     */
    static final Color SURFACE_RED = new Color(254, 242, 242);

    /**
     * Focus outline shared by native focusable controls.
     */
    static final Outline FOCUS_OUTLINE = new Outline(
        new Color(96, 165, 250), BorderStyle.SOLID, 2, 1
    );

    /**
     * Focus ring shared by native focusable controls.
     */
    static final BoxShadow FOCUS_SHADOW = new BoxShadow(
        0, 0, 0, 3, new Color(37, 99, 235, 38)
    );

    /**
     * Default short interaction transition.
     */
    static final Transition TRANSITION = new Transition(140, TimingFunction.EASE_OUT);

    /**
     * Prevents instantiation.
     */
    private DefaultTheme() {
    }
}
