# Адаптивная компоновка

`Stack`, `ResponsiveGrid` и `FormLayout` принимают блочных детей, как `Panel`.
Они смотрят на доступное место, а не на user-agent. Простая перестройка не
переупорядочивает и не пересоздаёт DOM, поэтому сохраняются значения полей,
фокус, выделение и порядок Tab. Для перестановки самих детей служит обычный API
`add`/`remove` с правилами владения R12.

```java
Stack actions = new Stack(Stack.Direction.HORIZONTAL);
actions.setGap(12);
actions.setAlignment(Stack.Alignment.CENTER);
actions.add(new Section(new Button("Обновить")));
actions.add(new Section(new Button("Сохранить")));

ResponsiveGrid grid = new ResponsiveGrid();
grid.setGap(10);
grid.setColumnWidths(160, 0);
grid.add(new Section(new TextWidget("Карточка A")));
grid.add(new Section(new TextWidget("Карточка B")));

FormLayout columns = new FormLayout();
columns.setBreakpoint(480);
columns.setGap(12);
Form form = new Form("Учебная заявка", "Отправить", columns);
form.addField(new FormField("title", "Название учебного предмета"));
```

- **Stack.** Направление по умолчанию вертикальное; горизонтальные строки
  переносятся, если не вызвано `setWrap(false)`. `setDirection` и
  `setAlignment(START|CENTER|END|STRETCH)` меняют оформление текущих элементов.
- **ResponsiveGrid.** Браузер подбирает количество колонок по ширине самой
  сетки. Указанный минимум (40–2048 CSS px) может сжиматься в узком контейнере.
  Максимум 0 делит доступную ширину поровну. Фиксированный максимум ограничивает
  ширину трека и влияет на расчёт количества колонок CSS `auto-fit`; справа
  может остаться место. Разрыв от 0 до 128 px одинаков по обеим осям.
- **FormLayout.** Подпись стоит слева, а поле, помощь и ошибка справа, если
  ширина содержимого достигает настраиваемого порога (по умолчанию 520 CSS px,
  диапазон 240–2048). Ниже порога подпись над полем. Узкий вложенный контейнер
  на широком экране также остаётся одноколоночным. Если ResizeObserver
  недоступен, поля остаются вертикальными. Наблюдение прекращается при
  отсоединении и возобновляется при подключении.
- Переданный `new Form(name, caption, layout)` макет должен быть пустым и
  неприсоединённым. Добавляйте поля через `form.addField`. Отдельный FormLayout
  отображает подписи, но не запускает валидацию или отправку. Существующий
  конструктор Form оставляет оформление R20 без изменений.
- Обычные длинные строки переносятся; принудительный NoWrap и элементы
  с собственным жёстким внутренним размером требуют более широкого контейнера.
  Кнопка, Enter и ошибки продолжают использовать [форму R20](FORMS.ru.md).

Реальные примеры: [Stack и ResponsiveGrid](WIDGET_GALLERY.ru.md#adaptive-layouts),
[FormLayout](WIDGET_GALLERY.ru.md#adaptive-form).
