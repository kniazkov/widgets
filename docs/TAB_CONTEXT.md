# Revocable tab context

A `Client` identifies one server-side page instance; `browserId` identifies the browser across
its tabs. Neither is the correct key for a temporary operator mode. `PageContext.tabContext`
provides a third identity, issued by the server and shared by internal pages in one live document.

```java
TabContext tab = context.tabContext;
UUID key = tab.getId();

// Application-owned mode lookup; this identity is not authorization.
if (tab.isActive() && currentEmployeeMayOperate()) {
    useModeFor(key);
}

// End the mode before returning control to the browser.
removeApplicationMode(key);
tab.revoke();
```

Applications own their mode data and bind it to the authenticated employee. Check permissions,
current login and `tab.isActive()` on every privileged command, including asynchronous work.
Clear/revoke the relevant contexts when logging out or changing identity. Never use the selected
customer's browser ID as a permission, put it in a public link, or replace the real `browserId`.
Application mode registries should discard entries whose context is inactive. The framework
stores identity/liveness only; it does not enable an operator mode or impersonate a customer.

## Deliberate lifecycle policy

| Event | Behavior |
| --- | --- |
| Internal registered-page navigation | New Client, same TabContext |
| Back/Forward within the live document | Same context; retained page synchronizes before its DOM is shown |
| Temporary connection loss | Same context if still active on the same server |
| Expired page Client, active context | Rebuild the page under the same context |
| Full reload, hard navigation, restored browser session | New context; operator mode must be entered again |
| New window/tab, middle click, copied URL, Duplicate Tab | New context, even when sessionStorage/history were copied |
| Back/Forward cache restoring an unloaded document | Reload to create a new context; disposed runtimes are not resumed |
| Server ID change, unknown/revoked/expired context | Drop all retained pages in this document and start a fresh context |
| Another tab navigates or revokes its context | This tab's identity is unchanged |

The token lives only in a closure owned by `navigation.js`. It is **not** written to localStorage,
sessionStorage, history.state, window.name or a public URL. No unreliable duplicate-tab detection
or copied storage token is treated as proof of tab continuity. This conservative policy intentionally
resets the mode on reload. Native browser Duplicate Tab creates a new document; the E2E suite also
opens a same-origin child with copied sessionStorage and a copied token in its URL and verifies that
neither is used. Existing browser-wide page-cache invalidation remains a separate mechanism;
context revocation itself does not broadcast that invalidation to other tabs.

## Wire protocol and compatibility

`new instance` accepts an optional reserved `tabId`. Absence creates a fresh context; a successful
response adds its UUID as `tabId` alongside the existing `id` and `serverId`. New browser code sends
creation requests via POST, stripping any `tabId` from page/query parameters before adding the
in-memory token. It reuses the returned identity on internal navigation and retries. An invalid,
unknown, browser-mismatched or revoked token returns `result:false`, `tabContextExpired:true` and
`serverId`, without constructing a page. It never silently resumes an invalid identity.

Existing clients that omit the new field keep working, with an independent context per new page.
Existing applications can ignore the additive `PageContext` field and response field. A manually
constructed PageContext can initially have a null tabContext; Application populates it before page
creation. New browser code also tolerates old servers which do not return tabId, though such servers
cannot provide the tab-isolation guarantee. `tabId` is reserved and is excluded from page parameters.

The server associates the context with its original browserId. This is a consistency check, **not an
authentication boundary**: protocol IDs/browser IDs are not substitutes for authenticated sessions,
CSRF protection or application permissions. Never grant access merely because a request contains
a valid tab token. Deliberately forged protocol requests from an authorized origin remain the
application's responsibility.

## Revocation, events and resource lifetime

`revoke()` is terminal. A new context always receives a new UUID. Old Client objects hold the
original object and cannot recreate it by polling, navigating Back or submitting queued events.
Synchronization and uploads check liveness, and each individual queued widget event checks again.
If one event revokes the context, later events in the same request are skipped. Browser recovery
on `tabContextExpired` clears every cached runtime in that document before displaying fresh content.

Revocation does not interrupt an operation already executing. Applications must serialize logout,
permission changes and financial/inventory operations themselves and revalidate before committing.
There is no callback under the context monitor, avoiding a new context/root-widget lock cycle.

Accepted page creation/synchronization/upload traffic renews a context for the configured client
lifetime. Reads of `isActive()` do not renew it. Once its inactivity limit expires it cannot be revived;
the watchdog removes expired/revoked registry entries. Abandoned documents therefore do not retain
registry entries indefinitely, even if unload requests are lost. Full reload does not depend on an
unload notification to isolate its new context. Applications must not persist a mode keyed only by
a token and restore it after a context is gone.

## Checks

`TabContextTest` verifies independent tabs, navigation object identity, unchanged browserId,
reserved parameters, malformed/foreign/revoked IDs, server replacement, expiration, queued events
and uploads after revocation. Navigation JS tests cover cache disposal, server changes, Client-only
expiration and query-token injection. `tab-context.spec.js` exercises actual internal navigation,
Back/Forward, revocation, reload, independent tabs, opener-copied sessionStorage and reconnection.
The `/tab-context` and `/tab-context-next` routes are test-server fixtures, not production routes.
