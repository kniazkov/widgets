# Image dimensions

`com.kniazkov.widgets.images.ImageDimensions` reads image metadata and returns
the typed `com.kniazkov.widgets.common.IntrinsicSize` value:

```java
IntrinsicSize uploaded = ImageDimensions.read(bytes, "logo.svg");
IntrinsicSize local = ImageDimensions.read(Path.of("logos", "logo.png"));
IntrinsicSize existingFile = ImageDimensions.read(new File("logos/logo.svg"));

ImageWidget logo = new ImageWidget("/logo/logo.svg");
logo.setIntrinsicSize(uploaded);
```

The filename selects the SVG parser when it ends in `.svg` (case insensitive).
Supply a filename, not a MIME type or URL with query parameters. Other formats
are detected from their bytes using installed ImageIO readers, regardless of the
filename extension. PNG, JPEG, GIF and BMP are covered by tests; other raster
formats depend on the installed ImageIO plugins. This utility does not invoke
the separate HEIC decoder used by `ImageLoader`.

Raster readers inspect the first image's header with `getWidth(0)`/`getHeight(0)`;
they do not decode pixels or allocate a full raster. Dimensions are those encoded
in the file, **before EXIF rotation**. Thus they need not match a rotated image
returned by `ImageLoader.load`. The file overload uses seekable file access rather
than loading the entire file into a `byte[]`. The byte overload uses a memory-backed
ImageIO stream without requiring a temporary disk cache. All readers and streams
are closed, including on failure.

SVG is parsed as XML without rasterization. The reader supports:

- Absolute `width` and `height`: unitless/px, pt, pc, in, cm, mm and q, at 96 CSS px/in.
- Scientific notation and fractional dimensions, rounded up to positive integer pixels.
- A positive `viewBox` width/height when absolute dimensions are unavailable.
- One absolute dimension plus a `viewBox` ratio to infer the other dimension.

Percentages, `auto` and context-dependent CSS values cannot be resolved without a
viewport; the reader uses the `viewBox` fallback when available. It does not evaluate
stylesheets, inherited styles or transforms. The returned size is metadata useful
for layout, not a browser-computed bounding rectangle.

SVG parsing rejects DTDs and disables external entities, schemas and XInclude.
Files are streamed into the XML parser, but the SVG DOM still occupies memory.
Callers remain responsible for upload/file-size limits and allowed filesystem roots;
the reader does not enforce an application-specific size limit or static-source boundary.
Reading dimensions is not image validation or sanitization: raster pixel data may be
incomplete even if its header dimensions can be read, and SVG script content is not removed.

Missing files, unsupported formats, malformed XML and undeterminable, non-finite or
out-of-range dimensions throw `IOException`. The API never silently returns a made-up
size or `IntrinsicSize.NONE`. A caller can catch the exception and choose an explicit
fallback. Null arguments are programming errors and throw `NullPointerException`.
