# Выпуск Widgets

## Политика версий

Maven-координаты: `com.kniazkov:widgets:MAJOR.MINOR.PATCH`. Первая новая база —
**0.2.1**, кандидат на выпуск после накопленных изменений исторической 0.1.
Наличие этой версии в pom/CHANGELOG не означает публикацию на Maven Central.

До 1.0 несовместимые API/протокольные изменения увеличивают MINOR, совместимые
исправления — PATCH. После 1.0 несовместимость увеличивает MAJOR, новые совместимые
возможности — MINOR, исправления — PATCH. Для разработки возможен `-SNAPSHOT`,
но release pipeline принимает только фиксированную `X.Y.Z`. Для следующего
кандидата увеличь версию заранее; опубликованный номер повторно не используется.

Каждая версия имеет раздел в [CHANGELOG.md](../../CHANGELOG.md): новые возможности,
breaking changes, действия потребителя и ограничения отката. Для 0.1 нельзя
обещать бинарную совместимость: разные коммиты распространялись под одним номером.
Потребитель должен пересобраться и пройти собственные тесты.

Тег выпуска — **`vX.Y.Z`**, аннотированный и неизменяемый. Не перемещай существующий
тег и не перезаписывай опубликованные Maven-артефакты. Исправление выпуска требует
нового PATCH/MINOR/MAJOR по правилам выше. Тег ставится на проверенный commit;
тег создаёт сопровождающий.

## Идентичность Java и браузера

Maven фильтрует только `src/main/release/widgets-release.properties`. В JAR находятся:

- версия Maven;
- HTTP envelope version (сейчас 1);
- ordered update protocol version (сейчас 2);
- bundle negotiation contract version (сейчас 1).

`ReleaseInfo.version()/httpProtocol()/updateProtocol()/buildId()` доступны потребителю.
Build ID — SHA-256 упорядоченных имён и байтов загруженных классов
`com/kniazkov/widgets`, HTML/CSS/JS/шрифтов и release descriptor. Он вычисляется один
раз на classloader и не включает ZIP-время, порядок записей и классы потребителя.
Одинаковые class directory и JAR дают одинаковый ID. Изменение Java или ресурсов
под тем же Maven-номером всё равно меняет ID. Другой компилятор может изменить
байткод: разворачивай один и тот же собранный артефакт на всех узлах.

Поддержаны обычные class directories, библиотечные JAR и assembly JAR (включая
`jar-with-dependencies`). Вложенные JAR/нестандартные classloader без
file code source и relocation пакета не заявлены. Нельзя перекрывать стандартные
Widgets-ресурсы посторонними файлами на classpath или менять их после запуска.
ID подтверждает совместимость байтов, но **не заменяет подпись, авторизацию или
доверенное происхождение артефакта**.

HTTP bootstrap сообщает `X-Widgets-Build` и `X-Widgets-Release`; JavaScript и CSS
в его ссылках получают `?widgets-build=<ID>`. Если такой URL пришёл на другой
bundle, сервер отвечает **404/no-store**, не выдавая новый код под старым адресом.
Существующий `widgets-version=<content hash>` для статических ресурсов сохраняется.
Ресурсы продолжают проходить обычную ETag/revalidation-политику; release descriptor
не опубликован как произвольный classpath-файл.

Идентификатор встроен и в `shared/transport.js`, и в изолированный page runtime.
Их несовпадение также приводит к отказу: старый runtime не может заимствовать ID
нового транспорта. При `new instance` клиент
передаёт его как `clientBuild`, сервер сравнивает **до вызова Page.create**. При
отсутствии или несовпадении возвращается протокольный JSON с
`protocolMismatch=true`, `clientBuildMismatch=true`, ожидаемым ID и версией.
HTTP-статус остаётся 200 для совместимости с существующим обработчиком протокольных
отказов; новая UI-сессия, обработчики и бизнес-события не создаются.

Новый JS останавливается с пояснением и предлагает ручную перезагрузку. Старые
клиенты, знающие только `protocolMismatch`, показывают свой protocol error.
Автоматического бесконечного reload и переноса старых событий в новую сессию нет.
Заголовки/ID публичны и не являются разрешением доступа. Существующие cookie,
CSRF, channel token, epoch и fencing из R37 продолжают действовать.

При изменении формы HTTP envelope или ordered updates увеличь соответствующий
номер descriptor **и** константу JS. При изменении handshake — bundleProtocol.
Release check проверяет согласованность Java/JS номеров. Точное совпадение build ID
намеренно строже «совместимого диапазона SemVer»: разные сборки не смешиваются
в одной UI-сессии, даже если изменение формально совместимо.

## Проверка кандидата и выпуск

На чистом checkout:

```sh
mvn -B -ntp clean install
mvn -B -ntp -f src/test/consumer/pom.xml clean verify
node scripts/check-release.mjs
npm ci
npm run test:browser
```

Требуются JDK 21, Maven и Node 22; `java`/`jar` берутся из JAVA_HOME, если он задан.
Сборка `install` проходит `verify` и локально устанавливает кандидат для независимого
Maven consumer. В pipeline consumer собирается отдельным Maven-проектом с фиксированной
зависимостью, а затем запускается против проверяемого JAR. Это smoke отдельного
потребителя.

`Release candidate` запускается для PR, вручную и при push `v*`. Он проверяет:

1. SemVer Maven, наличие changelog, версию consumer и совпадение имени тега с Maven.
2. Для tag build — совпадение тега и checkout; для PR — отсутствие уже выпущенного
   тега этого номера. Поэтому после выпуска следующий PR должен поднять версию.
3. Наличие основного JAR, sources и javadoc; отсутствие test/e2e классов.
4. Все HTML/CSS/JS/шрифты исходного bundle попали в JAR без изменения, descriptor
   отфильтрован и протокольные версии Java/JS согласованы.
5. Consumer компилируется, запускает сервер из JAR, отдаёт публичную страницу и
   ресурсы; class directory и JAR имеют одинаковый ID, изменённый CSS — другой.

Артефакт `release-candidate` содержит JAR-файлы и `release-verification.json`
(SHA checkout, версия, ожидаемый тег, build ID, SHA-256 основного JAR, результат
smoke). Обычный Tests CI отдельно выполняет `mvn verify` на Linux/Windows и полный
browser suite. E2E подменяет JS на клиент без build ID/с неверным ID, проверяет отказ,
отсутствие созданного UI и успешную работу после явного reload.

После зелёных проверок и слияния на выбранном проверенном commit:

```sh
git tag -a v0.2.1 -m 'Widgets 0.2.1' <проверенный-SHA>
git push origin v0.2.1
```

Сборка тега проверяет пакет с временным ключом. Для публикации открой проверенный тег
в чистой рабочей копии и используй свой ключ подписи.

## Загрузка в Maven Central

Понадобятся JDK 21, Maven 3.9 или новее, GnuPG и право публикации в пространстве
`com.kniazkov`. Можно использовать тот же действующий токен и GPG-ключ, что и для
других библиотек этого пространства. Публичный ключ должен быть доступен Central.
[Настройка GPG](https://central.sonatype.org/publish/requirements/gpg/)

Добавь сервер с идентификатором `central` в раздел `<servers>` своего
`~/.m2/settings.xml` (в Windows — `%USERPROFILE%\.m2\settings.xml`). Впиши имя
и пароль, выданные вместе с токеном Central Portal. [Получить токен](https://central.sonatype.com/usertoken)

```xml
<server>
    <id>central</id>
    <username>ИМЯ_ИЗ_ТОКЕНА</username>
    <password>ПАРОЛЬ_ИЗ_ТОКЕНА</password>
</server>
```

Из корня репозитория выполни:

```sh
mvn deploy
```

Maven подписывает родительский POM `widgets-modules` и пять библиотек версии
`0.2.1`: `widgets`, `widgets-core`, `widgets-http`, `widgets-db` и `widgets-images`.
У каждой библиотеки в пакете есть POM, основной JAR, исходники и Javadoc. Если
локально несколько ключей, укажи нужный отпечаток через `-Dgpg.keyname=ОТПЕЧАТОК`.

Команда загружает один пакет и ждёт завершения валидации. Когда статус станет
**Validated**, открой [Deployments](https://central.sonatype.com/publishing/deployments),
выбери эту загрузку и нажми **Publish**. После появления статуса **Published**
проверь быстрый старт с чистым локальным Maven-репозиторием.

Чтобы проверить сборку у себя до загрузки, выполни `mvn clean deploy -DskipUpload=true`.
Подписанный пакет появится в `modules/target/central-publishing/central-bundle.zip`.
Его содержимое, подписи и контрольные суммы проверяет `python3 scripts/check-central-bundle.py`.
