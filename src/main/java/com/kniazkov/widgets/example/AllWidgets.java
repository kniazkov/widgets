/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.images.MonochromaticImageSource;
import com.kniazkov.widgets.view.ActiveImage;
import com.kniazkov.widgets.view.ActiveText;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Cell;
import com.kniazkov.widgets.view.CheckBox;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.ImageWidget;
import com.kniazkov.widgets.view.InlineBlock;
import com.kniazkov.widgets.view.InlineWidget;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.MarginDecorator;
import com.kniazkov.widgets.view.Panel;
import com.kniazkov.widgets.view.PasswordInput;
import com.kniazkov.widgets.view.RootWidget;
import com.kniazkov.widgets.view.Row;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.Table;
import com.kniazkov.widgets.view.TextArea;
import com.kniazkov.widgets.view.TextWidget;

/**
 * Displays every concrete widget provided by the framework.
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
public class AllWidgets {
    /**
     * Creates the example.
     */
    public AllWidgets() {
    }

    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(final String[] args) {
        final Page page = (root, parameters) -> {
            addDescription(root, "RootWidget", "the entire page");

            final Section section = new Section();
            root.add(section);
            addName(section, "Section");
            section.add(new TextWidget("inline widget container"));

            final Panel panel = new Panel();
            root.add(panel);
            final Section panelContent = new Section();
            panel.add(panelContent);
            addName(panelContent, "Panel");
            panelContent.add(new TextWidget("block widget container"));

            final InlineBlock inlineBlock = new InlineBlock();
            inlineBlock.createText("block content inside an inline widget");
            addInlineExample(root, "InlineBlock", inlineBlock);

            final MarginDecorator margin = new MarginDecorator(new TextWidget("decorated text"));
            margin.setMargin(5);
            addInlineExample(root, "MarginDecorator", margin);

            addInlineExample(root, "TextWidget", new TextWidget("plain text"));
            addInlineExample(root, "ActiveText", new ActiveText("clickable text"));
            addInlineExample(root, "InputField", new InputField("editable text"));
            addInlineExample(root, "PasswordInput", new PasswordInput("password"));
            addInlineExample(root, "TextArea", new TextArea("multiline\ntext"));
            addInlineExample(root, "CheckBox", new CheckBox());
            addInlineExample(root, "Button", new Button("button"));
            addInlineExample(root, "FileLoader", new FileLoader("choose file"));

            final ImageWidget image = new ImageWidget(
                new MonochromaticImageSource(Color.BLUE, 80, 40)
            );
            addInlineExample(root, "ImageWidget", image);

            final ActiveImage activeImage = new ActiveImage(
                new MonochromaticImageSource(Color.GREEN, 80, 40)
            );
            addInlineExample(root, "ActiveImage", activeImage);

            addDescription(root, "Table", "contains the Row and Cell widgets below");
            final Table table = new Table();
            root.add(table);
            final Row row = new Row();
            table.add(row);
            final Cell rowCell = new Cell();
            row.add(rowCell);
            rowCell.createText("Row");
            final Cell cell = new Cell();
            row.add(cell);
            cell.createText("Cell");
        };

        final Application application = new Application(page);
        final Options options = new Options.Builder().build();
        Server.start(application, options);
    }

    /**
     * Adds a named inline widget on its own line.
     *
     * @param root page root
     * @param name widget class name
     * @param widget widget to display
     */
    private static void addInlineExample(final RootWidget root, final String name,
                                         final InlineWidget<?> widget) {
        final Section section = new Section();
        root.add(section);
        addName(section, name);
        section.add(widget);
    }

    /**
     * Adds a named text description on its own line.
     *
     * @param root page root
     * @param name widget class name
     * @param description description to display
     */
    private static void addDescription(final RootWidget root, final String name,
                                       final String description) {
        final Section section = new Section();
        root.add(section);
        addName(section, name);
        section.add(new TextWidget(description));
    }

    /**
     * Adds a bold name to a section.
     *
     * @param section target section
     * @param name name to display
     */
    private static void addName(final Section section, final String name) {
        final TextWidget title = new TextWidget(name + ": ");
        title.setFontWeight(FontWeight.BOLD);
        section.add(title);
    }
}
