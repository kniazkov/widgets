/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

function validLocalAction(action) {
    if (!action || typeof action !== "object") return false;
    const fields = (...names) => Object.keys(action).sort().join() === names.sort().join();
    if (["go to page", "open page in new tab"].includes(action.action)) {
        if (!fields("action", "href") || typeof action.href !== "string") return false;
        try {
            return ["http:", "https:"].includes(new URL(action.href, document.baseURI).protocol);
        } catch {
            return false;
        }
    }
    if (typeof action.target !== "string" || !/^#[1-9][0-9]*$/.test(action.target)) return false;
    if (["focus widget", "scroll to widget"].includes(action.action))
        return fields("action", "target");
    return (
        action.action === "select text" &&
        fields("action", "target", "start", "end") &&
        Number.isSafeInteger(action.start) &&
        Number.isSafeInteger(action.end) &&
        action.start >= 0 &&
        action.end >= action.start &&
        action.end <= 65536
    );
}

// This allowlist is deliberately separate from the server update dispatcher.
function performLocalAction(source, event, action) {
    if (
        !validLocalAction(action) ||
        source.disabled ||
        source._treePaused ||
        source.isConnected === false
    )
        return false;
    // Navigation/popup actions require an explicit user activation, never an input echo or timer.
    if (event !== "click") return false;
    if (action.action === "open page in new tab") {
        openPageInNewTab(action);
        return true;
    }
    if (action.action === "go to page") {
        goToPage(action);
        return true;
    }
    const target = widgets[action.target];
    if (
        !target?.isConnected ||
        target._treePaused ||
        target.disabled ||
        target.closest("[hidden], [inert]") ||
        !target.getClientRects().length
    )
        return false;
    if (action.action === "scroll to widget") {
        target.scrollIntoView({ block: "nearest", inline: "nearest", behavior: "instant" });
    } else if (action.action === "focus widget") {
        target.focus({ preventScroll: true });
    } else {
        if (!target.setSelectionRange || target._composingText) return false;
        try {
            target.focus({ preventScroll: true });
            target.setSelectionRange(action.start, action.end);
        } catch {
            return false;
        }
    }
    return true;
}
