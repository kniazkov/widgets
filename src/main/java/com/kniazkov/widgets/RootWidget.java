/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The root widget — a special container that holds all other widgets in a UI hierarchy.
 * <p>
 *     This widget is always present at the top of the widget tree and has no parent.
 *     It is implicitly created by the framework when a new page or client session is initialized.
 * </p>
 *
 * <p>
 *     {@code RootWidget} implements {@link BlockContainer} and stores a list of {@link BlockWidget}
 *     instances, typically laid out vertically (top to bottom). It exposes factory methods to
 *     create common block-level widgets such as {@link Paragraph}.
 * </p>
 *
 * <p>
 *     In addition to serving as the root of the widget tree, the root widget also maintains
 *     a shared {@link StyleSet}, which holds default styles for the application. These styles
 *     can be accessed by child widgets during initialization to ensure consistent appearance.
 * </p>
 *
 * <p>
 *     The root widget cannot be instantiated manually — it is created automatically and managed
 *     by the framework.
 * </p>
 */
public final class RootWidget extends Widget implements BlockContainer {
    /**
     * List of block-level children contained by this root widget.
     */
    private final List<BlockWidget> children;

    /**
     * Shared default styles accessible from all widgets in this tree.
     */
    private final StyleSet styles;

    /**
     * Constructs the root widget.
     * <p>
     *     Called internally by the framework when a new {@link Client} is created.
     * </p>
     *
     * @param client The owning client
     */
    RootWidget(final Client client) {
        super(client, null);
        this.children = new ArrayList<>();
        this.styles = new StyleSet();
    }

    @Override
    public boolean accept(final WidgetVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    String getType() {
        return "root";
    }

    @Override
    void handleEvent(final String type, final Optional<JsonObject> data) {
        // Root widget does not handle events directly
    }

    @Override
    public List<BlockWidget> getTypedChildren() {
        return Collections.unmodifiableList(new ArrayList<>(this.children));
    }

    @Override
    public Paragraph createParagraph() {
        final Paragraph widget = new Paragraph(this.getClient(), this);
        this.appendChild(widget);
        return widget;
    }

    @Override
    public void remove(final Widget widget) {
        this.children.remove(widget);
    }

    /**
     * Adds a new child widget and sends an update to the client.
     *
     * @param child The block widget to add
     */
    private void appendChild(final BlockWidget child) {
        this.children.add(child);
        this.sendToClient(new AppendChild(this.getWidgetId(), child.getWidgetId()));
    }

    /**
     * Returns the shared default style set for this root.
     * <p>
     *     This can be used by widgets to apply consistent appearance based on centralized styles.
     *     Changes to styles here will affect all widgets using forked models derived from them.
     * </p>
     *
     * @return The default style set
     */
    public StyleSet getDefaultStyles() {
        return this.styles;
    }
}
