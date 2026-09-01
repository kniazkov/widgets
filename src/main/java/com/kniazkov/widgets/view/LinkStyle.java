/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import java.util.Set;

/**
 * Style definition for {@link Link}.
 */
public class LinkStyle extends Style implements HasStyledText, HasColor, HasCursor,
        HasTransition {
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(
        State.NORMAL,
        State.HOVERED,
        State.FOCUSED,
        State.ACTIVE
    );

    /**
     * The global default link style.
     */
    public static final LinkStyle DEFAULT = new LinkStyle();

    /**
     * Creates the default link style from the interactive text style.
     */
    private LinkStyle() {
        super(ActiveTextStyle.DEFAULT);
        this.setCursorModel(State.FOCUSED, this.getCursorModel(State.NORMAL).asCascading());
        this.setFontFaceModel(State.FOCUSED, this.getFontFaceModel(State.NORMAL).asCascading());
        this.setFontSizeModel(State.FOCUSED, this.getFontSizeModel(State.NORMAL).asCascading());
        this.setFontWeightModel(
            State.FOCUSED,
            this.getFontWeightModel(State.NORMAL).asCascading()
        );
        this.setItalicModel(State.FOCUSED, this.getItalicModel(State.NORMAL).asCascading());
        this.setColorModel(State.FOCUSED, this.getColorModel(State.HOVERED).asCascading());
    }

    /**
     * Creates a new link style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public LinkStyle(final LinkStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public LinkStyle derive() {
        return new LinkStyle(this);
    }
}
