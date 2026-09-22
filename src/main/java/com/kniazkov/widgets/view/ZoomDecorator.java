/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.protocol.ConfigureZoom;
import com.kniazkov.widgets.protocol.SetChild;
import java.util.Objects;

/**
 * A clipped inline viewport that magnifies any inline widget using pinch or mouse wheel.
 *
 * <p>Drag enlarged content to pan. Scale starts at 1 and is limited to 8 by default.
 * Zoom and pan are local presentation state and do not generate server traffic.
 * Replacing the child, changing the limit or calling {@link #resetZoom()} resets the view.
 * Use explicit viewport dimensions when the content is larger than the available page area.</p>
 *
 * <p>Gestures consume touch movement inside the viewport. Interactive descendants still accept
 * ordinary clicks; clicks synthesized after a pan or pinch are suppressed.</p>
 */
public final class ZoomDecorator extends InlineWidget<InlineBlockStyle>
        implements Decorator<InlineWidget<?>>, HasWidth, HasHeight, HasMargin,
        HasPadding, HasBgColor, HasBorder, HasBoxSizing {
    /**
     * Decorated content.
     */
    private InlineWidget<?> child;

    /**
     * Maximum permitted magnification.
     */
    private double maxScale = 8;

    /**
     * Returns the default viewport style.
     * @return default style
     */
    public static InlineBlockStyle getDefaultStyle() {
        return InlineBlock.getDefaultStyle();
    }

    /**
     * Creates an empty viewport.
     */
    public ZoomDecorator() {
        this(new TextWidget());
    }

    /**
     * Wraps an inline widget.
     * @param child content
     */
    public ZoomDecorator(final InlineWidget<?> child) {
        this(getDefaultStyle(), child);
    }

    /**
     * Creates a styled viewport.
     * @param style viewport style
     * @param child content
     */
    public ZoomDecorator(final InlineBlockStyle style, final InlineWidget<?> child) {
        super(Objects.requireNonNull(style, "style"));
        this.put(child);
    }

    @Override
    public String getType() {
        return "zoom decorator";
    }

    @Override
    public InlineWidget<?> getChild() {
        return this.child;
    }

    @Override
    public void put(final InlineWidget<?> widget) {
        Objects.requireNonNull(widget, "widget");
        if (widget == this || (widget instanceof Container container
            && container.collectChildren(Widget.class).contains(this))) {
            throw new IllegalArgumentException("A decorator cannot contain itself or an ancestor");
        }
        if (this.child == widget) {
            return;
        }
        widget.remove();
        final InlineWidget<?> old = this.child;
        this.child = null;
        if (old != null) {
            old.setParent(null);
        }
        this.child = widget;
        widget.setParent(this);
        this.pushUpdate(new SetChild(widget.getId(), this.getId()));
    }

    @Override
    public void remove(final Widget<?> widget) {
        if (widget != null && widget == this.child) {
            this.put(new TextWidget());
        }
    }

    /**
     * Returns the scale limit.
     * @return maximum scale
     */
    public double getMaxScale() {
        return this.maxScale;
    }

    /**
     * Changes the scale limit and resets the viewport.
     * @param scale finite maximum scale, at least 1
     */
    public void setMaxScale(final double scale) {
        if (!Double.isFinite(scale) || scale < 1) {
            throw new IllegalArgumentException("Maximum scale must be finite and at least 1");
        }
        this.maxScale = scale;
        this.resetZoom();
    }

    /**
     * Restores scale 1 and the initial content position in the browser.
     */
    public void resetZoom() {
        this.pushUpdate(new ConfigureZoom(this.getId(), this.maxScale));
    }
}
