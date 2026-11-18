/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.protocol.SetSplitPane;
import java.util.Objects;

/**
 * Two ordered areas separated by a focusable handle. The model expresses the first area's
 * percentage of the space available after reserving the handle. The browser previews drags
 * locally and commits on release. A narrow horizontal container switches to stacked areas.
 */
public final class SplitPane extends Panel implements HasDisabledState {
    /**
     * Direction in which the two children are laid out.
     */
    public enum Axis {
        /**
         * Side by side, switching to stacked areas below 480 CSS pixels.
         */
        HORIZONTAL,
        /**
         * Top and bottom at every width.
         */
        VERTICAL
    }

    /**
     * Preferred layout axis.
     */
    private final Axis axis;
    /**
     * Accessible name of the resize handle.
     */
    private final String label;
    /**
     * Lowest allowed first-area percentage.
     */
    private final int minimum;
    /**
     * Highest allowed first-area percentage.
     */
    private final int maximum;
    /**
     * Application-owned position model.
     */
    private final Model<Integer> position;
    /**
     * Strong reference required by the weak-listener model contract.
     */
    private final Listener<Integer> changed = ignored -> this.publish();

    /**
     * @param axis preferred layout direction
     * @param label accessible handle name identifying the primary area
     * @param minimum lower percentage in 0..100
     * @param maximum upper percentage, strictly greater than minimum and at most 100
     * @param position application-owned model, reusable by another page or pane
     * @param first primary content
     * @param second secondary content
     */
    @SuppressWarnings("this-escape")
    public SplitPane(final Axis axis, final String label, final int minimum, final int maximum,
            final Model<Integer> position, final BlockWidget<?> first,
            final BlockWidget<?> second) {
        this.axis = Objects.requireNonNull(axis);
        this.label = Objects.requireNonNull(label);
        if (label.isBlank() || minimum < 0 || minimum >= maximum || maximum > 100) {
            throw new IllegalArgumentException("Expected a handle name and valid percent limits");
        }
        this.minimum = minimum;
        this.maximum = maximum;
        this.position = Objects.requireNonNull(position);
        this.validateChildren(new BlockWidget<?>[]{first, second});
        super.add(first);
        super.add(second);
        this.position.addListener(this.changed);
        this.onRetire(() -> this.position.removeListener(this.changed));
        this.subscribeToEvent(Event.SPLIT_RESIZE);
        this.publish();
    }

    @Override
    public String getType() {
        return "split pane";
    }

    @Override
    public void add(final BlockWidget<?> child) {
        if (this.getChildCount() >= 2) {
            throw new IllegalStateException("Both split pane areas are occupied");
        }
        super.add(child);
        this.publish();
    }

    @Override
    public void remove(final Widget<?> child) {
        final int count = this.getChildCount();
        super.remove(child);
        if (count != this.getChildCount()) {
            this.publish();
        }
    }

    /**
     * @return the original application model
     */
    public Model<Integer> getPositionModel() {
        return this.position;
    }

    /**
     * @return effective percentage after applying this pane's bounds
     */
    public int getPosition() {
        final Integer value = this.position.getData();
        return value == null ? this.minimum : Math.clamp(value, this.minimum, this.maximum);
    }

    /**
     * @param value requested whole percentage, clamped to this pane's range
     */
    public void setPosition(final int value) {
        this.requireUsable();
        if (!this.position.setData(Math.clamp(value, this.minimum, this.maximum))) {
            this.publish();
        }
    }

    /**
     * @param value proposed browser commit
     * @return whether both areas exist and the proposed position is in range
     */
    public boolean acceptsPosition(final int value) {
        return this.getChildCount() == 2 && value >= this.minimum && value <= this.maximum;
    }

    /**
     * Sends authoritative geometry, including when the model rejects a browser commit.
     */
    private void publish() {
        final JsonObject data = new JsonObject();
        data.addString("axis", this.axis == Axis.HORIZONTAL ? "horizontal" : "vertical");
        data.addString("label", this.label);
        data.addNumber("minimum", this.minimum);
        data.addNumber("maximum", this.maximum);
        data.addNumber("position", this.getPosition());
        this.pushUpdate(new SetSplitPane(this.getId(), data));
    }
}
