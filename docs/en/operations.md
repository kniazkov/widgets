# Update and operate a Widgets application

[Web Widgets](../../README.md) · [Русский](../ru/operations.md)

## Upgrade and roll back

Pin every Widgets module to the same Maven version. Before 1.0 a minor release can break compatibility. Read the [changelog](../../CHANGELOG.md), rebuild your application and inspect `mvn dependency:tree`. Applications built against the historical 0.1 artifact need recompilation because different builds shared that version.

Test the packaged application on staging. Open a page, submit a form and try a file upload if your application accepts files. Deploy the same application JAR and its matching browser resources together. The browser uses the build ID to detect stale resources. Keep an old application artifact and a backup of persistent data. Check schema compatibility before rolling back. Widgets does not migrate your application's database.

## When the connection drops

During a short outage the browser retries. If the server still owns the page, the draft and history survive. It can acknowledge a retried event without running its handler twice. Use application-level idempotency for external operations such as charging a payment.

A server restart discards in-memory pages, sessions and pending events. The browser opens a new page at the same URL. Save important drafts to durable storage before deployment. When using several instances, route a live session to its owner. A different instance cannot resume its widget tree.

## Upload limits

By default a file may be up to 128 MiB. Widgets sends 64 KiB chunks. Set the file and chunk limits separately with `Options.Builder.setMaxFileSize` and `setChunkSize`. The largest supported chunk is 1 MiB. Allow room for multipart headers in your proxy's request limit.

The default registry allows 16 outstanding files per client, 64 per application and 256 MiB of declared sizes across them. The 300-second lifetime includes processing. Copy content that must survive the upload callback to application storage. [Upload widgets](widgets/files.md)
