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

One SEVERE record from `com.kniazkov.widgets.base.HttpHandler` contains the original stack and cause,
plus the bounded request metadata described below. Bodies, query strings, cookies, authorization
headers and form values are not appended. Exception messages are application-defined, so application
code should not put passwords or customer data in exceptions.
Existing expected static-file failures retain their existing handling.

Action routing returns HTTP 404 for missing, empty or unknown actions, without invoking an action
handler. POST actions must be fields of a URL-encoded or multipart form; an `action` in the query
string or a JSON body does not substitute for a POST form field. A POST without that field is an
unrecognized request, not an unexpected server exception. GET requests without an action continue
to serve application pages and static resources normally.

## Missing-action diagnostics

A request with no parsed action produces one WARNING from `HttpHandler`, beginning with
`Widgets action request rejected: missing action`. It includes:

- A fresh server-generated `requestId`, also returned in the `X-Widgets-Request-Id` response header.
  The browser transport includes this ID in its HTTP failure console message when available.
- HTTP method and decoded path, excluding the query string.
- Content-Type media type (without parameters), declared Content-Length and actual `bodyBytes`.
  The body is not opened or copied for logging.
- Counts of parsed form fields and file fields; booleans for `action` in the form/query and for
  `client`/`browserId` in the form. No field values or arbitrary field names are logged.
- User-Agent, to help distinguish Safari/iPhone from other clients. This header is untrusted
  and does not prove the origin of a request.

Path and selected header values are limited to 240 characters and stripped of control characters
and line/paragraph separators. Paths and User-Agent remain client-supplied metadata; do not put
secrets in URL paths. Unexpected failures use the same metadata, preserving the original throwable
even when metadata collection fails. Their HTTP response is still owned by webserver, so the
correlation header is currently provided only for missing-action rejections.

These warnings distinguish an empty body from a nonempty body with no parsed action, but do not
alone prove a multipart parser bug. Successful requests are not logged. The 404 response has
`Cache-Control: no-store`; it does not turn a rejected protocol operation into a successful one.

## Browser failures

HTTP 5xx responses are logged to the browser console with their status and converted by the browser
transport into the existing fatal client-error signal. The page shows `Client Error` and stops its
synchronization loop instead of replaying a potentially partly executed action. Server response
bodies are not displayed or logged by the transport. Network failures retain the reconnect behavior;
other non-200 statuses are recorded in the console.

When JavaScript fails after a client has been created, `showClientError` makes one best-effort POST
`action=report error` containing its client ID, error name/message and stack. Name and message are
included explicitly because Safari stacks may contain only call frames. The application accepts at
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
