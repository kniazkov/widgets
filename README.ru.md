# Web Widgets

[English](README.md) · Русский

С Widgets можно писать интерактивные веб-страницы на Java. Компоновку страницы, её состояние и
обработчики событий можно описать в одном методе. Дерево виджетов хранится на сервере.
Браузер показывает страницу, передаёт действия пользователя в Java и обновляет виджеты,
когда меняются связанные с ними модели.

[Быстрый старт](docs/ru/quick-start.md) · [Каталог виджетов](docs/WIDGET_GALLERY.ru.md) · [Развёртывание с HTTPS](docs/ru/https.md) · [Обновление и эксплуатация](docs/ru/operations.md)

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

## Запусти счётчик

Понадобятся JDK 21 и Maven 3.9. Добавь в POM зависимость `com.kniazkov:widgets-http:0.2.0`. В [быстром старте с Maven](docs/ru/quick-start.md) есть полный POM, Java-файл и команда запуска.

[Примеры](src/main/java/com/kniazkov/widgets/example) · [Лицензия MIT](LICENSE)
