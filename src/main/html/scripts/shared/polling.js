/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// One deadline timer per document. Page channels keep independent credentials and ACKs.
function createPollingCoordinator() {
    const entries = new Set();
    let timer = null;
    function arm() {
        clearTimeout(timer);
        timer = null;
        if (entries.size) {
            const next = Math.min(...[...entries].map(entry => entry.due));
            timer = setTimeout(tick, Math.max(0, next - Date.now()));
        }
    }
    function tick() {
        timer = null;
        const now = Date.now();
        for (const entry of [...entries]) {
            if (entry.due <= now) {
                entry.due = now + entry.delay();
                entry.poll();
            }
        }
        arm();
    }
    function visible() {
        if (document.visibilityState === "hidden") return;
        for (const entry of entries) {
            if (entry.active()) entry.due = Date.now();
        }
        arm();
    }
    return {
        register(poll, active, maximum = 30000) {
            if (!entries.size) document.addEventListener("visibilitychange", visible);
            const entry = {
                poll,
                active,
                idle: 2500,
                delay() {
                    return Math.min(
                        maximum,
                        document.visibilityState === "hidden" || !active() ? 30000 : this.idle
                    );
                }
            };
            entry.due = Date.now() + entry.delay();
            entries.add(entry);
            arm();
            return {
                received(busy) {
                    entry.idle = busy ? 2500 : Math.min(10000, entry.idle * 2);
                    entry.due = Date.now() + entry.delay();
                    arm();
                },
                dispose() {
                    entries.delete(entry);
                    if (!entries.size) document.removeEventListener("visibilitychange", visible);
                    arm();
                }
            };
        }
    };
}
