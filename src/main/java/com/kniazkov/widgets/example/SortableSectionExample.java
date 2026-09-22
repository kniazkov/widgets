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
import com.kniazkov.widgets.view.InlineWidget;
import com.kniazkov.widgets.view.SortableSection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * A photo-order editor and a second sortable collection of arbitrary inline widgets.
 * Run from the repository root and open http://localhost:8080.
 */
public final class SortableSectionExample {
    /**
     * Static entry point only.
     */
    private SortableSectionExample() {
    }

    /**
     * Starts the example.
     * @param args unused arguments
     */
    public static void main(final String[] args) {
        final Page page = (root, parameters) -> {
            root.add(new Section(new TextWidget("Photo order editor")));
            root.add(new Section(new TextWidget(
                "Drag a card with a finger or mouse. Cards wrap on narrow screens. "
                    + "Keyboard: focus a card and press Alt+Left/Right."
            )));
            final SortableSection photos = new SortableSection();
            photos.setPadding(12);
            final Map<InlineWidget<?>, String> names = new IdentityHashMap<>();
            final AtomicInteger number = new AtomicInteger();
            final TextWidget status = new TextWidget();
            final Runnable showOrder = () -> status.setText("Saved order: "
                + photos.getChildren().stream().map(names::get).collect(Collectors.joining(", ")));
            final Runnable addPhoto = () -> {
                final String name = "Photo " + number.incrementAndGet();
                final ImageWidget image = new ImageWidget("/house.png");
                image.setWidth(160);
                final Button remove = new Button("Remove");
                final TextWidget caption = new TextWidget(name);
                caption.setColor(Color.WHITE);
                final InlineBlock card = new InlineBlock(
                    new Section(image), new Section(caption), new Section(remove)
                );
                card.setBgColor(Color.DARK_SLATE_GRAY);
                card.setPadding(12);
                card.setMargin(6);
                card.setBorderStyle(BorderStyle.SOLID);
                card.setBorderWidth(1);
                card.setBorderColor(Color.GRAY);
                names.put(card, name);
                remove.onClick(event -> {
                    photos.remove(card);
                    names.remove(card);
                    showOrder.run();
                });
                photos.add(card);
                showOrder.run();
            };
            for (int index = 0; index < 6; index++) {
                addPhoto.run();
            }
            photos.onReorder(order -> showOrder.run());
            final Button add = new Button("Add photo");
            add.onClick(event -> addPhoto.run());
            final Button reverse = new Button("Reverse on server");
            reverse.onClick(event -> {
                for (int index = 1; index < photos.getChildCount(); index++) {
                    photos.move(index, 0);
                }
                showOrder.run();
            });
            final TextWidget animation = new TextWidget("Animation: 250 ms");
            final Section animationControls = new Section(animation);
            for (final int duration : new int[] {0, 150, 250, 600}) {
                final Button speed = new Button(duration == 0 ? "No animation" : duration + " ms");
                speed.onClick(event -> {
                    photos.setAnimationDuration(duration);
                    animation.setText("Animation: " + duration + " ms");
                });
                animationControls.add(speed);
            }
            root.add(new Section(add, reverse));
            root.add(animationControls);
            root.add(photos);
            root.add(new Section(status));
            root.add(new Section(new TextWidget("Arbitrary inline content")));
            final SortableSection mixed = new SortableSection(
                new TextWidget("Plain text"),
                new InlineBlock(new Section(new TextWidget("A card with two lines")),
                    new Section(new TextWidget("No image required"))),
                new TextWidget("Another label")
            );
            mixed.setPadding(20);
            final TextWidget mixedStatus = new TextWidget("Move the labels or the card.");
            mixed.onReorder(order -> mixedStatus.setText(
                "New first widget: " + order.get(0).getType()
            ));
            root.add(mixed);
            root.add(new Section(mixedStatus));
        };
        Server.start(new Application(page), new Options.Builder().setPort(8080).build());
    }
}
