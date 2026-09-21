# Request and browser diagnostics

Widgets uses `java.util.logging`. With the default Java logging configuration, SEVERE records and
stack traces go to stderr; a systemd service with `StandardError=journal` captures them in its journal.
Custom JUL handlers/levels can change this routing.

## Server failures

The outer `HttpHandler.handle` boundary logs `ServerException`, `RuntimeException` and `Error` with
its original throwable, then rethrows the **same instance** to webserver. This covers action handlers,
page creation, upload callbacks, form parsing inside widgets and response construction. Action and
upload handlers no longer turn exceptions into HTTP 200 `{clientError:true}`. The underlying server
chooses the HTTP status (normally 500 for an unexpected exception).

One SEVERE record from `com.kniazkov.widgets.base.HttpHandler` contains the original stack and cause.
Request bodies, headers and query strings are not appended to this record. Exception messages are
application-defined, so application code should not put passwords or customer data in exceptions.
Existing expected static-file failures retain their existing handling.

## Browser failures

HTTP 5xx responses are logged to the browser console with their status and converted by the browser
transport into the existing fatal client-error signal. The page shows `Client Error` and stops its
synchronization loop instead of replaying a potentially partly executed action. Server response
bodies are not displayed or logged by the transport. Network failures retain the reconnect behavior;
other non-200 statuses are recorded in the console.

When JavaScript fails after a client has been created, `showClientError` makes one best-effort POST
`action=report error` containing its client ID and error stack/message. The application accepts at
most one report per live client, truncates it to 8192 characters and removes control characters before
logging a SEVERE `Browser-reported error` record with client/server identifiers. These are untrusted
browser reports, not proof of a server exception. No form contents, cookies or full protocol updates
are collected automatically, but application error text/stacks may themselves contain sensitive data.

Reporting does not retry, extend the client lifetime or prevent the original error overlay. If there
is no client yet, the client has expired, or the network/server is unavailable, the report cannot be
recorded server-side; the browser console remains the source. Reports do not provide authentication
beyond the existing client identifier and are subject to webserver's ordinary request size limits.

## Boundaries outside widgets

No webserver changes are included. Request parsing before widgets is invoked, handler timeouts
managed by webserver and socket/write failures after widgets returns remain outside this boundary.
This also does not install logging handlers or modify the host application's systemd service.
