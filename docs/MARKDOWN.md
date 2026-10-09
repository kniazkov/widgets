# Markdown documents

`Markdown` is a `BlockWidget<MarkdownStyle>` for long documents such as store terms,
delivery information and privacy policies. It fills the available width and wraps long words.
The source is an ordinary `Model<String>`; no special model or parser dependency is required.

```java
StringModel source = new StringModel("# Terms\n\nRead **carefully**.");
MarkdownStyle style = Markdown.getDefaultStyle().derive();
style.setFontFace(() -> "Georgia, serif");
style.setFontSize("18px");
Markdown document = new Markdown(style, source);
root.add(document);

source.setData("## Updated terms\n\n- First condition\n- Second condition");
```

Constructors accept no arguments, a string, a string model, or a style followed by either
source representation. `setText`, `setTextModel`, `getText` and `getTextModel` use the normal
reactive binding. Rebinding detaches the old source. An empty string clears the document.
Updating text replaces its rendered contents; it does not preserve selection inside the document.

Font face and base font size use the standard reactive styling API. `MarkdownStyle` derives
from the text theme (16px initially); derive a local style before changing it to avoid changing
the shared defaults. Headings use 2, 1.6, 1.3, 1.15, 1 and 0.9 times the base size for levels
1–6; code uses a monospace face at 0.9 times the inherited size. Line height is 1.6 and internal
spacing scales with the font. Color, outer margin, padding and visibility are also available.

## Supported subset

- ATX headings: `#` through `######`, followed by a space (or an empty heading).
- Paragraphs separated by blank lines. A single newline becomes ordinary whitespace.
- Hard line breaks: two trailing spaces or a backslash before the newline.
- Bold: `**text**` or `__text__`; italic: `*text*` or `_text_`; combined: `***text***`.
- Strikethrough: `~~text~~`. Backslash escapes Markdown punctuation.
- Inline code between matching backtick runs; fenced code with at least three backticks or
  tildes. Fence language labels are accepted but there is no syntax highlighting.
- Consecutive unordered items using `-`, `+` or `*`; ordered items using `1.` or `1)`.
  Ordered lists retain their first number. Indent continuation lines and nested lists to the
  parent item's content column using spaces. Blank lines terminate a list; loose lists with
  multiple paragraphs per item are outside this subset.
- Quotes: prefix each line, including blank lines within the quote, with `>`.
- Horizontal rules: three or more `-`, `*` or `_`, optionally separated by spaces.
- Inline links: `[caption](destination)`. Captions may contain emphasis. Destinations must
  contain no whitespace or parentheses; percent-encode those characters when needed.
  Links support HTTP, HTTPS, mailto, tel, relative paths and fragments, and navigate in the
  same tab as ordinary HTML links. They do not emit widget click events.

This is intentionally a small renderer, not a complete CommonMark implementation. Setext
headings, indented code, raw HTML, images, tables, task lists, reference links, automatic URL
linking, HTML entities, footnotes and full CommonMark delimiter rules are not implemented.
Unsupported syntax is ordinary text; independently supported markup in it can still format.
Nesting is limited to 32 levels; deeper content is displayed as text.

HTML is always literal text, including inside code. The renderer creates DOM nodes without
injecting HTML. Links with unsupported schemes, such as `javascript:` or `data:`, are displayed
as literal Markdown. Images are not loaded.

## Examples

`AllWidgets` includes a short delivery-terms sample. For the complete demonstration:

```bash
./run.sh com.kniazkov.widgets.example.MarkdownExample
```

Open `http://localhost:8000`. The example covers the supported syntax, shares a string model
with a live source editor, and provides font size, font face and reset buttons. Its sample
policy is demonstration content, not a legal document.
