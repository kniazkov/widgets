# Serve a Widgets application over HTTPS

[Web Widgets](../../README.md) · [Русский](../ru/https.md)

## TLS in the Java server

For an existing `Application application`, provide a PKCS#12 keystore containing the
certificate chain and private key for your domain. Read its password into a `char[] password`
from your deployment’s secret store. Use `java.io.File`, `com.kniazkov.webserver.SslOptions`
and the Widgets `Options` and `Server` classes.

```java
SslOptions tls = new SslOptions.Builder()
    .setKeyStoreFile(new File("server.p12"))
    .setPassword(password)
    .build();
Options options = new Options.Builder().setPort(8443).setSslOptions(tls).build();
Server.start(application, options);
```

The server listens for HTTPS on port 8443. Open the application using the hostname on the
certificate. Clear the original password array after building the TLS options.

## TLS at a reverse proxy

When a proxy on the same host serves HTTPS, forward requests to the Java listener on
loopback. Set the public origin to the scheme, hostname and optional port that users enter
in the browser. The example uses `java.net.InetAddress`.

```java
Options options = new Options.Builder()
    .setBindAddress(InetAddress.getLoopbackAddress())
    .setPort(8080)
    .setPublicOrigin("https://app.example.com")
    .build();
Server.start(application, options);
```

Open the public HTTPS URL and click a button that changes a model. Seeing the updated value
checks that both page loading and interactive requests work through the proxy.
