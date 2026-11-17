/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.client.Focus;
import com.kniazkov.widgets.client.OpenPageInNewTab;
import com.kniazkov.widgets.client.ScrollIntoView;
import com.kniazkov.widgets.client.SelectText;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Integration example for local interaction, IME and delayed server normalization.
 * Uses existing widgets only; no new visual component is introduced.
 */
public final class LocalActions {
    /**
     * Static example entry point.
     */
    private LocalActions() {
    }

    /**
     * @return a fresh page demonstrating the local-action contract
     */
    public static Page page() {
        return (root, context) -> {
            final InputField editor = new InputField("abcdef");
            editor.setAccessibleName("IME editor");
            editor.setInputDebounce(200);
            final TextWidget status = new TextWidget("Committed edits: 0");
            final AtomicInteger edits = new AtomicInteger();
            editor.onTextInput(text -> {
                status.setText("Committed edits: " + edits.incrementAndGet());
                editor.setText(text.toUpperCase(Locale.ROOT));
            });
            final TextWidget result = new TextWidget("No business command");
            editor.onEnter(event -> result.setText("Submitted: " + editor.getText()));
            final Button focus = new Button("Local focus and rejected command");
            focus.onClickOnClient(new Focus(editor));
            focus.onClick(event -> result.setText("Business command rejected"));
            final Button select = new Button("Select characters 1 to 4");
            select.onClickOnClient(new SelectText(editor, 1, 4));
            final Button scroll = new Button("Scroll to editor");
            scroll.onClickOnClient(new ScrollIntoView(editor));
            final Button open = new Button("Open example in new tab");
            open.onClickOnClient(new OpenPageInNewTab("/local-actions"));
            root.add(new Section(new TextWidget("Local actions and IME")));
            root.add(new Section(editor));
            root.add(new Section(focus, select, scroll, open));
            root.add(new Section(status, result));
        };
    }

    /**
     * Starts the standalone integration example.
     * @param args unused arguments
     */
    public static void main(final String[] args) {
        final Application application = new Application(page());
        application.addPage("local-actions", page());
        Server.start(application, new Options.Builder().build());
    }
}
