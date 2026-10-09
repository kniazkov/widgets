/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// Registers an event handler through either the standard or legacy DOM API.
function addEvent(object, type, callback) {
    if (typeof object == "string") {
        object = document.getElementById(object);
    }
    if (object == null || typeof object == "undefined") {
        return;
    }
    if (object.addEventListener) {
        object.addEventListener(type, callback, false);
    } else if (object.attachEvent) {
        object.attachEvent("on" + type, callback);
    } else {
        object["on" + type] = callback;
    }
}

function isMobileDevice() {
    const userAgent = navigator.userAgent || navigator.vendor || window.opera;

    const byUserAgent = /iPhone|iPad|iPod|Android|Windows Phone|IEMobile|Opera Mini/i.test(
        userAgent
    );

    const byTouchMac = navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1;

    const byScreen =
        window.matchMedia("(max-width: 768px)").matches &&
        window.matchMedia("(pointer: coarse)").matches;

    return byUserAgent || byTouchMac || byScreen;
}

function escapeHtml(unsafe) {
    return unsafe
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function log(message) {
    console["log"](message);
}

function readBit(number, bitIndex) {
    return (number & (1 << bitIndex)) !== 0;
}

function setBit(number, bitIndex) {
    return number | (1 << bitIndex);
}

function clearBit(number, bitIndex) {
    return number & ~(1 << bitIndex);
}

function truncate(text, maxLength) {
    if (text.length <= maxLength) {
        return text;
    }
    return text.slice(0, maxLength - 3) + "...";
}
