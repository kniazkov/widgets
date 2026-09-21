/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.IntrinsicSize;
import com.kniazkov.widgets.model.Model;

/**
 * Base class for all image widgets.
 *
 * @param <S> Widget style
 */
public abstract class BaseImageWidget<S extends Style> extends InlineWidget<S>
        implements HasBorder, HasMargin, HasAbsoluteWidth, HasAbsoluteHeight, HasOpacity,
        HasBoxShadow, HasCursor, HasTransition, HasBoxSizing {
    /**
     * Creates a new image widget.
     *
     * @param style Widget style
     */
    public BaseImageWidget(final S style) {
        super(style);
    }

    /**
     * Reserves the image box before its bytes arrive using HTML width/height attributes.
     * CSS size limits still apply; the image is contained in the box without distortion.
     * Supply dimensions from trusted image metadata, not the configured maximum limits.
     *
     * @param width original width in pixels, positive
     * @param height original height in pixels, positive
     */
    public void setIntrinsicSize(final int width, final int height) {
        this.setIntrinsicSize(new IntrinsicSize(width, height));
    }

    /**
     * @return the model storing original image dimensions
     */
    public Model<IntrinsicSize> getIntrinsicSizeModel() {
        return this.getModel(State.ANY, Property.INTRINSIC_SIZE);
    }

    /**
     * @param model replacement model for original dimensions
     */
    public void setIntrinsicSizeModel(final Model<IntrinsicSize> model) {
        this.setModel(State.ANY, Property.INTRINSIC_SIZE, model);
    }

    /**
     * @return original dimensions, or IntrinsicSize.NONE
     */
    public IntrinsicSize getIntrinsicSize() {
        return this.getIntrinsicSizeModel().getData();
    }

    /**
     * @param size original dimensions, or IntrinsicSize.NONE to remove the size hints
     */
    public void setIntrinsicSize(final IntrinsicSize size) {
        this.getIntrinsicSizeModel().setData(size);
    }

    /**
     * Removes the reserved size and lets the browser discover the image dimensions again.
     */
    public void clearIntrinsicSize() {
        this.setIntrinsicSize(IntrinsicSize.NONE);
    }
}
