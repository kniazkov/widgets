/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.e2e;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.base.StaticSource;
import com.kniazkov.widgets.view.ImageWidget;
import com.kniazkov.widgets.view.InlineBlock;
import com.kniazkov.widgets.view.SortableSection;
import com.kniazkov.widgets.view.ZoomDecorator;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Carousel;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.PasswordInput;
import com.kniazkov.widgets.view.TextArea;
import com.kniazkov.widgets.view.DropDownList;
import com.kniazkov.widgets.view.SuggestionField;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.StringModel;
import java.util.List;
import java.util.ArrayList;
import com.kniazkov.widgets.view.Link;
import com.kniazkov.widgets.view.MessagePopup;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Test-only application used by the Playwright end-to-end suite.
 */
public final class E2ETestServer {
    /**
     * Prevents construction of the static test entry point.
     */
    private E2ETestServer() {
        /*
         * Static entry point only.
         */
    }

    /**
     * Starts the test application on the port supplied by the cross-platform Node runner.
     *
     * @param args a single HTTP port argument
     */
    public static void main(final String[] args) throws java.io.IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected one HTTP port argument");
        }

        final Page page = (root, context) -> {
            final Section content = new Section();
            final TextWidget status = new TextWidget("Waiting for browser event");
            final Button button = new Button("Run full chain");
            final FileLoader loader = new FileLoader("Upload binary files");
            final Button popupLauncher = new Button("Show modal message");
            final Carousel carousel = new Carousel(
                "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' "
                    + "width='300' height='180'%3E%3Crect width='100%25' height='100%25' "
                    + "fill='red'/%3E%3C/svg%3E",
                "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' "
                    + "width='300' height='180'%3E%3Crect width='100%25' height='100%25' "
                    + "fill='blue'/%3E%3C/svg%3E"
            );
            carousel.setWidth(300);
            carousel.setHeight(180);
            loader.setMultipleInputFlag(true);

            button.onClick(event -> status.setText("Java handled the click"));
            popupLauncher.onClick(event -> {
                final Button cancel = new Button("Cancel");
                final Button close = new Button("Close modal message");
                final MessagePopup popup = new MessagePopup(
                    "Modal message", cancel, close
                );
                cancel.onClick(cancelEvent -> popup.remove());
                close.onClick(closeEvent -> popup.remove());
                root.add(popup);
            });
            carousel.onSelect(index -> status.setText("Carousel selected " + index));
            loader.onSelect(descriptor -> {
                final TextWidget upload = new TextWidget(
                    "Selected " + descriptor.getName() + " 0%"
                );
                descriptor.getLoadingPercentageModel().addListener(
                    percent -> upload.setText(
                        "Selected " + descriptor.getName() + " " + percent + "%"
                    )
                );
                descriptor.onLoad(file -> upload.setText(
                    "Loaded " + file.getName() + " 100% " + sha256(file.getContent())
                ));
                content.add(upload);
            });
            content.add(status);
            content.add(button);
            content.add(loader);
            content.add(popupLauncher);
            final Button dismissLauncher = new Button("Show dismissible modal");
            dismissLauncher.onClick(event -> {
                final Button toggle = new Button("Toggle outside closing");
                final MessagePopup popup = new MessagePopup("Dismissible modal", toggle);
                popup.setCloseOnOutsideClick(true);
                toggle.onClick(click -> popup.setCloseOnOutsideClick(
                    !popup.isCloseOnOutsideClick()
                ));
                root.add(popup);
            });
            content.add(dismissLauncher);
            content.add(carousel);
            root.add(content);
        };

        final StaticSource images = StaticSource.classpath("/cache-images", E2ETestServer.class,
            "/static-test");
        final String logoUrl = images.versionedUrl("logo.svg");
        final Options options = new Options.Builder()
            .setPort(Integer.parseInt(args[0]))
            .setBindAddress(InetAddress.getLoopbackAddress())
            .addStaticSource(images)
            .build();
        final Application application = new Application(page);
        application.addPage("form-controls", (root, context) -> {
            root.add(new Section(new InputField(), new PasswordInput(), new TextArea(),
                new SuggestionField("Suggestion"), new DropDownList("Option")));
            final InputField large = new InputField();
            large.setFontSize("20px");
            root.add(new Section(large));
            final TextWidget status = new TextWidget("Clicks: 0");
            final java.util.concurrent.atomic.AtomicInteger clicks =
                new java.util.concurrent.atomic.AtomicInteger();
            final Button button = new Button("Tap twice");
            button.onClick(event -> status.setText("Clicks: " + clicks.incrementAndGet()));
            root.add(new Section(button, status, new FileLoader("Choose file")));
        });
        application.addPage("suggestions", (root, context) -> {
            final StringModel fittings = new StringModel("Латунь с родиевым покрытием");
            final List<Model<String>> history = new ArrayList<>(List.of(
                fittings, new StringModel("Серебро"), new StringModel("Золото")
            ));
            final SuggestionField first = new SuggestionField(history);
            final SuggestionField next = new SuggestionField(history);
            final Button rename = new Button("Rename source value");
            rename.onClick(event -> fittings.setData("Родированная латунь"));
            final TextWidget current = new TextWidget();
            first.onTextInput(text -> current.setText("Value: " + text));
            final Button save = new Button("Save value");
            save.onClick(event -> {
                history.add(new StringModel(first.getText()));
                first.setSuggestionModels(history);
                next.setSuggestionModels(history);
            });
            root.add(new Section(first));
            root.add(new Section(save, rename, current));
            root.add(new Section(next));
            final SuggestionField colors = new SuggestionField("Black", "White", "Blue");
            colors.setSuggestionSeparatorModel(new StringModel(","));
            final TextWidget colorValue = new TextWidget();
            colors.onTextInput(text -> colorValue.setText("Colors: " + text));
            root.add(new Section(colors, colorValue));
        });
        application.addPage("image-cache", (root, context) -> {
            final Section container = new Section();
            final Runnable show = () -> {
                container.removeAll();
                final ImageWidget image = new ImageWidget(logoUrl);
                image.setIntrinsicSize(350, 100);
                image.setMaxWidth(200);
                image.setMaxHeight(80);
                container.add(image);
            };
            final Button recreate = new Button("Recreate logo");
            recreate.onClick(event -> show.run());
            root.add(new Section(recreate));
            root.add(container);
            root.add(new Section(new TextWidget("Below logo")));
            show.run();
        });
        application.addPage("catalog", (root, context) -> {
            final Section header = new Section();
            header.add(new InputField());
            final Link product = new Link("Open product");
            product.setHref("/product?id=42");
            header.add(product);
            final Button navigate = new Button("Navigate from Java");
            navigate.onClick(event -> root.goToPage("/product?id=43"));
            header.add(navigate);
            root.add(header);
            for (int index = 0; index < 80; index++) {
                final Section row = new Section();
                row.setTopPadding(20);
                row.setBottomPadding(20);
                row.add(new TextWidget("Catalog row " + index));
                root.add(row);
            }
        });
        application.addPage("product", (root, context) -> {
            final Section content = new Section();
            content.add(new TextWidget("Product " + context.parameters.get("id")));
            final Button clear = new Button("Clear cached pages");
            clear.onClick(event -> root.clearPageCache());
            content.add(clear);
            root.add(content);
        });
        application.addPage("fitted-zoom", (root, context) -> {
            final int width = Integer.parseInt(context.parameters.getOrDefault("width", "2400"));
            final int height = Integer.parseInt(context.parameters.getOrDefault("height", "1200"));
            final com.kniazkov.widgets.view.ImageWidget image =
                new com.kniazkov.widgets.view.ImageWidget(
                    "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='"
                        + width + "' height='" + height + "'%3E%3Crect width='100%25' "
                        + "height='100%25' fill='navy'/%3E%3C/svg%3E"
                );
            final ZoomDecorator zoom = new ZoomDecorator(image);
            zoom.setWidth(() -> "min(100vw, 100dvh)");
            zoom.setHeight(() -> "min(100vw, 100dvh)");
            zoom.setFitContent(true);
            final com.kniazkov.widgets.view.ModalPopup popup =
                new com.kniazkov.widgets.view.ModalPopup(new Section(zoom));
            popup.setWidth(() -> "min(100vw, 100dvh)");
            popup.setHeight(() -> "min(100vw, 100dvh)");
            popup.setPadding(0);
            popup.setBorderWidth(0);
            popup.setCloseOnOutsideClick(true);
            root.add(popup);
        });
        application.addPage("gestures", (root, context) -> {
            final SortableSection sortable = new SortableSection();
            final TextWidget orderStatus = new TextWidget("No reorder yet");
            for (int index = 1; index <= 3; index++) {
                final InlineBlock card = new InlineBlock(
                    new Section(new TextWidget("Sort card " + index))
                );
                card.setWidth(100);
                card.setHeight(80);
                card.setMargin(4);
                sortable.add(card);
            }
            sortable.onReorder(order -> orderStatus.setText("First: "
                + ((TextWidget) ((Section) ((InlineBlock) order.get(0)).getChild(0))
                    .getChild(0)).getText()));
            final Button slowSorting = new Button("Slow sorting");
            slowSorting.onClick(event -> sortable.getAnimationDurationModel().setData(600));
            final Button instantSorting = new Button("Instant sorting");
            instantSorting.onClick(event -> sortable.getAnimationDurationModel().setData(0));
            root.add(new Section(slowSorting, instantSorting));
            root.add(sortable);
            root.add(new Section(orderStatus));
            final TextWidget clicks = new TextWidget("Zoom clicks: 0");
            final java.util.concurrent.atomic.AtomicInteger count =
                new java.util.concurrent.atomic.AtomicInteger();
            final Button nested = new Button("Zoom child button");
            nested.onClick(event -> clicks.setText("Zoom clicks: " + count.incrementAndGet()));
            final InlineBlock content = new InlineBlock(new Section(nested));
            content.setWidth(300);
            content.setHeight(200);
            final ZoomDecorator zoom = new ZoomDecorator(content);
            zoom.setWidth(300);
            zoom.setHeight(200);
            final Button reset = new Button("Reset viewport");
            reset.onClick(event -> zoom.resetZoom());
            root.add(new Section(zoom));
            final Button limit = new Button("Limit zoom to 2x");
            limit.onClick(event -> zoom.getMaxScaleModel().setData(2.0));
            root.add(new Section(reset, limit, clicks));
        });
        Server.start(application, options);
    }

    /**
     * Calculates a stable digest for browser-to-Java binary verification.
     *
     * @param data uploaded bytes
     * @return lowercase SHA-256 digest
     */
    private static String sha256(final byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (final NoSuchAlgorithmException error) {
            throw new AssertionError("SHA-256 is unavailable", error);
        }
    }
}
