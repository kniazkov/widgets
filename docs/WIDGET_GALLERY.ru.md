# Каталог виджетов

[Web Widgets](../README.ru.md) · [English](WIDGETS.md)

Выбери задачу или [найди Java-класс по алфавиту](#classes). В каждой статье есть фрагмент Java и снимок работающего приложения AllWidgets.

| Задача | Как выбрать |
| --- | --- |
| <a id="horizontal-line"></a><a id="text-widget"></a><a id="markdown"></a><a id="active-text"></a><a id="link"></a><a id="button"></a><a id="image-widget"></a><a id="active-image"></a><a id="zoom-decorator"></a><a id="countdown"></a><a id="carousel"></a><a id="carousel-selected"></a><a id="accessible-widgets"></a>[Вывести текст, изображения и кнопки](ru/widgets/display.md) | Для меняющейся надписи подойдёт TextWidget, для форматированного блока — Markdown, для перехода — Link, для команды — Button. |
| <a id="input-field"></a><a id="numeric-hints"></a><a id="suggestion-field"></a><a id="keyboard-login-demo"></a><a id="form"></a><a id="password-input"></a><a id="text-area"></a><a id="suggestion-open"></a>[Ввести текст и отправить форму](ru/widgets/forms.md) | Для одной строки начни с InputField. Добавь FormField, если нужны подпись, подсказка и ошибка. Несколько значений можно отправить одной формой Form. |
| <a id="temporal"></a><a id="numbers"></a><a id="slider"></a>[Ввести число, дату или время](ru/widgets/numbers.md) | Для точного десятичного ввода выбирай NumberField, для изменения кнопками — QuantityField, а для выбора положения на ограниченной шкале — Slider. |
| <a id="typed-choices"></a><a id="check-box"></a><a id="radio-button"></a><a id="labeled-choices"></a><a id="drop-down-list"></a>[Выбрать одно или несколько значений](ru/widgets/choices.md) | Select даёт нативный список для одного значения. ComboBox добавляет поиск и может разрешать свободный ввод. MultiSelect хранит несколько выбранных вариантов. Для независимой настройки «да/нет» удобен флажок. |
| <a id="root-widget"></a><a id="sticky-panel"></a><a id="no-wrap"></a><a id="adaptive-layouts"></a><a id="adaptive-form"></a><a id="overlay-stack"></a><a id="sortable-section"></a><a id="layout-composition"></a><a id="split-pane"></a>[Скомпоновать блоки и строчные элементы](ru/widgets/layout.md) | Для строки с надписями и кнопками подойдёт Section, для группы блоков — Panel, для колонок по ширине контейнера — ResponsiveGrid. |
| <a id="tree-view"></a>[Перейти между разделами и узлами дерева](ru/widgets/navigation.md) | Tabs и Accordion открывают содержимое внутри страницы. Breadcrumbs ведёт на другие страницы, а TreeView загружает иерархию по мере раскрытия веток. |
| <a id="data-table"></a><a id="table"></a>[Показать таблицу или список записей](ru/widgets/tables.md) | Если в ячейках нужны виджеты, выбирай Table. Если записи приходят из источника данных и нужны фильтр, сортировка или постраничный просмотр, выбирай DataTable. |
| <a id="popup-launchers"></a><a id="message-popup"></a><a id="message-popup-narrow"></a><a id="popup"></a><a id="modal-popup"></a><a id="dialog"></a>[Открыть окно, меню или диалог](ru/widgets/dialogs.md) | Для своего модального окна подойдёт ModalPopup, для получения результата — Dialog, а для панели рядом с вызвавшей её кнопкой — Popover. |
| <a id="feedback"></a>[Показать прогресс, ошибку или результат](ru/widgets/feedback.md) | FieldError ставь рядом с полем, которое нужно исправить. Для постоянного сообщения подойдёт Alert, для короткого уведомления — Toast. |
| <a id="file-loader"></a><a id="file-loader-complete"></a>[Выбрать и загрузить файл](ru/widgets/files.md) | FileLoader передаёт каждую загрузку твоему обработчику. UploadManager и UploadList вместе дают прогресс, отмену и повтор. |

| [Сканировать QR-коды](ru/widgets/camera.md) | QrScanner |

## Запусти AllWidgets

В Maven-проекте из [быстрого старта](ru/quick-start.md) замени зависимость `com.kniazkov:widgets-http:0.2.1` на `com.kniazkov:widgets:0.2.1`. Полная библиотека включает пример. В `<mainClass>` укажи `com.kniazkov.widgets.example.AllWidgets`. Чтобы загрузились изображение дома и значок страницы, сохрани [house.png](../www/house.png) и [widgets-icon.svg](../www/widgets-icon.svg) в каталог `www` рядом с POM.

```sh
mvn compile exec:java
```

Открой [localhost:8080](http://localhost:8080/) и попробуй элементы, показанные на снимках. В [исходнике AllWidgets](../src/main/java/com/kniazkov/widgets/example/AllWidgets.java) видны компоновка и обработчики. Короткие фрагменты каталога предназначены для фабрики страницы. Её параметр `root` — это RootWidget. Блок добавляй в `root` напрямую, а строчный виджет сначала помести в `Section`.

<a id="classes"></a>

## Java-классы по алфавиту

**A** · [Accordion](ru/widgets/navigation.md#accordion) · [ActiveImage](ru/widgets/display.md#activeimage) · [ActiveText](ru/widgets/display.md#activetext) · [Alert](ru/widgets/feedback.md#alert)

**B** · [Breadcrumbs](ru/widgets/navigation.md#breadcrumbs) · [Busy](ru/widgets/feedback.md#busy) · [Button](ru/widgets/display.md#button)

**C** · [Carousel](ru/widgets/display.md#carousel) · [Cell](ru/widgets/tables.md#cell) · [CheckBox](ru/widgets/choices.md#checkbox) · [CheckBoxWithText](ru/widgets/choices.md#checkboxwithtext) · [ComboBox](ru/widgets/choices.md#combobox) · [Countdown](ru/widgets/display.md#countdown)

**D** · [DataTable](ru/widgets/tables.md#datatable) · [DateField](ru/widgets/numbers.md#datefield) · [DateRange](ru/widgets/numbers.md#daterange) · [Dialog](ru/widgets/dialogs.md#dialog) · [DropDownList](ru/widgets/choices.md#dropdownlist)

**E** · [EmptyState](ru/widgets/feedback.md#emptystate)

**F** · [FieldError](ru/widgets/feedback.md#fielderror) · [FileLoader](ru/widgets/files.md#fileloader) · [Form](ru/widgets/forms.md#form) · [FormField](ru/widgets/forms.md#formfield) · [FormLayout](ru/widgets/layout.md#formlayout)

**H** · [HorizontalLine](ru/widgets/display.md#horizontalline)

**I** · [ImageWidget](ru/widgets/display.md#imagewidget) · [InlineBlock](ru/widgets/layout.md#inlineblock) · [InputField](ru/widgets/forms.md#inputfield)

**L** · [Label](ru/widgets/forms.md#label) · [Link](ru/widgets/display.md#link)

**M** · [MarginDecorator](ru/widgets/layout.md#margindecorator) · [Markdown](ru/widgets/display.md#markdown) · [Menu](ru/widgets/dialogs.md#menu) · [MessagePopup](ru/widgets/dialogs.md#messagepopup) · [ModalPopup](ru/widgets/dialogs.md#modalpopup) · [MultiSelect](ru/widgets/choices.md#multiselect)

**N** · [NoWrap](ru/widgets/layout.md#nowrap) · [NumberField](ru/widgets/numbers.md#numberfield)

**O** · [OverlayStack](ru/widgets/layout.md#overlaystack)

**P** · [Panel](ru/widgets/layout.md#panel) · [PasswordInput](ru/widgets/forms.md#passwordinput) · [Popover](ru/widgets/dialogs.md#popover) · [Popup](ru/widgets/dialogs.md#popup) · [Progress](ru/widgets/feedback.md#progress)

**Q** · [QrScanner](ru/widgets/camera.md#qrscanner) · [QuantityField](ru/widgets/numbers.md#quantityfield)

**R** · [RadioButton](ru/widgets/choices.md#radiobutton) · [RadioButtonWithText](ru/widgets/choices.md#radiobuttonwithtext) · [ResponsiveGrid](ru/widgets/layout.md#responsivegrid) · [RootWidget](ru/widgets/layout.md#rootwidget) · [Row](ru/widgets/tables.md#row)

**S** · [Section](ru/widgets/layout.md#section) · [Select](ru/widgets/choices.md#select) · [Slider](ru/widgets/numbers.md#slider) · [SortableSection](ru/widgets/layout.md#sortablesection) · [SplitPane](ru/widgets/layout.md#splitpane) · [Stack](ru/widgets/layout.md#stack) · [StickyPanel](ru/widgets/layout.md#stickypanel) · [SuggestionField](ru/widgets/forms.md#suggestionfield)

**T** · [Table](ru/widgets/tables.md#table) · [Tabs](ru/widgets/navigation.md#tabs) · [TextArea](ru/widgets/forms.md#textarea) · [TextFlow](ru/widgets/layout.md#textflow) · [TextWidget](ru/widgets/display.md#textwidget) · [TimeField](ru/widgets/numbers.md#timefield) · [Toast](ru/widgets/feedback.md#toast) · [Tooltip](ru/widgets/dialogs.md#tooltip) · [TreeView](ru/widgets/navigation.md#treeview)

**U** · [UploadList](ru/widgets/files.md#uploadlist) · [UploadManager](ru/widgets/files.md#uploadmanager)

**Z** · [ZoomDecorator](ru/widgets/display.md#zoomdecorator)
