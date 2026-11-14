# Визуальный каталог Widgets

Снимки всех **37 публичных конкретных классов виджетов** из `view`. Изображения вырезаны из скриншотов реально собранного AllWidgets на облачном компьютере 11 ноября 2026 года. Это состояние библиотеки, а не обещание, что найденные ревью дефекты уже исправлены.

Базовый commit: `7112edfa10eea3f3f895e426895242f432eba41e`; демонстрации дополнены в этой ветке. Точный SHA-256 исходника AllWidgets, пиксельные координаты вырезок и хеши изображений записаны в [manifest.json](images/widgets/manifest.json). Исходный полный снимок: 1348 × 6775 px; браузер сообщал viewport 1363 × 936 CSS px, DPR 1. Кадры окон: 1348 × 926 px. Размер возвращённого снимка отличается от viewport; вырезки сделаны в его пиксельных координатах, без масштабирования и ретуши.

Запуск: `./run.sh com.kniazkov.widgets.example.AllWidgets 18087`, затем `http://localhost:18087/`. Аргумент порта необязателен; по умолчанию 8080. См. [протокол облачной проверки](CLOUD_VERIFICATION.ru.md).

## Покрытие классов

| Класс | Изображение |
| --- | --- |
| `ActiveImage` | [ActiveImage](#active-image) |
| `ActiveText` | [ActiveText](#active-text) |
| `Button` | [Button](#button) |
| `Carousel` | [Carousel](#carousel), [Carousel — второй слайд](#carousel-selected) |
| `Cell` | [Table](#table) |
| `CheckBox` | [CheckBox](#check-box) |
| `CheckBoxWithText` | [CheckBoxWithText / RadioButtonWithText](#labeled-choices) |
| `Countdown` | [Countdown](#countdown) |
| `DropDownList` | [DropDownList](#drop-down-list) |
| `FileLoader` | [FileLoader](#file-loader) |
| `HorizontalLine` | [HorizontalLine](#horizontal-line) |
| `ImageWidget` | [ImageWidget](#image-widget) |
| `InlineBlock` | [Layout composition](#layout-composition) |
| `InputField` | [InputField](#input-field) |
| `Link` | [Link](#link) |
| `MarginDecorator` | [Layout composition](#layout-composition) |
| `Markdown` | [Markdown](#markdown) |
| `MessagePopup` | [MessagePopup](#message-popup) |
| `ModalPopup` | [ModalPopup](#modal-popup) |
| `NoWrap` | [NoWrap](#no-wrap) |
| `OverlayStack` | [OverlayStack](#overlay-stack) |
| `Panel` | [Layout composition](#layout-composition) |
| `PasswordInput` | [PasswordInput](#password-input) |
| `Popup` | [Popup](#popup) |
| `RadioButton` | [RadioButton](#radio-button) |
| `RadioButtonWithText` | [CheckBoxWithText / RadioButtonWithText](#labeled-choices) |
| `RootWidget` | [All widgets](#root-widget) |
| `Row` | [Table](#table) |
| `Section` | [Layout composition](#layout-composition) |
| `SortableSection` | [SortableSection](#sortable-section) |
| `StickyPanel` | [StickyPanel — scroll the page to see this block stick to the top.](#sticky-panel) |
| `SuggestionField` | [SuggestionField](#suggestion-field), [SuggestionField — раскрытый список](#suggestion-open) |
| `Table` | [Table](#table) |
| `TextArea` | [TextArea](#text-area) |
| `TextFlow` | [Layout composition](#layout-composition) |
| `TextWidget` | [TextWidget](#text-widget) |
| `ZoomDecorator` | [ZoomDecorator](#zoom-decorator) |

Абстрактные классы не имеют отдельного изображения: `BaseImageWidget`, `BlockWidget`, `InlineWidget`, `LabeledChoice`, `Widget`. `RadioGroup` — невизуальная модель взаимоисключения, показанная через RadioButton; стили, интерфейсы Has*, модели, контроллеры и UploadingFile не являются самостоятельными визуальными виджетами. Row/Cell показаны внутри Table, базовые контейнеры — в композиции.

## Изображения

<a id="root-widget"></a>
### All widgets

Начальное состояние; варианты подписаны внутри карточки.

![All widgets: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/root-widget.png)

<a id="sticky-panel"></a>
### StickyPanel — scroll the page to see this block stick to the top.

Начальное состояние; варианты подписаны внутри карточки.

![StickyPanel — scroll the page to see this block stick to the top.: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/sticky-panel.png)

<a id="horizontal-line"></a>
### HorizontalLine

Начальное состояние; варианты подписаны внутри карточки.

![HorizontalLine: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/horizontal-line.png)

<a id="text-widget"></a>
### TextWidget

Начальное состояние; варианты подписаны внутри карточки.

![TextWidget: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/text-widget.png)

<a id="markdown"></a>
### Markdown

Начальное состояние; варианты подписаны внутри карточки.

![Markdown: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/markdown.png)

<a id="no-wrap"></a>
### NoWrap

Начальное состояние; варианты подписаны внутри карточки.

![NoWrap: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/no-wrap.png)

<a id="active-text"></a>
### ActiveText

Начальное состояние; варианты подписаны внутри карточки.

![ActiveText: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/active-text.png)

<a id="link"></a>
### Link

Начальное состояние; варианты подписаны внутри карточки.

![Link: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/link.png)

<a id="input-field"></a>
### InputField

Начальное состояние; варианты подписаны внутри карточки.

![InputField: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/input-field.png)

<a id="numeric-hints"></a>
### Numeric input hints

Начальное состояние; варианты подписаны внутри карточки.

![Numeric input hints: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/numeric-hints.png)

<a id="suggestion-field"></a>
### SuggestionField

Начальное состояние; варианты подписаны внутри карточки.

![SuggestionField: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/suggestion-field.png)

<a id="password-input"></a>
### PasswordInput

Начальное состояние; варианты подписаны внутри карточки.

![PasswordInput: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/password-input.png)

<a id="text-area"></a>
### TextArea

Начальное состояние; варианты подписаны внутри карточки.

![TextArea: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/text-area.png)

<a id="check-box"></a>
### CheckBox

Начальное состояние; варианты подписаны внутри карточки.

![CheckBox: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/check-box.png)

<a id="radio-button"></a>
### RadioButton

Начальное состояние; варианты подписаны внутри карточки.

![RadioButton: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/radio-button.png)

<a id="labeled-choices"></a>
### CheckBoxWithText / RadioButtonWithText

Начальное состояние; варианты подписаны внутри карточки.

![CheckBoxWithText / RadioButtonWithText: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/labeled-choices.png)

<a id="drop-down-list"></a>
### DropDownList

Начальное состояние; варианты подписаны внутри карточки.

![DropDownList: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/drop-down-list.png)

<a id="button"></a>
### Button

Начальное состояние; варианты подписаны внутри карточки.

![Button: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/button.png)

<a id="popup-launchers"></a>
### Popup

Начальное состояние; варианты подписаны внутри карточки.

![Popup: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/popup-launchers.png)

<a id="file-loader"></a>
### FileLoader

Начальное состояние; варианты подписаны внутри карточки.

![FileLoader: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/file-loader.png)

<a id="image-widget"></a>
### ImageWidget

Начальное состояние; варианты подписаны внутри карточки.

![ImageWidget: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/image-widget.png)

<a id="active-image"></a>
### ActiveImage

Начальное состояние; варианты подписаны внутри карточки.

![ActiveImage: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/active-image.png)

<a id="overlay-stack"></a>
### OverlayStack

Начальное состояние; варианты подписаны внутри карточки.

![OverlayStack: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/overlay-stack.png)

<a id="table"></a>
### Table

Начальное состояние; варианты подписаны внутри карточки.

![Table: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/table.png)

<a id="sortable-section"></a>
### SortableSection

Начальное состояние; варианты подписаны внутри карточки.

![SortableSection: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/sortable-section.png)

<a id="zoom-decorator"></a>
### ZoomDecorator

Начальное состояние; варианты подписаны внутри карточки.

![ZoomDecorator: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/zoom-decorator.png)

<a id="layout-composition"></a>
### Layout composition

Начальное состояние; варианты подписаны внутри карточки.

![Layout composition: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/layout-composition.png)

<a id="countdown"></a>
### Countdown

Начальное состояние; варианты подписаны внутри карточки.

![Countdown: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/countdown.png)

<a id="carousel"></a>
### Carousel

Начальное состояние; варианты подписаны внутри карточки.

![Carousel: Начальное состояние; варианты подписаны внутри карточки.](images/widgets/carousel.png)

<a id="message-popup"></a>
### MessagePopup

Открытое сообщение; видна модальная подложка вокруг окна.

![MessagePopup: Открытое сообщение; видна модальная подложка вокруг окна.](images/widgets/message-popup.png)

<a id="popup"></a>
### Popup

Открытое немодальное окно; фон страницы остаётся активным.

![Popup: Открытое немодальное окно; фон страницы остаётся активным.](images/widgets/popup.png)

<a id="modal-popup"></a>
### ModalPopup

Открытое модальное окно с произвольным содержимым и полем ввода.

![ModalPopup: Открытое модальное окно с произвольным содержимым и полем ввода.](images/widgets/modal-popup.png)

Вариант **Hide and reopen** показывает одно и то же окно до и после `hide()`.
При скрытии окна или его предка подложка тоже перестаёт блокировать страницу. Кнопка
**Count page click** позволяет это проверить. Внутри окна можно открыть вложенное,
а его подложка закроет только верхнее окно. Подробности в [контракте виджетов](WIDGETS.md#temporarily-hiding-a-modal).

![Открытое окно Lifecycle modal и синяя подложка в широкой версии галереи.](images/widgets/modal-lifecycle-desktop-open.png)
*Широкий экран: открытое окно и подложка.*

![Галерея без подложки, под кнопкой Count page click видно Page clicks: 1.](images/widgets/modal-lifecycle-desktop-restored.png)
*Широкий экран: страница снова принимает клики.*

![Открытое окно и подложка в галерее шириной 390 пикселей.](images/widgets/modal-lifecycle-mobile-open.png)
*Экран 390 px: окно и подложка.*

![Страница шириной 390 пикселей без подложки с обновлённым счётчиком.](images/widgets/modal-lifecycle-mobile-restored.png)
*Экран 390 px: после скрытия окна страница снова интерактивна.*


<a id="suggestion-open"></a>
### SuggestionField — раскрытый список

Фокус в поле, открыт список подсказок перед выбором с клавиатуры.

![SuggestionField — раскрытый список: Фокус в поле, открыт список подсказок перед выбором с клавиатуры.](images/widgets/suggestion-open.png)

<a id="carousel-selected"></a>
### Carousel — второй слайд

После drag браузер показал 2 / 3; Java обновила подпись Selected image: 2.

![Carousel — второй слайд: После drag браузер показал 2 / 3; Java обновила подпись Selected image: 2.](images/widgets/carousel-selected.png)

## Как обновлять каталог

1. Добавить новый виджет в API-документацию и AllWidgets. Показать обычное состояние и применимые disabled/invalid/loading/empty/open состояния.
2. Собрать текущую версию, открыть её в браузере облачного компьютера. Проверить действие до и после ответа Java, клавиатуру и узкий viewport. Автоматические браузерные тесты дополняют ручную проверку.
3. Снять интерфейс. Вырезать виджет с полезными отступами; для окна оставить немного подложки. Не заменять интерфейс рисованной имитацией, не включать посторонние окна, браузерные вкладки или личные данные.
4. Сохранить crop в `docs/images/widgets`, добавить alt и подпись, обновить `manifest.json` (класс, состояние, источник, координаты, хеш). Для существующего класса заменять устаревшие изображения.
5. Выполнить `npm run check:gallery`, `mvn verify`, `npm run test:browser`; проверить каждый crop визуально в документации. Известные дефекты фиксировать отдельной задачей, не маскировать снимком.
