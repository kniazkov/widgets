/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.Controller;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.controller.ReorderEvent;
import com.kniazkov.widgets.protocol.AppendChild;
import com.kniazkov.widgets.protocol.ConfigureSorting;
import com.kniazkov.widgets.protocol.RemoveChild;
import com.kniazkov.widgets.protocol.SetChildOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A wrapping section whose inline children can be reordered with a mouse or finger.
 *
 * <p>Drag a child to change its position. Interactive descendants (buttons, links and inputs)
 * keep their normal behavior; drag the surrounding card instead. Touch gestures started on
 * a card belong to this container, so scroll the page outside the cards.</p>
 *
 * <p>The server validates versioned moves before calling {@link #onReorder(Controller)}.
 * Programmatic moves do not invoke that controller. Child identities and models survive moves.</p>
 *
 * <p>The dragged child follows the pointer above its siblings without changing their layout.
 * On release, children animate into their new positions; {@link #setAnimationDuration(int)}
 * controls the settling duration (250 milliseconds by default).</p>
 */
public final class SortableSection extends BlockWidget<SectionStyle>
        implements TypedContainer<InlineWidget<?>>, HasHorizontalAlignment,
        HasVerticalAlignment, HasMargin, HasPadding, HasHiddenState {
    /**
     * Ordered children.
     */
    private final List<InlineWidget<?>> children = new ArrayList<>();

    /**
     * Revision incremented whenever membership or order changes.
     */
    private int revision;

    /**
     * Duration of drop and reorder animations in milliseconds.
     */
    private int animationDuration = 250;

    /**
     * Returns the default section style.
     * @return default style
     */
    public static SectionStyle getDefaultStyle() {
        return Section.getDefaultStyle();
    }

    /**
     * Creates a sortable section.
     * @param children initial inline widgets
     */
    public SortableSection(final InlineWidget<?>... children) {
        this(getDefaultStyle(), children);
    }

    /**
     * Creates a styled sortable section.
     * @param style section style
     * @param children initial inline widgets
     */
    public SortableSection(final SectionStyle style, final InlineWidget<?>... children) {
        super(Objects.requireNonNull(style, "style"));
        for (final InlineWidget<?> child : children) {
            this.add(child);
        }
    }

    @Override
    public String getType() {
        return "sortable section";
    }

    @Override
    public int getChildCount() {
        return this.children.size();
    }

    @Override
    public InlineWidget<?> getChild(final int index) {
        return this.children.get(index);
    }

    /**
     * Returns an immutable snapshot suitable for persisting the new order.
     * @return current child order
     */
    public List<InlineWidget<?>> getChildren() {
        return List.copyOf(this.children);
    }

    @Override
    public void add(final InlineWidget<?> widget) {
        Objects.requireNonNull(widget, "widget");
        if (widget instanceof Container container
            && container.collectChildren(Widget.class).contains(this)) {
            throw new IllegalArgumentException("A section cannot contain an ancestor");
        }
        if (this.children.contains(widget)) {
            return;
        }
        widget.remove();
        this.children.add(widget);
        widget.setParent(this);
        this.pushUpdate(new AppendChild(widget.getId(), this.getId()));
        this.revision++;
        this.publishOrder();
    }

    @Override
    public void remove(final Widget<?> widget) {
        if (this.children.remove(widget)) {
            this.pushUpdate(new RemoveChild(widget.getId(), this.getId()));
            widget.setParent(null);
            this.revision++;
            this.publishOrder();
        }
    }

    /**
     * Moves a child to a final zero-based position without recreating it.
     * @param from original index
     * @param to final index
     */
    public void move(final int from, final int to) {
        Objects.checkIndex(from, this.children.size());
        Objects.checkIndex(to, this.children.size());
        if (from != to) {
            this.children.add(to, this.children.remove(from));
            this.revision++;
            this.publishOrder();
        }
    }

    /**
     * Returns the duration of drop and reorder animations.
     * @return duration in milliseconds; defaults to 250
     */
    public int getAnimationDuration() {
        return this.animationDuration;
    }

    /**
     * Sets the duration of drop, cancellation and reorder animations.
     * The dragged widget always follows the pointer immediately. Zero disables settling
     * animations; browsers requesting reduced motion also settle immediately.
     * @param milliseconds nonnegative animation duration
     */
    public void setAnimationDuration(final int milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException("Animation duration must be nonnegative");
        }
        this.animationDuration = milliseconds;
        this.pushUpdate(new ConfigureSorting(this.getId(), milliseconds));
    }

    /**
     * Registers a callback invoked after a valid browser move has changed the server order.
     * @param controller receives an immutable snapshot of the new order
     */
    public void onReorder(final Controller<List<InlineWidget<?>>> controller) {
        Objects.requireNonNull(controller, "controller");
        this.setController(Event.REORDER, data -> controller.handleEvent(this.getChildren()));
    }

    /**
     * Validates a browser request; stale or invalid requests only resynchronize the browser.
     * @param event requested move
     * @return whether a real move was accepted
     */
    public boolean acceptReorder(final ReorderEvent event) {
        int from = -1;
        int before = "".equals(event.before) ? this.children.size() : -1;
        for (int index = 0; index < this.children.size(); index++) {
            final String id = this.children.get(index).getId().toString();
            if (id.equals(event.child)) {
                from = index;
            }
            if (id.equals(event.before)) {
                before = index;
            }
        }
        if (event.revision != this.revision || from < 0 || before < 0 || from == before) {
            this.publishOrder();
            return false;
        }
        final int to = before > from ? before - 1 : before;
        if (from == to) {
            this.publishOrder();
            return false;
        }
        this.move(from, to);
        return true;
    }

    /**
     * Sends the current order and revision without recreating any child.
     */
    private void publishOrder() {
        this.pushUpdate(new SetChildOrder(this.getId(), this.children.stream()
            .map(child -> child.getId().toString()).toList(), this.revision));
    }
}
