/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.ImageWidget;
import com.kniazkov.widgets.view.InlineBlock;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import com.kniazkov.widgets.view.ZoomDecorator;
import com.kniazkov.widgets.view.ZoomDecoratorStyle;

/**
 * Demonstrates image inspection and zooming a composite inline widget.
 * Run from the repository root and open http://localhost:8080.
 */
public final class ZoomDecoratorExample {
    /**
     * Static entry point only.
     */
    private ZoomDecoratorExample() {
    }

    /**
     * Starts the example.
     * @param args unused arguments
     */
    public static void main(final String[] args) {
        final Page page = (root, parameters) -> {
            root.add(new Section(new TextWidget("Inspect a product image")));
            root.add(new Section(new TextWidget(
                "Pinch with two fingers or turn the mouse wheel to zoom. "
                    + "Drag enlarged content to pan; use Reset to return to the original view."
            )));
            final ImageWidget image = new ImageWidget("/house.png");
            image.setWidth(320);
            final ZoomDecoratorStyle photoStyle = ZoomDecoratorStyle.DEFAULT.derive();
            final ZoomDecorator photo = new ZoomDecorator(photoStyle, image);
            photo.setBgColor(Color.DARK_SLATE_GRAY);
            photo.setWidth(320);
            photo.setHeight(240);
            photo.setMaxWidth("100%");
            photo.setBorderStyle(BorderStyle.SOLID);
            photo.setBorderWidth(1);
            photo.setBorderColor(Color.GRAY);
            final Button reset = new Button("Reset image");
            reset.onClick(event -> photo.resetZoom());
            final Button limit = new Button("Limit to 3x");
            limit.onClick(event -> photoStyle.getMaxScaleModel().setData(3.0));
            final Button full = new Button("Allow 8x");
            full.onClick(event -> photoStyle.getMaxScaleModel().setData(8.0));
            final Button replace = new Button("Replace content with text");
            replace.onClick(event -> {
                final TextWidget text = new TextWidget("Any inline widget can be zoomed");
                text.setColor(Color.WHITE);
                photo.put(text);
            });
            final Button restore = new Button("Restore image");
            restore.onClick(event -> photo.put(image));
            root.add(new Section(photo));
            root.add(new Section(reset, limit, full, replace, restore));

            root.add(new Section(new TextWidget("Composite widget: image, text and button")));
            final ImageWidget thumbnail = new ImageWidget("/house.png");
            thumbnail.setWidth(220);
            final TextWidget count = new TextWidget("Button clicks: 0");
            count.setColor(Color.WHITE);
            final TextWidget caption = new TextWidget("House — detail card");
            caption.setColor(Color.WHITE);
            final java.util.concurrent.atomic.AtomicInteger clicks =
                new java.util.concurrent.atomic.AtomicInteger();
            final Button click = new Button("Try a normal click");
            click.onClick(event -> count.setText("Button clicks: " + clicks.incrementAndGet()));
            final InlineBlock card = new InlineBlock(
                new Section(caption),
                new Section(thumbnail), new Section(click), new Section(count)
            );
            card.setBgColor(Color.DARK_SLATE_GRAY);
            card.setPadding(12);
            final ZoomDecorator composite = new ZoomDecorator(card);
            composite.setBgColor(Color.DARK_SLATE_GRAY);
            composite.setWidth(300);
            composite.setHeight(300);
            composite.setMaxWidth("100%");
            final Button resetCard = new Button("Reset card");
            resetCard.onClick(event -> composite.resetZoom());
            root.add(new Section(composite));
            root.add(new Section(resetCard));
        };
        Server.start(new Application(page), new Options.Builder().setPort(8080).build());
    }
}
