/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// One scheduler per page runtime, with no per-widget listeners or network ticks.
let countdownPresent = false;
let countdownClock = null;
let countdownTask = null;
let countdownListening = false;
let countdownDisposed = false;

function createCountdown() {
    countdownPresent = true;
    const widget = document.createElement("span");
    widget.className = "widgets-countdown";
    widget.setAttribute("role", "timer");
    widget.setAttribute("aria-live", "off");
    widget.textContent = "--:--";
    widget._countdownDeadline = null;
    return widget;
}

function setCountdownDeadline(data) {
    const widget = widgets[data.widget];
    if (
        !widget?.classList.contains("widgets-countdown") ||
        !Number.isSafeInteger(data.deadline) ||
        Math.abs(data.deadline) > 8640000000000000
    ) {
        return false;
    }
    widget._countdownDeadline = data.deadline;
    refreshCountdowns();
    return true;
}

function sampleCountdownClock(serverNow) {
    if (!Number.isSafeInteger(serverNow)) return;
    countdownClock = { serverNow, monotonic: performance.now() };
    refreshCountdowns();
}

function countdownVisibilityChanged() {
    if (!document.hidden) {
        // performance.now may pause during OS sleep. Wait for a fresh server sample.
        countdownClock = null;
        sendSynchronizeRequest();
    }
    refreshCountdowns();
}

function refreshCountdowns() {
    clearTimeout(countdownTask);
    countdownTask = null;
    if (countdownDisposed || !countdownPresent) return;
    const visible = Object.values(widgets).filter(
        widget => widget._countdownDeadline != null && widget.isConnected
    );
    if (visible.length > 0 && !countdownListening) {
        document.addEventListener("visibilitychange", countdownVisibilityChanged);
        countdownListening = true;
    } else if (visible.length === 0 && countdownListening) {
        document.removeEventListener("visibilitychange", countdownVisibilityChanged);
        countdownListening = false;
    }
    if (document.hidden) return;
    const now =
        countdownClock &&
        countdownClock.serverNow + Math.max(0, performance.now() - countdownClock.monotonic);
    let nextDelay = Infinity;
    for (const widget of visible) {
        let text = "--:--";
        if (now != null) {
            const seconds = Math.max(0, Math.ceil((widget._countdownDeadline - now) / 1000));
            const hours = Math.floor(seconds / 3600);
            const minutes = Math.floor(seconds / 60) % 60;
            const pad = value => String(value).padStart(2, "0");
            text = (hours ? hours + ":" : "") + pad(minutes) + ":" + pad(seconds % 60);
            if (seconds > 0) {
                nextDelay = Math.min(
                    nextDelay,
                    Math.max(1, widget._countdownDeadline - now - (seconds - 1) * 1000)
                );
            }
        }
        if (widget.textContent !== text) widget.textContent = text;
    }
    if (Number.isFinite(nextDelay)) countdownTask = setTimeout(refreshCountdowns, nextDelay);
}

function disposeCountdowns() {
    countdownDisposed = true;
    clearTimeout(countdownTask);
    countdownTask = null;
    document.removeEventListener("visibilitychange", countdownVisibilityChanged);
    countdownListening = false;
}
