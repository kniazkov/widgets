# ADR R34: сменный транспорт, HTTP по умолчанию и кандидат WSS

Дата: 17 ноября 2026. Статус: **принято; опциональная реализация R35–R36 доступна**.
Подключение и текущий контракт: [PUSH_TRANSPORT.ru.md](../../PUSH_TRANSPORT.ru.md).
Ниже сохранены основания первоначального решения R34. Задача [#138](https://github.com/kniazkov/widgets/issues/138).

## Решение

Сохраняем адаптивный HTTP polling как транспорт по умолчанию и fallback.
Для R35–R36 выбираем **один WSS на документ с независимыми каналами страниц**;
бинарные файлы продолжают передаваться отдельными HTTP-запросами. Не открываем
по сокету на каждый виджет или каждую кешированную страницу.

Это выбор направления разработки, а не утверждение, что WebSocket уже быстрее
на реальном Java-сервере. Текущая библиотека webserver 2.0.0 не предоставляет
контракт upgrade/асинхронной записи. R34 не включает скрытый production endpoint,
новый сервер или автоматическое переключение всех приложений на WebSocket.
Node gateway находится только в `scripts/transport`, использует devDependency
`ws` и не попадает в Java JAR/manifest клиентских скриптов.

## Что показывает исходная нагрузка

R33 ([PR #185](https://github.com/kniazkov/widgets/pull/185), head `4bd3ba4`,
измеренный CI merge `2e419ef4fa8ef17222847cc8c253c81fbe511c04`) дал следующие
значения click→DOM p95 в настоящем Chromium:

| Фоновых HTTP-каналов | Повтор 1, мс | Повтор 2, мс |
|---|---:|---:|
| 1 | 73,92 | 68,26 |
| 100 | 124,15 | 82,07 |
| 1000 | 92,87 | 84,86 |

Это Xeon Platinum 8573C, 4 видимых CPU, JVM 1 GiB, Chromium 151.0.7922.34,
по 100 кликов; фоновый генератор ограничен 16 HTTP workers. Время включает
Playwright. Эти числа не являются SLA и не показывают задержку независимого
серверного изменения при пустом polling. Методика: [R33](../LOAD_TESTING.ru.md).

Измерения **не обосновывают срочную замену HTTP ради скорости клика**. Основная
цель будущего push — доставка изменений, появившихся без пользовательского
события, и уменьшение пустого трафика. Polling оставляет ожидание следующего
планового запроса; его наличие следует из алгоритма R32, а не из приведённых
выше измерений. Нельзя подменять этот вывод обещанием конкретного выигрыша WSS.

## Сравнение вариантов

| Вариант | Задержка и запросы | Соединения/worker limits | Proxy/timeouts | Reconnect и сложность |
|---|---|---|---|---|
| Адаптивный HTTP (сейчас) | Событие отправляется сразу; самостоятельное серверное изменение ждёт опроса. Пустые ответы продолжаются. | Короткие операции, отдельный запрос на страницу; документный планировщик ограничивает конкурентность. | Обычные HTTP-запросы, таймаут запроса уже поддержан. | Есть event ACK, replay, resync, session/epoch. Минимальный риск совместимости. |
| Long polling | Ответ удерживается до изменения/таймаута; после ответа нужен новый запрос. Возможен промежуток между запросами. | Как минимум один ожидающий запрос на документ; блокирующий handler текущего сервера занимает ресурс worker. Нужно отделить ожидание от обработки. | Ожидание согласовать с proxy/read timeout; промежуточные узлы могут обрывать соединение. | Сохраняет HTTP auth, но требует отмены старого ожидания, heartbeat, multiplex и bounded queues. Умеренная сложность, не бесплатное изменение интервала. |
| SSE + HTTP | Поток server→browser; события/ACK/upload остаются HTTP. | Один поток на документ плюс короткие POST. Нужны настоящий streaming и независимые очереди страниц. | Проверить buffering и timeouts; просто Content-Type `text/event-stream` недостаточен. | EventSource умеет reconnect/Last-Event-ID, но это не наш event ACK и не независимые page checkpoints. Нативный API ограничивает передачу заголовков; auth нельзя переносить в URL. |
| WSS + HTTP uploads | Двунаправленный поток для событий и обновлений, без POST на каждое событие. | Один socket на документ; bounded per-page очереди и общий лимит документа. Ожидание socket не занимает HTTP worker. | Нужны upgrade, TLS, Origin, heartbeat и настройки reverse proxy. | Больше кода: handshake/auth, correlation, resume, generation fence и fallback. Позволяет сохранить один физический путь событий/ответов и независимые логические каналы. |

Сравнение опирается на [RFC 6202](https://www.rfc-editor.org/rfc/rfc6202.html),
[RFC 6455](https://www.rfc-editor.org/rfc/rfc6455.html),
[WHATWG EventSource](https://html.spec.whatwg.org/multipage/server-sent-events.html),
[WebSockets API](https://websockets.spec.whatwg.org/) и
[nginx WebSocket proxying](https://nginx.org/en/docs/http/websocket.html).
Практические значения proxy timeouts необходимо проверять в конкретном деплое.

### Почему выбран WSS, а не SSE/long polling

Нам нужен двунаправленный документный канал: пользовательские команды,
подтверждения обновлений и данные нескольких активных/кешированных страниц.
SSE оставляет отдельный путь команд и требует такого же серверного streaming
адаптера. Long polling также требует асинхронного ожидания и разграничения
worker budgets. Поэтому не реализуем два промежуточных транспорта, прежде чем
проверить WSS. Если R35–R36 не подтвердят устойчивость/стоимость, HTTP остаётся
работоспособным; ADR можно пересмотреть по тем же сценариям, без смены протокола.

## Граница протокола и транспорта

`shared/transport-channel.js` содержит общий `createProtocolTransport(driver, limits)`.
`createHttpTransport()` использует этот контракт и прежний HTTP encoder/status mapper.
Существующий `sendRequest(query, callback, method, files, channel)` остаётся
совместимым входом; `configureProtocolSecurity` по-прежнему получает bootstrap
секреты. Новый API не делает event IDs и не меняет `UpdateStream`.

| API | Обязательство |
|---|---|
| `connect()` | Один переход new→connecting→open. Может вернуть Promise для асинхронного driver. Закрытый экземпляр не переоткрывается. |
| `send(packet, callback)` | Не более `maxInFlight` запросов; ответ ровно один раз, возвращается функция отмены. При отказе/неопределённом исходе callback получает null. Нет скрытой очереди и автоматического повтора. |
| `channel(credentials)` | Логическая страница с отдельными token/server epoch/update epoch. Не более `maxChannels`; credential object хранится только в памяти. |
| `channel.send(query, callback, files)` | Отправляет исходные event IDs/checkpoint без перенумерации и без подтверждения за владельца протокола. |
| `channel.close()` | Отменяет операции этой страницы, оставляя остальные страницы документа живыми. Не заменяет серверный `kill`/unsubscribe: их отправляет владелец lifecycle. |
| `receive(listener)` | Подписка на непрошенные сообщения будущего push driver; возвращает unsubscribe. Сообщение ещё должно пройти version/epoch/checkpoint проверки протокола. HTTP driver сам push не создаёт. |
| `close()` | Терминально закрывает физический транспорт, отменяет незавершённые операции, удаляет подписки. Поздние ответы старой операции игнорируются. |

Пакет — `{query, method?, files?, channel?}`; данные callback — прежняя строка
протокольного ответа либо null. Driver: `connect(deliver, disconnected)`,
`send(packet, done) -> cancel`, `close()`. Он не применяет DOM и не выбирает
повторяемость команды. `classifyHttpResponse` сохраняет прежнее отображение
401/403/413/426/5xx и несовместимой версии. Общий HTTP transport допускает до
64 незавершённых запросов; при переполнении возвращает null как временный
транспортный отказ, не создавая бесконечную очередь. На обычном пути ограничения
R32 существенно ниже этого предела.

Пример после штатного bootstrap (не альтернативный способ авторизации):

```js
const transport = createHttpTransport({ maxInFlight: 8, maxChannels: 4 });
await transport.connect();
const page = transport.channel({
    token: negotiated.channelToken,
    epoch: negotiated.serverId,
    updateEpoch: negotiated.updateEpoch
});
page.send({ action: "synchronize", client: negotiated.id,
    events: [], lastUpdate: "#" + appliedCheckpoint }, raw => {
    // Передать raw владельцу протокола: проверить epoch и применить обновления.
    // Сам факт получения байтов не означает event ACK или успешное применение DOM.
});
page.close();
transport.close();
```

Новый видимый виджет не добавлен: AllWidgets/галерея не меняются.

## Контракт будущего WSS (R35–R36)

1. TLS и exact Origin проверяются до upgrade. Cookie доказывает браузерную сессию;
   до `AUTH` нельзя подписываться на страницы или выполнять команды. Bootstrap
   CSRF/session передаются в первом управляющем сообщении, не в URL, subprotocol
   или storage. У handshake есть конечный deadline и лимит unauthenticated sockets.
2. `OPEN_PAGE` предъявляет page token, server epoch, update epoch и последний
   **применённый** checkpoint. Авторизация повторяется на каждом сообщении и при
   server push; существующий socket не сохраняет доступ после logout.
3. Физический `requestId` коррелирует ответ, но не заменяет event ID. У каждой
   страницы независимы `lastEvent`, `lastUpdate` и replay window. ACK другой
   страницы/поколения не освобождает её очередь. Дубликат команды не исполняется
   повторно; потерянный ответ не означает, что команда не была выполнена.
4. `UPDATES` не продвигает checkpoint до успешного применения клиентом. Пропуск,
   истёкшее replay window или новый server epoch требуют resync/snapshot.
5. Reconnect создаёт новый physical generation, но повторяет неполученные ACK
   команды с прежними event IDs. Отмена физического запроса не является отменой
   бизнес-действия. Между HTTP fallback и WSS запрещены два независимых отправителя
   одной страницы; переключение проходит через generation fence.
6. Heartbeat обслуживает доступность соединения, не подтверждает бизнес-команды
   и не продлевает отозванную сессию. Начальные настройки для проверки R35:
   ping каждые 20 с, deadline 40 с, proxy read timeout не менее 75 с. Это проектные
   значения, требующие измерений за реальным proxy, а не настройки текущего API.
7. Лимиты задаются отдельно для frame bytes, документа, страницы, ожидающих auth,
   in-flight команд и upload workers. Медленный потребитель получает backpressure
   или закрытие с resync, а не неограниченную очередь. Fair scheduling не позволяет
   большой кешированной странице остановить активную.
8. Binary upload остаётся HTTP с существующими chunk ID/receipt/budget. Падение
   socket не повторяет файл целиком и не создаёт второй upload descriptor.

## Серверное ограничение и SPI

Проверенный API зависимости `com.kniazkov:webserver:2.0.0`:
`Response.getData(): byte[]`; `Server` предоставляет start/stop/port/bind address,
но не upgrade/stream writer. Проверить можно `javap` по разрешённому Maven JAR.
Текущий `Widgets Server` передаёт обычный `HttpHandler`; нельзя удерживать его
root/client monitor в ожидании следующего push-события. Обработка соединения
должна быть отдельно от коротких HTTP/upload workers.

R35 должен предоставить адаптер с asynchronous open/write/close/cancellation,
ограниченной исходящей очередью и server-origin/auth hook. При выборе между
расширением webserver и другим поддерживаемым Java-сервером обязательны реальные
upgrade/stream/worker-release тесты. R34 не объявляет такой SPI реализованным:
общий Java протокол остаётся точкой применения auth/event ACK/update replay.

## Воспроизводимый сравнительный прототип

```sh
npm ci
mvn -B -ntp verify dependency:build-classpath -Dmdep.outputFile=target/runtime-classpath.txt
npx playwright install --with-deps chromium
node scripts/run-transport-probe.mjs
```

[Workflow](../../../.github/workflows/transport-probe.yml) выполняет два повторения
HTTP и WS-прототипа в настоящем Chromium. В каждом: одинаковый новый Java-процесс,
одинаковая страница и 100 последовательных кликов; далее один набор контрактных
сценариев. Исходные данные/версии/SHA/квантили/traffic и результаты проверок
сохраняются в `target/transport-probe/report.json`, ошибки Java — `server.log`.
Сравниваются один и тот же business handler и protocol boundary. UI-измерение
включает Playwright, а `connectMs` также включает установку лабораторного adapter.
Это не чистое handshake latency. Отдельного прогрева нет; порядок HTTP→WS одинаков
в обоих повторах. Транспортные цифры надо сравнивать с разбросом повторений.

Топология прототипа: browser → loopback Node gateway → неизменённый Java HTTP handler.
HTTP проходит тот же gateway, WS кадры преобразуются в HTTP **внутри стенда**.
Origin gateway проверяется отдельно; cookie берётся из upgrade request, а не из
произвольного поля кадра. Backend origin фиксирован, пользователь не выбирает
адрес назначения. На Java сохраняются реальные сессии, токены и R13/R17 проверки.
Бинарные запросы идут обычным HTTP. Разрыв закрывает незавершённые backend fetch;
Java-действие при этом могло уже выполниться.

Лабораторные лимиты: 32 sockets, 16 in-flight/frame requests на socket, 128 KiB
входной frame, 512 KiB output buffer, без compression. Ping/проверка pong каждые
10 с; HTTP backend deadline 10 с. Это ограничения эксперимента, не production defaults.
`ws` закреплён на 8.23.0 в devDependencies и lockfile.

Измеряются front POST count/body bytes и JSON frame count/body bytes; TLS/HTTP/WS
заголовки и маскирование не включены. Backend POST для WS учитываются отдельно,
поэтому исчезновение POST в браузере не выдаётся за исчезновение работы сервера.
Нагрузка здесь — один документ, а не повтор 1000 клиентов R33. Настоящих server
push, WSS/TLS, proxy buffering и 1000 sockets этот мост не проверяет; это обязательные
следующие проверки R35/R36. Функция `receive` пока проверена контрактным тестом,
а потоковая delivery интеграция остаётся R36.

Общие сценарии для обоих физических путей:

- один event ID исполняется один раз;
- неправильный page token/update epoch и чужой браузер с украденным page token
  не получают доступ;
- обрыв после выполнения команды, reconnect и повтор того же ID не удваивают действие;
- медленный клиент получает resync после истечения replay window, snapshot сохраняет
  бизнес-состояние;
- превышение 64 событий не меняет состояние;
- logout отзывает уже открытый канал; рестарт Java отклоняет старые session/epoch;
- файл 64 KiB с проверкой содержимого проходит HTTP и при выбранном WS-прототипе.

Полные значения текущего сравнительного прогона и SHA прикладываются в PR R34;
артефакт хранится 7 дней, итоговая таблица в PR сохраняется постоянно. R33-таблица
выше — исходная production-база, а не выдуманные результаты ещё не реализованного push.

## Измеренный результат прототипа R34

[Облачный прогон 44082722474](https://github.com/kniazkov/widgets/actions/runs/44082722474),
head `d5861f4c1a092dbfad5c4386a8f03b327a7fc137`, проверенный merge
`e855793545b8d8432bc1250ffbae0ceef9169075`. AMD EPYC 9V74, 4 видимых CPU,
16 760 348 672 байт RAM, Temurin 21.0.13+9, Node 22.23.4,
Chromium 151.0.7922.34. Оба повторения обоих путей прошли все восемь
контрактных проверок и HTTP binary upload. После этого добавлен отдельный
regression-тест освобождения sibling requests при исключении callback;
финальный SHA повторно проверяется тем же workflow.

| Транспорт | Повтор | click→DOM p50, мс | p95, мс | p99, мс | Front POST | WS request frames | WS→backend POST |
|---|---:|---:|---:|---:|---:|---:|---:|
| HTTP | 1 | 50,03 | 64,63 | 79,65 | 100 | 0 | 0 |
| WS-прототип | 1 | 50,01 | 56,13 | 65,42 | 0 | 100 | 100 |
| HTTP | 2 | 49,95 | 52,57 | 64,37 | 100 | 0 | 0 |
| WS-прототип | 2 | 49,99 | 53,14 | 58,47 | 0 | 100 | 100 |

За 100 событий в каждом повторе HTTP request/response bodies составили
71 892 / 43 476 байт; WS JSON request/response payload — 64 484 / 59 968 байт.
Заголовки HTTP, TLS и WS framing в эти числа не включены. В WS payload прототипа
также находятся auth и обёртка HTTP-ответа, поэтому это **не сравнение полных
сетевых байтов**. Upgrade выполнен до измеряемой фазы; нулевой upgrade count
в её delta не означает отсутствие handshake. На HTTP Java тоже получает 100
POST: последняя колонка считает лишь дополнительные POST моста WS→HTTP.

Вывод: на этом стенде median практически одинаков, диапазоны p95 пересекаются;
двух повторов недостаточно для утверждения о выигрыше. Прототип убирает POST
с browser→gateway участка, но оставляет их все на gateway→Java участке и
не демонстрирует уменьшения серверной работы или задержки настоящего push.
Это подтверждает решение **сохранить HTTP default**, а выбор WSS оставить
направлением следующей реализации с обязательными дополнительными измерениями.
Сам мост нельзя переносить в production как «готовый быстрый транспорт».

## Совместимость, rollout и откат

В R34 меняется только граница браузерного HTTP adapter, сохраняются headers,
multipart, callback/status mapping, event/update IDs, checkpoints и upload API.
Все старые Java/JS/E2E тесты остаются в обязательном Tests workflow.
`createProtocolTransport` сам ничего не переподключает и не включает WSS.

R35: реализовать и проверить серверный адаптер с длительными соединениями,
отдельными resource budgets, настоящим TLS/reverse proxy и отзывом доступа.
R36: реализовать push/мультиплексирование/heartbeat/resume с флагом opt-in;
прогнать общие протокольные сценарии и R33-нагрузку на HTTP и WSS. Только после
этого решать о новом default. Измерить event→DOM для независимых серверных событий,
CPU/heap/GC/очереди/requests и влияние idle sockets на обычный HTTP/upload.

Откат rollout: выключить флаг WSS, завершить physical generation и возобновить
HTTP владельцем существующих event IDs/checkpoints; при несовместимой epoch —
resync, без слепого повторения неидемпотентных действий. SessionExpired/accessDenied
никогда не обходятся fallback. В R34 достаточно оставить штатный HTTP entry point:
лабораторный socket adapter вообще не загружается обычным приложением.
