# Sorting and zooming inline widgets

Both widgets accept arbitrary `InlineWidget<?>` content, including `ImageWidget`, `TextWidget`,
`InlineBlock`, and composite cards. Neither widget decodes images or assumes image-specific models.

## SortableSection

`SortableSection` is a block-level, wrapping container with the same `SectionStyle` and alignment,
margin, padding, and visibility properties as `Section`.

```java
final ImageWidget front = new ImageWidget("/house.png");
front.setWidth(160);
final ImageWidget detail = new ImageWidget("/house.png");
detail.setWidth(160);
final SortableSection photos = new SortableSection(front, detail);
root.add(photos);
photos.onReorder(order -> {
    // Map these stable widget identities to your product photo records, then persist their order.
    // The server-side children have already been reordered here.
});
photos.setAnimationDuration(250); // Milliseconds; 0 disables settling animations.
photos.move(0, 1); // Move to the final index; does not call onReorder.
```

- Constructors accept inline children, optionally preceded by a `SectionStyle`.
- `add`, `remove`, `removeAll`, `getChild`, and `getChildCount` follow the container API.
  Adding an already-owned child is a no-op; adding a child from another container transfers it.
- `getChildren()` returns an immutable snapshot in display order.
- `move(from, to)` accepts existing zero-based indices. Invalid indices throw before changing state.
- `onReorder` runs once for an accepted, changed browser order. It receives an immutable snapshot.
  Sorting synchronizes even without a callback. Programmatic operations do not call the callback.
- Drag with the primary mouse button or one finger; a six-pixel threshold distinguishes a drag
  from a tap. The actual child follows the pointer above its siblings while its layout slot stays
  intact. Siblings stay still until release, then move smoothly into their new positions along
  with the dropped card. Cards can wrap and have different dimensions.
- `getAnimationDuration()` / `setAnimationDuration(int milliseconds)` controls drop, cancellation,
  keyboard and programmatic reorder animations. The default is **250 ms**; **0** disables them.
  Negative durations are rejected. Pointer tracking is immediate at every setting. The browser's
  `prefers-reduced-motion` setting disables settling animations too.
- Server acknowledgements leave a running drop animation uninterrupted. A different authoritative
  order animates from the currently visible positions. Detachment clears active animations.
- Nested buttons, links, inputs, editable content and gesture widgets retain their own interactions;
  start dragging on the surrounding card. A button used as the entire child is intentionally not a
  drag handle; wrap it in an `InlineBlock` with a visible non-interactive area.
- Focus a direct child and use **Alt+Left / Alt+Right** to move it one position.
- **Escape**, pointer cancellation, losing pointer capture, a second finger, or a server order
  update cancels the active drag. Cancellation restores the original order.
- Touch movement starting on a child belongs to sorting (`touch-action: none`). Scroll the page
  outside the sortable children. Native image dragging and selection are disabled in the drag area.

The browser sends only the moved child ID, its next sibling ID, and the observed container revision
when a drag ends. Another move can start after the server acknowledges that request. The server
validates membership and revision before applying the move. Stale,
unknown-child, or no-op requests do not invoke the callback; the authoritative order is resent.
Reordering preserves DOM nodes, Java widget identities, models, and event handlers.

## ZoomDecorator

`ZoomDecorator` is an inline viewport implementing `Decorator<InlineWidget<?>>`. It reuses
`InlineBlockStyle` and exposes width, height, maximum dimensions, margin, padding, background,
border and box sizing.

```java
final ImageWidget image = new ImageWidget("/house.png");
image.setWidth(320);
final ZoomDecorator viewer = new ZoomDecorator(image);
viewer.setWidth(320);
viewer.setHeight(240);
viewer.setMaxWidth("100%");
viewer.setMaxScale(8);
root.add(new Section(viewer));
final Button reset = new Button("Reset");
reset.onClick(event -> viewer.resetZoom());
root.add(new Section(reset));
```

- Constructors create an empty text placeholder, wrap an inline widget, or accept a style and child.
- `put` / `setWidget` replaces content and resets the viewport. Removing the child installs an empty
  text placeholder, keeping the decorator's exactly-one-child contract.
- `getMaxScale()` defaults to **8**. `setMaxScale(value)` accepts finite values at least **1** and
  resets the viewport. A limit of 1 disables magnification.
- `resetZoom()` returns to scale **1** and the original position.
- Mouse wheel zoom keeps the content point under the cursor stationary where pan bounds permit.
  Pixel, line and page wheel deltas are supported.
- Two-finger pinch zoom follows the midpoint; lifting one finger continues as a one-finger pan.
- Drag with one finger or the primary mouse button to pan enlarged content. Panning is clamped to
  the scaled content bounds, so content cannot be lost outside the viewport.
- The viewport clips overflow. Give the child and viewport suitable dimensions: scale 1 means the
  child's CSS size, not automatic image fitting. Images can use `/house.png` regardless of server port.
- Ordinary child clicks remain available; synthesized clicks following a pan/pinch are suppressed.
- Touch gestures and wheel scrolling inside the viewport belong to zooming. Scroll the page outside
  it. Zoom and pan are local browser presentation state, not server models or per-frame events.
- Size changes and image load re-clamp the current position. Replacing content clears active pointers.

A private DOM wrapper receives the transform, preserving the child's own styling and transform.
The same mechanism works for a text label or an `InlineBlock` containing multiple sections.

## Runnable demonstrations

Build and run either example with the root script (one at a time):

```bash
./run.sh com.kniazkov.widgets.example.SortableSectionExample
./run.sh com.kniazkov.widgets.example.ZoomDecoratorExample
```

The script builds the project and resolves `www/house.png` from the repository directory.

- `com.kniazkov.widgets.example.SortableSectionExample`: a photo-order editor with six cards,
  removal buttons, adding photos, server-side reversal, animation speed controls (0/150/250/600 ms),
  saved-order feedback, and mixed inline content.
- `com.kniazkov.widgets.example.ZoomDecoratorExample`: a house image viewer, reset and limit controls,
  content replacement, and a second zoomable composite card with a working button.

Both examples listen at **http://localhost:8080**. The existing package is named `example` (singular).
`AllWidgets` also contains a compact example of each widget and uses its existing server configuration.

## Tests

Java tests cover parent ownership, move validation, callback ordering, stale revisions, stable child
identity, decorator replacement, scale validation and protocol updates. JavaScript tests cover mouse
and touch sorting, wrapped rows, keyboard moves, cancellation, click suppression, wheel anchoring,
scale/pan bounds, pinch-to-pan transitions and content replacement. Playwright tests exercise actual
browser pointer capture, Java round trips, wheel/pinch gestures and nested button clicks.
