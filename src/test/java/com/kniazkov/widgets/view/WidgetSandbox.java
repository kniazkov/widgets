/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.protocol.Update;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * An isolated server-side environment for exercising one widget without a client or HTTP server.
 * The subject is mounted in the smallest legal widget tree, outgoing protocol updates can be
 * drained, and synthetic client events can be delivered directly to it.
 *
 * @param <W> subject widget type
 */
final class WidgetSandbox<W extends Widget<?>> {
    /**
     * Root of the isolated widget tree.
     */
    private final RootWidget root;

    /**
     * Widget under test.
     */
    private final W subject;

    /**
     * Creates a sandbox and mounts the subject in the smallest legal hierarchy.
     *
     * @param widget widget under test
     */
    private WidgetSandbox(final W widget) {
        this.subject = widget;
        if (widget instanceof RootWidget) {
            this.root = (RootWidget) widget;
        } else {
            this.root = new RootWidget();
            this.mount(widget);
        }
    }

    /**
     * Opens a sandbox for a widget.
     *
     * @param widget widget under test
     * @param <W> widget type
     * @return initialized sandbox
     */
    static <W extends Widget<?>> WidgetSandbox<W> open(final W widget) {
        return new WidgetSandbox<>(widget);
    }

    /**
     * Returns the subject widget.
     *
     * @return widget under test
     */
    W getSubject() {
        return this.subject;
    }

    /**
     * Delivers a synthetic browser event to the subject.
     *
     * @param event event type
     * @param data serialized event data
     */
    void fire(final Event<?> event, final JsonObject data) {
        this.subject.handleEvent(event, data);
    }

    /**
     * Drains all updates currently queued by the isolated widget tree.
     *
     * @return updates serialized in protocol order
     */
    List<JsonObject> drainUpdates() {
        final Set<Update> pending = new TreeSet<>();
        for (final Widget<?> widget : this.root) {
            widget.getUpdates(pending);
        }
        final List<JsonObject> result = new ArrayList<>(pending.size());
        for (final Update update : pending) {
            final JsonObject object = new JsonObject();
            update.serialize(object);
            result.add(object);
        }
        return result;
    }

    /**
     * Clears all queued setup updates.
     */
    void clearUpdates() {
        this.drainUpdates();
    }

    /**
     * Selects updates with the given action that target the specified widget.
     *
     * @param updates serialized updates
     * @param action protocol action
     * @param widget target widget
     * @return matching updates
     */
    static List<JsonObject> findUpdates(final List<JsonObject> updates, final String action,
            final Widget<?> widget) {
        final List<JsonObject> result = new ArrayList<>();
        for (final JsonObject update : updates) {
            if (action.equals(update.get("action").getStringValue())
                    && widget.getId().toString().equals(
                        update.get("widget").getStringValue())) {
                result.add(update);
            }
        }
        return result;
    }

    /**
     * Selects all updates with the given action.
     *
     * @param updates serialized updates
     * @param action protocol action
     * @return matching updates
     */
    static List<JsonObject> findUpdates(final List<JsonObject> updates, final String action) {
        final List<JsonObject> result = new ArrayList<>();
        for (final JsonObject update : updates) {
            if (action.equals(update.get("action").getStringValue())) {
                result.add(update);
            }
        }
        return result;
    }

    /**
     * Mounts any concrete widget using the smallest container accepted by its type.
     *
     * @param widget widget to mount
     */
    private void mount(final Widget<?> widget) {
        if (widget instanceof BlockWidget) {
            this.root.add((BlockWidget<?>) widget);
        } else if (widget instanceof InlineWidget) {
            this.root.add(new Section((InlineWidget<?>) widget));
        } else if (widget instanceof Row) {
            this.root.add(new Table((Row) widget));
        } else if (widget instanceof Cell) {
            this.root.add(new Table(new Row((Cell) widget)));
        } else {
            throw new IllegalArgumentException("Unsupported widget type: " + widget.getType());
        }
    }
}
