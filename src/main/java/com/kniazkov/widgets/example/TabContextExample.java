/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.base.TabContext;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.StringModel;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.Link;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Demonstrates shared per-tab state, internal navigation and terminal context revocation.
 * The note is demonstration data, not an authorization or operator-mode implementation.
 */
public final class TabContextExample {
    /**
     * Prevents construction of the example entry point.
     */
    private TabContextExample() {
    }

    /**
     * Starts two pages sharing notes only within the same live tab context.
     *
     * @param args unused command-line arguments
     */
    public static void main(final String[] args) {
        /*
         * Values do not reference their keys. Abandoned contexts and their notes can be
         * collected after the server and its page clients release those contexts.
         */
        final Map<TabContext, Model<String>> notes =
            Collections.synchronizedMap(new WeakHashMap<>());
        final Application application = new Application(
            page(notes, "First page", "/second", "Go to second page"));
        application.addPage("second",
            page(notes, "Second page", "/", "Go to first page"));
        Server.start(application, new Options.Builder().build());
    }

    /**
     * Creates one page using application-owned state keyed by the server context.
     *
     * @param notes shared context registry
     * @param title page heading
     * @param destination internal destination
     * @param linkText navigation label
     * @return page factory
     */
    private static Page page(
        final Map<TabContext, Model<String>> notes,
        final String title,
        final String destination,
        final String linkText
    ) {
        return (root, context) -> {
            final TabContext tab = context.tabContext;
            final Model<String> note = notes.computeIfAbsent(tab,
                ignored -> new StringModel("New tab note").asSynchronized());
            root.add(new Section(new TextWidget(title)));
            root.add(new Section(new TextWidget("Browser ID: " + context.browserId)));
            root.add(new Section(new TextWidget("Tab context ID: " + tab.getId())));

            final InputField input = new InputField();
            input.setTextModel(note);
            final TextWidget echo = new TextWidget();
            echo.setTextModel(note);
            root.add(new Section(new TextWidget("Edit this tab's note: "), input));
            root.add(new Section(new TextWidget("Shared note: "), echo));
            root.add(new Section(new Link(linkText, destination)));

            final Button clear = new Button("Clear this tab's note");
            clear.onClick(event -> {
                if (tab.isActive()) {
                    note.setData("");
                }
            });
            final Button revoke = new Button("Revoke this tab context");
            revoke.onClick(event -> {
                notes.remove(tab);
                tab.revoke();
            });
            final Button open = new Button("Open an independent tab");
            open.onClick(event -> root.openPageInNewTab("/"));
            root.add(new Section(clear, revoke, open));

            root.add(new Section(new TextWidget(
                "Edit the note, then navigate or use Back/Forward: the ID and note stay shared.")));
            root.add(new Section(new TextWidget(
                "A new or duplicated tab has a new context but keeps the same browser ID.")));
            root.add(new Section(new TextWidget(
                "Reload or revoke: a fresh context and note replace the old ones.")));
            root.add(new Section(new TextWidget(
                "Tab identity is not authorization. Real applications must check user rights.")));
        };
    }
}
