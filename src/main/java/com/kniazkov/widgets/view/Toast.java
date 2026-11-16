/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.view.Feedback.Severity;
import com.kniazkov.widgets.protocol.SetFeedback;
import java.util.ArrayDeque;
import java.util.Objects;

/**
 * Bounded inline notification outlet. Important errors persist until explicitly cleared.
 * Browser dismissal releases the server entry; expired metadata is lazily removed.
 */
public final class Toast extends Panel {
    /**
     * Bounded retained notifications.
     */
    private final ArrayDeque<Entry> entries = new ArrayDeque<>();
    /**
     * Maximum retained entries.
     */
    private final int capacity;
    /**
     * Localized dismissal caption.
     */
    private final String dismiss;
    /**
     * Monotonic notification identity.
     */
    private long sequence;

    /**
     * Creates an outlet without stealing focus or covering other controls.
     * @param capacity queue limit, 1 through 20
     * @param dismiss localized close button caption
     */
    @SuppressWarnings("this-escape")
    public Toast(final int capacity, final String dismiss) {
        if (capacity < 1 || capacity > 20 || Objects.requireNonNull(dismiss).isBlank()) {
            throw new IllegalArgumentException("Invalid toast configuration");
        }
        this.subscribeToEvent(com.kniazkov.widgets.controller.Event.TOAST_DISMISS);
        this.capacity = capacity;
        this.dismiss = dismiss;
        this.onRetire(this.entries::clear);
        this.sendEntries();
    }

    @Override
    public String getType() {
        return "feedback";
    }

    /**
     * Offers a notification. A full queue rejects new messages rather than losing old errors.
     * @param text localized nonempty message
     * @param severity semantic severity
     * @param lifetimeMillis zero for persistent; otherwise 1000..86400000 milliseconds
     * @return whether the notification was accepted
     */
    public boolean offer(final String text, final Severity severity, final long lifetimeMillis) {
        this.requireUsable();
        Objects.requireNonNull(severity);
        if (Objects.requireNonNull(text).isBlank() || text.length() > 8192
                || lifetimeMillis != 0 && (lifetimeMillis < 1000 || lifetimeMillis > 86400000)) {
            throw new IllegalArgumentException("Invalid toast content or lifetime");
        }
        this.expire();
        if (this.entries.size() == this.capacity) {
            return false;
        }
        final long expiry = severity == Severity.ERROR || lifetimeMillis == 0
            ? 0 : System.currentTimeMillis() + lifetimeMillis;
        this.entries.addLast(new Entry(++this.sequence, text, severity, expiry));
        this.sendEntries();
        return true;
    }

    /**
     * Checks a browser-supplied stable notification ID.
     * @param id opaque decimal identity
     * @return whether the entry belongs to this outlet
     */
    public boolean contains(final String id) {
        return this.entries.stream().anyMatch(entry -> Long.toString(entry.id).equals(id));
    }

    /**
     * Dismisses an owned notification; repeating this operation is harmless.
     * @param id stable identity
     */
    public void dismiss(final String id) {
        this.requireUsable();
        if (this.entries.removeIf(entry -> Long.toString(entry.id).equals(id))) {
            this.sendEntries();
        }
    }

    /**
     * @return retained entries after removing expired notifications
     */
    public int getCount() {
        this.expire();
        return this.entries.size();
    }

    /**
     * Explicitly clears all messages, including persistent errors.
     */
    public void clear() {
        this.requireUsable();
        this.entries.clear();
        this.sendEntries();
    }

    /**
     * Removes expired server metadata without creating timer threads.
     */
    private void expire() {
        final long now = System.currentTimeMillis();
        this.entries.removeIf(entry -> entry.expires != 0 && entry.expires <= now);
    }

    /**
     * Publishes a bounded snapshot; unchanged entries retain browser deadlines.
     */
    private void sendEntries() {
        final JsonObject data = new JsonObject();
        data.addString("kind", "toast");
        data.addString("dismiss", this.dismiss);
        final var items = data.createArray("items");
        final long now = System.currentTimeMillis();
        for (final Entry entry : this.entries) {
            final var item = new JsonObject();
            item.addString("id", Long.toString(entry.id));
            item.addString("text", entry.text);
            item.addString("severity", entry.severity.name().toLowerCase(java.util.Locale.ROOT));
            item.addNumber("ttl", entry.expires == 0 ? 0 : Math.max(1, entry.expires - now));
            items.addElement(item);
        }
        this.pushUpdate(new SetFeedback(this.getId(), data));
    }

    /**
     * Immutable retained metadata.
     * @param id stable notification identity
     * @param text plain text
     * @param severity semantic severity
     * @param expires server deadline, or zero for persistent
     */
    private record Entry(long id, String text, Severity severity, long expires) { }
}
