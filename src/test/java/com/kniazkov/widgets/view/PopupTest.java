/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.HorizontalAlignment;
import com.kniazkov.widgets.common.VerticalAlignment;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests popup containers, positioning models and the ready-to-use message popup.
 */
public final class PopupTest {
    /**
     * Verifies the common popup container and positioning contract.
     */
    @Test
    public void popupIsCenteredBlockContainer() {
        final Section first = new Section(new TextWidget("First"));
        final Section second = new Section(new TextWidget("Second"));
        final Popup popup = new Popup(first, second);

        assertTrue(popup instanceof BlockContainer);
        assertEquals(HorizontalAlignment.CENTER, popup.getHorizontalAlignment());
        assertEquals(VerticalAlignment.MIDDLE, popup.getVerticalAlignment());
        assertEquals(2, popup.getChildCount());
        assertSame(first, popup.getChild(0));
        assertSame(second, popup.getChild(1));
        assertSame(popup, first.getParent().get());

        popup.remove(first);

        assertEquals(1, popup.getChildCount());
        assertTrue(first.getParent().isEmpty());
    }

    /**
     * Verifies that backdrop color changes use the dedicated protocol property.
     */
    @Test
    public void modalPopupUpdatesBackdropColor() {
        final ModalPopup popup = new ModalPopup();
        final WidgetSandbox<ModalPopup> sandbox = WidgetSandbox.open(popup);
        final Color defaultColor = popup.getBackdropColor();

        assertEquals(Color.WHITE, new Color(
            defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue()
        ));
        assertTrue(defaultColor.getAlpha() < 255);

        sandbox.clearUpdates();
        popup.setBackdropColor(new Color(1, 2, 3, 128));
        final List<JsonObject> updates = WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "set backdrop color", popup
        );

        assertEquals(1, updates.size());
        final JsonObject color = (JsonObject) updates.get(0).get("backdrop color");
        assertEquals(1, color.get("r").getIntegerValue());
    }

    /**
     * Verifies the one- and two-button message layouts.
     */
    @Test
    public void messagePopupAcceptsOneOrTwoButtons() {
        final Button ok = new Button("OK");
        final MessagePopup oneButton = new MessagePopup("Saved", ok);
        final Button cancel = new Button("Cancel");
        final Button delete = new Button("Delete");
        final MessagePopup twoButtons = new MessagePopup("Delete?", cancel, delete);

        assertEquals(2, oneButton.getChildCount());
        assertSame(ok, ((Section) oneButton.getChild(1)).getChild(0));
        assertEquals(2, ((Section) twoButtons.getChild(1)).getChildCount());
        assertSame(cancel, ((Section) twoButtons.getChild(1)).getChild(0));
        assertSame(delete, ((Section) twoButtons.getChild(1)).getChild(1));
    }
}
