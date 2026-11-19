# Web Widgets

English · [Русский](README.ru.md)

Build interactive web pages in Java. Define a page’s layout, state and event handlers together.
Widgets keeps the widget tree on the server. Its browser client displays the page, sends user
actions to Java and updates the affected widgets when their models change.

[Quick start](docs/en/quick-start.md) · [Widget catalog](docs/WIDGETS.md) · [Deploy with HTTPS](docs/en/https.md)

## A button and a counter

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

Here the **model** holds an integer, the **view** displays it as text, and the **controller**
is the handler passed to `onClick`. A click increments the model. The text follows because
`setTextModel` binds it to the counter. You can see all three parts in the same Java method.

## Run the counter

Use JDK 21 and Maven 3.9. Add `com.kniazkov:widgets-http:0.2.0` to your POM. The [Maven quick start](docs/en/quick-start.md) includes the complete POM, the Java file and the command to run it.

[Examples](src/main/java/com/kniazkov/widgets/example) · [MIT license](LICENSE)
