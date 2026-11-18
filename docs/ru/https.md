# Как открыть приложение Widgets по HTTPS

[Web Widgets](../../README.ru.md) · [English](../en/https.md)

## TLS на Java-сервере

Для готового `Application application` понадобится хранилище PKCS#12 с цепочкой
сертификатов и закрытым ключом твоего домена. Прочитай пароль хранилища в массив
`char[] password` из хранилища секретов приложения. В примере используются
`java.io.File`, `com.kniazkov.webserver.SslOptions`, а также `Options` и `Server` из Widgets.

```java
SslOptions tls = new SslOptions.Builder()
    .setKeyStoreFile(new File("server.p12"))
    .setPassword(password)
    .build();
Options options = new Options.Builder().setPort(8443).setSslOptions(tls).build();
Server.start(application, options);
```

Сервер принимает HTTPS-соединения на порту 8443. Открой приложение по имени хоста,
указанному в сертификате. После создания настроек TLS очисти исходный массив с паролем.

## TLS на обратном прокси

Если HTTPS обслуживает прокси на той же машине, направь запросы на локальный порт
Java-сервера. В `setPublicOrigin` укажи схему, имя хоста и, при необходимости, порт
из адреса, который пользователь вводит в браузере. Здесь используется `java.net.InetAddress`.

```java
Options options = new Options.Builder()
    .setBindAddress(InetAddress.getLoopbackAddress())
    .setPort(8080)
    .setPublicOrigin("https://app.example.com")
    .build();
Server.start(application, options);
```

Открой публичный HTTPS-адрес и нажми кнопку, которая меняет модель. Если значение
обновилось, через прокси проходят и загрузка страницы, и действия пользователя.
