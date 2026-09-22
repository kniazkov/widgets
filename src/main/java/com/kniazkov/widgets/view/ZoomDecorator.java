/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.protocol.ResetZoom;
import com.kniazkov.widgets.protocol.SetChild;
import java.util.Objects;

/**
 * A clipped inline viewport that magnifies any inline widget using pinch or mouse wheel.
 *
 * <p>Drag enlarged content to pan. Scale starts at 1 and is limited to 8 by default.
 * Zoom and pan are local presentation state and do not generate server traffic.
 * Replacing the child, changing the limit or calling {@link #resetZoom()} resets the view.
 * Enable {@link #setFitContent(boolean)} to fit and center the entire content initially.
 * In that mode scale 1 means the fitted view, and maxScale is relative to it.
 * The fit adapts to viewport and content size changes, including image loading.
 * Use explicit viewport dimensions when the content is larger than the available page area.</p>
 *
 * <p>Gestures consume touch movement inside the viewport. Interactive descendants still accept
 * ordinary clicks; clicks synthesized after a pan or pinch are suppressed.</p>
 */
public final class ZoomDecorator extends InlineWidget<ZoomDecoratorStyle>
        implements Decorator<InlineWidget<?>>, HasWidth, HasHeight, HasMargin,
        HasPadding, HasBgColor, HasBorder, HasBoxSizing, HasMaxScale, HasFitContent {
    /**
     * Decorated content.
     */
    private InlineWidget<?> child;

    /**
     * Returns the default viewport style.
     * @return default style
     */
    public static ZoomDecoratorStyle getDefaultStyle() {
        return ZoomDecoratorStyle.DEFAULT;
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
    public ZoomDecorator(final ZoomDecoratorStyle style, final InlineWidget<?> child) {
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
     * Restores the initial view (fitted and centered when fitContent is enabled).
     */
    public void resetZoom() {
        this.pushUpdate(new ResetZoom(this.getId()));
    }
}
