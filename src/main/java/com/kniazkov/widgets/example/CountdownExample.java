/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Countdown;
import com.kniazkov.widgets.view.Link;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.time.Instant;

/**
 * Demonstrates deadlines, zero, replacement and cached navigation at localhost:8080.
 */
public final class CountdownExample {
    /**
     * Prevents construction of the entry point.
     */
    private CountdownExample() {
    }

    /**
     * Starts the example.
     *
     * @param args unused arguments
     */
    public static void main(final String[] args) {
        final Application application = new Application(createPage("/second"));
        application.addPage("second", createPage("/"));
        Server.start(application, new Options.Builder().build());
    }

    /**
     * Creates a self-contained countdown demonstration page.
     *
     * @param next internal navigation destination
     * @return page factory
     */
    public static Page createPage(final String next) {
        return (root, context) -> {
            final Countdown countdown = new Countdown(Instant.now().plusSeconds(10));
            final Section holder = new Section(countdown);
            final Button restart = new Button("Restart 10 seconds");
            restart.onClick(event -> countdown.setDeadline(Instant.now().plusSeconds(10)));
            final Button hour = new Button("Set one hour");
            hour.onClick(event -> countdown.setDeadline(Instant.now().plusSeconds(3601)));
            final Button expire = new Button("Set past deadline");
            expire.onClick(event -> countdown.setDeadline(Instant.now().minusSeconds(1)));
            final Button remove = new Button("Remove countdown");
            remove.onClick(event -> countdown.remove());
            final Button attach = new Button("Attach countdown");
            attach.onClick(event -> {
                if (countdown.getParent().isEmpty()) {
                    holder.add(countdown);
                }
            });
            root.add(new Section(new TextWidget("Server deadline countdown")));
            root.add(holder);
            root.add(new Section(restart, hour, expire, remove, attach));
            root.add(new Section(new Link("Next countdown page", next)));
            root.add(new Section(new TextWidget(
                "Zero only changes the display. No order or stock action is performed.")));
        };
    }
}
