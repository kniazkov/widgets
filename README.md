# Web Widgets

English · [Русский](README.ru.md)

Build interactive web pages in Java. Define a page’s layout, state and event handlers together.
Widgets keeps the widget tree on the server. Its browser client displays the page, sends user
actions to Java and updates the affected widgets when their models change.

[Quick start](#quick-start) · [Widget catalog](docs/WIDGETS.md) · [Deploy with HTTPS](docs/en/https.md)

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

## Quick start

With Git, JDK 21, Maven 3 and Bash installed, run the bundled example:

```bash
git clone https://github.com/kniazkov/widgets.git
cd widgets
./run.sh com.kniazkov.widgets.example.ClickCounter
```

Open [localhost:8080](http://localhost:8080/). Click **Click me** twice. The counter goes
from 0 to 1 to 2. Press Ctrl+C in the terminal to stop the server.
[Full example with imports and server setup](src/main/java/com/kniazkov/widgets/example/ClickCounter.java)

For your own Maven project, use `com.kniazkov:widgets-http`. To install the modules from
this checkout into your local Maven repository, run `mvn -f modules/pom.xml clean install`.
The [minimal application's pom.xml](src/test/minimal/pom.xml) shows the matching version and dependency.

[Examples](src/main/java/com/kniazkov/widgets/example) · [Visual gallery (Russian)](docs/WIDGET_GALLERY.ru.md) · [MIT license](LICENSE)
