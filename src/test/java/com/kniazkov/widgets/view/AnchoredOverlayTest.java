/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.Event;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

/**
 * Anchored ownership, generation checks and single-action menu selection.
 */
public final class AnchoredOverlayTest {
    /**
     * @param action command verb
     * @param key menu key or empty
     * @param revision expected generation
     * @return wire command
     */
    private static JsonObject command(final String action, final String key, final int revision) {
        final JsonObject data = new JsonObject();
        data.addString("action", action);
        data.addString("key", key);
        data.addNumber("revision", revision);
        return data;
    }

    /**
     * A repeated choice, stale update and disabled item cannot execute an application action.
     */
    @Test
    public void menuSelectionIsGenerationChecked() {
        final AtomicInteger calls = new AtomicInteger();
        final Button anchor = new Button("Actions");
        final Menu menu = new Menu(anchor, "Actions", List.of(
            new Menu.Item("yes", "Enabled", true, calls::incrementAndGet),
            new Menu.Item("no", "Disabled", false, calls::incrementAndGet)));
        final Panel page = new Panel(new Section(anchor), menu);
        WidgetSandbox.open(page);
        assertFalse(menu.isOpen());
        Event.OVERLAY_COMMAND.process(menu, command("open", "", 1));
        assertTrue(menu.isOpen());
        Event.OVERLAY_COMMAND.process(menu, command("choose", "no", 2));
        assertTrue(menu.isOpen());
        Event.OVERLAY_COMMAND.process(menu, command("choose", "yes", 2));
        Event.OVERLAY_COMMAND.process(menu, command("choose", "yes", 2));
        assertEquals(1, calls.get());
        assertFalse(menu.isOpen());
        assertTrue(menu.open());
        menu.setItems(List.of(new Menu.Item("yes", "Replacement", true, calls::incrementAndGet)));
        Event.OVERLAY_COMMAND.process(menu, command("choose", "yes", 4));
        assertEquals(1, calls.get());
        assertTrue(menu.isOpen());
        Event.OVERLAY_COMMAND.process(menu, command("choose", "yes", 5));
        assertEquals(2, calls.get());
        page.dispose();
    }

    /**
     * An unmounted, foreign, disabled, hidden or retired anchor cannot open its overlay.
     */
    @Test
    public void anchorCapabilityAndDisposal() {
        final Button anchor = new Button("Settings");
        final Popover popover = new Popover(anchor, "Settings");
        final InputField field = new InputField("Retained");
        popover.addBody(new Section(field));
        assertFalse(popover.open());
        final Panel page = new Panel(new Section(anchor), popover);
        WidgetSandbox.open(page);
        assertFalse(field.isInteractionEnabled());
        anchor.disable();
        assertFalse(popover.open());
        anchor.enable();
        anchor.hide();
        assertFalse(popover.open());
        anchor.show();
        assertTrue(popover.open());
        assertTrue(field.isInteractionEnabled());
        popover.close();
        assertFalse(field.isInteractionEnabled());
        assertTrue(popover.open());
        assertEquals("Retained", field.getText());
        anchor.dispose();
        Event.OVERLAY_COMMAND.process(popover, command("close", "", 3));
        assertFalse(popover.isOpen());
        assertFalse(popover.open());
        popover.dispose();
        assertTrue(field.isRetired());
        assertThrows(IllegalStateException.class, popover::open);
        page.dispose();
    }

    /**
     * Invalid configuration and malformed or cross-widget commands have no side effects.
     */
    @Test
    public void invalidConfigurationAndPayload() {
        final Button anchor = new Button("Actions");
        final Menu menu = new Menu(anchor, "Actions", List.of());
        final Panel page = new Panel(new Section(anchor), menu);
        WidgetSandbox.open(page);
        final Menu.Item item = new Menu.Item("same", "Action", true, () -> { });
        assertThrows(IllegalArgumentException.class, () -> menu.setItems(List.of(item, item)));
        assertEquals(List.of(), menu.getItems());
        assertThrows(IllegalArgumentException.class,
            () -> new Menu.Item("", "Action", true, () -> { }));
        final JsonObject extra = command("open", "", 1);
        extra.addBoolean("unexpected", true);
        menu.processOverlayCommand(extra);
        menu.processOverlayCommand(new JsonObject());
        menu.processOverlayCommand(null);
        assertFalse(menu.isOpen());
        assertTrue(Event.OVERLAY_COMMAND.rejectionReason(anchor, command("open", "", 1))
            .isPresent());
        final Popover foreign = new Popover(anchor, "Foreign");
        WidgetSandbox.open(foreign);
        assertFalse(foreign.open());
        foreign.dispose();
        page.dispose();
    }

    /**
     * Tooltip delays are bounded; the anchor is not owned or disposed with its description.
     */
    @Test
    public void tooltipIsSupplementaryAndDoesNotOwnAnchor() {
        final Button anchor = new Button("Help");
        final Tooltip tooltip = new Tooltip(anchor, "Supplementary text");
        final Panel page = new Panel(new Section(anchor), tooltip);
        WidgetSandbox.open(page);
        tooltip.setHoverDelays(0, 5000);
        assertThrows(IllegalArgumentException.class, () -> tooltip.setHoverDelays(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> tooltip.setHoverDelays(0, 5001));
        tooltip.setText("Updated text");
        assertTrue(tooltip.open());
        tooltip.setPlacement(AnchoredOverlay.Placement.TOP);
        assertEquals(AnchoredOverlay.Placement.TOP, tooltip.getPlacement());
        tooltip.dispose();
        assertFalse(anchor.isRetired());
        page.dispose();
    }

    /**
     * Cleanup remains possible after hiding an ancestor; interactive actions remain forbidden.
     */
    @Test
    public void hiddenContainerAllowsOnlyGenerationCheckedClose() {
        final AtomicInteger calls = new AtomicInteger();
        final Button anchor = new Button("Actions");
        final Menu menu = new Menu(anchor, "Actions", List.of(
            new Menu.Item("yes", "Enabled", true, calls::incrementAndGet)));
        final Panel container = new Panel(new Section(anchor), menu);
        final Panel page = new Panel(container);
        WidgetSandbox.open(page);
        assertTrue(menu.open());
        container.hide();
        Event.OVERLAY_COMMAND.process(menu, command("choose", "yes", 2));
        assertEquals(0, calls.get());
        Event.OVERLAY_COMMAND.process(menu, command("close", "", 1));
        assertTrue(menu.isOpen());
        Event.OVERLAY_COMMAND.process(menu, command("close", "", 2));
        assertFalse(menu.isOpen());
        Event.OVERLAY_COMMAND.process(menu, command("open", "", 3));
        assertFalse(menu.isOpen());
        container.show();
        assertTrue(menu.open());
        Event.OVERLAY_COMMAND.process(menu, command("choose", "yes", 4));
        assertEquals(1, calls.get());
        page.dispose();
    }

}
