# Серверный WebSocket-адаптер (R35)

Реализация [ADR R34](maintainers/adr/0034-transport.ru.md): отдельный асинхронный listener
Jetty 12.1.15 (Java 21), подключаемый явно. `com.kniazkov:webserver:2.0.0` продолжает
обслуживать прежний HTTP-протокол и бинарные upload без изменений.

**Граница SPI:** этот адаптер отвечает за аутентифицированные текстовые сообщения.
Готовый widget push, подписки страниц, replay и автоматический HTTP fallback
реализованы поверх него в [PushServer (R36)](PUSH_TRANSPORT.ru.md).
Обычный интерфейс продолжает использовать HTTP, пока приложение явно не включит push.

Jetty предоставляет настоящий upgrade, асинхронные записи и demand-driven чтение.
Поэтому длительное соединение не занимает worker старого webserver, а медленный
получатель не вызывает блокирующую запись под монитором клиента/корня виджетов.
Это уже не лабораторный WS→HTTP bridge из R34.

## Подключение

```java
final Application app = new Application((root, context) -> {
    root.add(new TextWidget("Hello"));
});
Server.start(app, new Options.Builder()
    .setPort(8080)
    .setBindAddress(InetAddress.getLoopbackAddress())
    .setPublicOrigin("https://example.com")
    .build());
final StreamServer stream = StreamServer.start(app,
    StreamOptions.defaults(8081, "https://example.com"),
    (connection, message) -> connection.send(message));
// app.close() closes both listeners; stream.close() closes only the stream adapter.
```

Импорты: `com.kniazkov.widgets.base.*`, `com.kniazkov.widgets.view.TextWidget`,
`java.net.InetAddress`. Echo здесь демонстрирует только интеграцию транспорта.
Рабочий проверяемый пример с upload, отзывом сессии и отдельными метриками:
[`StreamTestServer`](../src/test/java/com/kniazkov/widgets/e2e/StreamTestServer.java).

Listener **всегда** слушает `127.0.0.1`; наружу его порт не публикуется. TLS завершается
на доверенном reverse proxy. `publicOrigin` HTTP и stream должны совпадать, чтобы
bootstrap выдавал корректную Secure/HttpOnly `__Host-widgets-session` cookie.
Незашифрованный public origin разрешён только для localhost/loopback разработки.
`StreamServer.start` вызывается после успешного `Server.start`.

Зависимости Jetty подключены через Maven. SLF4J binding библиотека не навязывает:
приложение выбирает свой совместимый SLF4J 2 provider. В тестах используется JUL.
Без provider Jetty предупреждает о выключенном логировании; метрики адаптера
доступны независимо от него. Не включайте debug-дампы HTTP-заголовков/кадров в production.

## Reverse proxy и таймауты

Пример Nginx: `map` находится в `http`, locations — внутри TLS `server` для
`example.com`. Сертификат и обычные лимиты HTTP задаются конфигурацией сервиса.

```nginx
map $http_upgrade $connection_upgrade {
    default upgrade;
    ''      close;
}

location = /widgets-stream {
    proxy_pass http://127.0.0.1:8081;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection $connection_upgrade;
    proxy_set_header Host $host;
    proxy_read_timeout 75s;
    proxy_send_timeout 75s;
    proxy_buffering off;
}
location / {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
}
```

Proxy должен сохранять Origin, Cookie и Sec-Fetch-Site, не подменять их доверенными
значениями. Используйте отдельные лимиты соединений/запросов на proxy (в том числе
до upgrade). Бюджет адаптера относится к зарезервированным WebSocket upgrades,
а не к TCP-соединениям, ещё читающим HTTP-заголовки. Настройте TLS/HTTP header
таймауты и rate limiting на внешней границе. Проверочный Node TLS proxy в CI —
тестовый эквивалент маршрутизации, не проверка конкретной версии Nginx.

По умолчанию: AUTH за 5 с; ping каждые 20 с; ответ pong, незавершённая запись или
обработчик — не более 40 с; принудительное закрытие сокета — через 1 с после
неуспешной попытки отправить close. Proxy timeout должен быть больше интервала
heartbeat и серверного deadline (пример — 75 с). Клиенту следует ограничить
ожидание ready и при отказе оставить HTTP доступным; переподключение и повторы
не встроены в SPI. Heartbeat не является бизнес-ACK.

## Авторизация и сообщения

1. Upgrade только на `/widgets-stream`, без query string, с subprotocol
   `widgets.stream.v1`. Ровно один Origin должен совпасть с настроенным origin.
   Если есть Sec-Fetch-Site, он должен быть `same-origin`.
2. Cookie должна принадлежать существующей активной `BrowserSession`. Повторная
   cookie с тем же именем отклоняется. Upgrade не создаёт и не продлевает сессию.
   Неверная авторизация/Origin → HTTP 403; исчерпан бюджет соединений → HTTP 503.
3. Первое текстовое сообщение:

   ```json
   {"type":"auth","version":1,"session":"bootstrap session UUID","csrf":"bootstrap CSRF token"}
   ```

   Идентификатор и CSRF берутся из уже полученного HTTP bootstrap в памяти
   документа; cookie остаётся HttpOnly. AUTH ограничен 1024 UTF-16 символами плюс
   общим лимитом UTF-8 сообщения. До успешного AUTH пользовательский handler не
   вызывается. Секреты не помещаются в URL, subprotocol или постоянное хранилище.
4. Ответ: `{"type":"ready","version":1,"serverEpoch":"..."}`. Далее handler
   получает текстовые сообщения; бинарные WS-кадры отклоняются (upload — по HTTP).
5. Активность владельца проверяется при AUTH, входящем сообщении, исходящей
   отправке и watchdog. Logout/revoke закрывает уже открытое соединение. AUTH и
   обработанные сообщения продлевают обычный idle budget сессии, heartbeat — нет.

`connection.owner()` — браузерная сессия, **не разрешение на бизнес-операцию**.
Приложение проверяет пользователя/роли/владение объектами отдельно. Для R36 ещё
нужны проверки logical page channels и протокольной последовательности. Наличие
stream не отменяет существующие проверки HTTP.

## Ресурсы и конкурентность

| Настройка | По умолчанию | Что ограничивает |
|---|---:|---|
| connections | 1024 | Все зарезервированные upgrades, включая AUTH и закрывающиеся |
| workers | 4 | Одновременно исполняемые callback |
| tasks | 64 | Глобальная очередь callback |
| messageBytes | 65536 | Размер одного текста в UTF-8 |
| queuedMessages | 64 | Исходящие сообщения на соединение, включая текущую запись |
| queuedBytes | 262144 | Исходящие UTF-8 байты на соединение, включая текущую запись |

I/O Jetty использует собственный пул 4–16 потоков; пользовательские callback —
отдельный ограниченный пул. Для одного соединения callback последовательны:
следующее сообщение читается только после окончания текущего. Переполнение
очереди callback или исходящей очереди закрывает соединение (1013); превышение
размера — 1009; бинарный кадр — 1003; AUTH/revoke — 1008; deadline/остановка —
1001; ошибка handler — 1011. При сетевом обрыве клиент может увидеть 1006.

`send` не блокирует до передачи по сети. `true` означает постановку в очередь,
а не доставку/применение клиентом. Очередь включает текущую асинхронную запись;
после close все её резервации снимаются. Не сохраняйте неограниченные внешние
очереди перед `send`. Для записи не удерживается монитор endpoint.

Deadline отменяет queued/running task и посылает interrupt. Java не может безопасно
убить handler, игнорирующий interrupt: он продолжит занимать один из ограниченных
workers. Используйте прерываемые операции, собственные таймауты внешних сервисов
и короткие callback. I/O watchdog не вызывает пользовательский код и не берёт
монитор root/client; изменение виджетов делайте коротким `executeOnRoot`.

`getMetrics()` возвращает фиксированный набор числовых gauges: `connections`,
`activeHandlers`, `queuedHandlers`, `outgoingMessages`, `outgoingBytes`.
Токенов, текстов сообщений и ID сессий в метриках нет. Это наблюдение, не атомарный
снимок всех соединений; после завершения обработчиков и закрытия значения равны нулю.
Повторный close безопасен; закрытый Application не позволяет повторный запуск.

## Проверка

```sh
mvn -B -ntp verify
npm ci
npx playwright install --with-deps chromium webkit
npm run test:browser
mvn -B -ntp -DskipTests test-compile dependency:build-classpath \
  -Dmdep.outputFile=target/runtime-classpath.txt
node scripts/run-stream-probe.mjs
```

`StreamServerTest` проверяет настоящий upgrade/auth/close/timeout, отказ
неверного Origin/cookie/AUTH, лимит соединений, отзыв сессии, binary/oversize,
исключение handler и доступность HTTP при зависшем callback. Отдельный управляемый
writer проверяет удержание байтов/сообщений до завершения записи и переполнение.

Workflow `Stream adapter` запускает реальный Chromium через HTTPS/WSS reverse
proxy: 32 соединения, два зависших обработчика, обычное событие и upload 64 KiB,
heartbeat, обрыв proxy, новый upgrade, доступность HTTP при отказе upgrade,
отзыв сессии и возврат метрик к нулю. Node peer без pong проверяет deadline.
Самоподписанный сертификат и `ignoreHTTPSErrors` используются **только** в тесте.
Артефакт `stream-probe` содержит SHA, browser version, результаты и серверный лог;
ключ сертификата в артефакт не включается. Полный старый HTTP/browser CI сохранён.

Новых визуальных виджетов нет: AllWidgets и галерея изображений не меняются.
