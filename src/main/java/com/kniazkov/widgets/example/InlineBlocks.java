/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.VerticalAlignment;
import com.kniazkov.widgets.view.InlineBlock;
import com.kniazkov.widgets.view.InlineBlockStyle;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

/**
 * A demonstration class showcasing the use of {@link InlineBlock} widgets.
 * <p>
 * This example creates a series of colored inline blocks with text labels, demonstrating how
 * inline blocks flow in the document while maintaining block-level styling properties like width,
 * height, padding, and borders.
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

public class InlineBlocks {
    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Page page = (root, parameters) -> {
            final Section section = new Section();
            root.add(section);

            final InlineBlockStyle style = InlineBlock.getDefaultStyle().derive();
            style.setWidth(100);
            style.setHeight(150);
            style.setMargin(2);
            style.setPadding(5, 2);
            style.setBorderColor(Color.BLACK);
            style.setBorderStyle(BorderStyle.SOLID);
            style.setBorderWidth(1);
            style.setBorderRadius(5);

            InlineBlock block = new InlineBlock(style);
            section.add(block);
            block.setBgColor(Color.RED);
            block.createText("Red");

            block = new InlineBlock(style);
            section.add(block);
            block.setBgColor(Color.ORANGE);
            block.createText("Orange");

            block = new InlineBlock(style);
            section.add(block);
            block.setBgColor(Color.YELLOW);
            block.createText("Yellow");

            block = new InlineBlock(style);
            section.add(block);
            block.setBgColor(Color.GREEN);
            block.createText("Green");

            block = new InlineBlock(style);
            section.add(block);
            block.setBgColor(Color.CYAN);
            block.createText("Cyan");

            block = new InlineBlock(style);
            section.add(block);
            block.setBgColor(Color.BLUE);
            block.createText("Blue");

            block = new InlineBlock(style);
            section.add(block);
            block.setBgColor(Color.VIOLET);
            block.createText("Violet");
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }
}
