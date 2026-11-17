/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.protocol.SetAnchoredOverlay;
import java.util.Objects;

/**
 * Shared implementation of element-anchored, nonmodal overlays. Mount the overlay in the
 * same page and, for a modal anchor, inside that modal. Content is retained while closed.
 * Browser commands carry a generation so a stale dismissal or repeated choice is harmless.
 */
public abstract class AnchoredOverlay extends Panel {
    /**
     * Preferred side; the browser flips and shifts inside the visual viewport.
     */
    public enum Placement {
        /**
         * Prefer below the anchor.
         */
        BOTTOM,
        /**
         * Prefer above the anchor.
         */
        TOP,
        /**
         * Prefer the anchor's left side.
         */
        LEFT,
        /**
         * Prefer the anchor's right side.
         */
        RIGHT
    }

    /**
     * Non-owning anchor reference.
     */
    private final Widget<?> anchor;
    /**
     * Owned content, server-hidden while closed.
     */
    private final Panel content = new Panel();
    /**
     * Preferred placement.
     */
    private Placement placement = Placement.BOTTOM;
    /**
     * Current open state.
     */
    private boolean open;
    /**
     * Command generation.
     */
    private int revision;
    /**
     * Tooltip opening delay, milliseconds.
     */
    private int showDelay = 400;
    /**
     * Tooltip closing delay, milliseconds.
     */
    private int hideDelay = 120;

    /**
     * @param anchor non-owning trigger; interactive overlays require a native Button
     */
    @SuppressWarnings("this-escape")
    protected AnchoredOverlay(final Widget<?> anchor) {
        super(overlayStyle());
        this.anchor = Objects.requireNonNull(anchor, "anchor");
        this.content.hide();
        super.add(this.content);
        this.subscribeToEvent(Event.OVERLAY_COMMAND);
    }

    /**
     * @return an independent surface style; application styles can still override it
     */
    private static PanelStyle overlayStyle() {
        final PanelStyle style = Panel.getDefaultStyle().derive();
        style.setBgColor(com.kniazkov.widgets.common.Color.WHITE);
        style.setBorderStyle(com.kniazkov.widgets.common.BorderStyle.SOLID);
        style.setBorderColor(new com.kniazkov.widgets.common.Color(160, 174, 192));
        style.setBorderWidth(1);
        style.setBorderRadius(8);
        style.setPadding(12);
        style.setBoxSizing(com.kniazkov.widgets.common.BoxSizing.BORDER_BOX);
        style.setBoxShadow(new com.kniazkov.widgets.common.BoxShadow(0, 4, 16,
            new com.kniazkov.widgets.common.Color(15, 23, 42, 35)));
        return style;
    }

    /**
     * @return non-owning trigger
     */
    public final Widget<?> getAnchor() {
        return this.anchor;
    }

    /**
     * @return whether the server currently permits content interaction
     */
    public final boolean isOpen() {
        return this.open;
    }

    /**
     * Opens only for an enabled anchor attached to this page.
     * @return whether the overlay transitioned to open
     */
    public final boolean open() {
        this.requireUsable();
        if (this.open || !this.anchorAvailable()) {
            return false;
        }
        this.open = true;
        this.revision++;
        this.content.show();
        this.publishOverlay();
        return true;
    }

    /**
     * Closes without destroying content; repeated close is harmless.
     */
    public final void close() {
        if (!this.isRetired() && this.open) {
            this.open = false;
            this.revision++;
            this.publishOverlay();
            if (!this.overlayKind().equals("tooltip")) {
                this.content.hide();
            }
        }
    }

    /**
     * @param placement preferred side before viewport collision handling
     */
    public final void setPlacement(final Placement placement) {
        this.requireUsable();
        this.placement = Objects.requireNonNull(placement, "placement");
        this.publishOverlay();
    }

    /**
     * @return preferred side
     */
    public final Placement getPlacement() {
        return this.placement;
    }

    /**
     * @param show opening delay in milliseconds, 0..5000
     * @param hide closing delay in milliseconds, 0..5000
     */
    protected final void setDelays(final int show, final int hide) {
        this.requireUsable();
        if (show < 0 || show > 5000 || hide < 0 || hide > 5000) {
            throw new IllegalArgumentException("Overlay delays must be within 0..5000 ms");
        }
        this.showDelay = show;
        this.hideDelay = hide;
        this.publishOverlay();
    }

    /**
     * Adds owned content. Callers must not modify the internal container through inherited APIs.
     * @param widget new unattached content
     */
    protected final void addContent(final BlockWidget<?> widget) {
        this.requireUsable();
        this.content.add(widget);
    }

    /**
     * @return true only when the anchor can open an overlay in this page
     */
    private boolean anchorAvailable() {
        return !this.isRetired() && !this.anchor.isRetired() && this.isInteractionEnabled()
            && this.anchor.isInteractionEnabled() && this.getRootWidget().isPresent()
            && this.getRootWidget().equals(this.anchor.getRootWidget());
    }

    /**
     * Validates a browser command before model mutation or application actions.
     * @param action open, close or choose
     * @param key menu key, empty for visibility commands
     * @param generation generation supplied by the browser
     * @return whether the command is admissible
     */
    public final boolean acceptsOverlayCommand(final String action, final String key,
            final int generation) {
        if (this.isRetired() || action == null || key == null || generation != this.revision) {
            return false;
        }
        return switch (action) {
            case "open" -> key.isEmpty() && !this.open && this.anchorAvailable();
            case "close" -> key.isEmpty() && this.open;
            case "choose" -> this.open && this.anchorAvailable()
                && this instanceof Menu menu && menu.acceptsKey(key);
            default -> false;
        };
    }

    /**
     * Rechecks capability even when invoked directly by application code.
     * @param command validated or untrusted command object
     */
    public final void processOverlayCommand(final JsonObject command) {
        if (Event.OVERLAY_COMMAND.rejectionReason(this, command).isPresent()) {
            return;
        }
        final String action = command.get("action").stringValue();
        final String key = command.get("key").stringValue();
        final int generation = command.get("revision").intValue();
        if (!this.acceptsOverlayCommand(action, key, generation)) {
            return;
        }
        if (action.equals("open")) {
            this.open();
        } else if (action.equals("choose")) {
            ((Menu) this).choose(key);
        } else {
            this.close();
        }
    }

    /**
     * Invalidates commands recorded against an older menu configuration.
     */
    protected final void invalidateOverlayCommands() {
        this.revision++;
    }

    /**
     * Publishes a complete immutable snapshot; configuration does not reset the open generation.
     */
    protected final void publishOverlay() {
        if (this.overlayKind().equals("tooltip")) {
            this.content.show();
        }
        final JsonObject state = new JsonObject();
        state.addString("anchor", this.anchor.getId().toString());
        state.addString("content", this.content.getId().toString());
        state.addString("kind", this.overlayKind());
        state.addString("placement", this.placement.name().toLowerCase(java.util.Locale.ROOT));
        state.addBoolean("open", this.open);
        state.addNumber("revision", this.revision);
        state.addNumber("showDelay", this.showDelay);
        state.addNumber("hideDelay", this.hideDelay);
        state.addElement("items", this.menuSnapshot());
        this.pushUpdate(new SetAnchoredOverlay(this.getId(), state));
    }

    /**
     * @return wire kind
     */
    protected abstract String overlayKind();

    /**
     * @return menu entries, empty for other overlays
     */
    protected JsonArray menuSnapshot() {
        return new JsonArray();
    }
}
