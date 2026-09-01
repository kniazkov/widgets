/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.BoxShadow;
import com.kniazkov.widgets.common.BoxSizing;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.Cursor;
import com.kniazkov.widgets.common.Outline;
import com.kniazkov.widgets.common.Overflow;
import com.kniazkov.widgets.common.TextDecoration;
import com.kniazkov.widgets.common.TimingFunction;
import com.kniazkov.widgets.common.Transition;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests typed visual properties and their protocol representation.
 */
public final class ModernStylePropertiesTest {
    /**
     * Verifies CSS serialization of the new immutable value objects.
     */
    @Test
    public void valueObjectsProduceCss() {
        assertEquals(
            "2px 4px 8px -1px rgba(0,0,0,0.5)",
            new BoxShadow(2, 4, 8, -1, new Color(0, 0, 0, 128)).getCSSCode()
        );
        assertEquals(
            "inset 0px 1px 2px 0px rgb(128,128,128)",
            new BoxShadow(0, 1, 2, 0, Color.GRAY, true).getCSSCode()
        );
        assertEquals(
            "all 180ms ease-out 20ms",
            new Transition(180, TimingFunction.EASE_OUT, 20).getCSSCode()
        );
        assertEquals("none", BoxShadow.NONE.getCSSCode());
        assertEquals("none", Transition.NONE.getCSSCode());
        assertEquals("hidden", Overflow.HIDDEN.getCSSCode());
        assertEquals("line-through", TextDecoration.LINE_THROUGH.getCSSCode());
    }

    /**
     * Verifies widget model changes produce typed protocol updates.
     */
    @Test
    public void buttonProducesModernStyleUpdates() {
        final Button button = new Button();
        final WidgetSandbox<Button> sandbox = WidgetSandbox.open(button);
        sandbox.clearUpdates();

        button.setBoxShadow(State.FOCUSED, new BoxShadow(0, 0, 0, 3, Color.BLUE));
        button.setOutline(
            State.FOCUSED,
            new Outline(Color.BLUE, BorderStyle.SOLID, 2, 2)
        );
        button.setCursor(State.NORMAL, Cursor.POINTER);
        button.setTransition(new Transition(150, TimingFunction.EASE_OUT));
        button.setBoxSizing(BoxSizing.BORDER_BOX);

        final List<JsonObject> updates = sandbox.drainUpdates();
        final JsonObject shadow = singleUpdate(updates, "set box shadow", button);
        assertEquals("focused", shadow.get("state").getStringValue());
        assertEquals(
            "0px 0px 0px 3px rgb(0,0,255)",
            shadow.get("box shadow").getStringValue()
        );

        final JsonObject outline = singleUpdate(updates, "set outline", button);
        assertEquals("focused", outline.get("state").getStringValue());
        final JsonObject value = (JsonObject) outline.get("outline");
        assertEquals("solid", value.get("style").getStringValue());
        assertEquals("2px", value.get("width").getStringValue());
        assertEquals("2px", value.get("offset").getStringValue());

        assertEquals(
            "pointer",
            singleUpdate(updates, "set cursor", button).get("cursor").getStringValue()
        );
        assertEquals(
            "all 150ms ease-out 0ms",
            singleUpdate(updates, "set transition", button).get("transition").getStringValue()
        );
        assertEquals(
            "border-box",
            singleUpdate(updates, "set box sizing", button).get("box sizing").getStringValue()
        );
    }

    /**
     * Verifies text decoration defaults and its state-dependent protocol update.
     */
    @Test
    public void styledTextSupportsTextDecoration() {
        final TextWidget text = new TextWidget("Text");
        assertEquals(TextDecoration.NONE, text.getTextDecoration());

        final Link link = new Link("Link");
        assertEquals(TextDecoration.UNDERLINE, link.getTextDecoration());
        assertEquals(TextDecoration.UNDERLINE, link.getTextDecoration(State.FOCUSED));

        final WidgetSandbox<Link> sandbox = WidgetSandbox.open(link);
        sandbox.clearUpdates();
        link.setTextDecoration(State.HOVERED, TextDecoration.OVERLINE);

        final JsonObject update = singleUpdate(
            sandbox.drainUpdates(), "set text decoration", link
        );
        assertEquals("hovered", update.get("state").getStringValue());
        assertEquals("overline", update.get("text decoration").getStringValue());
    }

    /**
     * Verifies text fields expose padding through the regular style protocol.
     */
    @Test
    public void inputFieldSupportsPadding() {
        final InputField input = new InputField();
        assertEquals("8px", input.getLeftPadding().getCSSCode());
        assertEquals("8px", input.getRightPadding().getCSSCode());
        assertEquals("8px", input.getTopPadding().getCSSCode());
        assertEquals("8px", input.getBottomPadding().getCSSCode());

        final WidgetSandbox<InputField> sandbox = WidgetSandbox.open(input);
        sandbox.clearUpdates();
        input.setPadding(18, 9);

        final JsonObject update = singleUpdate(
            sandbox.drainUpdates(), "set padding", input
        );
        final JsonObject padding = (JsonObject) update.get("padding");
        assertEquals("18px", padding.get("left").getStringValue());
        assertEquals("18px", padding.get("right").getStringValue());
        assertEquals("9px", padding.get("top").getStringValue());
        assertEquals("9px", padding.get("bottom").getStringValue());
    }

    /**
     * Verifies visible widgets are usable without constructing custom styles.
     */
    @Test
    public void visibleWidgetsHaveModernDefaults() {
        final Button button = new Button();
        assertEquals(Cursor.POINTER, button.getCursor());
        assertEquals(BoxSizing.BORDER_BOX, button.getBoxSizing());
        assertEquals("8px", button.getBorderRadius().getCSSCode());

        final InputField input = new InputField();
        assertEquals(Cursor.TEXT, input.getCursor());
        assertEquals(BoxSizing.BORDER_BOX, input.getBoxSizing());
        assertEquals("8px", input.getBorderRadius().getCSSCode());

        assertEquals("96px", new TextArea().getHeight().getCSSCode());
        assertEquals("10px", new ImageWidget("image.png").getBorderRadius().getCSSCode());
        assertEquals(Cursor.POINTER, new ActiveImage("image.png").getCursor());
        assertEquals(0.75, new CheckBox().getOpacity(State.DISABLED), 0.0);
        assertEquals("", new Table().getWidth().getCSSCode());
        assertEquals("0px", new Cell().getLeftPadding().getCSSCode());
    }

    /**
     * Verifies tables offer neutral layout and decorated data-table defaults.
     */
    @Test
    public void tableOffersNeutralAndDecoratedStyles() {
        final Table plain = new Table();
        assertEquals(BorderStyle.NONE, plain.getBorderStyle());
        assertEquals(Overflow.VISIBLE, plain.getOverflow());
        assertEquals("", plain.getWidth().getCSSCode());
        assertEquals("0px", plain.getCellSpacing().getCSSCode());
        assertEquals("0px", plain.getCell(0, 0).getLeftPadding().getCSSCode());

        final Table decorated = new Table(TableStyle.DECORATED);
        assertEquals(BorderStyle.SOLID, decorated.getBorderStyle());
        assertEquals(Overflow.HIDDEN, decorated.getOverflow());
        assertEquals("100.0%", decorated.getWidth().getCSSCode());
        assertEquals("1px", decorated.getCellSpacing().getCSSCode());
        assertEquals("11px", decorated.getCell(0, 0).getLeftPadding().getCSSCode());

        final TableStyle custom = TableStyle.DECORATED.derive();
        custom.getDefaultCellStyle().setPadding(20);
        assertEquals("11px", TableStyle.DECORATED.getDefaultCellStyle()
            .getLeftPadding().getCSSCode());
        assertEquals("20px", new Table(custom).getCell(0, 0)
            .getLeftPadding().getCSSCode());
    }

    /**
     * Verifies ready-to-use button styles include matching text styles.
     */
    @Test
    public void buttonOffersDefaultPrimaryAndDangerStyles() {
        final Button standard = new Button(ButtonStyle.DEFAULT, "Default");
        assertEquals(Color.WHITE, standard.getBgColor());
        assertEquals(DefaultTheme.TEXT, ((TextWidget) standard.getChild()).getColor());

        final Button primary = new Button(ButtonStyle.PRIMARY, "Save");
        assertEquals(DefaultTheme.PRIMARY, primary.getBgColor());
        assertEquals(Color.WHITE, ((TextWidget) primary.getChild()).getColor());

        final Button danger = new Button(ButtonStyle.DANGER, "Delete");
        assertEquals(DefaultTheme.DANGER, danger.getBgColor());
        assertEquals(Color.WHITE, ((TextWidget) danger.getChild()).getColor());

        final ButtonStyle custom = ButtonStyle.PRIMARY.derive();
        custom.getDefaultTextStyle().setColor(Color.BLACK);
        assertEquals(Color.WHITE, ButtonStyle.PRIMARY.getDefaultTextStyle().getColor());
        assertEquals(Color.BLACK, ((TextWidget) new Button(custom, "Custom").getChild())
            .getColor());
    }

    /**
     * Verifies overflow changes use the typed style protocol.
     */
    @Test
    public void tableProducesOverflowUpdate() {
        final Table table = new Table();
        final WidgetSandbox<Table> sandbox = WidgetSandbox.open(table);
        sandbox.clearUpdates();

        table.setOverflow(Overflow.HIDDEN);

        final JsonObject update = singleUpdate(
            sandbox.drainUpdates(), "set overflow", table
        );
        assertEquals("hidden", update.get("overflow").getStringValue());
    }

    /**
     * Returns the only matching update.
     *
     * @param updates serialized updates
     * @param action expected action
     * @param widget expected widget
     * @return matching update
     */
    private static JsonObject singleUpdate(final List<JsonObject> updates, final String action,
            final Widget<?> widget) {
        final List<JsonObject> matches = WidgetSandbox.findUpdates(updates, action, widget);
        assertEquals("update count for " + action, 1, matches.size());
        return matches.get(0);
    }
}
