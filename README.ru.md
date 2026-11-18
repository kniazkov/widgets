# Web Widgets

[English](README.md) · Русский

С Widgets можно писать интерактивные веб-страницы на Java. Компоновку страницы, её состояние и
обработчики событий можно описать в одном методе. Дерево виджетов хранится на сервере.
Браузер показывает страницу, передаёт действия пользователя в Java и обновляет виджеты,
когда меняются связанные с ними модели.

[Быстрый старт](docs/ru/quick-start.md) · [Каталог виджетов](docs/WIDGET_GALLERY.ru.md) · [Развёртывание с HTTPS](docs/ru/https.md)

## Кнопка и счётчик

```java
Page page = (root, context) -> {
    IntegerModel count = new IntegerModel();
    TextWidget value = new TextWidget();
    value.setTextModel(new IntegerToStringModel(count));

    Button button = new Button("Click me");
    button.onClick(event -> count.setData(count.getData() + 1));
    root.add(new Section(button, new TextWidget("Click counter: "), value));
};
```

Здесь **модель** хранит целое число, **представление** показывает его текстом, а роль
**контроллера** выполняет обработчик, переданный в `onClick`. Нажатие кнопки увеличивает
число. Надпись обновляется сама благодаря привязке через `setTextModel`.

## Запуск готового примера

Понадобятся Git, JDK 21, Maven 3 и Bash. Склонируй репозиторий и запусти готовый пример:

```bash
git clone https://github.com/kniazkov/widgets.git
cd widgets
./run.sh com.kniazkov.widgets.example.ClickCounter
```

Открой [localhost:8080](http://localhost:8080/) и дважды нажми **Click me**. Счётчик покажет
сначала 0, затем 1 и 2. Чтобы остановить сервер, нажми Ctrl+C в терминале.
[Полный пример с импортами и запуском сервера](src/main/java/com/kniazkov/widgets/example/ClickCounter.java)

Чтобы создать свой проект, пройди [быстрый старт с Maven](docs/ru/quick-start.md).

[Примеры](src/main/java/com/kniazkov/widgets/example) · [Справочник классов (на английском)](docs/WIDGETS.md) · [Лицензия MIT](LICENSE)
