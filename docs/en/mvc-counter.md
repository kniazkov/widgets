# MVC in one click

[Quick start](quick-start.md) · [Русский](../ru/mvc-counter.md)

Run the counter from the quick start with `mvn compile exec:java` and open
[localhost:8080](http://localhost:8080/). Click **Click me** once. The page now shows **1**.
Where did that number change?

## Follow one click

Here is the page from `src/main/java/example/Counter.java` in your quick-start project.
The library’s [ClickCounter example](../../src/main/java/com/kniazkov/widgets/example/ClickCounter.java)
uses the same pattern.

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

The **model** is `count`. It holds an integer and starts at zero. The **view** is the button and
the text widgets arranged in a `Section`. When Java builds the page, `setTextModel` binds the
number on screen to `count` through an adapter. The adapter gives the text widget a string
such as `"0"` while the counter still holds the integer `0`.

1. You click the button in the browser. Widgets delivers that event to the Java server.
2. The **controller** is the lambda registered with `onClick`. It reads 0 with `getData()`,
   adds 1 and stores the result with `setData(...)`.
3. The model notifies the adapter, which supplies `"1"` to the bound text widget. Widgets sends
   the text update to the browser. The same page now displays the new count.

To start at ten, change `new IntegerModel()` to `new IntegerModel(10)`. To count in twos,
change `+ 1` to `+ 2` in the handler. Try both changes together. After restarting the
application and opening the page, the first click should take you from 10 to 12.

## Exercise: show the count twice

Add these lines just after the existing `root.add(...)` inside the page lambda. The new row
needs its own text widget, but it can use the same text model as the first row.

```java
TextWidget mirror = new TextWidget();
mirror.setTextModel(value.getTextModel());
root.add(new Section(new TextWidget("Total: "), mirror));
```

Stop the server with Ctrl+C, run `mvn compile exec:java` again and reopen the page.
With the original starting value and increment, both rows show **0**, then **1**, then **2**
as you click. If you kept the changes above, both show **10**, **12**, **14**.
One handler updates the integer once per click. Each label follows the same binding.

Widgets in this example · [Button](widgets/display.md#button) · [TextWidget](widgets/display.md#textwidget) · [Section](widgets/layout.md#section)

[Next: validate input and save a form](form-models.md)
