/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.model.StringModel;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests typed accessibility contracts and the serialized reactive updates.
 */
public final class AccessibleSemanticsTest {
    /**
     * Proves the plain text values and stable IDs travel independently of label edits.
     */
    @Test
    public void namesDescriptionsAndReferencesAreTypedReactiveProperties() {
        final InputField field = new InputField();
        final TextWidget label = new TextWidget("Label");
        final TextWidget help = new TextWidget("Help");
        final TextWidget error = new TextWidget("Error");
        final WidgetSandbox<InputField> sandbox = WidgetSandbox.open(field);
        final StringModel name = new StringModel("Fallback");
        final String literal = "A <span> \"quoted\" & value";

        sandbox.clearUpdates();
        field.setAccessibleNameModel(name);
        name.setData(literal);
        field.setAccessibleDescription(literal);
        field.setAccessibleLabel(label);
        field.setAccessibleDescriptionWidget(help);
        field.setAccessibleError(error);
        final List<JsonObject> updates = sandbox.drainUpdates();
        assertEquals(literal, value(updates, "accessible name", field));
        assertEquals(literal, value(updates, "accessible description", field));
        assertEquals(label.getId().toString(), value(updates, "accessible label", field));
        assertEquals(help.getId().toString(), value(updates, "accessible description widget",
            field));
        assertEquals(error.getId().toString(), value(updates, "accessible error", field));
        assertThrows(IllegalArgumentException.class, () -> field.setAccessibleLabel(field));

        field.setAccessibleLabel(null);
        assertEquals("", value(sandbox.drainUpdates(), "accessible label", field));
        assertEquals(literal, name.getData());
    }

    /**
     * Verifies native button compatibility, the choice proxy and image and heading models.
     */
    @Test
    public void semanticTypesPreserveTheirNativeWidgetContracts() {
        final ImageWidget icon = new ImageWidget("/icon.svg");
        final Button action = Button.icon(icon, "Refresh");
        assertSame(icon, action.getChild());
        assertEquals("button", action.getType());
        assertTrue(icon.getModel(State.ANY, Property.DECORATIVE_IMAGE).getData());
        assertEquals("Refresh", action.getAccessibleName());
        assertThrows(IllegalArgumentException.class,
            () -> Button.icon(new ImageWidget("/icon.svg"), " "));

        final CheckBoxWithText choice = new CheckBoxWithText("Choice");
        choice.setAccessibleName("Explicit choice");
        assertEquals("Explicit choice", choice.getCheckBox().getAccessibleName());

        final TextWidget heading = new TextWidget("Heading");
        final WidgetSandbox<TextWidget> sandbox = WidgetSandbox.open(heading);
        heading.setHeadingLevel(HeadingLevel.H2);
        assertEquals(2, WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "set heading level", heading
        ).getLast().get("heading level").intValue());

        final ImageWidget image = new ImageWidget("/icon.svg");
        final WidgetSandbox<ImageWidget> imageSandbox = WidgetSandbox.open(image);
        final StringModel alt = new StringModel("Before");
        image.setAlternativeTextModel(alt);
        alt.setData("After & <literal>");
        image.setDecorative(true);
        final List<JsonObject> updates = imageSandbox.drainUpdates();
        assertEquals("After & <literal>", value(updates, "alternative text", image));
        assertTrue(WidgetSandbox.findUpdates(updates, "set decorative image", image).getLast()
            .get("decorative image").booleanValue());
    }

    /**
     * Returns the latest string property serialized for the widget.
     * @param updates available updates
     * @param property semantic property
     * @param widget update target
     * @return serialized string value
     */
    private static String value(final List<JsonObject> updates, final String property,
            final Widget<?> widget) {
        return WidgetSandbox.findUpdates(updates, "set " + property, widget).getLast()
            .get(property).stringValue();
    }
}
