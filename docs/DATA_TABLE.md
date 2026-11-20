# DataTable and DataProvider

`DataTable` presents a bounded page from an asynchronous `DataProvider` without
changing the existing structural `Table`, `Row` or `Cell` API.

```java
var columns = List.of(
    new DataTable.DataColumn("reference", "Order", true),
    new DataTable.DataColumn("status", "State", false));
var table = new DataTable("Completed orders", columns, 20, query ->
    orders.findAsync(query.filter(), query.sortKey(), query.descending(),
                     query.offset(), query.size())
          .thenApply(result -> new DataProvider.Page(result.rows(), result.matchCount())));
table.onSelect(keys -> selectionChanged(keys));
```

Use immutable `DataProvider.DataRow(key, cells)` with display strings keyed
by schema column. Row keys must identify the same application record across
sorts, filters and pages. Matching captions neither combine records nor change
sort keys. Only display data and keys are sent to the browser; do not include
secrets in either. The provider applies filtering, sorting and paging before
returning the bounded page plus **filtered** total. Index is zero-based and
offset uses a long. Empty filter means the application's unfiltered view.

The first page is requested when the table first enters the viewport. Call
`reload()` on the page thread after mounting to fetch it earlier.

The provider should return a `CompletableFuture` without blocking the page
thread. New requests and retirement call `cancel(true)` as a hint; completions
from a stale request never replace the current page. Loading keeps the previous
page in place with `aria-busy`; an error retains it, with a visible generic
message and Retry. A successful zero-result query replaces rows with the
empty status. Clearing the filter does not delete data.

Selection is independent of current page. `getSelectedKeys()` and `onSelect`
use immutable snapshots; `clearSelection()` clears everything.
`updateRow` replaces only an already visible record without moving it.
`removeRow` forgets an authoritative deleted key even if currently hidden;
persist the change in your backing store first, then `reload` to refill
the page. Keeping a visible key also retains its DOM row/checkbox and focus
where possible; if the focused row disappears, the filter takes its place.

The widget uses native table/caption/headers with `scope=col`, `aria-sort`
on the active header, native sort buttons and named row checkboxes. Tab
navigates filter, scroll region, and controls; Space selects a focused row,
and Enter/Space activates a sort button. A narrow screen scrolls **only the
table region**, not an enlarged page. Localized chrome is configured with
`DataTable.Messages`; the application supplies the actual titles/cells.

Limits: 1–32 rows per page, 1–8 columns, 160 UTF-16 units per cell,
96 per key and 120 for the filter. Selection retains up to 512 keys. There
are no arbitrary child widgets in cells, no automatic database writes, and
no virtualization. See the [browser gallery](en/widgets/tables.md#shot-data-table-wide).

The row viewport has a fixed height of `20rem` with internal scrolling, so loading and page changes do not shift controls below the table. Override `--widgets-data-table-height` to customize it. The status line reserves space even when empty.
