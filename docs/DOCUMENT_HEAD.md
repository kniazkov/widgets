# Document title, favicon and search metadata

Configure the initial HTML document through immutable `Options`:

```java
Options options = new Options.Builder()
    .setTitle("Svetelka Jewelry")
    .setFaviconUrl("/icons/favicon.png")
    .setDescription("Handmade necklaces, earrings and bracelets.")
    .setLanguage("en")
    .setRobots("index, follow")
    .build();
Server.start(application, options);
```

| Builder method | HTML output | Default |
| --- | --- | --- |
| `setTitle` | `<title>` in the browser tab | Empty; element omitted |
| `setFaviconUrl` | `<link rel="icon" href="…">` | Empty; element omitted |
| `setDescription` | `<meta name="description" content="…">` | Empty; element omitted |
| `setRobots` | `<meta name="robots" content="…">` | Empty; no explicit crawler directives |
| `setLanguage` | `<html lang="…">` | `en` |

Each setting has a corresponding getter on `Options`. Builder changes do not mutate previously
built options. Values must not be null. Metadata is plain text, escaped when rendered; callers
must not pre-escape it or pass HTML. Empty strings omit optional elements. Language accepts tags
such as `en` and `ru-RU`, not an empty string.

The favicon must be a public root-relative path (such as `/icons/favicon.png`) or an absolute
HTTP(S) URL. Relative paths without a leading slash and non-HTTP schemes are rejected. Put a local
file under `www`, or use an explicit `StaticSource` mount. Setting the URL does not create or
upload the image. PNG, ICO and SVG can be served through the existing static resource handler.
The `AllWidgets` example uses `www/widgets-icon.svg` and a configured browser title.

The server emits this metadata in the initial response for the root and every registered page;
JavaScript is not required to read it. These are site-wide settings, so client-side navigation
retains the same title and metadata unless the page overrides them. They do not affect widgets or visible page headings. RootWidget overrides are described below.

## Search indexing scope

A description can help search engines construct a snippet, but does not guarantee that they will
use that exact text. Robots directives such as `noindex, nofollow` apply to **every page** in the
application. Leaving robots empty does not explicitly prohibit indexing; external crawler rules
still apply. A robots tag is not access control and does not replace authentication.

Options alone does not add per-page metadata, server-rendered widget content, a sitemap,
`robots.txt`, canonical links, or structured product data. Search engines still need to discover
and render the application's content. Do not set one shared canonical URL for all product pages.
Application-specific sitemap and robots files can be served from `www` or `StaticSource` mounts.
There is no dedicated keywords option: Google does not use the keywords meta tag for ranking.

References:

- [Google: supported meta tags](https://developers.google.com/search/docs/crawling-indexing/special-tags)
- [Google: JavaScript SEO](https://developers.google.com/search/docs/crawling-indexing/javascript/javascript-seo-basics)
- [Google: robots directives](https://developers.google.com/search/docs/crawling-indexing/robots-meta-tag)

## Per-page reactive overrides

A page can override the site title, description and robots directives on its `RootWidget`:

```java
Page productPage = (root, context) -> {
    root.setTitle("Silver bracelet — Svetelka Jewelry");
    root.setDescription("A handmade silver bracelet with natural stones.");
    // Build the product widgets here.
};
```

`setTitleModel(Model<String>)`, `setDescriptionModel(Model<String>)` and
`setRobotsModel(Model<String>)` bind metadata to reactive string models. Matching getters return
models or their current effective values. Every root starts with independent models initialized
from its `Options`; leaving a field untouched keeps the site default. Values are plain text.
An empty override removes the corresponding head element; it does not mean fallback.
`resetTitle()`, `resetDescription()`, `resetRobots()` and `resetMetadata()` restore Options values
and detach previous model bindings. Null setters and null model references are rejected.
Favicon and document language remain site-wide Options settings.

Only the active page updates the document head. Navigating to a new page restores site defaults
while loading; Back/Forward restores the retained page's effective metadata. Delayed replies from
hidden pages update their own state without overwriting the visible page's metadata. This also
supports model changes after the page has been created.

These overrides arrive through the widget protocol after the initial HTML response. The initial
response still contains Options metadata: this is not per-page server-side HTML rendering.
A crawler must execute the application to see RootWidget overrides. In particular, do not set
site-wide `noindex` expecting a page override to undo it: crawlers may skip JavaScript rendering
once they see `noindex` in the initial response. For reliable crawler-specific per-page rules,
initial-response metadata will require a separate request-aware API.
