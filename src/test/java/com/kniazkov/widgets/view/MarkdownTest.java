/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.StringModel;
import com.kniazkov.widgets.common.FontSize;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Markdown uses the ordinary reactive text and style protocol as a block widget.
 */
public final class MarkdownTest {
    /**
     * Initial content, changes and model replacement retain standard binding behavior.
     */
    @Test public void bindsAndReplacesSource() {
        final StringModel source = new StringModel("# Initial");
        final Markdown document = new Markdown(source);
        assertTrue(document instanceof BlockWidget<?>);
        assertEquals("markdown", document.getType());
        assertEquals("# Initial", document.getText());
        final var sandbox = WidgetSandbox.open(document);
        sandbox.clearUpdates();
        source.setData("**Updated**");
        assertEquals(1, sandbox.drainUpdates().size());
        assertEquals("**Updated**", document.getText());
        final StringModel replacement = new StringModel("Replacement");
        document.setTextModel(replacement);
        sandbox.clearUpdates();
        source.setData("Detached");
        assertEquals(0, sandbox.drainUpdates().size());
        replacement.setData("");
        assertEquals(1, sandbox.drainUpdates().size());
        assertEquals("", document.getText());
        assertEquals("", new Markdown().getText());
    }

    /**
     * Typography cascades and local changes remain isolated between documents.
     */
    @Test public void cascadesTypography() {
        final MarkdownStyle style = Markdown.getDefaultStyle().derive();
        style.setFontSize("18px");
        final Markdown first = new Markdown(style, "One");
        final Markdown second = new Markdown(style, "Two");
        final var sandbox = WidgetSandbox.open(first);
        sandbox.clearUpdates();
        style.setFontSize("20px");
        style.setFontFace(() -> "Georgia, serif");
        assertEquals(2, sandbox.drainUpdates().size());
        assertEquals(FontSize.parse("20px"), first.getFontSize());
        assertEquals("Georgia, serif", first.getFontFace().getName());
        first.setFontSize("24px");
        assertEquals(FontSize.parse("20px"), second.getFontSize());
        assertEquals(FontSize.parse("24px"), first.getFontSize());
    }
}
