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
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.images.CircleProgressBarCreator;
import com.kniazkov.widgets.images.ImageDecodeLimits;
import com.kniazkov.widgets.images.ImageLoader;
import com.kniazkov.widgets.images.ImageProcessor;
import com.kniazkov.widgets.images.ImageSource;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.ImageWidget;
import com.kniazkov.widgets.view.Section;
import java.awt.image.BufferedImage;
import java.io.IOException;

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
     * Creates the example.
     */
    public LoadImages() {
    }

    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final ImageDecodeLimits limits = ImageDecodeLimits.defaults();
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
                descriptor.getLoadingPercentageModel().addListener(listener);
                descriptor.onLoad(file -> {
                    if (!descriptor.executeOnRoot(() -> {
                        descriptor.getLoadingPercentageModel().removeListener(listener);
                        widget.setSource(progress.getWaitingImageSource());
                    })) {
                        return;
                    }
                    try {
                        if (file.getSize() > limits.maxEncodedBytes()) {
                            throw new IOException("Encoded image exceeds size limit");
                        }
                        final BufferedImage original = ImageLoader.load(
                            file.getType(), file.getContent(), limits);
                        final BufferedImage cropped = ImageProcessor.cropToSquare(original);
                        final BufferedImage resized = ImageProcessor.resizeToFit(cropped, 300);
                        final ImageSource source = ImageSource.fromImage(resized);
                        descriptor.executeOnRoot(() -> {
                            widget.setSource(source);
                            widget.setBorderStyle(BorderStyle.SOLID);
                        });
                    } catch (final IOException ex) {
                        descriptor.executeOnRoot(() -> widget.dispose());
                    }
                });
            });
        };

        final Application application = new Application(page);
        final Options options = new Options.Builder().build();
        Server.start(application, options);
    }
}
