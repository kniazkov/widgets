# MVC на примере одного нажатия

[Быстрый старт](quick-start.md) · [English](../en/mvc-counter.md)

Запусти счётчик из быстрого старта командой `mvn compile exec:java` и открой
[localhost:8080](http://localhost:8080/). Нажми **Click me** один раз. На странице появилась **1**.
Где именно изменилось число?

## Что происходит при нажатии

Вот страница из файла `src/main/java/example/Counter.java`, который ты создал в быстром старте.
Так же устроен [пример ClickCounter из библиотеки](../../src/main/java/com/kniazkov/widgets/example/ClickCounter.java).

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

**Модель** здесь — `count`. Она хранит целое число с начальным значением 0.
**Представление** — кнопка и надписи, собранные в `Section`. При создании страницы
`setTextModel` связывает числовую надпись с моделью через адаптер. Благодаря ему
счётчик работает с целым числом `0`, а надпись получает строку `"0"`.

1. Ты нажимаешь кнопку в браузере. Widgets передаёт событие на Java-сервер.
2. **Контроллер** — лямбда, переданная в `onClick`. Она читает 0 через `getData()`,
   прибавляет 1 и записывает результат через `setData(...)`.
3. Модель уведомляет адаптер об изменении. Надпись получает от него строку `"1"`,
   а Widgets отправляет обновлённый текст в браузер. Та же страница теперь показывает единицу.

Чтобы начать с десяти, замени `new IntegerModel()` на `new IntegerModel(10)`.
Чтобы считать двойками, замени `+ 1` на `+ 2` в обработчике. Попробуй оба изменения сразу.
Перезапусти приложение и открой страницу. После первого нажатия 10 сменится на 12.

## Упражнение: покажи счётчик в двух местах

Добавь эти строки сразу после существующего вызова `root.add(...)` внутри лямбды страницы.
Во второй строке будет свой текстовый виджет. Текстовую модель он возьмёт у первого.

```java
TextWidget mirror = new TextWidget();
mirror.setTextModel(value.getTextModel());
root.add(new Section(new TextWidget("Total: "), mirror));
```

Останови сервер сочетанием Ctrl+C, снова выполни `mvn compile exec:java` и открой страницу.
С исходными настройками обе строки показывают **0**, после первого нажатия — **1**,
после второго — **2**. Если ты сохранил изменения из предыдущего раздела, увидишь **10**,
**12**, **14**. Обработчик меняет число один раз за нажатие, обе надписи получают новое значение.

Виджеты в примере · [Button](widgets/display.md#button) · [TextWidget](widgets/display.md#textwidget) · [Section](widgets/layout.md#section)

[Дальше — проверь ввод и сохрани значение в форме](form-models.md)
