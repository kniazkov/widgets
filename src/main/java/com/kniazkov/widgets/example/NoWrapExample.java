/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.InlineBlock;
import com.kniazkov.widgets.view.Link;
import com.kniazkov.widgets.view.NoWrap;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

/**
 * Demonstrates atomic inline groups. Run and open http://localhost:8000.
 */
public final class NoWrapExample {
    /**
     * Utility class.
     */
    private NoWrapExample() {
    }

    /**
     * Starts a live example with adjustable line width and reactive content.
     *
     * @param args unused command-line arguments
     */
    public static void main(final String[] args) {
        Server.start(new Application((root, context) -> {
            final TextWidget amount = new TextWidget("1 250");
            final NoWrap price = new NoWrap(amount, new TextWidget(" RUB"));
            final NoWrap policy = new NoWrap(new TextWidget("our "),
                new Link("privacy policy", "https://example.com/privacy"), new TextWidget("."));
            final InlineBlock line = new InlineBlock(new Section(
                new TextWidget("Price: "), price, new TextWidget(". Read "), policy));
            line.setWidth(240);
            final Button narrow = new Button("Narrow line");
            narrow.onClick(event -> line.setWidth(160));
            final Button wide = new Button("Wide line");
            wide.onClick(event -> line.setWidth(600));
            final Button update = new Button("Update price");
            update.onClick(event -> amount.setText("12 500"));
            root.add(new Section(new TextWidget("Groups wrap as a whole; resize the line:")));
            root.add(new Section(narrow, wide, update));
            root.add(new Section(line));
        }), new Options.Builder().build());
    }
}
