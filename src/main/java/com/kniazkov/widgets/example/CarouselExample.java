/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.HorizontalAlignment;
import com.kniazkov.widgets.images.ImageSource;
import com.kniazkov.widgets.images.SvgImageSource;
import com.kniazkov.widgets.view.Carousel;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

/**
 * Demonstrates carousel swipes, clicks, and selection events.
 */
public final class CarouselExample {
    /**
     * Prevents construction of the example entry point.
     */
    private CarouselExample() {
        /*
         * Static entry point only.
         */
    }

    /**
     * Starts the example server.
     *
     * @param args ignored command-line arguments
     */
    public static void main(final String[] args) {
        final Page page = (root, parameters) -> {
            final Carousel carousel = new Carousel(
                slide("#dc2626", "First"),
                slide("#2563eb", "Second"),
                slide("#16a34a", "Third")
            );
            carousel.setWidth(480);
            carousel.setHeight(320);

            final TextWidget status = new TextWidget("Selected image: 1");
            carousel.onSelect(index -> status.setText("Selected image: " + (index + 1)));
            carousel.onClick(event -> status.setText(
                "Clicked image: " + (carousel.getSelectedIndex() + 1)
            ));

            final Section images = new Section(carousel);
            images.setHorizontalAlignment(HorizontalAlignment.CENTER);
            final Section text = new Section(status);
            text.setHorizontalAlignment(HorizontalAlignment.CENTER);
            root.add(images);
            root.add(text);
        };

        Server.start(new Application(page), new Options.Builder().build());
    }

    /**
     * Creates a self-contained colored SVG slide.
     *
     * @param color slide color
     * @param label slide label
     * @return image source
     */
    private static ImageSource slide(final String color, final String label) {
        return new SvgImageSource() {
            @Override
            protected String getSvg() {
                return "<svg xmlns='http://www.w3.org/2000/svg' width='480' height='320'>"
                    + "<rect width='100%' height='100%' fill='" + color + "'/>"
                    + "<text x='50%' y='50%' text-anchor='middle' dominant-baseline='middle' "
                    + "fill='white' font-family='sans-serif' font-size='48'>"
                    + label + "</text></svg>";
            }
        };
    }
}
