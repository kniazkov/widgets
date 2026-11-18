# Первое приложение Widgets с Maven

[Web Widgets](../../README.ru.md) · [English](../en/quick-start.md)

Начни в пустом каталоге. В конце у тебя будет отдельное Java-приложение с кнопкой, которая
увеличивает счётчик в браузере. Понадобятся Git, Bash, JDK 21 и Maven 3.9.
Выполни `mvn -version` и убедись, что сам Maven использует Java 21.

## Установи Widgets

В примере используется `com.kniazkov:widgets-http:0.2.0`. Эта версия пока доступна из исходников
и ожидает публикации в Maven Central. Из пустого каталога установи веб-модуль указанной ревизии.
Maven также установит родительский POM и модуль ядра. Этот шаг достаточно выполнить один раз.
Когда эти координаты появятся в Central, можно будет начать со следующего раздела:
Maven сам скачает зависимость, указанную в POM.

```bash
git clone https://github.com/kniazkov/widgets.git widgets
git -C widgets checkout --detach d1f627b7c1b17a5e722f7d502db48e32b56e03de
mvn -f widgets/modules/pom.xml -pl :widgets-http -am clean install
```

## Создай приложение

Создай проект рядом с копией исходников библиотеки. Дальше все пути указаны от корня нового проекта.

```bash
mkdir -p hello-widgets/src/main/java/example
cd hello-widgets
```

Сохрани этот файл под именем `pom.xml`. Maven возьмёт Widgets из локального репозитория,
куда ты установил библиотеку на предыдущем шаге.

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

Сохрани страницу вместе с точкой входа в файле `src/main/java/example/Counter.java`.

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

## Запусти и проверь

Из каталога с `pom.xml` твоего приложения выполни:

```bash
mvn compile exec:java
```

Открой [localhost:8080](http://localhost:8080/). Счётчик покажет **0**. Нажми **Click me**
дважды: появится **1**, затем **2**. Чтобы остановить приложение, нажми Ctrl+C в терминале.

## Какую зависимость выбрать?

- `widgets-http` содержит сервер и подключает виджеты, модели и ресурсы браузера, нужные для этой страницы.
- Добавь `widgets-db` для работы с записями и их хранением или `widgets-images` для декодирования и обработки изображений. Для этой ревизии расширь список модулей команды установки (например, `-pl :widgets-http,:widgets-db`) и зависимость той же версии в свой POM.
- Полный артефакт `com.kniazkov:widgets` объединяет все модули и примеры. Если нужен весь комплект, подключи его как единственную зависимость Widgets. Чтобы собрать его из той же копии исходников, выполни `mvn -f widgets/pom.xml clean install` из каталога, куда клонировал Widgets.
