/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.model.IntegerToStringModel;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * A demonstration program showcasing the {@link FileLoader} widget for file uploads.
 * <p>
 * This example creates a file upload button that, when clicked, allows users to
 * select and upload files. The program displays real-time upload progress for each file
 * and saves completed files to the server's filesystem upon successful upload.
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
public class LoadFiles {
    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Page page = (root, parameters) -> {
            final Section main = new Section();
            root.add(main);

            final FileLoader loader = new FileLoader("Click me");
            main.add(loader);
            loader.setMultipleInputFlag(true);
            loader.acceptImagesOnly();
            loader.onSelect(descriptor -> {
                final Section section = new Section();
                root.add(section);
                final TextWidget loading = new TextWidget("Loading");
                section.add(loading);
                section.add(new TextWidget(" '"));
                final TextWidget name = new TextWidget(descriptor.getName());
                name.setFontWeight(FontWeight.BOLD);
                section.add(name);
                section.add(new TextWidget("', type: '"));
                final TextWidget type = new TextWidget(descriptor.getType());
                type.setFontWeight(FontWeight.BOLD);
                section.add(type);
                section.add(new TextWidget("', "));
                final TextWidget percent = new TextWidget();
                percent.setFontWeight(FontWeight.BOLD);
                percent.setTextModel(
                    new IntegerToStringModel(descriptor.getLoadingPercentageModel())
                );
                section.add(percent);
                section.add(new TextWidget("%"));
                descriptor.onLoad(file-> {
                    section.add(new TextWidget(", size: "));
                    final TextWidget size = new TextWidget(String.valueOf(file.getSize()));
                    size.setFontWeight(FontWeight.BOLD);
                    section.add(size);
                    loading.setText("Loaded");
                    percent.setColor(Color.BLUE);
                    try {
                        Files.write(Paths.get(file.getName()), file.getContent());
                    } catch (IOException ignored) {
                    }
                });
            });
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
