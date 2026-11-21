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
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.common.StickySide;
import com.kniazkov.widgets.images.ImageSource;
import com.kniazkov.widgets.images.SvgImageSource;
import com.kniazkov.widgets.model.StringModel;
import com.kniazkov.widgets.model.BooleanModel;
import com.kniazkov.widgets.view.ActiveImage;
import com.kniazkov.widgets.view.ActiveImageStyle;
import com.kniazkov.widgets.view.ActiveText;
import com.kniazkov.widgets.view.ActiveTextStyle;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.ButtonStyle;
import com.kniazkov.widgets.view.Carousel;
import com.kniazkov.widgets.view.Countdown;
import com.kniazkov.widgets.view.Cell;
import com.kniazkov.widgets.view.CheckBox;
import com.kniazkov.widgets.view.ChoiceOption;
import com.kniazkov.widgets.view.ComboBox;
import com.kniazkov.widgets.view.Select;
import com.kniazkov.widgets.view.MultiSelect;
import com.kniazkov.widgets.view.CheckBoxWithText;
import com.kniazkov.widgets.view.RadioButtonWithText;
import com.kniazkov.widgets.view.DataProvider;
import com.kniazkov.widgets.view.DataTable;
import com.kniazkov.widgets.view.TreeView;
import com.kniazkov.widgets.view.SplitPane;
import com.kniazkov.widgets.view.Slider;
import com.kniazkov.widgets.model.SliderModel;
import com.kniazkov.widgets.model.IntegerModel;
import com.kniazkov.widgets.view.DropDownList;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.Form;
import com.kniazkov.widgets.view.DateField;
import com.kniazkov.widgets.view.TimeField;
import com.kniazkov.widgets.view.DateRange;
import java.math.BigDecimal;
import com.kniazkov.widgets.model.NumberModel;
import com.kniazkov.widgets.model.NumberValue;
import com.kniazkov.widgets.view.NumberField;
import com.kniazkov.widgets.view.Tabs;
import com.kniazkov.widgets.view.Menu;
import com.kniazkov.widgets.view.Popover;
import com.kniazkov.widgets.view.Tooltip;
import com.kniazkov.widgets.view.Accordion;
import com.kniazkov.widgets.view.Breadcrumbs;
import com.kniazkov.widgets.view.QuantityField;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import com.kniazkov.widgets.view.FormField;
import com.kniazkov.widgets.view.FormLayout;
import com.kniazkov.widgets.view.HeadingLevel;
import com.kniazkov.widgets.view.ImageWidget;
import com.kniazkov.widgets.view.ImageWidgetStyle;
import com.kniazkov.widgets.view.InlineBlock;
import com.kniazkov.widgets.view.InlineBlockStyle;
import com.kniazkov.widgets.view.InlineWidget;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.SuggestionField;
import com.kniazkov.widgets.model.Model;
import java.util.ArrayList;
import com.kniazkov.widgets.view.InputFieldStyle;
import com.kniazkov.widgets.view.Link;
import com.kniazkov.widgets.view.LinkStyle;
import com.kniazkov.widgets.view.MessagePopup;
import com.kniazkov.widgets.view.ModalPopup;
import com.kniazkov.widgets.view.Popup;
import com.kniazkov.widgets.view.MarginDecorator;
import com.kniazkov.widgets.view.TextFlow;
import java.time.Instant;
import com.kniazkov.widgets.view.OverlayStack;
import com.kniazkov.widgets.view.HorizontalLine;
import com.kniazkov.widgets.view.Panel;
import com.kniazkov.widgets.view.QrScanner;
import com.kniazkov.widgets.view.PanelStyle;
import com.kniazkov.widgets.view.PasswordInput;
import com.kniazkov.widgets.view.RadioButton;
import com.kniazkov.widgets.view.RadioGroup;
import com.kniazkov.widgets.view.ResponsiveGrid;
import com.kniazkov.widgets.view.RootWidget;
import com.kniazkov.widgets.view.RootWidgetStyle;
import com.kniazkov.widgets.view.Row;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.SectionStyle;
import com.kniazkov.widgets.view.State;
import com.kniazkov.widgets.view.SortableSection;
import com.kniazkov.widgets.view.ZoomDecorator;
import com.kniazkov.widgets.view.Stack;
import com.kniazkov.widgets.view.StickyPanel;
import com.kniazkov.widgets.view.StickyPanelStyle;
import com.kniazkov.widgets.view.Table;
import com.kniazkov.widgets.view.TableStyle;
import com.kniazkov.widgets.view.Markdown;
import com.kniazkov.widgets.view.NoWrap;
import com.kniazkov.widgets.view.TextArea;
import com.kniazkov.widgets.view.TextWidget;
import com.kniazkov.widgets.view.TextWidgetStyle;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
        final Page page = galleryPage();
        final Application application = new Application(page);
        final Options options = new Options.Builder()
            .setPort(args.length == 0 ? 8080 : Integer.parseInt(args[0]))
            .setTitle("Web Widgets — All Widgets")
            .setFaviconUrl("/widgets-icon.svg")
            .setDescription("Interactive examples of the Web Widgets components.")
            .setLanguage("en")
            .build();
        Server.start(application, options);
    }

    /**
     * Creates the gallery page for embedding or local browser verification.
     *
     * @return interactive gallery page
     */
    public static Page galleryPage() {
        return (root, parameters) -> buildGallery(root);
    }

    /**
     * Builds the complete gallery for one page.
     *
     * @param root page root
     */
    private static void buildGallery(final RootWidget root) {
        root.setTitle("All Widgets — Component Gallery");
        root.setDescription("Explore the interactive gallery of Web Widgets components.");
        configurePage(root);
        addIntroduction(root);
        addStickyPanel(root);
        final Panel separators = addCard(root, "HorizontalLine",
            "Reactive color, thickness, width, spacing and opacity.");
        separators.add(new HorizontalLine());
        final HorizontalLine accent = new HorizontalLine();
        accent.setColor(Color.fromString("#5278a3"));
        accent.setHeight(3);
        accent.setWidth("70%");
        accent.setVerticalMargin(12);
        separators.add(accent);
        final Button recolor = new Button("Change separator color");
        recolor.onClick(event -> accent.setColor(Color.fromString("#b89361")));
        separators.add(new Section(recolor));
        addTextWidgets(root);
        addMarkdown(root);
        addNoWrap(root);
        addActiveTextWidgets(root);
        addLinks(root);
        addInputFields(root);
        addNumericHints(root);
        addSuggestionFields(root);
        addKeyboardLogin(root);
        addFormDemo(root);
        addAnchoredOverlays(root);
        addNavigation(root);
        addNumberFields(root);
        addTemporalFields(root);
        addPasswordInputs(root);
        addTextAreas(root);
        addCheckBoxes(root);
        addRadioButtons(root);
        addLabeledChoices(root);
        addDropDownLists(root);
        addTypedChoices(root);
        addDataTable(root);
        addTreeView(root);
        addSplitPane(root);
        addSlider(root);
        addUploadManager(root);
        addFeedback(root);
        addDialogs(root);
        addButtons(root);
        addAccessibleSemantics(root);
        addPopups(root);
        addFileLoaders(root);
        addQrScanner(root);
        addImages(root);
        addActiveImages(root);
        addOverlayStacks(root);
        addTables(root);
        addGestureWidgets(root);
        addLayoutWidgets(root);
        addAdaptiveLayouts(root);
        addAdaptiveForm(root);
        addCountdown(root);
        addCarousel(root);
    }

    /**
     * Demonstrates local QR recognition without changing application data.
     * @param root gallery root
     */
    private static void addQrScanner(final RootWidget root) {
        final Panel card = addCard(root, "QrScanner",
            "Scan any QR: packaging, a link, Wi-Fi details or plain text. Results stay text.");
        final TextWidget result = new TextWidget("No QR received");
        final TextWidget status = new TextWidget("Camera is disabled.");
        final TextWidget toggleText = new TextWidget(
            Button.getDefaultStyle().getDefaultTextStyle(), "Enable camera");
        final Button toggle = new Button(toggleText);
        final QrScanner[] scanner = new QrScanner[1];
        scanner[0] = new QrScanner(code -> {
            result.setText(code);
            scanner[0].setPaused(false);
        });
        scanner[0].onStatus(state -> {
            final String message = switch (state) {
                case OPENING -> "Opening camera…";
                case READY -> "Show a QR. Remove it for two seconds before scanning it again.";
                case WAITING -> "Processing QR…";
                case HTTPS_REQUIRED -> "Camera access requires HTTPS.";
                case DENIED -> "Camera permission was denied.";
                case UNAVAILABLE -> "Camera unavailable.";
                case INVALID -> "This QR does not match the configured filter.";
                case CLOSED -> "Camera stopped.";
            };
            status.setText(message);
            switch (state) {
                case HTTPS_REQUIRED, DENIED, UNAVAILABLE, CLOSED -> {
                    scanner[0].disable();
                    toggleText.setText("Enable camera");
                }
                default -> { }
            }
        });
        toggle.onClick(event -> {
            scanner[0].setDisabledFlag(!scanner[0].isDisabled());
            toggleText.setText(scanner[0].isDisabled() ? "Enable camera" : "Disable camera");
            status.setText(scanner[0].isDisabled() ? "Camera is disabled." : "Opening camera…");
        });
        card.add(scanner[0]);
        card.add(new Section(toggle));
        card.add(new Section(status));
        card.add(new Section(result));
    }

    /**
     * Exercises native local dates, minute precision and inclusive date ranges in Form.
     * @param root gallery page
     */
    private static void addTemporalFields(final RootWidget root) {
        final Panel card = addCard(root, "Date, time and ranges",
            "Sample dates in 2028. Empty values stay empty; "
                + "no time zone is assumed.");
        final DateField date = new DateField();
        date.setBounds(LocalDate.of(2028, 1, 1), LocalDate.of(2028, 12, 31));
        date.setLocale(Locale.US);
        final FormField dateRow = new FormField("deliveryDate", "Delivery date", date);
        dateRow.setRequired(true);
        dateRow.setConstraintMessages("Enter a delivery date.", "Choose a date in 2028.");
        dateRow.setHelp("Use the calendar or type a date.");
        final TimeField time = new TimeField();
        time.setBounds(LocalTime.of(9, 0), LocalTime.of(18, 0));
        time.setLocale(Locale.US);
        final FormField timeRow = new FormField("deliveryTime", "Delivery time", time);
        timeRow.setRequired(true);
        timeRow.setConstraintMessages("Enter a delivery time.",
            "Time must be between 09:00 and 18:00.");
        timeRow.setHelp("Local time to the minute, without a time zone.");
        final DateRange range = new DateRange("stay", "Start date", "End date");
        range.setBounds(LocalDate.of(2028, 1, 1), LocalDate.of(2028, 12, 31));
        range.setLocale(Locale.US);
        range.setErrorMessage("Enter both dates, with the start no later than the end.");
        range.getStartField().setConstraintMessages("Enter a start date.", "Choose a date in "
            + "2028.");
        range.getEndField().setConstraintMessages("Enter an end date.", "Choose a date in 2028.");
        range.getStartField().setHelp(
            "Both endpoints are inclusive; leave both empty for no range.");
        final Form form = new Form("Sample delivery", "Validate dates");
        form.setMessages(new Form.Messages("Validating…", "Dates accepted.",
            "Check the dates and time.", "Validation failed."));
        form.addField(dateRow);
        form.addField(timeRow);
        form.addDateRange(range);
        final TextWidget result = new TextWidget("Dates have not been submitted");
        form.onSubmit(values -> {
            result.setText("Accepted: " + date.getValue().orElseThrow() + " / "
                + time.getValue().orElseThrow() + " / "
                + range.getValue().map(value -> value.start() + " — " + value.end())
                    .orElse("none"));
            return java.util.concurrent.CompletableFuture.completedFuture(Form.Result.success());
        });
        final Button locale = new Button("Switch locale: US / UK");
        locale.onClick(event -> {
            final Locale next = date.getLocale().equals(Locale.US)
                ? Locale.UK : Locale.US;
            date.setLocale(next);
            time.setLocale(next);
            range.setLocale(next);
            result.setText("Locale: " + next.toLanguageTag() + "; display: "
                + date.formatValue() + " / " + time.formatValue());
        });
        final DateField disabled = new DateField();
        disabled.setValue(LocalDate.of(2028, 2, 29));
        disabled.disable();
        final FormField disabledRow = new FormField(
            "unavailableDate", "Disabled date", disabled);
        card.add(form);
        card.add(new Section(locale));
        card.add(new Section(result));
        card.add(disabledRow);
    }

    /**
     * Demonstrates anchored actions, editable popovers and supplementary descriptions.
     * @param root gallery page
     */
    private static void addAnchoredOverlays(final RootWidget root) {
        final Panel card = addCard(root, "Menus, popovers and tooltips",
            "Escape closes the top layer. Tooltips supplement visible labels.");
        final TextWidget result = new TextWidget("Actions: 0");
        final java.util.concurrent.atomic.AtomicInteger count =
            new java.util.concurrent.atomic.AtomicInteger();
        final Button actions = new Button("Item actions");
        final Menu menu = new Menu(actions, "Item actions", List.of(
            new Menu.Item("edit", "Edit item description", true,
                () -> result.setText("Editing · Actions: " + count.incrementAndGet())),
            new Menu.Item("archive", "Archive (disabled)", false,
                () -> result.setText("Disabled actions must not run")),
            new Menu.Item("copy", "Create a copy", true,
                () -> result.setText("Copying · Actions: " + count.incrementAndGet()))));
        final Button longTrigger = new Button("Long menu");
        final Menu longMenu = new Menu(longTrigger, "Long action menu",
            java.util.stream.IntStream.rangeClosed(1, 16).mapToObj(index ->
                new Menu.Item("step-" + index,
                    "Additional action for the selected item #" + index, true,
                    () -> result.setText("Selected item " + index))).toList());
        final Button settings = new Button("Delivery settings");
        final Popover popover = new Popover(settings, "Delivery options");
        final InputField note = new InputField("Leave at reception");
        note.setAccessibleName("Delivery note");
        final TextWidget details = new TextWidget("Specify a convenient delivery time.");
        final Button grow = new Button("More details");
        grow.onClick(event -> details.setText(
            "Detailed information about delivery and collection. ".repeat(8)));
        final Button close = new Button("Done");
        close.onClick(event -> popover.close());
        popover.addBody(new Section(new TextWidget("Delivery note")));
        popover.addBody(new Section(note));
        popover.addBody(new Section(details));
        popover.addBody(new Section(grow, close));
        final Button help = new Button("Production time");
        final Tooltip tooltip = new Tooltip(help,
            "Timing depends on materials. Confirm the date before payment.");
        final Button disabled = new Button("Disabled menu");
        disabled.disable();
        final Menu disabledMenu = new Menu(disabled, "Disabled menu", List.of(
            new Menu.Item("none", "Disabled action", true, () -> result.setText("Error"))));
        final Button removeAnchor = new Button("Remove settings");
        removeAnchor.onClick(event -> settings.dispose());
        final Button dialogTrigger = new Button("Menu in a dialog");
        dialogTrigger.onClick(event -> {
            final var dialog = com.kniazkov.widgets.view.Dialog.confirmation(
                "Dialog with a menu", "Escape closes the menu first, then the dialog.",
                new com.kniazkov.widgets.view.Dialog.Messages("Confirm", "Cancel", "Close",
                    "Saving…", "Error", "Check the data"));
            dialog.setCloseOnEscape(true);
            final Button trigger = new Button("Nested actions");
            final Menu nested = new Menu(trigger, "Actions inside a dialog", List.of(
                new Menu.Item("nested", "Select in dialog", true,
                    () -> result.setText("Action from dialog"))));
            dialog.getContent().add(new Section(trigger));
            dialog.getContent().add(nested);
            root.add(dialog);
        });
        card.add(new Section(actions, settings, help));
        card.add(new Section(longTrigger, disabled, removeAnchor, dialogTrigger));
        card.add(new Section(result));
        card.add(menu);
        card.add(longMenu);
        card.add(popover);
        card.add(tooltip);
        card.add(disabledMenu);
    }

    /**
     * Demonstrates keyed navigation, retained drafts and native ancestor links.
     * @param root gallery page
     */
    private static void addNavigation(final RootWidget root) {
        final Panel card = addCard(root, "Navigation: tabs and sections",
            "Use arrows to move focus and Enter to open. Switching preserves content.");
        final Breadcrumbs path = new Breadcrumbs("Item path", List.of(
            new Breadcrumbs.Item("Home", "/"),
            new Breadcrumbs.Item("Handmade item catalog", "/all-widgets"),
            new Breadcrumbs.Item("Detailed information about the selected item", "")));
        card.add(path);
        final Tabs tabs = new Tabs();
        tabs.setAccessibleName("Item details");
        tabs.addTab("description", "Item description", () -> {
            final InputField draft = new InputField("My draft");
            draft.setAccessibleName("Item note");
            return new Panel(new Section(new TextWidget("The note is preserved when switching "
                + "tabs:")),
                new Section(draft));
        });
        tabs.addTab("delivery", "Delivery and collection", () ->
            new Section(new TextWidget("Choose collection or delivery.")));
        tabs.addTab("reviews", "Customer reviews", () ->
            new Section(new TextWidget("Customer reviews will appear here.")));
        tabs.setItemEnabled("reviews", false);
        tabs.addTab("hidden", "Hidden tab", () ->
            new Section(new TextWidget("Hidden content is not created.")));
        tabs.setItemVisible("hidden", false);
        card.add(tabs);
        final Button reorder = new Button("Reorder tabs");
        reorder.onClick(event ->
            tabs.reorder(List.of("delivery", "description", "reviews", "hidden").stream()
                .filter(tabs.getKeys()::contains).toList()));
        final Button replace = new Button("Update delivery");
        replace.onClick(event -> {
            if (tabs.getKeys().contains("delivery")) {
                tabs.replaceContent("delivery", () ->
                    new Section(new TextWidget("Delivery options updated by the server.")));
            }
        });
        final Button remove = new Button("Remove delivery");
        remove.onClick(event -> {
            if (tabs.getKeys().contains("delivery")) {
                tabs.removeItem("delivery");
            }
        });
        card.add(new Section(reorder, replace, remove));
        final Accordion single = new Accordion(false);
        single.setAccessibleName("Frequently asked questions");
        single.addSection("care", "How to care for handmade items", () ->
            new Section(new TextWidget("Store separately and wipe with a soft dry cloth.")));
        single.addSection("packaging", "Gift wrapping and special requests", () ->
            new Section(new TextWidget("We can wrap the item and include your note.")));
        single.getExpandedKeysModel().setData(java.util.Set.of("care"));
        card.add(single);
        final Accordion multiple = new Accordion(true);
        multiple.setAccessibleName("Multiple open sections");
        multiple.addSection("materials", "Item materials", () ->
            new Section(new TextWidget("Glass, pearls and metal fittings.")));
        multiple.addSection("warranty", "Warranty and service", () ->
            new Section(new TextWidget("Contact us if the item needs repair.")));
        multiple.getExpandedKeysModel().setData(java.util.Set.of("materials", "warranty"));
        card.add(multiple);
    }

    /**
     * Demonstrates exact decimal strings, localized drafts and reactive quantity constraints.
     * @param root gallery page
     */
    private static void addNumberFields(final RootWidget root) {
        final Panel card = addCard(root, "Numbers without precision loss",
            "Decimal commas, exact steps and server-side quantity validation.");
        final NumberField amount = new NumberField();
        amount.setLocale(Locale.forLanguageTag("ru"));
        amount.setConversionMessage("Enter a decimal with a comma, such as 12,50.");
        amount.setValue(new BigDecimal("9007199254740993.01"));
        final FormField price = new FormField("amount", "Exact amount", amount);
        price.setHelp("Step: 0.01. Values beyond JavaScript safe integers retain precision.");
        price.setRequired(true);
        price.setConstraintMessages("Enter an amount.", "Check precision and the 0.01 step.");
        final QuantityField quantity = new QuantityField("quantity", "Quantity");
        quantity.setActionNames("Decrease quantity", "Increase quantity");
        quantity.getEditor().getValueModel().configure(NumberValue.of(BigDecimal.ZERO),
            NumberValue.of(new BigDecimal("5")), BigDecimal.ONE);
        quantity.getEditor().setValue(new BigDecimal("2"));
        quantity.getEditor().setConversionMessage("Enter a whole quantity.");
        quantity.setConstraintMessages("Enter a quantity.", "Quantity is outside the current "
            + "bounds.");
        quantity.setRequired(true);
        quantity.setHelp("From 0 to 5, step 1. One press per step, without auto-repeat.");
        final Form form = new Form("Exact numbers", "Validate numbers");
        form.setMessages(new Form.Messages("Validating…", "Numbers accepted.",
            "Check the numbers.", "Validation failed."));
        form.addField(price);
        form.addField(quantity);
        final TextWidget result = new TextWidget("Numbers have not been submitted");
        form.onSubmit(values -> {
            result.setText("Exact: " + amount.getValue().orElseThrow().toPlainString()
                + " / " + quantity.getEditor().getValue().orElseThrow().toPlainString());
            return CompletableFuture.completedFuture(Form.Result.success());
        });
        final Button limit = new Button("Quantity limit: 2 / 5");
        final boolean[] small = {false};
        limit.onClick(event -> {
            small[0] = !small[0];
            quantity.getEditor().getValueModel().configure(NumberValue.of(BigDecimal.ZERO),
                NumberValue.of(new BigDecimal(small[0] ? "2" : "5")), BigDecimal.ONE);
            quantity.setHelp(small[0] ? "Current maximum: 2" : "Current maximum: 5");
        });
        final QuantityField disabled = new QuantityField(
            "disabledQuantity", "Disabled quantity");
        disabled.getEditor().setValue(BigDecimal.ONE);
        disabled.getEditor().disable();
        disabled.setActionNames("Decrease disabled quantity", "Increase disabled quantity");
        final NumberField english = new NumberField();
        english.setValue(new BigDecimal("1234.50"));
        english.setReadOnly(true);
        card.add(form);
        card.add(new Section(limit));
        card.add(new Section(result));
        card.add(disabled);
        card.add(new FormField("englishAmount", "Read-only number (en-US)", english));
    }

    /**
     * Exercises typed confirmation, a long validated form, retry and nested modal focus.
     * @param root gallery page
     */
    private static void addDialogs(final RootWidget root) {
        final Panel card = addCard(root, "Dialogs with results",
            "Sample form: the first submission fails; retry succeeds.");
        final var labels = new com.kniazkov.widgets.view.Dialog.Messages(
            "Confirm", "Cancel", "Close dialog", "Saving sample request…",
            "Could not save. Please retry.", "Check the request fields.");
        final TextWidget result = new TextWidget("Dialog has not finished");
        final Button confirm = new Button("Open confirmation");
        confirm.onClick(event -> {
            final var dialog = com.kniazkov.widgets.view.Dialog.confirmation(
                "Confirm action", "Confirm this sample action?", labels);
            dialog.setCloseOnOutsideClick(true);
            dialog.result().thenAccept(value -> result.setText("Result: " + value.reason()));
            root.add(dialog);
        });
        final Button edit = new Button("Open long form");
        edit.onClick(event -> {
            final var attempts = new java.util.concurrent.atomic.AtomicInteger();
            final TextWidget count = new TextWidget("Save attempts: 0");
            final var dialog = new com.kniazkov.widgets.view.Dialog<String>(
                "Sample request", labels, values -> {
                    final int attempt = attempts.incrementAndGet();
                    count.setText("Save attempts: " + attempt);
                    return CompletableFuture.supplyAsync(() -> {
                        if (attempt == 1) {
                            throw new IllegalStateException("Simulated unavailable service");
                        }
                        return values.get("sample");
                    }, CompletableFuture.delayedExecutor(2, java.util.concurrent.TimeUnit.SECONDS));
                });
            dialog.setCloseOnOutsideClick(true);
            dialog.getContent().add(new Section(count));
            final Button nested = new Button("Open nested confirmation");
            nested.onClick(innerEvent -> {
                final var inner = com.kniazkov.widgets.view.Dialog.confirmation(
                    "Nested confirmation", "This is a separate sample action.", labels);
                dialog.add(inner);
            });
            dialog.getContent().add(new Section(nested));
            final FormField sample = new FormField("sample", "Sample name");
            sample.setRequired(true);
            sample.setConstraintMessages("Enter a sample name.", "Check the name.");
            sample.setMaxLength(80);
            dialog.addField(sample);
            for (int index = 1; index <= 7; index++) {
                final FormField note = new FormField("note" + index,
                    "Additional note for stage " + index);
                note.setHelp("Optional field for testing long forms and scrolling.");
                dialog.addField(note);
            }
            dialog.result().thenAccept(value -> result.setText("Result: " + value.reason()
                + value.value().map(text -> " · " + text).orElse("")));
            root.add(dialog);
        });
        card.add(new Section(confirm, edit));
        card.add(new Section(result));
    }

    /**
     * Shows localized feedback and operation state without changing keyboard focus.
     * @param root gallery owner
     */
    private static void addFeedback(final RootWidget root) {
        final Panel card = addCard(root, "Messages and operation status",
            "Messages do not steal focus; errors stay until dismissed.");
        final var info = new com.kniazkov.widgets.view.Alert("Changes saved.");
        info.setSeverity(com.kniazkov.widgets.view.Feedback.Severity.SUCCESS);
        card.add(info);
        final var warning = new com.kniazkov.widgets.view.Alert(
            "Check the data before submitting.");
        warning.setSeverity(com.kniazkov.widgets.view.Feedback.Severity.WARNING);
        card.add(warning);
        final var error = new com.kniazkov.widgets.view.FieldError(
            "Check the address: a sample long field validation message.");
        card.add(error);
        final var empty = new com.kniazkov.widgets.view.EmptyState("You have no active orders.");
        final Button retry = new Button("Load again");
        empty.add(new Section(retry));
        card.add(empty);
        final var busy = new com.kniazkov.widgets.view.Busy("Checking item availability…");
        final Panel operation = new Panel(busy);
        card.add(operation);
        final Button start = new Button("Start check");
        start.onClick(event -> {
            operation.getChild(0).dispose();
            operation.add(new com.kniazkov.widgets.view.Busy("Checking item availability…"));
        });
        final Button noData = new Button("Show empty result");
        noData.onClick(event -> {
            empty.setText("You have no active orders.");
            ((com.kniazkov.widgets.view.Progress) operation.getChild(0)).finish(
                "Check complete: no orders",
                com.kniazkov.widgets.view.Feedback.Severity.INFO);
        });
        final Button fail = new Button("Check failed");
        fail.onClick(event -> ((com.kniazkov.widgets.view.Progress) operation.getChild(0)).finish(
            "Check failed. Please try again.",
            com.kniazkov.widgets.view.Feedback.Severity.ERROR));
        card.add(new Section(start, noData, fail));
        final var progress = new com.kniazkov.widgets.view.Progress("Uploading photos");
        progress.setPercentage(40);
        progress.onCancel("Cancel upload", "Upload cancelled", () ->
            info.setText("Cancellation requested; you can start a new operation."));
        card.add(progress);
        final var toast = new com.kniazkov.widgets.view.Toast(3, "Dismiss notification");
        toast.offer("Could not reach the server. Please retry.",
            com.kniazkov.widgets.view.Feedback.Severity.ERROR, 1000);
        card.add(toast);
        final Button notify = new Button("Show notification");
        notify.onClick(event -> {
            if (!toast.offer("Item added to the list.",
                    com.kniazkov.widgets.view.Feedback.Severity.SUCCESS, 5000)) {
                info.setText("Notification queue is full.");
            }
        });
        retry.onClick(event -> {
            if (!((com.kniazkov.widgets.view.Progress) operation.getChild(0)).isRunning()) {
                operation.getChild(0).dispose();
                operation.add(new com.kniazkov.widgets.view.Busy("Checking item availability…"));
            }
            empty.setText("Items found: 2");
            ((com.kniazkov.widgets.view.Progress) operation.getChild(0)).finish(
                "Check complete", com.kniazkov.widgets.view.Feedback.Severity.SUCCESS);
        });
        card.add(new Section(notify));
    }

    /**

     * Demonstrates managed transfers and idempotent completion.

     */
    private static void addUploadManager(final RootWidget root) {
        final Panel card = addCard(root, "UploadManager / UploadList",
            "Multiple files, per-file progress, cancellation and retry. Try a .txt file.");
        final var list = new com.kniazkov.widgets.view.UploadList();
        final java.util.Set<String> completed =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
        final java.util.Set<String> failed =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
        final var manager = new com.kniazkov.widgets.view.UploadManager(
            "Upload managed files", list,
            10, 2 * 1024 * 1024, (key, file) -> {
                if (completed.contains(key)) {
                    return;
                }
                if (file.getName().startsWith("retry") && failed.add(key)) {
                    throw new java.io.IOException("Demonstration failure");
                }
                Thread.sleep(800);
                completed.add(key);
            });
        manager.setAcceptedFiles(".txt,.bin");
        card.add(new Section(manager));
        card.add(list);
    }

    /**
     * Shared decimal selection; the disabled copy shows the last committed value.
     * @param root gallery page
     */
    private static void addSlider(final RootWidget root) {
        final Panel card = addCard(root, "Bracelet size",
            "Choose a length from 14 to 22 cm in steps of 0.25 cm.");
        final BigDecimal initial = new BigDecimal("18.50");
        final SliderModel length = new SliderModel(new BigDecimal("14"), new BigDecimal("22"),
            new BigDecimal("0.25"), initial);
        final Slider slider = new Slider(length, "Bracelet length", "cm");
        final Slider confirmed = new Slider(length, "Confirmed length", "cm");
        confirmed.disable();
        card.add(new Section(new TextWidget("Bracelet length")));
        card.add(slider);
        card.add(new Section(new TextWidget("Confirmed length")));
        card.add(confirmed);
        final Button reset = new Button("Reset length");
        reset.onClick(event -> length.setData(initial));
        card.add(new Section(reset));
    }

    /**
     * Resizable catalogue and nested detail panes sharing the same editing surface.
     * @param root gallery page
     */
    private static void addSplitPane(final RootWidget root) {
        final Panel card = addCard(root, "Catalog editor",
            "Drag the divider or focus it with Tab and use the arrow keys.");
        final Panel catalog = splitArea("Collections", "Stained glass", 232, 241, 248);
        final Panel details = splitArea("Pendant", "Northern lights", 242, 238, 247);
        final Panel notes = splitArea("Materials", "Glass, copper foil", 235, 244, 239);
        final SplitPane detailPane = new SplitPane(SplitPane.Axis.VERTICAL, "Card height",
            20, 80, new IntegerModel(55), details, notes);
        detailPane.setHeight("100%");
        final IntegerModel width = new IntegerModel(38);
        final SplitPane editor = new SplitPane(SplitPane.Axis.HORIZONTAL, "Collection list size",
            20, 75, width, catalog, detailPane);
        editor.setHeight(300);
        final Button reset = new Button("Reset sizes");
        reset.onClick(event -> {
            width.setData(38);
            detailPane.setPosition(55);
        });
        final BooleanModel locked = new BooleanModel(false);
        editor.setDisabledStateModel(locked);
        detailPane.setDisabledStateModel(locked);
        final CheckBoxWithText lock = new CheckBoxWithText(
            new StringModel("Lock sizes"), locked);
        card.add(editor);
        card.add(new Section(reset, lock));
    }

    /**
     * @param title area heading
     * @param text catalogue content
     * @param red red channel
     * @param green green channel
     * @param blue blue channel
     * @return sample pane content
     */
    private static Panel splitArea(final String title, final String text, final int red,
            final int green, final int blue) {
        final Panel area = new Panel();
        area.setPadding(10);
        area.setBgColor(new Color(red, green, blue));
        area.add(new Section(new TextWidget(textStyle(TEXT, "16px", FontWeight.BOLD), title)));
        area.add(new Section(new TextWidget(text)));
        return area;
    }

    /**
     * Demonstrates lazy category browsing and branch status announcements.
     * @param root gallery page
     */
    private static void addTreeView(final RootWidget root) {
        final Panel card = addCard(root, "Category tree",
            "Use arrows to move focus and Enter to select a category.");
        final TreeView tree = new TreeView(List.of(
            new TreeView.Node("glass", "Stained glass", true),
            new TreeView.Node("empty", "New collection", true),
            new TreeView.Node("offline", "Disabled archive", true)),
            key -> CompletableFuture.supplyAsync(() -> switch (key) {
                case "glass" -> List.of(new TreeView.Node("pendants", "Pendants", true),
                    new TreeView.Node("earrings", "Colored glass earrings", false));
                case "pendants" -> List.of(new TreeView.Node("northern",
                    "A collection of long handmade pendants featuring northern landscapes", false));
                case "empty" -> List.of();
                default -> throw new IllegalStateException("Archive unavailable");
            }, CompletableFuture.delayedExecutor(900, TimeUnit.MILLISECONDS)),
            new TreeView.Messages("Loading…", "Nothing here yet", "Loading failed"));
        tree.setAccessibleName("Item categories");
        final TextWidget toggleLabel = new TextWidget("Disable tree");
        final Button disable = new Button(toggleLabel);
        disable.onClick(event -> {
            tree.setDisabledFlag(!tree.isDisabled());
            toggleLabel.setText(tree.isDisabled() ? "Enable tree" : "Disable tree");
        });
        card.add(tree);
        card.add(new Section(disable));
    }

    /**
     * Demonstrates paged synthetic records, stable row identity and real asynchronous updates.
     * @param root gallery page
     */
    private static void addDataTable(final RootWidget root) {
        final Panel card = addCard(root, "Data table",
            "Sample archive: sorting, filters and selection with stable keys.");
        final List<DataProvider.DataRow> archive = new java.util.concurrent.CopyOnWriteArrayList<>(
            java.util.stream.IntStream.rangeClosed(1, 125).mapToObj(number ->
                demoOrder(number, "Completed")).toList());
        final DataTable table = new DataTable("Sample order archive", List.of(
            new DataTable.DataColumn("order-name", "Order name", true),
            new DataTable.DataColumn("phase", "Status", true),
            new DataTable.DataColumn("comment", "Reviewer note", false)), 5,
            query -> loadDemoOrders(archive, query),
            new DataTable.Messages("Order filter", "Select", "Previous", "Next",
                "Retry", "Loading rows…", "No matching orders.",
                "Archive temporarily unavailable.", "Page"));
        final Button update = new Button("Update first record");
        update.onClick(event -> {
            if (!archive.isEmpty() && archive.getFirst().key().equals("order-1")) {
                final var changed = demoOrder(1, "Updated");
                archive.set(0, changed);
                CompletableFuture.delayedExecutor(1400, TimeUnit.MILLISECONDS).execute(
                    () -> root.execute(() -> {
                        if (table.getRootWidget().orElse(null) == root) {
                            table.updateRow(changed);
                        }
                    }));
            }
        });
        final Button remove = new Button("Delete first order");
        remove.onClick(event -> {
            archive.removeIf(row -> row.key().equals("order-1"));
            table.removeRow("order-1");
            table.reload();
        });
        final Button disable = new Button("Enable / disable table");
        disable.onClick(event -> table.setDisabledFlag(!table.isDisabled()));
        card.add(new Section(update, remove, disable));
        card.add(table);
    }

    /**
     * Builds a synthetic archive record with separate stable key and visible values.
     * @param number logical identifier
     * @param phase display state
     * @return demo row
     */
    private static DataProvider.DataRow demoOrder(final int number, final String phase) {
        return new DataProvider.DataRow("order-" + number, Map.of(
            "order-name", "Sample order #" + number, "phase", phase,
            "comment", "History: sample record " + number));
    }

    /**
     * Reads and sorts a local synthetic list; nondeterministic work is represented by futures.
     * @param archive backing collection
     * @param query provider query
     * @return bounded asynchronous page
     */
    private static CompletableFuture<DataProvider.Page> loadDemoOrders(
            final List<DataProvider.DataRow> archive, final DataProvider.Query query) {
        final String filter = query.filter().strip().toLowerCase(java.util.Locale.ROOT);
        final List<DataProvider.DataRow> filtered = new ArrayList<>(archive.stream().filter(row ->
            switch (filter) {
                case "active" -> false;
                case "slow" -> row.key().equals("order-1");
                case "fast" -> row.key().equals("order-2");
                default -> row.cells().get("order-name").toLowerCase(java.util.Locale.ROOT)
                    .contains(filter);
            }).toList());
        if (!query.sortKey().isEmpty()) {
            final java.util.Comparator<DataProvider.DataRow> order =
                java.util.Comparator.comparing(row -> row.cells().get(query.sortKey()));
            filtered.sort(query.descending() ? order.reversed() : order);
        }
        final int first = (int) Math.min(query.offset(), filtered.size());
        final int last = Math.min(first + query.size(), filtered.size());
        final var result = new CompletableFuture<DataProvider.Page>() {
            @Override
            public boolean cancel(final boolean interrupt) {
                /*
                 * The slow simulated request finishes despite best-effort cancellation.
                 */
                return false;
            }
        };
        CompletableFuture.delayedExecutor(filter.equals("slow") ? 2700 : 220,
            TimeUnit.MILLISECONDS).execute(() -> {
                if (filter.equals("error")) {
                    result.completeExceptionally(new IllegalArgumentException("Demo failure"));
                } else {
                    result.complete(new DataProvider.Page(filtered.subList(first, last),
                        filtered.size()));
                }
            });
        return result;
    }

    /**
     * Shows independent single, multiple, strict search and free text values.
     * @param root gallery page
     */
    private static void addTypedChoices(final RootWidget root) {
        final Panel card = addCard(root, "Typed selection",
            "Identical labels preserve distinct keys; this demo uses sample data.");
        final List<ChoiceOption<Integer>> options = List.of(
            new ChoiceOption<>("paper-a", "Paper", 101),
            new ChoiceOption<>("paper-b", "Paper", 102),
            new ChoiceOption<>("ink", "Ink", 103),
            new ChoiceOption<>("archive", "Archive (disabled)", 104, true)
        );
        final Select<Integer> single = new Select<>(options);
        single.setAccessibleName("Material: single");
        single.setPlaceholder("Choose a material");
        final TextWidget result = new TextWidget("Material code: none");
        single.onChoose(keys -> result.setText("Material code: "
            + (keys.isEmpty() ? "none" : single.getSelectedValues().getFirst())));
        final Button reorder = new Button("Reorder materials");
        reorder.onClick(event -> single.setOptions(options.reversed()));
        final Button remove = new Button("Remove second paper");
        remove.onClick(event -> {
            single.setOptions(options.stream().filter(item -> !item.key().equals("paper-b"))
                .toList());
            result.setText("Material code: "
                + (single.getSelectedValues().isEmpty() ? "none"
                    : single.getSelectedValues().getFirst()));
        });
        card.add(new Section(single, result, reorder, remove));

        final MultiSelect<Integer> multi = new MultiSelect<>(options);
        multi.setAccessibleName("Materials: multiple");
        multi.setPlaceholder("Choose one or more");
        multi.getSelectionModel().setData(List.of("paper-a", "ink"));
        final Button clear = new Button("Clear materials");
        clear.onClick(event -> multi.clear());
        final Select<Integer> disabled = new Select<>(options);
        disabled.setAccessibleName("Disabled selection");
        disabled.setPlaceholder("Selection unavailable");
        disabled.disable();
        card.add(new Section(multi, clear, disabled));

        final ComboBox<Integer> search = new ComboBox<>(List.of(), false);
        search.setAccessibleName("Warehouse search");
        search.setPlaceholder("For example, slow or fast");
        final TextWidget outcome = new TextWidget("No warehouse selected");
        search.onChoose(keys -> outcome.setText(keys.isEmpty() ? "No warehouse selected"
            : "Warehouse code: " + search.getSelectedValues().getFirst()));
        search.setProvider(AllWidgets::findWarehouse);
        card.add(new Section(search, outcome));

        final ComboBox<Integer> free = new ComboBox<>(options, true);
        free.setAccessibleName("Note or material");
        free.setPlaceholder("Free text or an existing material");
        free.setValidState(false);
        card.add(new Section(free, new TextWidget("Free text example (invalid field)")));
    }

    /**
     * Sample async provider includes a task that observes cancellation but finishes anyway.
     * @param query current search
     * @param cursor opaque page cursor
     * @return future with only fabricated catalogue items
     */
    private static CompletableFuture<ComboBox.Page<Integer>> findWarehouse(final String query,
            final String cursor) {
        final CompletableFuture<ComboBox.Page<Integer>> result = new CompletableFuture<>() {
            @Override
            public boolean cancel(final boolean interrupt) {
                /*
             * This demo models a provider that cannot abort already-started I/O.
             */
                return false;
            }
        };
        final long delay = query.equals("slow") ? 3000 : 140;
        CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS).execute(() -> {
            if (query.equals("error")) {
                result.completeExceptionally(new IllegalStateException("Demo provider failed"));
                return;
            }
            final List<ChoiceOption<Integer>> items;
            if (query.equals("none")) {
                items = List.of();
            } else if (query.equals("slow")) {
                items = List.of(new ChoiceOption<>("slow", "Slow result", 201));
            } else if (query.equals("fast")) {
                items = List.of(new ChoiceOption<>("fast", "Fast result", 202));
            } else if (cursor.isEmpty()) {
                items = List.of(new ChoiceOption<>("main", "Main warehouse", 203),
                    new ChoiceOption<>("long",
                        "VeryLongWarehouseLabelWithoutSpacesToTestWrappingOnMobileScreens", 204),
                    new ChoiceOption<>("disabled", "Closed warehouse", 205, true));
            } else {
                items = java.util.stream.IntStream.rangeClosed(1, 20)
                    .mapToObj(number -> new ChoiceOption<>("extra-" + number,
                        "Backup warehouse " + number, 300 + number)).toList();
            }
            final String next = cursor.isEmpty() && items.size() == 3 ? "more" : "";
            result.complete(new ComboBox.Page<>(items, next));
        });
        return result;
    }

    /**
     * Shows the visible composition of the basic layout widgets.
     * @param root gallery root
     */
    private static void addLayoutWidgets(final RootWidget root) {
        final Panel card = addCard(root, "Layout composition",
            "RootWidget contains Panels; Sections contain InlineBlocks and inline widgets.");
        final TextFlow flow = new TextFlow();
        flow.add(new TextWidget("TextFlow wraps text and "));
        flow.add(new Link("links", "#"));
        flow.add(new TextWidget(" together as a paragraph, preserving ordinary spaces."));
        card.add(flow);
        final MarginDecorator spaced = new MarginDecorator(new TextWidget("24 px margin"));
        spaced.setMargin(24);
        final InlineBlock block = new InlineBlock(new Section(spaced));
        block.setBgColor(new Color(226, 232, 240));
        card.add(new Section(new TextWidget("Section:"), block, new TextWidget("InlineBlock")));
    }

    /**
     * Shows one stack that changes direction and responsive grids in two container widths.
     * @param root gallery root
     */
    private static void addAdaptiveLayouts(final RootWidget root) {
        final Panel card = addCard(root, "Adaptive Stack and ResponsiveGrid",
            "Layout follows container width, not the device type.");
        final Stack stack = new Stack(Stack.Direction.HORIZONTAL);
        stack.setGap(8);
        stack.setAlignment(Stack.Alignment.START);
        stack.add(adaptiveTile("Alpha · Stack"));
        stack.add(adaptiveTile("Beta · Stack"));
        final Button flip = new Button("Switch Stack direction");
        final AtomicInteger flips = new AtomicInteger();
        flip.onClick(event -> stack.setDirection(flips.incrementAndGet() % 2 == 1
            ? Stack.Direction.VERTICAL : Stack.Direction.HORIZONTAL));
        card.add(stack);
        card.add(new Section(flip));
        final ResponsiveGrid grid = new ResponsiveGrid();
        grid.setGap(10);
        grid.setColumnWidths(138, 0);
        grid.add(adaptiveTile("Wide grid A"));
        grid.add(adaptiveTile("Wide grid B"));
        grid.add(adaptiveTile("LongWordWithoutSpacesToTestGridWrapping"));
        card.add(grid);
        final Panel narrow = new Panel();
        final ResponsiveGrid inner = new ResponsiveGrid();
        inner.setWidth(225);
        inner.setGap(6);
        inner.setColumnWidths(128, 0);
        inner.add(adaptiveTile("Narrow grid A"));
        inner.add(adaptiveTile("Narrow grid B"));
        narrow.add(inner);
        card.add(new Section(feedback("Nested grid: container no wider than 225 px.")));
        card.add(narrow);
    }

    /**
     * Creates a display tile with no fixed minimum content width.
     * @param text visible label
     * @return block used by the adaptive layout demo
     */
    private static Panel adaptiveTile(final String text) {
        final TextFlow flow = new TextFlow();
        flow.add(new TextWidget(text));
        final Panel tile = new Panel(flow);
        tile.setBgColor(new Color(226, 232, 240));
        tile.setPadding(7);
        return tile;
    }

    /**
     * Shows the same native labeled fields in columns or rows without replacing nodes.
     * @param root gallery root
     */
    private static void addAdaptiveForm(final RootWidget root) {
        final Panel card = addCard(root, "Adaptive form",
            "Wide container: labels on the left. Narrow: labels above fields.");
        final FormLayout layout = new FormLayout();
        layout.setBreakpoint(480);
        layout.setGap(10);
        final Form form = new Form("Adaptive sample request", "Validate layout", layout);
        form.setMessages(new Form.Messages("Validating layout…", "Layout validated.",
            "Complete the highlighted field.", "Could not validate the layout."));
        final FormField title = new FormField("title",
            "Full sample item description for testing label wrapping");
        title.setRequired(true);
        title.setConstraintMessages("Enter a description.", "Check the description.");
        title.setHelp("In two-column mode, this hint stays beside the field.");
        title.getEditor().setWidth(300);
        final FormField note = new FormField("note", "Additional note");
        note.getEditor().setWidth(300);
        form.addField(title);
        form.addField(note);
        card.add(form);
        final Button breakpoint = new Button("Switch form breakpoint");
        final AtomicInteger changes = new AtomicInteger();
        breakpoint.onClick(event -> layout.setBreakpoint(changes.incrementAndGet() % 2 == 1
            ? 800 : 480));
        card.add(new Section(breakpoint));
    }

    /**
     * Shows a running and expired server deadline.
     * @param root gallery root
     */
    private static void addCountdown(final RootWidget root) {
        final Panel card = addCard(root, "Countdown",
            "The browser counts down locally; reaching zero has no business effect.");
        final Countdown running = new Countdown(Instant.now().plusSeconds(1200));
        final Button restart = new Button("Restart 20 minutes");
        restart.onClick(event -> running.setDeadline(Instant.now().plusSeconds(1200)));
        card.add(new Section(variant("Running", running),
            variant("Expired", new Countdown(Instant.EPOCH)), variant("Reset", restart)));
    }

    /**
     * Shows an image sequence and server-observed selection.
     * @param root gallery root
     */
    private static void addCarousel(final RootWidget root) {
        final Panel card = addCard(root, "Carousel",
            "Swipe or drag between three images; selection travels to the server.");
        final Carousel carousel = new Carousel(demoImage("#2563eb", "#7c3aed", "1 / 3"),
            demoImage("#059669", "#0d9488", "2 / 3"),
            demoImage("#ea580c", "#dc2626", "3 / 3"));
        carousel.setWidth(280);
        carousel.setHeight(160);
        final TextWidget selected = feedback("Selected image: 1");
        carousel.onSelect(event -> selected.setText("Selected image: "
            + (carousel.getSelectedIndexModel().getData() + 1)));
        card.add(new Section(carousel));
        card.add(new Section(selected));
    }

    /**
     * Adds compact sorting and zoom examples.
     * @param root gallery root
     */
    private static void addGestureWidgets(final RootWidget root) {
        final Panel sorting = addCard(root, "SortableSection",
            "Drag inline cards with a mouse or finger; Alt+Left/Right also changes order.");
        final SortableSection items = new SortableSection();
        for (int index = 1; index <= 3; index++) {
            final InlineBlock tile = new InlineBlock(new Section(new TextWidget("Card " + index)));
            tile.setPadding(18);
            tile.setMargin(6);
            tile.setBgColor(new Color(226, 232, 240));
            items.add(tile);
        }
        final TextWidget status = new TextWidget("Reorder the cards.");
        items.onReorder(order -> status.setText("First widget ID: " + order.get(0).getId()));
        sorting.add(items);
        sorting.add(new Section(status));
        final Panel zooming = addCard(root, "ZoomDecorator",
            "Pinch or use the wheel to zoom; drag to pan enlarged content.");
        final ImageWidget image = new ImageWidget("/house.png");
        image.setWidth(280);
        final ZoomDecorator zoom = new ZoomDecorator(image);
        zoom.setBgColor(Color.DARK_SLATE_GRAY);
        zoom.setWidth(280);
        zoom.setHeight(210);
        zoom.setMaxWidth("100%");
        final Button reset = new Button("Reset zoom");
        reset.onClick(event -> zoom.resetZoom());
        zooming.add(new Section(zoom));
        zooming.add(new Section(reset));
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
        final TextWidget heading = new TextWidget(
            textStyle(TEXT, "30px", FontWeight.BOLD), "All widgets"
        );
        heading.setHeadingLevel(HeadingLevel.H1);
        panel.add(new Section(heading));
        panel.add(new Section(new TextWidget(
            textStyle(MUTED, "15px", FontWeight.NORMAL),
            "A gallery of visible widgets, their states and common variations."
        )));
    }

    /**
     * Adds a panel that demonstrates its normal and sticky positions while scrolling.
     *
     * @param root page root
     */
    private static void addStickyPanel(final RootWidget root) {
        final StickyPanelStyle style = StickyPanel.getDefaultStyle().derive();
        style.setStickySide(StickySide.TOP);
        style.setBgColor(new Color(219, 234, 254));
        style.setBorderColor(PRIMARY);
        style.setBorderStyle(BorderStyle.SOLID);
        style.setBorderWidth(1);
        style.setBorderRadius(10);
        style.setPadding(14, 10);
        style.setBoxSizing(BoxSizing.BORDER_BOX);
        style.setBoxShadow(new BoxShadow(0, 4, 12, new Color(15, 23, 42, 25)));
        final StickyPanel panel = new StickyPanel(style);
        panel.setMargin(24, 12);
        panel.add(new Section(new TextWidget(
            textStyle(TEXT, "15px", FontWeight.SEMIBOLD),
            "StickyPanel — scroll the page to see this block stick to the top."
        )));
        root.add(panel);
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
     * Demonstrates a text and link that wrap together.
     *
     * @param root target root
     */
    private static void addNoWrap(final RootWidget root) {
        final Panel card = addCard(root, "NoWrap", "An inline group that stays on one line.");
        card.add(new Section(new TextWidget("Read "), new NoWrap(
            new TextWidget("our "), new Link("privacy policy", "https://example.com/privacy"),
            new TextWidget("."))));
    }

    /**
     * Adds a compact document sample.
     *
     * @param root page root
     */
    private static void addMarkdown(final RootWidget root) {
        final Panel card = addCard(root, "Markdown",
            "Block documents with headings, lists, links and reactive source text.");
        card.add(new Markdown("""
            ## Delivery terms
            We confirm the **price and delivery date** before shipping.

            - Carefully packed jewelry
            - Delivery by agreement
            - [Contact us](mailto:info@example.com)

            > Keep your order number when contacting support.
            """));
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
     * Adds native hyperlink variations.
     *
     * @param root page root
     */
    private static void addLinks(final RootWidget root) {
        final Panel card = addCard(root, "Link",
            "Native hyperlinks with a reactive destination model and keyboard focus.");
        final Section row = variantRow();
        card.add(row);
        row.add(variant("Default", new Link("Default link")));
        row.add(variant("Repository", new Link(
            "Open the widgets repository", "https://github.com/kniazkov/widgets"
        )));
        final LinkStyle customStyle = Link.getDefaultStyle().derive();
        customStyle.setColor(State.NORMAL, new Color(5, 150, 105));
        customStyle.setColor(State.HOVERED, new Color(4, 120, 87));
        customStyle.setColor(State.FOCUSED, new Color(4, 120, 87));
        customStyle.setColor(State.ACTIVE, new Color(6, 95, 70));
        row.add(variant("Custom", new Link(customStyle, "Custom link", "#")));
    }

    /**
     * Adds single-line input field variations.
     *
     * @param root page root
     */
    private static void addNumericHints(final RootWidget root) {
        final Panel card = addCard(root, "Numeric input hints",
            "Zero stays in the model; type directly over the gray hint. Clear to reset to zero.");
        final Section row = new Section();
        card.add(row);
        final var count = new com.kniazkov.widgets.model.IntegerModel(0);
        final InputField integer = new InputField();
        integer.setTextModel(new com.kniazkov.widgets.model.IntegerToStringModel(count, true));
        integer.setPlaceholder("0");
        integer.setInputMode(com.kniazkov.widgets.common.InputMode.NUMERIC);
        row.add(variant("Quantity", integer));
        final var amount = new com.kniazkov.widgets.model.RealNumberModel(0.0);
        final InputField decimal = new InputField();
        decimal.setTextModel(new com.kniazkov.widgets.model.RealToStringModel(amount, true));
        decimal.setPlaceholder("0.00");
        decimal.setPlaceholderColor(new Color(100, 116, 139));
        decimal.setInputMode(com.kniazkov.widgets.common.InputMode.DECIMAL);
        row.add(variant("Amount", decimal));
        final TextWidget value = new TextWidget();
        value.setTextModel(new com.kniazkov.widgets.model.RealToStringModel(amount));
        row.add(variant("Numeric model value", value));
    }

    /**
     * Adds input field style variants.
     * @param root gallery root
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
        unchecked.setAccessibleName("Notifications");
        final TextWidget uncheckedResult = feedback("Notifications disabled");
        unchecked.getCheckedStateModel().addListener(selected -> uncheckedResult.setText(
            "Notifications " + (selected ? "enabled" : "disabled")
        ));
        row.add(variant("Default", unchecked, uncheckedResult));
        final CheckBox checked = new CheckBox();
        checked.setAccessibleName("Automatic updates");
        checked.check();
        final TextWidget checkedResult = feedback("Automatic updates enabled");
        checked.getCheckedStateModel().addListener(selected -> checkedResult.setText(
            "Automatic updates " + (selected ? "enabled" : "disabled")
        ));
        row.add(variant("Checked", checked, checkedResult));
        final CheckBox disabled = new CheckBox();
        disabled.setAccessibleName("Disabled notifications");
        disabled.check();
        disabled.disable();
        row.add(variant("Disabled", disabled));
    }

    /**
     * Adds radio buttons joined into an exclusive group.
     *
     * @param root page root
     */
    private static void addRadioButtons(final RootWidget root) {
        final Panel card = addCard(root, "RadioButton",
            "Selecting one option clears the other; a selected option cannot be cleared by click.");
        final Section row = variantRow();
        card.add(row);
        final RadioButton first = new RadioButton();
        final RadioButton second = new RadioButton();
        first.setAccessibleName("First option");
        second.setAccessibleName("Second option");
        second.check();
        new RadioGroup(first, second);
        row.add(variant("First option", first));
        row.add(variant("Second option — selected", second));
        final RadioButton disabled = new RadioButton();
        disabled.setAccessibleName("Disabled option");
        disabled.check();
        disabled.disable();
        row.add(variant("Disabled", disabled));
    }

    /**
     * Demonstrates labeled choices, shared models and reactive disabled captions.
     * @param root page root
     */
    private static void addLabeledChoices(final RootWidget root) {
        final Panel card = addCard(root, "CheckBoxWithText / RadioButtonWithText",
            "Tab, Space and arrow keys work on these choices; captions also activate them.");
        final BooleanModel disabled = new BooleanModel(false);
        final BooleanModel subscribed = new BooleanModel(true);
        final StringModel caption = new StringModel("Receive product news");
        final CheckBoxWithText first = new CheckBoxWithText(caption, subscribed);
        final CheckBoxWithText mirror = new CheckBoxWithText(caption, subscribed);
        first.setDisabledStateModel(disabled);
        mirror.setDisabledStateModel(disabled);
        card.add(new Section(first));
        card.add(new Section(mirror));
        final RadioButtonWithText delivery = new RadioButtonWithText("Delivery");
        final RadioButtonWithText pickup = new RadioButtonWithText("Store pickup");
        delivery.check();
        new RadioGroup(delivery.getRadioButton(), pickup.getRadioButton());
        delivery.setDisabledStateModel(disabled);
        pickup.setDisabledStateModel(disabled);
        card.add(new Section(delivery));
        card.add(new Section(pickup));
        final CheckBoxWithText longCaption = new CheckBoxWithText(
            "Send a copy of the detailed delivery notice and tracking information "
                + "for every package, including orders shipped in several parts."
        );
        card.add(new Section(longCaption));
        card.add(new Section(new CheckBoxWithText(new StringModel("Disable choices"), disabled)));
        final InputField text = new InputField();
        text.setTextModel(caption);
        card.add(new Section(new TextWidget("Edit the shared caption: "), text));
    }

    /**
     * Shows one shared application action with fake inputs; this is not authentication.
     * @param root gallery root
     */
    private static void addKeyboardLogin(final RootWidget root) {
        final Panel card = addCard(root, "Keyboard login demo",
            "Test data only: user demo and code blue. Enter or Sign in runs the same validation.");
        final InputField user = new InputField();
        user.setAccessibleName("Demo user");
        final PasswordInput code = new PasswordInput();
        code.setAccessibleName("Demo code");
        final Button submit = new Button(ButtonStyle.PRIMARY, "Sign in demo");
        final TextWidget result = feedback("No attempts yet");
        final var attempts = new java.util.concurrent.atomic.AtomicInteger();
        final Runnable attempt = () -> {
            final boolean valid = user.getText().equals("demo") && code.getText().equals("blue");
            result.setText("Attempt " + attempts.incrementAndGet()
                + (valid ? ": demo accepted" : ": check the test data"));
        };
        user.onEnter(event -> attempt.run());
        code.onEnter(event -> attempt.run());
        submit.onClick(event -> attempt.run());
        card.add(new Section(variant("Demo user", user), variant("Demo code", code), submit));
        card.add(new Section(result));
    }

    /**
     * Shows a locally simulated server refusal and a successful retry with fictional data.
     * @param root page root
     */
    private static void addFormDemo(final RootWidget root) {
        final Panel card = addCard(root, "Sample form",
            "Sample data: enter 'Paper' twice. 'Taken' demonstrates rejection.");
        final Form form = new Form("Request with sample data", "Submit request");
        form.setMessages(new Form.Messages("Validating request…", "Sample request accepted.",
            "Check the highlighted fields.", "Could not submit the sample request. Please retry."));
        final FormField name = new FormField("title",
            "Full sample item name for the demonstration request");
        final FormField confirm = new FormField("confirm",
            "Repeat the same name to test validation across two fields");
        final FormField reference = new FormField("reference", "Sample number");
        for (final FormField field : List.of(name, confirm, reference)) {
            field.setConstraintMessages("Complete this field.", "Check this field's value.");
            form.addField(field);
        }
        name.setRequired(true);
        name.setMaxLength(40);
        name.setAutocomplete("off");
        name.setHelp("Use fictional data only. Maximum 40 characters.");
        confirm.setRequired(true);
        confirm.setMaxLength(40);
        reference.getEditor().setText("DEMO-20");
        reference.markPristine();
        reference.setReadOnly(true);
        reference.setHelp("The read-only number is submitted with the request.");
        form.setValidator(values -> values.get("title").equals(values.get("confirm"))
            ? Form.Result.success()
            : new Form.Result(Map.of("confirm", "Names in both fields must match."), ""));
        final AtomicInteger attempts = new AtomicInteger();
        final TextWidget count = feedback("Sample submissions: 0");
        form.onSubmit(values -> {
            count.setText("Sample submissions: " + attempts.incrementAndGet());
            return CompletableFuture.supplyAsync(() -> "Taken".equals(values.get("title"))
                ? new Form.Result(Map.of("title", "This sample is already reserved."),
                    "The server rejected the sample request.")
                : Form.Result.success(), CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS));
        });
        card.add(form);
        card.add(new Section(count));
    }

    /**
     * Demonstrates shared suggestions and application-controlled history.
     * @param root page root
     */
    private static void addSuggestionFields(final RootWidget root) {
        final Panel card = addCard(root, "SuggestionField",
            "Choose a previous value or type your own. Save to offer it in both fields.");
        final StringModel fittings = new StringModel("Rhodium-plated brass");
        final List<Model<String>> suggestions = new ArrayList<>(List.of(
            fittings, new StringModel("Silver"), new StringModel("Gold-plated brass")
        ));
        final SuggestionField first = new SuggestionField(suggestions);
        final SuggestionField second = new SuggestionField(suggestions);
        final InputField source = new InputField();
        source.setTextModel(fittings);
        final TextWidget value = feedback("");
        value.setTextModel(first.getTextModel());
        final Button save = new Button("Save suggestion");
        save.onClick(event -> {
            final String text = first.getText().trim();
            if (!text.isEmpty()
                    && suggestions.stream().noneMatch(model -> text.equals(model.getData()))) {
                suggestions.add(new StringModel(text));
                first.setSuggestionModels(suggestions);
                second.setSuggestionModels(suggestions);
            }
        });
        final SuggestionField disabled = new SuggestionField(List.of("Unavailable"));
        disabled.disable();
        final Section row = variantRow();
        row.add(variant("Fittings — type rhodium to filter", first, value));
        row.add(save);
        row.add(variant("Next product — shared suggestions", second));
        row.add(variant("Edit the first source model", source));
        row.add(variant("Disabled", disabled));
        final SuggestionField colors = new SuggestionField(List.of(
            new StringModel("Black"), new StringModel("White"), new StringModel("Blue")
        ));
        colors.setSuggestionSeparatorModel(new StringModel(","));
        colors.setText("Black, Wh");
        final TextWidget colorValue = feedback("");
        colorValue.setTextModel(colors.getTextModel());
        row.add(variant("Colors — use commas; edit any value", colors, colorValue));
        card.add(row);
    }

    /**
     * Adds native drop-down list variations.
     *
     * @param root page root
     */
    private static void addDropDownLists(final RootWidget root) {
        final Panel card = addCard(root, "DropDownList",
            "Native keyboard-accessible selection with reactive option texts and stable indices.");
        final Section row = variantRow();
        card.add(row);
        final DropDownList standard = new DropDownList("Small", "Medium", "Large");
        final TextWidget result = feedback("Selected index: none");
        standard.onSelect(index -> result.setText("Selected index: " + index));
        row.add(variant("Default", standard, result));

        final StringModel number = new StringModel("Number");
        final StringModel booleanValue = new StringModel("Yes / no");
        final StringModel text = new StringModel("Text");
        final DropDownList reactive = new DropDownList(List.of(
            number,
            booleanValue,
            text
        ));
        reactive.setSelectedIndex(1);
        row.add(variant("Reactive option models", reactive));

        final DropDownList disabled = new DropDownList("Unavailable");
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
            "Ready-to-use styles, a custom design and an image child.");
        final Section row = variantRow();
        card.add(row);
        final AtomicInteger clicks = new AtomicInteger();
        final Button standard = new Button("Default");
        final TextWidget standardResult = feedback("Not clicked yet");
        standard.onClick(event -> standardResult.setText(
            "Clicked " + clicks.incrementAndGet() + " time(s)"
        ));
        row.add(variant("Default", standard, standardResult));
        final Button primary = new Button(ButtonStyle.PRIMARY, "Primary");
        final TextWidget primaryResult = feedback("Not clicked yet");
        primary.onClick(event -> primaryResult.setText("Primary action clicked"));
        row.add(variant("Primary", primary, primaryResult));
        final Button destructive = new Button(ButtonStyle.DANGER, "Delete");
        final TextWidget dangerResult = feedback("Not clicked yet");
        destructive.onClick(event -> dangerResult.setText("Destructive action clicked"));
        row.add(variant("Destructive", destructive, dangerResult));
        final Button custom = new Button(customButtonStyle(), "Custom");
        final TextWidget customResult = feedback("Not clicked yet");
        custom.onClick(event -> customResult.setText("Custom action clicked"));
        row.add(variant("Fully custom", custom, customResult));
        final Button icon = Button.icon(ButtonStyle.PRIMARY, addIcon(), "Add item");
        final TextWidget iconResult = feedback("Not clicked yet");
        icon.onClick(event -> iconResult.setText("Icon action clicked"));
        row.add(variant("Image child", icon, iconResult));
        final Button disabled = new Button("Disabled");
        disabled.disable();
        row.add(variant("Disabled", disabled));
    }

    /**
     * Adds launchers for ready-to-use modal messages.
     *
     * @param root page root
     */
    private static void addPopups(final RootWidget root) {
        final Panel card = addCard(root, "Popup",
            "Modal messages can require a button or allow closing by clicking or tapping outside.");
        final Section row = variantRow();
        card.add(row);

        final Button oneButtonLauncher = new Button("Show message");
        oneButtonLauncher.onClick(event -> {
            final Button close = new Button(ButtonStyle.PRIMARY, "Close");
            final MessagePopup popup = new MessagePopup(
                "The operation completed successfully.", close
            );
            close.onClick(closeEvent -> popup.remove());
            root.add(popup);
        });
        row.add(variant("One button", oneButtonLauncher));

        final Button twoButtonLauncher = new Button(ButtonStyle.PRIMARY, "Show confirmation");
        twoButtonLauncher.onClick(event -> {
            final Button cancel = new Button("Cancel");
            final Button confirm = new Button(ButtonStyle.PRIMARY, "Confirm");
            final MessagePopup popup = new MessagePopup(
                "Do you want to continue?", cancel, confirm
            );
            cancel.onClick(closeEvent -> popup.remove());
            confirm.onClick(closeEvent -> popup.remove());
            root.add(popup);
        });
        row.add(variant("Two buttons", twoButtonLauncher));

        final Button outsideLauncher = new Button("Show dismissible message");
        outsideLauncher.onClick(event -> {
            final Button close = new Button("Close");
            final MessagePopup popup = new MessagePopup(
                "Click or tap outside this window to close it. Inside clicks keep it open.", close
            );
            popup.setCloseOnOutsideClick(true);
            final CheckBox enabled = new CheckBox();
            enabled.setAccessibleName("Allow closing by tapping outside");
            enabled.setCheckedStateModel(popup.getCloseOnOutsideClickModel());
            popup.add(new Section(enabled, new TextWidget("Allow closing by tapping outside")));
            close.onClick(closeEvent -> popup.remove());
            root.add(popup);
        });
        row.add(variant("Click or tap outside", outsideLauncher));
        final Button plainLauncher = new Button("Show plain popup");
        plainLauncher.onClick(event -> {
            final Popup popup = new Popup();
            final Button close = new Button("Close plain popup");
            close.onClick(closeEvent -> popup.remove());
            popup.add(new Section(new TextWidget("Popup: the page remains interactive.")));
            popup.add(new Section(close));
            root.add(popup);
        });
        row.add(variant("Non-modal", plainLauncher));
        final Button modalLauncher = new Button("Show custom modal");
        modalLauncher.onClick(event -> {
            final ModalPopup popup = new ModalPopup();
            final Button close = new Button("Close custom modal");
            close.onClick(closeEvent -> popup.remove());
            popup.add(new Section(new TextWidget("ModalPopup with caller-supplied content.")));
            popup.add(new Section(new InputField("Editable content")));
            popup.add(new Section(close));
            root.add(popup);
        });
        row.add(variant("Custom modal", modalLauncher));
        addModalVisibilityExample(root, row);
    }

    /**
     * Adds a stateful example of hide, reopen, nesting and dismissal.
     * @param root the page root
     * @param row gallery variant row
     */
    private static void addModalVisibilityExample(final RootWidget root, final Section row) {
        final Panel host = new Panel();
        root.add(host);
        final ModalPopup outer = new ModalPopup();
        outer.setBackdropColor(new Color(30, 64, 175, 112));
        outer.setAccessibleName("Lifecycle modal");
        outer.setAccessibleDescription(
            "Use Tab to move between actions; Escape closes this dialog."
        );
        outer.setCloseOnEscape(true);
        outer.hide();
        host.add(outer);
        final Button launcher = new Button("Open lifecycle modal");
        final Button clickPage = new Button("Count page click");
        final TextWidget count = feedback("Page clicks: 0");
        final var pageClicks = new java.util.concurrent.atomic.AtomicInteger();
        clickPage.onClick(event -> count.setText("Page clicks: " + pageClicks.incrementAndGet()));
        launcher.onClick(event -> {
            host.show();
            if (outer.getParent().isEmpty()) {
                host.add(outer);
            }
            outer.show();
        });
        final Button hide = new Button("Hide lifecycle modal");
        hide.onClick(event -> outer.hide());
        final Button hideHost = new Button("Hide modal ancestor");
        hideHost.onClick(event -> host.hide());
        final Button nested = new Button("Open nested modal");
        nested.onClick(event -> {
            final ModalPopup inner = new ModalPopup();
            inner.setBackdropColor(new Color(15, 23, 42, 128));
            inner.setCloseOnOutsideClick(true);
            inner.setCloseOnEscape(true);
            inner.setAccessibleName("Nested lifecycle modal");
            inner.setAccessibleDescription("Keyboard focus stays in this nested dialog.");
            final Button close = new Button("Close nested modal");
            final Button toggle = new Button("Toggle nested Escape");
            toggle.onClick(click -> inner.setCloseOnEscape(!inner.isCloseOnEscape()));
            final Button remove = new Button("Remove opener and close");
            remove.onClick(click -> {
                nested.dispose();
                inner.dispose();
            });
            close.onClick(click -> inner.dispose());
            inner.add(new Section(new TextWidget("Nested lifecycle modal")));
            inner.add(new Section(close));
            inner.add(new Section(toggle));
            inner.add(new Section(remove));
            outer.add(inner);
        });
        outer.add(new Section(new TextWidget("Lifecycle modal")));
        outer.add(new Section(hide));
        outer.add(new Section(hideHost));
        outer.add(new Section(nested));
        row.add(variant("Hide and reopen", launcher));
        row.add(variant("Page interaction", clickPage, count));
        addEmptyModalExamples(root, row);
    }

    /**
     * Demonstrates dialog fallback focus with and without an available opener.
     * @param root the page root
     * @param row gallery variant row
     */
    private static void addEmptyModalExamples(final RootWidget root, final Section row) {
        final Button empty = new Button("Open text-only dialog");
        empty.onClick(click -> root.add(textOnlyModal()));
        final Button disappearing = new Button("Open and remove launcher");
        disappearing.onClick(click -> {
            disappearing.dispose();
            root.add(textOnlyModal());
        });
        row.add(variant("No interactive content", empty));
        row.add(variant("Missing opener", disappearing));
    }

    /**
     * Creates a dialog that can only be dismissed by the backdrop or Escape.
     * @return text-only example
     */
    private static ModalPopup textOnlyModal() {
        final ModalPopup popup = new ModalPopup(
            new Section(new TextWidget("This dialog has no interactive controls."))
        );
        popup.setAccessibleName("Text-only dialog");
        popup.setAccessibleDescription("Press Escape or select the backdrop to close.");
        popup.setCloseOnEscape(true);
        popup.setCloseOnOutsideClick(true);
        return popup;
    }

    /**
     * Shows accessible relationships, image alternatives and native icon buttons.
     * @param root gallery root
     */
    private static void addAccessibleSemantics(final RootWidget root) {
        final Panel card = addCard(root, "Accessible widgets",
            "Named icon actions, linked field text and decorative images.");
        final Section row = variantRow();
        card.add(row);
        final Button refresh = Button.icon(accessibilityIcon(), "Refresh preview");
        final Button unavailable = Button.icon(accessibilityIcon(), "Refresh unavailable");
        unavailable.disable();
        final TextWidget status = feedback("Preview not refreshed");
        final AtomicInteger count = new AtomicInteger();
        refresh.onClick(event -> status.setText("Refresh count: " + count.incrementAndGet()));
        row.add(variant("Icon action", refresh, status));
        row.add(variant("Disabled action", unavailable));

        final ImageWidget informative = new ImageWidget(demoImage("#2563eb", "#1e40af", "N"));
        informative.setWidth(64);
        informative.setHeight(64);
        informative.setAlternativeText("North marker");
        final ImageWidget decorative = new ImageWidget(demoImage("#059669", "#047857", "✦"));
        decorative.setWidth(64);
        decorative.setHeight(64);
        decorative.setDecorative(true);
        row.add(variant("Informative image", informative));
        row.add(variant("Decorative image", decorative));
        addAccessibleField(card);
    }

    /**
     * Creates a small decorative icon for a native button.
     * @return image shown inside the button
     */
    private static ImageWidget accessibilityIcon() {
        final ImageWidget icon = new ImageWidget(new SvgImageSource() {
            @Override
            protected String getSvg() {
                return "<svg xmlns='http://www.w3.org/2000/svg' width='20' height='20' "
                    + "viewBox='0 0 20 20'><path d='M16 8a6 6 0 1 0 0 4M16 4v4h-4' "
                    + "fill='none' stroke='#1d4ed8' stroke-width='2' "
                    + "stroke-linecap='round' stroke-linejoin='round'/></svg>";
            }
        });
        icon.setWidth(20);
        icon.setHeight(20);
        return icon;
    }

    /**
     * Demonstrates reactive accessible name, help and validation error references.
     * @param card enclosing gallery panel
     */
    private static void addAccessibleField(final Panel card) {
        final TextWidget label = new TextWidget("Delivery code");
        final TextWidget help = new TextWidget("Use four digits.");
        final TextWidget error = new TextWidget("The code is incomplete.");
        final InputField field = new InputField();
        field.setAccessibleName("Shipping code");
        field.setMaxWidth("100%");
        field.setAccessibleLabel(label);
        field.setAccessibleDescriptionWidget(help);
        field.setAccessibleError(error);
        field.setValidState(false);
        card.add(new Section(label));
        card.add(new Section(field));
        card.add(new Section(help));
        card.add(new Section(error));
        final Button rename = new Button("Rename field label");
        rename.onClick(event -> label.setText("Updated delivery code"));
        final Button remove = new Button("Remove field label");
        remove.onClick(event -> label.dispose());
        final Button valid = new Button("Toggle field validity");
        valid.onClick(event -> {
            final boolean nowValid = !field.isValid();
            field.setValidState(nowValid);
            error.setText(nowValid ? "" : "The code is incomplete.");
        });
        card.add(new Section(rename, remove, valid));
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
        final FileLoader images = new FileLoader(ButtonStyle.PRIMARY, "Choose image");
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
     * Adds examples of widgets occupying the same visual area.
     *
     * @param root page root
     */
    private static void addOverlayStacks(final RootWidget root) {
        final Panel card = addCard(root, "OverlayStack",
            "Layers arbitrary widgets from bottom to top in one shared area.");
        final Section row = variantRow();
        card.add(row);

        final ImageWidget background = new ImageWidget(
            demoImage("#0f766e", "#2563eb", "")
        );
        background.setStyle(imageStyle(200, 120, 16));
        final Section centeredLabel = new Section(new TextWidget(
            textStyle(Color.WHITE, "22px", FontWeight.BOLD),
            "Overlay"
        ));
        centeredLabel.setCenterAlignment();
        centeredLabel.setMiddleAlignment();
        final OverlayStack contentSized = new OverlayStack(background, centeredLabel);
        contentSized.setFitToFirstChild(true);
        row.add(variant("Text over image (size from image)", contentSized));

        final TextWidget oldPrice = new TextWidget(
            textStyle(TEXT, "18px", FontWeight.NORMAL),
            "500.00"
        );
        final ImageWidget marker = new ImageWidget(markerStroke());
        marker.setStyle(imageStyle(72, 24, 0));
        row.add(variant(
            "Decoration over text",
            new OverlayStack(oldPrice, marker)
        ));
    }

    /**
     * Adds table variations.
     *
     * @param root page root
     */
    private static void addTables(final RootWidget root) {
        final Panel card = addCard(root, "Table",
            "An unstyled layout table and a ready-to-use decorated data table.");
        card.add(blockVariant("Default — unstyled", defaultTable()));
        card.add(blockVariant("Decorated", decoratedTable()));
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
        final TextWidget heading = new TextWidget(
            textStyle(TEXT, "22px", FontWeight.BOLD), title
        );
        heading.setHeadingLevel(HeadingLevel.H2);
        card.add(new Section(heading));
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
     * Creates a deliberately distinct button style using only the public style API.
     *
     * @return custom button style
     */
    private static ButtonStyle customButtonStyle() {
        final ButtonStyle style = Button.getDefaultStyle().derive();
        final Color normal = new Color(124, 58, 237);
        final Color hovered = new Color(109, 40, 217);
        final Color active = new Color(91, 33, 182);
        style.setBgColor(State.NORMAL, normal);
        style.setBgColor(State.HOVERED, hovered);
        style.setBgColor(State.FOCUSED, normal);
        style.setBgColor(State.ACTIVE, active);
        style.setBorderColor(State.NORMAL, normal);
        style.setBorderColor(State.HOVERED, hovered);
        style.setBorderColor(State.FOCUSED, normal);
        style.setBorderColor(State.ACTIVE, active);
        style.setBorderRadius(20);
        style.setHeight(42);
        style.setPadding(22, 8);
        style.setBoxShadow(State.NORMAL,
            new BoxShadow(0, 4, 10, new Color(124, 58, 237, 55)));
        style.setBoxShadow(State.HOVERED,
            new BoxShadow(0, 7, 16, new Color(124, 58, 237, 72)));
        style.setBoxShadow(State.ACTIVE,
            new BoxShadow(0, 2, 5, new Color(124, 58, 237, 50)));
        style.getDefaultTextStyle().setColor(Color.WHITE);
        style.getDefaultTextStyle().setFontWeight(FontWeight.BOLD);
        return style;
    }

    /**
     * Creates a small plus pictogram used as a button child.
     *
     * @return image widget containing the pictogram
     */
    private static ImageWidget addIcon() {
        final ImageWidget icon = new ImageWidget(new SvgImageSource() {
            @Override
            protected String getSvg() {
                return "<svg xmlns='http://www.w3.org/2000/svg' width='18' height='18' "
                    + "viewBox='0 0 18 18'><path d='M9 3v12M3 9h12' fill='none' "
                    + "stroke='white' stroke-width='2.2' stroke-linecap='round'/></svg>";
            }
        });
        icon.setStyle(imageStyle(18, 18, 0));
        return icon;
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
            file.onLoad(uploaded -> file.executeOnRoot(() -> result.setText(
                kind + " uploaded: " + uploaded.getName() + " (" + uploaded.getSize()
                    + " bytes)"
            )));
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
     * Creates a transparent marker stroke used by the overlay example.
     *
     * @return marker image source
     */
    private static ImageSource markerStroke() {
        return new SvgImageSource() {
            @Override
            protected String getSvg() {
                return "<svg xmlns='http://www.w3.org/2000/svg' width='72' height='24' "
                    + "viewBox='0 0 72 24'><path d='M2 15 C14 11 23 18 35 13 "
                    + "S56 17 70 10' fill='none' stroke='#dc2626' stroke-width='4' "
                    + "stroke-linecap='round' opacity='.9'/></svg>";
            }
        };
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
     * Creates a data table using the ready-to-use decorated style.
     *
     * @return table widget
     */
    private static Table decoratedTable() {
        final Table table = new Table(TableStyle.DECORATED);
        addDecoratedTableRow(table, 0, true, "Name", "Role", "Status");
        addDecoratedTableRow(table, 1, false, "Alice", "Designer", "Active");
        addDecoratedTableRow(table, 2, false, "Bob", "Developer", "Reviewing");
        addDecoratedTableRow(table, 3, false, "Carol", "Support", "Offline");
        return table;
    }

    /**
     * Adds content to one row of a decorated table.
     *
     * @param table target table
     * @param rowIndex zero-based row index
     * @param header whether this is a header row
     * @param values cell values
     */
    private static void addDecoratedTableRow(final Table table, final int rowIndex,
                                             final boolean header, final String... values) {
        for (int columnIndex = 0; columnIndex < values.length; columnIndex++) {
            final TextWidgetStyle text = textStyle(
                header ? MUTED : TEXT,
                "14px",
                header ? FontWeight.SEMIBOLD : FontWeight.NORMAL
            );
            table.getCell(rowIndex, columnIndex).add(
                new Section(new TextWidget(text, values[columnIndex]))
            );
        }
    }
}
