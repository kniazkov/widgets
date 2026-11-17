/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.PageContext;
import com.kniazkov.widgets.base.PublicDocument;
import com.kniazkov.widgets.base.PublicPage;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.RootWidget;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Public catalogue and long text available before JavaScript, with optional live controls.
 */
public final class PublicCatalog implements PublicPage {
    /**
     * Whether this instance displays a long document.
     */
    private final boolean article;

    /**
     * @param article whether to show the long text instead of the catalogue
     */
    public PublicCatalog(final boolean article) {
        this.article = article;
    }

    @Override
    public PublicDocument publicDocument(final Map<String, String> parameters) {
        final String title = this.article ? "Public terms" : "Public catalogue";
        final List<PublicDocument.Block> blocks = new ArrayList<>();
        blocks.add(PublicDocument.heading(1, title));
        if (this.article) {
            for (int index = 1; index <= 60; index++) {
                blocks.add(PublicDocument.heading(2, "Section " + index));
                blocks.add(PublicDocument.paragraph("Public information remains readable without "
                    + "JavaScript or a live widget session. Paragraph " + index));
            }
            blocks.add(PublicDocument.link("Catalogue", "/public-catalog"));
        } else {
            blocks.add(PublicDocument.paragraph("Handmade jewellery available to everyone."));
            blocks.add(PublicDocument.heading(2, "Glass pendant"));
            blocks.add(PublicDocument.paragraph("Price: 2500 RUB"));
            blocks.add(PublicDocument.image("/widgets-icon.svg", "Catalogue illustration",
                240, 120));
            blocks.add(PublicDocument.link("Read terms", "/public-terms"));
        }
        return new PublicDocument(title, "Public catalogue demonstration",
            "https://example.com/" + (this.article ? "public-terms" : "public-catalog"),
            "https://example.com/widgets-icon.svg", blocks);
    }

    @Override
    public void create(final RootWidget root, final PageContext context) {
        final TextWidget status = new TextWidget("Clicks: 0");
        final Button button = new Button("Try interaction");
        final int[] count = {0};
        button.onClick(event -> status.setText("Clicks: " + ++count[0]));
        root.add(new Section(button, status));
    }

    /**
     * @param args unused arguments
     */
    public static void main(final String[] args) {
        final Application application = new Application(new PublicCatalog(false));
        application.addPage("public-catalog", new PublicCatalog(false));
        application.addPage("public-terms", new PublicCatalog(true));
        Server.start(application, new Options.Builder().build());
    }
}
