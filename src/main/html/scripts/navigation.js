/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// History entries own independent runtimes, including detached DOM and protocol checkpoints.
function initNavigation(sessionId, address, data, addresses = [address]) {
    const pages = new Map();
    const routes = new Set(addresses);
    const stateKey = "widgetsPage";
    const invalidationKey = "widgetsPageCacheInvalidation";
    let active = null;
    let serial = 0;
    let accessOrder = 0;
    let scrollPersistenceWarning = false;
    let epoch = localStorage.getItem(invalidationKey);
    window.history.scrollRestoration = "manual";

    function newState() {
        return { id: sessionId + ":" + ++serial, x: 0, y: 0 };
    }

    function saveScroll() {
        if (!active || active.restoring) {
            return;
        }
        active.state.x = window.scrollX;
        active.state.y = window.scrollY;
    }

    // Scrolling only updates memory: frequent History API writes can be rejected by Safari.
    function persistScroll() {
        saveScroll();
        if (!active || window.history.state?.[stateKey]?.id !== active.state.id) {
            return;
        }
        try {
            window.history.replaceState(
                { ...window.history.state, [stateKey]: { ...active.state } },
                ""
            );
        } catch (error) {
            if (error.name !== "SecurityError" && error.name !== "QuotaExceededError") {
                throw error;
            }
            // Cached entries still retain exact coordinates when optional persistence is refused.
            if (!scrollPersistenceWarning) {
                scrollPersistenceWarning = true;
                console.warn("Widgets scroll history persistence unavailable", error.name);
            }
        }
    }

    function dispose(entry) {
        pages.delete(entry.state.id);
        entry.runtime.disposeClient();
        entry.root.remove();
        entry.error?.remove();
    }

    function prune() {
        const hidden = [...pages.values()].filter(entry => entry !== active);
        hidden.sort((a, b) => b.hiddenOrder - a.hiddenOrder);
        hidden.forEach((entry, index) => {
            if (index >= PAGE_CACHE_SIZE || Date.now() - entry.hiddenAt >= PAGE_CACHE_TTL) {
                dispose(entry);
            }
        });
    }

    function clearCache(broadcast = true) {
        for (const entry of pages.values()) {
            if (entry !== active) {
                dispose(entry);
            }
        }
        if (broadcast) {
            epoch = sessionId + ":" + ++serial;
            localStorage.setItem(invalidationKey, epoch);
        }
    }

    function restoreScroll(entry) {
        if (active !== entry || !entry.root.isConnected) {
            return;
        }
        window.scrollTo(entry.state.x, entry.state.y);
        if (
            (window.scrollX === entry.state.x && window.scrollY === entry.state.y) ||
            Date.now() >= entry.scrollDeadline
        ) {
            entry.restoring = false;
        }
    }

    function show(entry) {
        if (active === entry) {
            document.body.style.backgroundColor = entry.root.style.backgroundColor;
        }
        if (active !== entry || !entry.waiting) {
            return;
        }
        entry.waiting = false;
        entry.error?.remove();
        document.body.appendChild(entry.root);
        entry.restoring = true;
        entry.scrollDeadline = Date.now() + 2000;
        restoreScroll(entry);
        window.requestAnimationFrame(() => {
            restoreScroll(entry);
        });
    }

    function activate(state, url, initialData) {
        saveScroll();
        if (active) {
            active.hiddenAt = Date.now();
            active.hiddenOrder = ++accessOrder;
            active.root.remove();
            active.error?.remove();
        }
        prune();
        let entry = pages.get(state.id);
        if (!entry) {
            entry = {
                state,
                url,
                root: document.createElement("div"),
                hiddenAt: 0,
                waiting: true,
                restoring: true
            };
            entry.root.className = "widgets-page";
            entry.root.addEventListener(
                "load",
                () => {
                    if (entry.restoring && Date.now() < entry.scrollDeadline) {
                        restoreScroll(entry);
                    }
                },
                true
            );
            entry.runtime = createPageRuntime({
                root: entry.root,
                failure: overlay => {
                    if (entry.waiting && active === entry) {
                        entry.error?.remove();
                        entry.error = overlay.cloneNode(true);
                        document.body.appendChild(entry.error);
                    }
                },
                recovered: () => entry.error?.remove(),
                ready: () => show(entry),
                expire: () => {
                    dispose(entry);
                    if (active === entry) {
                        active = null;
                        activate(entry.state, entry.url);
                    }
                },
                clearCache: () => {
                    clearCache();
                    if (active !== entry && active) {
                        const current = active;
                        dispose(current);
                        active = null;
                        activate(current.state, current.url);
                    }
                },
                navigate: href => {
                    if (active === entry) {
                        navigate(href);
                    }
                }
            });
            pages.set(state.id, entry);
            active = entry;
            const parameters = initialData || Object.fromEntries(new URL(url).searchParams);
            entry.runtime.initClient(sessionId, new URL(url).pathname, parameters);
        } else {
            active = entry;
            entry.waiting = true;
            entry.restoring = true;
            // Verify the retained server instance before exposing its DOM again.
            entry.runtime.mainCycle();
        }
        prune();
    }

    function navigate(href) {
        const url = new URL(href, window.location.href);
        if (url.origin !== window.location.origin || !routes.has(url.pathname) || url.hash) {
            window.location.href = url.href;
            return;
        }
        persistScroll();
        const state = newState();
        window.history.pushState({ [stateKey]: { ...state } }, "", url.href);
        activate(state, url.href);
    }

    window.addEventListener("popstate", event => {
        const state = event.state?.[stateKey];
        if (!state) {
            window.location.reload();
            return;
        }
        activate(state, window.location.href);
    });
    window.addEventListener("scroll", saveScroll, { passive: true });
    for (const type of ["wheel", "touchstart", "pointerdown", "keydown"]) {
        window.addEventListener(
            type,
            () => {
                if (active && !active.waiting) {
                    active.restoring = false;
                }
            },
            { passive: true }
        );
    }
    document.addEventListener("click", event => {
        const link = event.target.closest?.("a[href]");
        if (
            !link ||
            event.defaultPrevented ||
            event.button !== 0 ||
            event.ctrlKey ||
            event.metaKey ||
            event.shiftKey ||
            event.altKey ||
            link.hasAttribute("download") ||
            (link.target && link.target !== "_self")
        ) {
            return;
        }
        const url = new URL(link.href);
        if (url.origin === window.location.origin && routes.has(url.pathname) && !url.hash) {
            event.preventDefault();
            navigate(url.href);
        }
    });
    window.addEventListener("storage", event => {
        if (event.key === invalidationKey && event.newValue !== epoch) {
            epoch = event.newValue;
            clearCache(false);
            if (active) {
                const entry = active;
                dispose(entry);
                active = null;
                activate(entry.state, entry.url);
            }
        }
    });
    window.addEventListener("pagehide", () => {
        persistScroll();
        for (const entry of pages.values()) {
            dispose(entry);
        }
    });
    window.addEventListener("error", event => active?.runtime.showClientError(event.error));
    window.addEventListener("unhandledrejection", event =>
        active?.runtime.showClientError(event.reason)
    );
    setInterval(prune, 1000);
    const state = window.history.state?.[stateKey] || newState();
    window.history.replaceState({ ...window.history.state, [stateKey]: { ...state } }, "");
    activate(state, window.location.href, data);
}
