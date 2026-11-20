# Widget catalog

[Web Widgets](../README.md) · [Русский](WIDGET_GALLERY.ru.md)

Choose a task below, or [look up a Java class](#classes). Each entry includes a Java fragment and a real screenshot from the AllWidgets application.

| Task | How to choose |
| --- | --- |
| [Show text, images and actions](en/widgets/display.md) | Use TextWidget for a single piece of live text, Markdown for formatted blocks, Link for a destination and Button for a command. |
| [Enter text and submit a form](en/widgets/forms.md) | Start with InputField for a single line. Add a FormField for a label, help and errors, then a Form when several values must be submitted together. |
| [Enter a number, date or time](en/widgets/numbers.md) | Choose NumberField for exact decimal input, QuantityField for step buttons or Slider to pick a position on a bounded scale. |
| [Choose one or more values](en/widgets/choices.md) | Select is a native single-choice list. ComboBox adds a search field and can accept free text. MultiSelect keeps a set of chosen options. A checkbox fits an independent on/off setting. |
| [Arrange blocks and inline content](en/widgets/layout.md) | Use Section for a row of inline widgets, Panel for a group of blocks and ResponsiveGrid for columns that adapt to the available width. |
| [Navigate sections and hierarchies](en/widgets/navigation.md) | Tabs and Accordion select content within the current page. Breadcrumbs link to other pages, while TreeView loads a hierarchy as branches open. |
| [Show a table or browse records](en/widgets/tables.md) | Choose Table when cells contain widgets. Choose DataTable when rows come from a provider and the reader needs to filter, sort or page through records. |
| <a id="temporarily-hiding-a-modal"></a>[Open a window, menu or dialog](en/widgets/dialogs.md) | Use ModalPopup for a custom blocking window, Dialog to await a result and Popover for controls that stay beside their trigger. |
| [Report progress, errors and outcomes](en/widgets/feedback.md) | Keep FieldError beside the editor it describes. Use an Alert for a persistent page message and a Toast for a short notification. |
| [Select and upload files](en/widgets/files.md) | FileLoader exposes each transfer to your handler. UploadManager and UploadList provide progress, cancellation and retry controls together. |

## Run AllWidgets

In the [quick-start Maven project](en/quick-start.md), replace the dependency `com.kniazkov:widgets-http:0.2.0` with `com.kniazkov:widgets:0.2.0`. The full artifact includes the example. Set `<mainClass>` to `com.kniazkov.widgets.example.AllWidgets`. For the house photo and the favicon, save [house.png](../www/house.png) and [widgets-icon.svg](../www/widgets-icon.svg) in a `www` directory next to your POM.

```sh
mvn compile exec:java
```

Open [localhost:8080](http://localhost:8080/) and try the controls pictured in the catalog. The [AllWidgets source](../src/main/java/com/kniazkov/widgets/example/AllWidgets.java) shows the complete pages and event handlers. Short fragments in the catalog go inside a page factory. `root` is its RootWidget. Add a block to `root` or put an inline widget in a `Section` first.

<a id="classes"></a>

## Java classes A–Z

**A** · [Accordion](en/widgets/navigation.md#accordion) · [ActiveImage](en/widgets/display.md#activeimage) · [ActiveText](en/widgets/display.md#activetext) · [Alert](en/widgets/feedback.md#alert)

**B** · [Breadcrumbs](en/widgets/navigation.md#breadcrumbs) · [Busy](en/widgets/feedback.md#busy) · [Button](en/widgets/display.md#button)

**C** · [Carousel](en/widgets/display.md#carousel) · [Cell](en/widgets/tables.md#cell) · [CheckBox](en/widgets/choices.md#checkbox) · [CheckBoxWithText](en/widgets/choices.md#checkboxwithtext) · [ComboBox](en/widgets/choices.md#combobox) · [Countdown](en/widgets/display.md#countdown)

**D** · [DataTable](en/widgets/tables.md#datatable) · [DateField](en/widgets/numbers.md#datefield) · [DateRange](en/widgets/numbers.md#daterange) · [Dialog](en/widgets/dialogs.md#dialog) · [DropDownList](en/widgets/choices.md#dropdownlist)

**E** · [EmptyState](en/widgets/feedback.md#emptystate)

**F** · [FieldError](en/widgets/feedback.md#fielderror) · [FileLoader](en/widgets/files.md#fileloader) · [Form](en/widgets/forms.md#form) · [FormField](en/widgets/forms.md#formfield) · [FormLayout](en/widgets/layout.md#formlayout)

**H** · [HorizontalLine](en/widgets/display.md#horizontalline)

**I** · [ImageWidget](en/widgets/display.md#imagewidget) · [InlineBlock](en/widgets/layout.md#inlineblock) · [InputField](en/widgets/forms.md#inputfield)

**L** · [Label](en/widgets/forms.md#label) · [Link](en/widgets/display.md#link)

**M** · [MarginDecorator](en/widgets/layout.md#margindecorator) · [Markdown](en/widgets/display.md#markdown) · [Menu](en/widgets/dialogs.md#menu) · [MessagePopup](en/widgets/dialogs.md#messagepopup) · [ModalPopup](en/widgets/dialogs.md#modalpopup) · [MultiSelect](en/widgets/choices.md#multiselect)

**N** · [NoWrap](en/widgets/layout.md#nowrap) · [NumberField](en/widgets/numbers.md#numberfield)

**O** · [OverlayStack](en/widgets/layout.md#overlaystack)

**P** · [Panel](en/widgets/layout.md#panel) · [PasswordInput](en/widgets/forms.md#passwordinput) · [Popover](en/widgets/dialogs.md#popover) · [Popup](en/widgets/dialogs.md#popup) · [Progress](en/widgets/feedback.md#progress)

**Q** · [QuantityField](en/widgets/numbers.md#quantityfield)

**R** · [RadioButton](en/widgets/choices.md#radiobutton) · [RadioButtonWithText](en/widgets/choices.md#radiobuttonwithtext) · [ResponsiveGrid](en/widgets/layout.md#responsivegrid) · [RootWidget](en/widgets/layout.md#rootwidget) · [Row](en/widgets/tables.md#row)

**S** · [Section](en/widgets/layout.md#section) · [Select](en/widgets/choices.md#select) · [Slider](en/widgets/numbers.md#slider) · [SortableSection](en/widgets/layout.md#sortablesection) · [SplitPane](en/widgets/layout.md#splitpane) · [Stack](en/widgets/layout.md#stack) · [StickyPanel](en/widgets/layout.md#stickypanel) · [SuggestionField](en/widgets/forms.md#suggestionfield)

**T** · [Table](en/widgets/tables.md#table) · [Tabs](en/widgets/navigation.md#tabs) · [TextArea](en/widgets/forms.md#textarea) · [TextFlow](en/widgets/layout.md#textflow) · [TextWidget](en/widgets/display.md#textwidget) · [TimeField](en/widgets/numbers.md#timefield) · [Toast](en/widgets/feedback.md#toast) · [Tooltip](en/widgets/dialogs.md#tooltip) · [TreeView](en/widgets/navigation.md#treeview)

**U** · [UploadList](en/widgets/files.md#uploadlist) · [UploadManager](en/widgets/files.md#uploadmanager)

**Z** · [ZoomDecorator](en/widgets/display.md#zoomdecorator)
