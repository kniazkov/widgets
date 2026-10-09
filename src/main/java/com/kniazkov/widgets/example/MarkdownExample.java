/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.model.StringModel;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Markdown;
import com.kniazkov.widgets.view.MarkdownStyle;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextArea;
import com.kniazkov.widgets.view.TextWidget;

/**
 * Live Markdown editor demonstrating the supported subset and reactive typography.
 * Run this class and open http://localhost:8000. The sample is not a legal document.
 */
public final class MarkdownExample {
    /**
     * Sample source covering every supported formatting construct.
     */
    private static final String DOCUMENT = """
        # Sample store policy
        This is a **demonstration**, not a legal document. Markdown uses an ordinary
        string model shared with the editor above.

        ## 1. Ordering
        Read the *description* and __price__ before placing an order.
        You can combine ***bold and italic*** and use ~~obsolete wording~~.

        1. Select an item.
        2. Confirm your details.
           - Name and telephone number
           - Delivery address
             - City
             - Street and building
        3. Wait for confirmation.

        ### 1.1. Delivery
        Delivery is arranged individually. A single newline
        continues the same paragraph.

        A new blank line begins another paragraph. Two trailing spaces or a backslash
        can force a line break:
        First line\\
        Second line

        #### Collection
        Contact the store before visiting.

        ##### Packaging
        Items are packed carefully.

        ###### Reminder
        Keep the order confirmation.

        > **Please note:** these are example terms.
        >
        > Quotes can contain paragraphs and lists:
        > - Contact support if you have questions.

        ---

        ## 2. Contact and technical notation
        [Website](https://example.com), [email](mailto:info@example.com),
        [telephone](tel:+70000000000), [relative link](/).

        Inline code: `order_id`, or double backticks: ``a ` character``.
        Escaped punctuation: \\*literal asterisks\\* and account_name.

        ```text
        order_id = 123
        <b>This is literal text, not HTML.</b>
        ```

        ~~~
        Tilde fences are supported too.
        ~~~

        HTML such as <script>alert('example')</script> is displayed literally.
        Images, tables, tasks, reference links and footnotes are not supported.
        """;

    /**
     * Prevents construction of this example entry point.
     */
    private MarkdownExample() {
    }

    /**
     * Starts the example server.
     *
     * @param args unused arguments
     */
    public static void main(final String[] args) {
        final Page page = (root, context) -> {
            final StringModel source = new StringModel(DOCUMENT);
            final MarkdownStyle style = Markdown.getDefaultStyle().derive();
            style.setFontFace(() -> "Georgia, serif");
            style.setFontSize("18px");
            style.setPadding(16);
            final Markdown document = new Markdown(style, source);
            final TextArea editor = new TextArea();
            editor.setTextModel(source);
            editor.setWidth(280);
            editor.setHeight(260);
            final Button smaller = new Button("16px");
            final Button larger = new Button("22px");
            final Button font = new Button("Sans serif");
            final Button reset = new Button("Reset document");
            smaller.onClick(event -> style.setFontSize("16px"));
            larger.onClick(event -> style.setFontSize("22px"));
            font.onClick(event -> style.setFontFace(() -> "Arial, sans-serif"));
            reset.onClick(event -> source.setData(DOCUMENT));
            root.add(new Section(new TextWidget("Edit Markdown source:")));
            root.add(new Section(editor));
            root.add(new Section(smaller, larger, font, reset));
            root.add(document);
        };
        Server.start(new Application(page), new Options.Builder().build());
    }
}
