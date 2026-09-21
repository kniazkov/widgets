# Static resource sources

Applications can publish several independent static subtrees without copying files into
`wwwRoot` or allowing symlinks to escape it. Register immutable `StaticSource` instances
in `Options.Builder`:

```java
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.StaticSource;
import java.nio.file.Path;

Options options = new Options.Builder()
    .setWwwRoot("/var/lib/my-app/www")
    .addStaticSource(StaticSource.directory(
        "/logos/", Path.of("/opt/my-app/logos")))
    .addStaticSource(StaticSource.classpath(
        "/icons/", MyApplication.class, "/public/icons/"))
    .build();
```

`GET /logos/shop.png` reads `/opt/my-app/logos/shop.png`.
`GET /icons/cart.svg` reads the `/public/icons/cart.svg` resource using
`MyApplication.class` as the classloader/module lookup anchor. For a conventional Maven
application, put that resource in `src/main/resources/public/icons/cart.svg`; ensure the
build actually packages it. Exploded `file:` resources and ordinary `jar:` resources are
supported. Other resource protocols are not supported and return 404. Named modules
remain subject to Java's resource visibility rules.

No classpath resource becomes public until its subtree is explicitly registered. A mount
publishes **all files beneath its root**; keep settings, keys, databases, compiled classes
and other private files outside that subtree. The root of the entire classpath cannot be
registered. Directory roots may be absent when options are built and created later.
Relative directory paths are made absolute when `StaticSource.directory` is called.

## Routing rules

1. Registered application pages and framework resources retain their existing priority.
2. Among matching sources, the longest complete URL prefix wins, regardless of registration
   order. `/assets/` matches `/assets/icon.svg`, but not `/assets-other/icon.svg`.
3. A matched source owns the request: a missing file returns 404, without trying a broader
   mount or `wwwRoot`. A security boundary violation returns 403 (malformed HTTP paths may
   instead be rejected by the webserver with 400).
4. Requests without a matching source use `wwwRoot`, with the same real-path confinement.

Prefixes are case-sensitive absolute paths other than `/`. A trailing slash is optional
at registration and is normalized. Duplicate prefixes are configuration errors; nested
prefixes are allowed. `/scripts`, `/fonts`, `/index.html`, `/style.css` and their descendants
are reserved for the framework. `Options.getStaticSources()` returns an immutable snapshot.
Changing the builder after `build()` does not change an existing server's routing.

Bare mount points, missing files and directories do not produce directory listings.
There are no SPA fallback rules. Query strings do not participate in resource lookup.
Content types are inferred using `com.kniazkov.webserver.ContentType.fromExtension`.
Unknown or missing extensions use `application/octet-stream`. The same mapping is used for
uploaded files when the browser supplies no MIME type.
Custom source files are served as bytes without the framework's bootstrap substitution or
JavaScript log removal.

## Security contract

- Paths are decoded once by the webserver. Sources validate decoded relative paths and do
  not decode them again. Empty segments, `.` and `..`, backslashes, controls and the characters
  `%`, `?`, `:`, `#`, `!` are rejected. This deliberately disallows ambiguous filenames even
  when the filesystem permits them. It also applies to `wwwRoot`. Spaces and Unicode names
  are supported using ordinary URL encoding.
- Filesystem access compares the requested file's real path with that source's real root,
  using path components rather than string prefixes. Symlinks within the root work;
  symlinks to files or directories outside it are forbidden. A configured root may itself
  be a symlink: its real destination defines the boundary. Exploded classpath resources
  receive the same confinement checks. JAR lookups require an exact entry inside the
  selected subtree; arbitrary classpath lookup is never a fallback.
- Filesystem roots and their parent directories must be managed by trusted code. Real-path
  checking and file reading are separate operations, not an atomic sandbox against a local
  attacker who can replace directories or symlinks concurrently. Upload handlers should
  create regular files with server-chosen names, not accept arbitrary filesystem paths or
  symlinks. Hard links and files deliberately placed under a public root are public too.
- Mounts are public, with no per-user authorization. Uploaded active content such as HTML
  or SVG is subject to the application's content policy. Files are read into memory, so
  applications should bound their size. This API does not change upload validation,
  authentication or HTTP range support.

Do not solve a failed symlink lookup by disabling the containment check: register a dedicated
directory source for the intended public data instead. Existing applications need no new
sources; the framework resource allowlist and the `wwwRoot` boundary remain in effect.

## Browser caching

Successful static GET responses include a strong `ETag` computed with SHA-256 from
the actual response bytes and `Cache-Control: private, no-cache`. This covers
`wwwRoot`, directory/classpath sources and bundled CSS/JavaScript, including the
generated page runtime. No application configuration is required.

Despite its name, `no-cache` permits browser storage: it requires validation before
reuse. On a subsequent request the browser can send `If-None-Match`. If the bytes
have not changed, widgets responds with `304 Not Modified`, the same cache policy
and ETag, and **no response body**. A changed file gets `200` with its new bytes and
tag, even when its URL, size and modification time remain unchanged. Strong and
weak request tags, tag lists, repeated headers and `*` are supported. Malformed
tag lists are ignored. See [HTTP conditional requests](https://www.rfc-editor.org/rfc/rfc9110.html#section-13.1.2)
and [HTTP cache directives](https://www.rfc-editor.org/rfc/rfc9111.html#section-5.2.2.4).

Files are resolved, checked and read before evaluating the condition. A deleted
file still returns `404`; a forbidden path still returns `403`. ETags do not bypass
source precedence or filesystem confinement. Registered application pages contain
session-specific bootstrap data: they use `no-store` and never return `304`.
Action responses do not enter the static caching path.

This saves repeated image transfer over slow connections but still requires a
network round trip and a server-side file read/hash. It does not enable long-lived
offline reuse, HTTP ranges, or HEAD (the current webserver supports GET and POST).
Replacing a logo at its existing URL is therefore visible on the next validated
load, without waiting for a cache lifetime to expire. Browser settings can disable
caching; a forced reload may transfer the entire file.

Images embedded as `data:` URLs (for example, `ImageSource.fromImage`) are part of
the interface payload rather than independently cacheable HTTP resources. Use
stable file URLs for images that should benefit from this mechanism.

### Versioned URLs without repeated validation

For public images that should display immediately on repeated use, generate a URL
from a registered source:

```java
StaticSource logos = StaticSource.directory("/logo", Paths.get("logos"));
Options options = new Options.Builder().addStaticSource(logos).build();
String url = logos.versionedUrl("medium.svg");
ImageWidget logo = new ImageWidget(url);
logo.setIntrinsicSize(new IntrinsicSize(350, 100));
logo.setMaxWidth(220);
logo.setMaxHeight(70);
```

`versionedUrl` reads and hashes the file and returns a root-relative, percent-encoded
URL such as `/logo/medium.svg?widgets-version=<sha256>`. It also works for classpath
sources. Pass a decoded path relative to that source, without a query or fragment.
Traversal and escaping symlinks are rejected, just as during HTTP serving. Register
the source in options; generating a URL does not register it or override nested mounts.

The server grants `Cache-Control: private, max-age=31536000, immutable` only when the
single `widgets-version` parameter exactly matches the SHA-256 of the response bytes.
A browser can then reuse the image without contacting the server, including when a
new `img` element is created through JavaScript, e.g. inside a popup. Normal URLs
retain the previous ETag/revalidation behavior. Arbitrary `?v=...` parameters do not
enable long-lived caching. A mismatched, empty or duplicate version parameter yields
`404` with `no-store`, never changed bytes under an old immutable URL. Explicit
conditional requests to a matching version still support `304`.

Generate the URL again after changing the file and update the image source. Keep
the generated URL while the file is unchanged; do not add random values or timestamps
on each render. Widgets does not retain historical file versions. A browser that has
cached an old version can continue displaying it until expiration, even after server
deletion; a new network request for that old version fails. These public files must
not require immediate revocation. Cache eviction, disabled caching or explicit reload
can still cause a network request. First use still needs a download.

### Reserving image space

`ImageWidget.setIntrinsicSize(width, height)` (also available on `ActiveImage`) sends
positive original pixel dimensions through the normal widget update protocol and
sets HTML `width`/`height` attributes before the bytes finish loading. Use actual
image metadata, not maximum-size settings. Widgets does not parse SVG files to infer
these values. `clearIntrinsicSize()` removes the attributes and restores browser
size discovery. Update the metadata when switching to an image with different dimensions.

In Java the value is `com.kniazkov.widgets.common.IntrinsicSize`, constructed from
two positive ints. `IntrinsicSize.NONE` is the explicit no-value implementation.
The property is `Property<IntrinsicSize>` with a dedicated `IntrinsicSizeModel`
(defaulting to `NONE`). Use `getIntrinsicSize()` / `setIntrinsicSize(size)` for the
value and `getIntrinsicSizeModel()` / `setIntrinsicSizeModel(model)` for binding.
The two-int setter is a convenience overload; `clearIntrinsicSize()` sets `NONE`.
Only JSON represents the value as `"350 100"` (or `""` for `NONE`); Java models
do not store or parse strings.

CSS dimensions and maximum-size limits remain independent. With this API the image
uses `object-fit: contain`: when limits constrain the reserved box to a different
ratio, the image retains its proportions and can leave empty space inside the box.
The box remains the same size before and after decoding, so following content does
not jump. Explicit CSS sizing (for example `height: auto`) can override the HTML size
hints; callers should check such overrides if they need a stable placeholder.

## Verification

Run `mvn verify` on Java 21. `StaticSourceTest` tests configuration, immutable options,
binary/Unicode files, traversal, symlinks and isolated exploded/JAR classloaders (including
JARs without directory entries). `StaticSourcesHttpTest` exercises real HTTP requests,
encoded traversal, prefix collisions, missing-file isolation, classpath exposure, page and
framework precedence. Existing `HttpHandlerSecurityTest` continues to cover the original
classpath and traversal regressions.
HTTP tests also check conditional requests for directory/classpath/bundled resources,
empty 304 bodies, replacement with unchanged size/mtime, deletion, forbidden symlinks,
and exclusion of dynamic page/action responses.
