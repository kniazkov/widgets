/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package example;

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
import java.util.List;
import java.util.Map;

/**
 * Standalone consumer of the minimal web distribution.
 */
public final class Minimal implements PublicPage {
    @Override
    public PublicDocument publicDocument(final Map<String, String> parameters) {
        return new PublicDocument("Minimal", "Module test", "https://example.com/",
            "https://example.com/icon.svg", List.of(PublicDocument.heading(1, "Minimal")));
    }

    @Override
    public void create(final RootWidget root, final PageContext context) {
        final TextWidget status = new TextWidget("Ready");
        final Button button = new Button("Test interaction");
        button.onClick(event -> status.setText("Clicked"));
        root.add(new Section(button, status));
    }

    /**
     * @param args listener port
     */
    public static void main(final String[] args) {
        Server.start(new Application(new Minimal()),
            new Options.Builder().setPort(Integer.parseInt(args[0])).build());
    }
}
