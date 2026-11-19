# Your first Widgets application with Maven

[Web Widgets](../../README.md) · [Русский](../ru/quick-start.md)

Start with an empty directory. By the end you will have a standalone Java application with a button
that increments a counter in your browser.

## Create the application

You need JDK 21 and Maven 3.9. Run `mvn -version` to check which JDK Maven uses.
Create a directory for the application and its Java source.

```bash
mkdir -p hello-widgets/src/main/java/example
cd hello-widgets
```

Save this as `pom.xml`. Maven downloads `com.kniazkov:widgets-http:0.2.0` and its dependencies from Maven Central on the first build.

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>example</groupId>
    <artifactId>hello-widgets</artifactId>
    <version>1.0</version>
    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    <dependencies>
        <dependency>
            <groupId>com.kniazkov</groupId>
            <artifactId>widgets-http</artifactId>
            <version>0.2.0</version>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.15.0</version>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.5.0</version>
                <configuration>
                    <mainClass>example.Counter</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Save the page and its entry point together in `src/main/java/example/Counter.java`.

```java
package example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.model.IntegerModel;
import com.kniazkov.widgets.model.IntegerToStringModel;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

public class Counter {
    public static void main(String[] args) {
        Page page = (root, context) -> {
            IntegerModel count = new IntegerModel();
            TextWidget value = new TextWidget();
            value.setTextModel(new IntegerToStringModel(count));

            Button button = new Button("Click me");
            button.onClick(event -> count.setData(count.getData() + 1));
            root.add(new Section(button, new TextWidget("Click counter: "), value));
        };
        Server.start(new Application(page), new Options.Builder().setPort(8080).build());
    }
}
```

## Run it

From the directory containing your application's `pom.xml`, run:

```bash
mvn compile exec:java
```

Open [localhost:8080](http://localhost:8080/). The counter starts at **0**. Click **Click me**
twice to see **1** and then **2**. Press Ctrl+C in the terminal to stop the application.

## Which dependency do I need?

- `widgets-http` includes the server and pulls in the widgets, models and browser resources needed for this page.
- Add `widgets-db` for the library's record and persistence APIs, or `widgets-images` for image decoding and processing.
- The full `com.kniazkov:widgets` artifact bundles all modules and examples. Use it on its own if you need that distribution.

[Next: follow a click through the model, view and controller](mvc-counter.md)
