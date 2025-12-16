/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.images.BufferedImageSource;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.images.CircleProgressBarCreator;
import com.kniazkov.widgets.images.ImageLoader;
import com.kniazkov.widgets.images.ImageProcessor;
import com.kniazkov.widgets.images.ImageSource;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.ImageWidget;
import com.kniazkov.widgets.view.Section;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * A demonstration program that allows uploading and displaying multiple images.
 * <p>
 * This example creates a file upload widget configured to accept multiple image files.
 * Uploaded images are automatically cropped to square aspect ratios and resized to
 * 300x300 pixels with high-quality rendering, then displayed in a gallery layout.
 *
 * <b>How to use</b>
 * <ol>
 *   <li>Run the program;</li>
 *   <li>
 *     Open your browser and go to
 *     <a href="http://localhost:8000">http://localhost:8000</a>.
 *   </li>
 * </ol>
 */
public class LoadImages {
    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Set<Listener<Integer>> listeners = new HashSet<>();
        final Page page = (root, parameters) -> {
            final Section main = new Section();
            root.add(main);

            final Section images = new Section();
            root.add(images);

            final CircleProgressBarCreator progress = new CircleProgressBarCreator(
                300,
                300
            );

            final FileLoader loader = new FileLoader("Click me");
            main.add(loader);
            loader.setMultipleInputFlag(true);
            loader.acceptImagesOnly();
            loader.onSelect(descriptor -> {
                final ImageWidget widget = new ImageWidget(
                    progress.getImageSource(0)
                );
                images.add(widget);
                widget.setWidth(300);
                widget.setHeight(300);
                widget.setBorderWidth(1);
                widget.setBorderColor(Color.BLACK);
                widget.setBorderStyle(BorderStyle.DASHED);
                widget.setMargin(5);
                final Listener<Integer> listener =
                    percent -> widget.setSource(progress.getImageSource(percent));
                listeners.add(listener);
                descriptor.getLoadingPercentageModel().addListener(listener);
                descriptor.onLoad(file-> {
                    try {
                        final BufferedImage original = ImageLoader.load(
                            file.getType(),
                            file.getContent()
                        );
                        final BufferedImage cropped = ImageProcessor.cropToSquare(original);
                        final BufferedImage resized = ImageProcessor.resizeToFit(
                            cropped,
                            300
                        );
                        widget.setSource(ImageSource.fromImage(resized));
                        widget.setBorderStyle(BorderStyle.SOLID);
                    } catch (final IOException ignored) {
                        images.remove(widget);
                    } finally {
                        listeners.remove(listener);
                    }
                });
            });
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
