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
retains the same title and metadata. They do not affect widgets or visible page headings.

## Search indexing scope

A description can help search engines construct a snippet, but does not guarantee that they will
use that exact text. Robots directives such as `noindex, nofollow` apply to **every page** in the
application. Leaving robots empty does not explicitly prohibit indexing; external crawler rules
still apply. A robots tag is not access control and does not replace authentication.

This feature does not add per-page metadata, server-rendered widget content, a sitemap,
`robots.txt`, canonical links, or structured product data. Search engines still need to discover
and render the application's content. Do not set one shared canonical URL for all product pages.
Application-specific sitemap and robots files can be served from `www` or `StaticSource` mounts.
There is no dedicated keywords option: Google does not use the keywords meta tag for ranking.

References:

- [Google: supported meta tags](https://developers.google.com/search/docs/crawling-indexing/special-tags)
- [Google: JavaScript SEO](https://developers.google.com/search/docs/crawling-indexing/javascript/javascript-seo-basics)
- [Google: robots directives](https://developers.google.com/search/docs/crawling-indexing/robots-meta-tag)
