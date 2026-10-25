# Server deadline countdown

```java
final Countdown countdown = new Countdown(Instant.now().plusSeconds(20 * 60));
root.add(new Section(countdown));
// Replace the deadline when the application explicitly changes it:
countdown.setDeadline(newDeadline);
```

`Countdown` is an inline widget. It displays `MM:SS`, or `H:MM:SS` above one hour,
rounding fractional seconds up and clamping expired deadlines to `00:00`. Its deadline
is an absolute `Instant` in server time, truncated to milliseconds. Null and dates outside
the browser date range (±8,640,000,000,000,000 epoch milliseconds) are rejected.
Typography uses an optional `TextWidgetStyle`; color and normal widget properties remain
reactive. Display text belongs to the browser, not to a text model.

Each existing synchronize response supplies `serverNow`, sampled after server processing.
The browser anchors it at receipt to `performance.now()` and calculates the difference
from the deadline on every render. It never decrements a counter or reads the device's
wall clock. Network transit time can make the display late by the response transit delay;
it is an informational display, not a precise distributed clock or an authorization check.

Changing digits makes **zero network requests**. The ordinary widgets synchronization
transport remains active and refreshes the time anchor, even when no deadline changed.
Visibility restoration requests one synchronization (coalesced with in-flight transport),
showing `--:--` until a fresh sample arrives: monotonic clocks can pause during OS sleep.
Internal Back/Forward verifies the cached page with the server before showing it.
A delayed callback recomputes elapsed time instead of accumulating timer drift.
Without a connection, the last sample continues monotonically while active; after sleep,
accurate server time requires a successful synchronization.

One scheduler is shared by countdowns in each page runtime. Detached subtrees and cached
pages do not tick. Removing the last connected countdown removes its visibility listener;
reattachment resumes from the current time. Runtime disposal clears the scheduler and
listener. Reaching zero stops ticking and sends no event: order cancellation, payments and
stock release are exclusively application/server responsibilities.

## Example

```bash
./run.sh com.kniazkov.widgets.example.CountdownExample
```

Open http://localhost:8080/. Try changing the deadline, removing and attaching the widget,
backgrounding the tab, and navigating to the second page and Back. Zero has no business effect.

Tests cover delayed event loops, wall-clock changes, sleep with a paused monotonic clock,
clock/deadline replacement, zero network requests for ticks, nested detach/reattach,
navigation cache and disposal. A browser test exercises the Java-to-browser round trip.
