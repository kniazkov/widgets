/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.BoxShadow;
import com.kniazkov.widgets.common.BoxSizing;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.Cursor;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.common.TimingFunction;
import com.kniazkov.widgets.common.Transition;
import com.kniazkov.widgets.images.ImageSource;
import com.kniazkov.widgets.images.SvgImageSource;
import com.kniazkov.widgets.view.ActiveImage;
import com.kniazkov.widgets.view.ActiveImageStyle;
import com.kniazkov.widgets.view.ActiveText;
import com.kniazkov.widgets.view.ActiveTextStyle;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.ButtonStyle;
import com.kniazkov.widgets.view.Cell;
import com.kniazkov.widgets.view.CheckBox;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.ImageWidget;
import com.kniazkov.widgets.view.ImageWidgetStyle;
import com.kniazkov.widgets.view.InlineBlock;
import com.kniazkov.widgets.view.InlineBlockStyle;
import com.kniazkov.widgets.view.InlineWidget;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.InputFieldStyle;
import com.kniazkov.widgets.view.Panel;
import com.kniazkov.widgets.view.PanelStyle;
import com.kniazkov.widgets.view.PasswordInput;
import com.kniazkov.widgets.view.RootWidget;
import com.kniazkov.widgets.view.RootWidgetStyle;
import com.kniazkov.widgets.view.Row;
import com.kniazkov.widgets.view.RowStyle;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.SectionStyle;
import com.kniazkov.widgets.view.State;
import com.kniazkov.widgets.view.Table;
import com.kniazkov.widgets.view.TextArea;
import com.kniazkov.widgets.view.TextWidget;
import com.kniazkov.widgets.view.TextWidgetStyle;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Displays the framework's visible widgets as an interactive component gallery.
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
     * Main text color.
     */
    private static final Color TEXT = new Color(15, 23, 42);

    /**
     * Secondary text color.
     */
    private static final Color MUTED = new Color(71, 85, 105);

    /**
     * Primary accent color.
     */
    private static final Color PRIMARY = new Color(37, 99, 235);

    /**
     * Primary hover color.
     */
    private static final Color PRIMARY_HOVER = new Color(29, 78, 216);

    /**
     * Primary pressed color.
     */
    private static final Color PRIMARY_ACTIVE = new Color(30, 64, 175);

    /**
     * Destructive action color.
     */
    private static final Color DANGER = new Color(220, 38, 38);

    /**
     * Destructive action hover color.
     */
    private static final Color DANGER_HOVER = new Color(185, 28, 28);

    /**
     * Neutral border color.
     */
    private static final Color BORDER = new Color(203, 213, 225);

    /**
     * Soft page background color.
     */
    private static final Color PAGE_BG = new Color(226, 232, 240);

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
        final Page page = (root, parameters) -> buildGallery(root);
        final Application application = new Application(page);
        final Options options = new Options.Builder().build();
        Server.start(application, options);
    }

    /**
     * Builds the complete gallery for one page.
     *
     * @param root page root
     */
    private static void buildGallery(final RootWidget root) {
        configurePage(root);
        addIntroduction(root);
        addTextWidgets(root);
        addActiveTextWidgets(root);
        addInputFields(root);
        addPasswordInputs(root);
        addTextAreas(root);
        addCheckBoxes(root);
        addButtons(root);
        addFileLoaders(root);
        addImages(root);
        addActiveImages(root);
        addTables(root);
    }

    /**
     * Applies the page-level appearance.
     *
     * @param root page root
     */
    private static void configurePage(final RootWidget root) {
        final RootWidgetStyle style = RootWidget.getDefaultStyle().derive();
        style.setBgColor(PAGE_BG);
        root.setStyle(style);
    }

    /**
     * Adds the gallery heading.
     *
     * @param root page root
     */
    private static void addIntroduction(final RootWidget root) {
        final Panel panel = new Panel(cardStyle());
        panel.setMargin(24, 24, 24, 12);
        root.add(panel);
        panel.add(new Section(new TextWidget(
            textStyle(TEXT, "30px", FontWeight.BOLD), "All widgets"
        )));
        panel.add(new Section(new TextWidget(
            textStyle(MUTED, "15px", FontWeight.NORMAL),
            "A gallery of visible widgets, their states and common variations."
        )));
    }

    /**
     * Adds text widget variations.
     *
     * @param root page root
     */
    private static void addTextWidgets(final RootWidget root) {
        final Panel card = addCard(root, "TextWidget",
            "Static text with typography and color variations.");
        final Section row = variantRow();
        card.add(row);
        row.add(variant("Default", new TextWidget("Default text style")));
        row.add(variant("Muted", new TextWidget(
            textStyle(MUTED, "15px", FontWeight.NORMAL), "Secondary information"
        )));
        row.add(variant("Accent", new TextWidget(
            textStyle(PRIMARY, "16px", FontWeight.SEMIBOLD), "Important value: 42"
        )));
        final TextWidget italic = new TextWidget(
            textStyle(new Color(124, 58, 237), "18px", FontWeight.MEDIUM),
            "Expressive italic text"
        );
        italic.setItalic(true);
        row.add(variant("Italic", italic));
    }

    /**
     * Adds interactive text variations.
     *
     * @param root page root
     */
    private static void addActiveTextWidgets(final RootWidget root) {
        final Panel card = addCard(root, "ActiveText",
            "Clickable text with hover, pressed and click behavior.");
        final Section row = variantRow();
        card.add(row);
        final ActiveText link = new ActiveText("Open details");
        final TextWidget linkResult = feedback("Nothing clicked yet");
        link.onClick(event -> linkResult.setText("Details requested"));
        row.add(variant("Default", link, linkResult));
        final ActiveText success = activeText(
            "Mark as complete", new Color(5, 150, 105), new Color(4, 120, 87)
        );
        final TextWidget successResult = feedback("Nothing clicked yet");
        success.onClick(event -> successResult.setText("Item marked as complete"));
        row.add(variant("Positive action", success, successResult));
        final ActiveText danger = activeText("Delete draft", DANGER, DANGER_HOVER);
        final TextWidget dangerResult = feedback("Nothing clicked yet");
        danger.onClick(event -> dangerResult.setText("Delete action clicked"));
        row.add(variant("Destructive action", danger, dangerResult));
    }

    /**
     * Adds single-line input field variations.
     *
     * @param root page root
     */
    private static void addInputFields(final RootWidget root) {
        final Panel card = addCard(root, "InputField",
            "Editable text in normal, filled, invalid and disabled states.");
        final Section row = variantRow();
        card.add(row);
        row.add(variant("Default", new InputField("Ivan Kniazkov")));
        final InputFieldStyle filledStyle = InputField.getDefaultStyle().derive();
        filledStyle.setBgColor(State.NORMAL, new Color(248, 250, 252));
        filledStyle.setBgColor(State.HOVERED, new Color(241, 245, 249));
        row.add(variant("Filled", new InputField(filledStyle, "Filled field")));
        final InputField invalid = new InputField("ivan@");
        invalid.setValidState(false);
        row.add(variant("Invalid", invalid));
        final InputField disabled = new InputField("Unavailable value");
        disabled.disable();
        row.add(variant("Disabled", disabled));
    }

    /**
     * Adds password input variations.
     *
     * @param root page root
     */
    private static void addPasswordInputs(final RootWidget root) {
        final Panel card = addCard(root, "PasswordInput",
            "Password fields use the same states while hiding entered characters.");
        final Section row = variantRow();
        card.add(row);
        row.add(variant("Default", new PasswordInput("secret")));
        final PasswordInput invalid = new PasswordInput("123");
        invalid.setValidState(false);
        row.add(variant("Invalid", invalid));
        final PasswordInput disabled = new PasswordInput("password");
        disabled.disable();
        row.add(variant("Disabled", disabled));
    }

    /**
     * Adds multi-line text area variations.
     *
     * @param root page root
     */
    private static void addTextAreas(final RootWidget root) {
        final Panel card = addCard(root, "TextArea",
            "Multi-line editing with normal, invalid and disabled states.");
        final Section row = variantRow();
        card.add(row);
        row.add(variant("Default", new TextArea(
            "A longer message can be edited here.\nIt may span several lines.")));
        final TextArea invalid = new TextArea("This description is too short.");
        invalid.setValidState(false);
        row.add(variant("Invalid", invalid));
        final TextArea disabled = new TextArea("This generated note cannot be edited.");
        disabled.disable();
        row.add(variant("Disabled", disabled));
    }

    /**
     * Adds checkbox variations and click feedback.
     *
     * @param root page root
     */
    private static void addCheckBoxes(final RootWidget root) {
        final Panel card = addCard(root, "CheckBox",
            "Unchecked, checked and disabled selections with click feedback.");
        final Section row = variantRow();
        card.add(row);
        final CheckBox unchecked = new CheckBox();
        final TextWidget uncheckedResult = feedback("Notifications disabled");
        unchecked.onClick(event -> uncheckedResult.setText(
            "Notifications " + (unchecked.isChecked() ? "enabled" : "disabled")
        ));
        row.add(variant("Default", unchecked, uncheckedResult));
        final CheckBox checked = new CheckBox();
        checked.check();
        final TextWidget checkedResult = feedback("Automatic updates enabled");
        checked.onClick(event -> checkedResult.setText(
            "Automatic updates " + (checked.isChecked() ? "enabled" : "disabled")
        ));
        row.add(variant("Checked", checked, checkedResult));
        final CheckBox disabled = new CheckBox();
        disabled.check();
        disabled.disable();
        row.add(variant("Disabled", disabled));
    }

    /**
     * Adds button variations and click reactions.
     *
     * @param root page root
     */
    private static void addButtons(final RootWidget root) {
        final Panel card = addCard(root, "Button",
            "Default, primary, destructive and disabled actions.");
        final Section row = variantRow();
        card.add(row);
        final AtomicInteger clicks = new AtomicInteger();
        final Button standard = new Button("Default");
        final TextWidget standardResult = feedback("Not clicked yet");
        standard.onClick(event -> standardResult.setText(
            "Clicked " + clicks.incrementAndGet() + " time(s)"
        ));
        row.add(variant("Default", standard, standardResult));
        final Button primary = button(primaryButtonStyle(), "Primary", Color.WHITE);
        final TextWidget primaryResult = feedback("Not clicked yet");
        primary.onClick(event -> primaryResult.setText("Primary action clicked"));
        row.add(variant("Primary", primary, primaryResult));
        final Button destructive = button(dangerButtonStyle(), "Delete", Color.WHITE);
        final TextWidget dangerResult = feedback("Not clicked yet");
        destructive.onClick(event -> dangerResult.setText("Destructive action clicked"));
        row.add(variant("Destructive", destructive, dangerResult));
        final Button disabled = new Button("Disabled");
        disabled.disable();
        row.add(variant("Disabled", disabled));
    }

    /**
     * Adds file loader variations and upload feedback.
     *
     * @param root page root
     */
    private static void addFileLoaders(final RootWidget root) {
        final Panel card = addCard(root, "FileLoader",
            "File selection for any file, images only, or several files at once.");
        final Section row = variantRow();
        card.add(row);
        final FileLoader anyFile = new FileLoader("Choose file");
        final TextWidget anyFileResult = feedback("No file selected");
        attachUploadFeedback(anyFile, "File", anyFileResult);
        row.add(variant("Default", anyFile, anyFileResult));
        final FileLoader images = fileLoader("Choose image", primaryButtonStyle(), Color.WHITE);
        images.acceptImagesOnly();
        final TextWidget imageResult = feedback("No image selected");
        attachUploadFeedback(images, "Image", imageResult);
        row.add(variant("Images only", images, imageResult));
        final FileLoader multiple = new FileLoader("Choose files");
        multiple.setMultipleInputFlag(true);
        final TextWidget multipleResult = feedback("No files selected");
        attachUploadFeedback(multiple, "Multiple upload", multipleResult);
        row.add(variant("Multiple", multiple, multipleResult));
    }

    /**
     * Adds static image variations.
     *
     * @param root page root
     */
    private static void addImages(final RootWidget root) {
        final Panel card = addCard(root, "ImageWidget",
            "The same image source can be rounded, elevated or displayed as an avatar.");
        final Section row = variantRow();
        card.add(row);
        final ImageWidget standard = new ImageWidget(demoImage("#2563eb", "#7c3aed", "W"));
        row.add(variant("Default", standard));
        final ImageWidget elevated = new ImageWidget(demoImage("#059669", "#06b6d4", "UI"));
        final ImageWidgetStyle elevatedStyle = imageStyle(160, 96, 16);
        elevatedStyle.setBorderColor(BORDER);
        elevatedStyle.setBorderStyle(BorderStyle.SOLID);
        elevatedStyle.setBorderWidth(1);
        elevatedStyle.setBoxShadow(new BoxShadow(0, 8, 20, new Color(15, 23, 42, 38)));
        elevated.setStyle(elevatedStyle);
        row.add(variant("Elevated", elevated));
        final ImageWidget avatar = new ImageWidget(demoImage("#f97316", "#db2777", "IK"));
        final ImageWidgetStyle avatarStyle = imageStyle(96, 96, 48);
        avatarStyle.setBorderColor(Color.WHITE);
        avatarStyle.setBorderStyle(BorderStyle.SOLID);
        avatarStyle.setBorderWidth(4);
        avatarStyle.setBoxShadow(new BoxShadow(0, 6, 18, new Color(15, 23, 42, 45)));
        avatar.setStyle(avatarStyle);
        row.add(variant("Avatar", avatar));
    }

    /**
     * Adds interactive image variations and click reactions.
     *
     * @param root page root
     */
    private static void addActiveImages(final RootWidget root) {
        final Panel card = addCard(root, "ActiveImage",
            "Interactive images change appearance on hover and while pressed.");
        final Section row = variantRow();
        card.add(row);
        final ActiveImage tile = new ActiveImage(
            demoImage("#2563eb", "#7c3aed", "Open card")
        );
        final TextWidget tileResult = feedback("Not clicked yet");
        tile.onClick(event -> tileResult.setText("Card opened"));
        row.add(variant("Default", tile, tileResult));
        final ActiveImage avatar = activeImage(96, 96, 48, "Profile");
        final TextWidget avatarResult = feedback("Not clicked yet");
        avatar.onClick(event -> avatarResult.setText("Profile selected"));
        row.add(variant("Interactive avatar", avatar, avatarResult));
    }

    /**
     * Adds table variations.
     *
     * @param root page root
     */
    private static void addTables(final RootWidget root) {
        final Panel card = addCard(root, "Table",
            "Tables compose rows and cells, with optional row hover feedback.");
        card.add(blockVariant("Default", defaultTable()));
        card.add(blockVariant("Interactive rows", interactiveTable()));
    }

    /**
     * Creates a styled gallery card and adds it to the page.
     *
     * @param root page root
     * @param title card title
     * @param description short card description
     * @return created card
     */
    private static Panel addCard(final RootWidget root, final String title,
                                 final String description) {
        final Panel card = new Panel(cardStyle());
        card.setMargin(24, 12);
        root.add(card);
        card.add(new Section(new TextWidget(
            textStyle(TEXT, "22px", FontWeight.BOLD), title
        )));
        final Section descriptionLine = new Section(new TextWidget(
            textStyle(MUTED, "14px", FontWeight.NORMAL), description
        ));
        descriptionLine.setMargin(0, 3, 0, 10);
        card.add(descriptionLine);
        return card;
    }

    /**
     * Creates the common surface style for gallery cards.
     *
     * @return card style
     */
    private static PanelStyle cardStyle() {
        final PanelStyle style = Panel.getDefaultStyle().derive();
        style.setBgColor(Color.WHITE);
        style.setBorderColor(BORDER);
        style.setBorderStyle(BorderStyle.SOLID);
        style.setBorderWidth(1);
        style.setBorderRadius(14);
        style.setPadding(22, 18);
        style.setBoxSizing(BoxSizing.BORDER_BOX);
        style.setBoxShadow(new BoxShadow(0, 4, 16, new Color(15, 23, 42, 20)));
        return style;
    }

    /**
     * Creates a row used to display several variants.
     *
     * @return variant row
     */
    private static Section variantRow() {
        final SectionStyle style = Section.getDefaultStyle().derive();
        style.setPadding(0);
        return new Section(style);
    }

    /**
     * Wraps one inline widget in a labeled variation surface.
     *
     * @param caption variation caption
     * @param widget widget to display
     * @return inline variation surface
     */
    private static InlineBlock variant(final String caption, final InlineWidget<?> widget) {
        return variant(caption, widget, null);
    }

    /**
     * Wraps one inline widget and its individual feedback in a labeled surface.
     *
     * @param caption variation caption
     * @param widget widget to display
     * @param result optional action result displayed under the widget
     * @return inline variation surface
     */
    private static InlineBlock variant(final String caption, final InlineWidget<?> widget,
                                       final TextWidget result) {
        final InlineBlockStyle style = InlineBlock.getDefaultStyle().derive();
        style.setBgColor(new Color(248, 250, 252));
        style.setBorderColor(BORDER);
        style.setBorderStyle(BorderStyle.SOLID);
        style.setBorderWidth(1);
        style.setBorderRadius(10);
        style.setMargin(6);
        style.setPadding(14);
        style.setBoxSizing(BoxSizing.BORDER_BOX);
        final InlineBlock block = new InlineBlock(style);
        final Section captionLine = new Section(new TextWidget(
            textStyle(MUTED, "12px", FontWeight.SEMIBOLD), caption.toUpperCase()
        ));
        captionLine.setMargin(0, 0, 0, 10);
        block.add(captionLine);
        block.add(new Section(widget));
        if (result != null) {
            final Section resultLine = new Section(result);
            resultLine.setMargin(0, 0, 10, 0);
            block.add(resultLine);
        }
        return block;
    }

    /**
     * Creates a local action-result label for one widget variation.
     *
     * @param text initial result text
     * @return result text widget
     */
    private static TextWidget feedback(final String text) {
        return new TextWidget(textStyle(MUTED, "12px", FontWeight.NORMAL), text);
    }

    /**
     * Wraps a table in a labeled full-width variation surface.
     *
     * @param caption variation caption
     * @param widget table to display
     * @return block variation surface
     */
    private static Panel blockVariant(final String caption, final Table widget) {
        final PanelStyle style = Panel.getDefaultStyle().derive();
        style.setBgColor(new Color(248, 250, 252));
        style.setBorderColor(BORDER);
        style.setBorderStyle(BorderStyle.SOLID);
        style.setBorderWidth(1);
        style.setBorderRadius(10);
        style.setMargin(0, 8);
        style.setPadding(14);
        style.setBoxSizing(BoxSizing.BORDER_BOX);
        final Panel panel = new Panel(style);
        final Section captionLine = new Section(new TextWidget(
            textStyle(MUTED, "12px", FontWeight.SEMIBOLD), caption.toUpperCase()
        ));
        captionLine.setMargin(0, 0, 0, 10);
        panel.add(captionLine);
        panel.add(widget);
        return panel;
    }

    /**
     * Creates a text style used by labels and content.
     *
     * @param color text color
     * @param size CSS font size
     * @param weight font weight
     * @return text style
     */
    private static TextWidgetStyle textStyle(final Color color, final String size,
                                             final FontWeight weight) {
        final TextWidgetStyle style = TextWidget.getDefaultStyle().derive();
        style.setFontSize(size);
        style.setFontWeight(weight);
        style.setColor(color);
        return style;
    }

    /**
     * Creates an interactive text widget.
     *
     * @param text displayed text
     * @param color normal color
     * @param hoverColor hover color
     * @return active text widget
     */
    private static ActiveText activeText(final String text, final Color color,
                                         final Color hoverColor) {
        final ActiveTextStyle style = ActiveText.getDefaultStyle().derive();
        style.setColor(State.NORMAL, color);
        style.setColor(State.HOVERED, hoverColor);
        style.setColor(State.ACTIVE, TEXT);
        return new ActiveText(style, text);
    }

    /**
     * Creates a primary button style.
     *
     * @return primary button style
     */
    private static ButtonStyle primaryButtonStyle() {
        return coloredButtonStyle(PRIMARY, PRIMARY_HOVER, PRIMARY_ACTIVE, PRIMARY);
    }

    /**
     * Creates a destructive button style.
     *
     * @return destructive button style
     */
    private static ButtonStyle dangerButtonStyle() {
        return coloredButtonStyle(DANGER, DANGER_HOVER, new Color(153, 27, 27), DANGER);
    }

    /**
     * Creates a button style with a colored background.
     *
     * @param normal normal background
     * @param hovered hover background
     * @param active pressed background
     * @param focus focus color
     * @return button style
     */
    private static ButtonStyle coloredButtonStyle(final Color normal, final Color hovered,
                                                  final Color active, final Color focus) {
        final ButtonStyle style = Button.getDefaultStyle().derive();
        style.setBgColor(State.NORMAL, normal);
        style.setBgColor(State.HOVERED, hovered);
        style.setBgColor(State.FOCUSED, normal);
        style.setBgColor(State.ACTIVE, active);
        style.setBorderColor(normal);
        style.setBorderColor(State.HOVERED, hovered);
        style.setBorderColor(State.ACTIVE, active);
        style.setBorderColor(State.FOCUSED, focus);
        style.setBoxShadow(State.NORMAL,
            new BoxShadow(0, 2, 5, new Color(15, 23, 42, 28)));
        style.setBoxShadow(State.HOVERED,
            new BoxShadow(0, 5, 12, new Color(15, 23, 42, 40)));
        style.setBoxShadow(State.ACTIVE,
            new BoxShadow(0, 1, 3, new Color(15, 23, 42, 35)));
        return style;
    }

    /**
     * Creates a button with explicitly styled child text.
     *
     * @param style button style
     * @param text button label
     * @param color label color
     * @return button
     */
    private static Button button(final ButtonStyle style, final String text, final Color color) {
        return new Button(style, new TextWidget(
            textStyle(color, "14px", FontWeight.SEMIBOLD), text
        ));
    }

    /**
     * Creates a file loader with explicitly styled child text.
     *
     * @param text button label
     * @param style button style
     * @param color label color
     * @return file loader
     */
    private static FileLoader fileLoader(final String text, final ButtonStyle style,
                                         final Color color) {
        final FileLoader loader = new FileLoader(style, text);
        loader.put(new TextWidget(textStyle(color, "14px", FontWeight.SEMIBOLD), text));
        return loader;
    }

    /**
     * Connects a file loader to its local result label.
     *
     * @param loader file loader
     * @param kind short upload kind
     * @param result local upload result
     */
    private static void attachUploadFeedback(final FileLoader loader, final String kind,
                                             final TextWidget result) {
        loader.onSelect(file -> {
            result.setText(kind + " selected: " + file.getName());
            file.onLoad(uploaded -> result.setText(
                kind + " uploaded: " + uploaded.getName() + " (" + uploaded.getSize()
                    + " bytes)"
            ));
        });
    }

    /**
     * Creates a static image style.
     *
     * @param width image width
     * @param height image height
     * @param radius corner radius
     * @return image style
     */
    private static ImageWidgetStyle imageStyle(final int width, final int height,
                                               final int radius) {
        final ImageWidgetStyle style = ImageWidget.getDefaultStyle().derive();
        style.setWidth(width);
        style.setHeight(height);
        style.setBorderRadius(radius);
        style.setMargin(0);
        style.setBoxSizing(BoxSizing.BORDER_BOX);
        return style;
    }

    /**
     * Creates an interactive image with distinct sources for all pointer states.
     *
     * @param width image width
     * @param height image height
     * @param radius corner radius
     * @param label image label
     * @return active image
     */
    private static ActiveImage activeImage(final int width, final int height,
                                           final int radius, final String label) {
        final ActiveImage image = new ActiveImage(demoImage("#2563eb", "#7c3aed", label));
        image.setSource(State.HOVERED, demoImage("#1d4ed8", "#6d28d9", label));
        image.setSource(State.ACTIVE, demoImage("#1e3a8a", "#581c87", label));
        final ActiveImageStyle style = ActiveImage.getDefaultStyle().derive();
        style.setWidth(width);
        style.setHeight(height);
        style.setBorderRadius(radius);
        image.setStyle(style);
        return image;
    }

    /**
     * Creates a self-contained gradient SVG for image examples.
     *
     * @param first first gradient color
     * @param second second gradient color
     * @param label centered image label
     * @return image source
     */
    private static ImageSource demoImage(final String first, final String second,
                                         final String label) {
        return new SvgImageSource() {
            @Override
            protected String getSvg() {
                return "<svg xmlns='http://www.w3.org/2000/svg' width='160' height='96' "
                    + "viewBox='0 0 320 192'><defs><linearGradient id='g' x1='0' y1='0' "
                    + "x2='1' y2='1'><stop stop-color='" + first + "'/><stop offset='1' "
                    + "stop-color='" + second + "'/></linearGradient></defs>"
                    + "<rect width='320' height='192' fill='url(#g)'/>"
                    + "<circle cx='270' cy='38' r='54' fill='white' opacity='.12'/>"
                    + "<circle cx='48' cy='178' r='76' fill='white' opacity='.09'/>"
                    + "<text x='160' y='108' text-anchor='middle' fill='white' "
                    + "font-family='Arial,sans-serif' font-size='38' font-weight='700'>"
                    + label + "</text></svg>";
            }
        };
    }

    /**
     * Creates a table using only unmodified default styles.
     *
     * @return table widget
     */
    private static Table defaultTable() {
        final Table table = new Table();
        addDefaultTableRow(table, "Name", "Role", "Status");
        addDefaultTableRow(table, "Alice", "Designer", "Active");
        addDefaultTableRow(table, "Bob", "Developer", "Reviewing");
        addDefaultTableRow(table, "Carol", "Support", "Offline");
        return table;
    }

    /**
     * Adds one unmodified default row to a table.
     *
     * @param table target table
     * @param values cell values
     */
    private static void addDefaultTableRow(final Table table, final String... values) {
        final Row row = new Row();
        table.add(row);
        for (final String value : values) {
            final Cell cell = new Cell();
            cell.add(new Section(new TextWidget(value)));
            row.add(cell);
        }
    }

    /**
     * Creates a table whose data rows advertise clickability.
     *
     * @return table widget
     */
    private static Table interactiveTable() {
        final Table table = new Table();
        addTableRow(table, true, false, "Name", "Role", "Status");
        addTableRow(table, false, true, "Alice", "Designer", "Active");
        addTableRow(table, false, true, "Bob", "Developer", "Reviewing");
        addTableRow(table, false, true, "Carol", "Support", "Offline");
        return table;
    }

    /**
     * Adds one row to a table.
     *
     * @param table target table
     * @param header whether this is a header row
     * @param interactive whether hover styling is enabled
     * @param values cell values
     */
    private static void addTableRow(final Table table, final boolean header,
                                    final boolean interactive, final String... values) {
        final RowStyle rowStyle = Row.getDefaultStyle().derive();
        rowStyle.setBgColor(header ? new Color(241, 245, 249) : Color.WHITE);
        if (interactive) {
            rowStyle.setBgColor(State.HOVERED, new Color(239, 246, 255));
            rowStyle.setCursor(Cursor.POINTER);
            rowStyle.setTransition(new Transition(120, TimingFunction.EASE_OUT));
        }
        final Row row = new Row(rowStyle);
        table.add(row);
        for (final String value : values) {
            final Cell cell = new Cell();
            final TextWidgetStyle text = textStyle(
                header ? MUTED : TEXT,
                "14px",
                header ? FontWeight.SEMIBOLD : FontWeight.NORMAL
            );
            cell.add(new Section(new TextWidget(text, value)));
            row.add(cell);
        }
    }
}
