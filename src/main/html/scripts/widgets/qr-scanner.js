// Keep presentation state separate from camera ownership and decoding.
class QrPresentationGate {
    constructor() {
        this.code = null;
        this.absentSince = null;
        this.busy = false;
    }
    frame(code, now) {
        if (this.busy) return false;
        if (!code) {
            if (this.absentSince === null) this.absentSince = now;
            if (now - this.absentSince >= 1500) this.code = null;
            return false;
        }
        this.absentSince = null;
        if (this.code === code) return false;
        this.code = code;
        this.busy = true;
        return true;
    }
    resume() {
        this.busy = false;
        this.absentSince = null;
    }
}

// Return data only; an optional prefix enables the application-specific UUID filter.
function qrProductCode(value, prefix) {
    if (typeof value !== "string" || !value.length) return null;
    if (prefix === "") return value;
    if (!value.startsWith(prefix)) return null;
    const id = value.slice(prefix.length);
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id)
        ? value
        : null;
}
// Invert source pixels too: colored backgrounds need another adaptive binarization pass.
function decodeQrFrame(pixels, width, height) {
    const normal = window.jsQR(pixels, width, height, { inversionAttempts: "attemptBoth" });
    if (normal) return normal;
    const inverse = new Uint8ClampedArray(pixels);
    for (let i = 0; i < inverse.length; i += 4) {
        inverse[i] = 255 - inverse[i];
        inverse[i + 1] = 255 - inverse[i + 1];
        inverse[i + 2] = 255 - inverse[i + 2];
    }
    return window.jsQR(inverse, width, height, { inversionAttempts: "dontInvert" });
}
const qrScanners = new Set();
function disposeQrScanners() {
    for (const scanner of [...qrScanners]) scanner.stop();
}
function createQrScanner() {
    const widget = document.createElement("div");
    const video = document.createElement("video");
    video.autoplay = true;
    video.muted = true;
    video.playsInline = true;
    video.setAttribute("playsinline", "");
    video.style.cssText = "width:100%;max-height:50vh;object-fit:contain";
    widget.append(video);
    const canvas = document.createElement("canvas"),
        ctx = canvas.getContext("2d", { willReadFrequently: true });
    const gate = new QrPresentationGate();
    let config = null,
        stream = null,
        timer = null,
        disabled = true,
        disposed = false,
        suspended = false,
        acquiring = false,
        generation = 0,
        lastStatus = null,
        sequence = 0,
        ack = 0;
    const report = status => {
        if (disabled || disposed || status === lastStatus) return;
        lastStatus = status;
        sendEventToServer(widget, "text input", { text: "status\n" + status });
    };
    const visible = () =>
        widget.isConnected && !document.hidden && widget.getClientRects().length > 0;
    const release = () => {
        generation++;
        acquiring = false;
        clearTimeout(timer);
        stream?.getTracks().forEach(track => track.stop());
        stream = null;
        video.srcObject = null;
    };
    const stop = () => {
        release();
        disposed = true;
        observer.disconnect();
        document.removeEventListener("visibilitychange", visibility);
        window.removeEventListener("pagehide", suspend);
        qrScanners.delete(scanner);
    };
    const suspend = () => {
        if (!stream && !acquiring) return;
        release();
        suspended = true;
        report("CLOSED");
    };
    const visibility = () => {
        if (document.hidden) suspend();
    };
    const scanner = { stop };
    qrScanners.add(scanner);
    // Cached pages can remain attached while hidden. Release pending requests as well.
    const observer = new MutationObserver(() => {
        if (!widget.isConnected) stop();
        else if (!visible()) suspend();
        else start();
    });
    observer.observe(document.documentElement, {
        subtree: true,
        childList: true,
        attributes: true,
        attributeFilter: ["style", "hidden"]
    });
    document.addEventListener("visibilitychange", visibility);
    window.addEventListener("pagehide", suspend);
    Object.defineProperty(widget, "disabled", {
        get: () => disabled,
        set: flag => {
            disabled = Boolean(flag);
            if (disabled) {
                release();
                sequence = ack;
                gate.code = null;
                gate.resume();
                lastStatus = null;
            } else {
                suspended = false;
                queueMicrotask(start);
            }
        }
    });
    widget.setText = text => {
        // An empty model value precedes the initial scanner configuration.
        if (!text) return false;
        config = JSON.parse(text);
        if (config.ack > ack) {
            ack = config.ack;
            sequence = Math.max(sequence, ack);
            gate.resume();
        }
        if (config.paused) gate.busy = true;
        else if (ack === sequence) gate.resume();
        if (stream) report(config.paused ? "WAITING" : "READY");
        queueMicrotask(start);
        return true;
    };
    function frame() {
        try {
            readFrame();
        } catch {
            release();
            suspended = true;
            report("UNAVAILABLE");
        }
    }
    function readFrame() {
        if (disabled || disposed || !stream) return;
        if (!visible()) {
            suspend();
            return;
        }
        if (!gate.busy && !config.paused && video.readyState >= 2 && video.videoWidth) {
            const scale = Math.min(1, 640 / video.videoWidth);
            canvas.width = Math.round(video.videoWidth * scale);
            canvas.height = Math.round(video.videoHeight * scale);
            ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
            const pixels = ctx.getImageData(0, 0, canvas.width, canvas.height);
            const decoded = decodeQrFrame(pixels.data, pixels.width, pixels.height);
            const code = decoded ? qrProductCode(decoded.data, config.prefix) : null;
            if (decoded && !code) report("INVALID");
            if (gate.frame(code, performance.now())) {
                sequence++;
                report("WAITING");
                sendEventToServer(widget, "text input", { text: sequence + "\n" + code });
            }
        }
        timer = setTimeout(frame, 120);
    }
    async function start() {
        if (disabled || disposed || suspended || acquiring || stream || !config || !visible())
            return;
        if (!window.isSecureContext) {
            suspended = true;
            report("HTTPS_REQUIRED");
            return;
        }
        if (!navigator.mediaDevices?.getUserMedia || !ctx || !window.jsQR) {
            suspended = true;
            report("UNAVAILABLE");
            return;
        }
        const request = ++generation;
        acquiring = true;
        report("OPENING");
        try {
            const acquired = await navigator.mediaDevices.getUserMedia({
                audio: false,
                video: { facingMode: { ideal: "environment" } }
            });
            if (request !== generation || disabled || disposed || !visible()) {
                acquired.getTracks().forEach(track => track.stop());
                if (request === generation) suspend();
                return;
            }
            stream = acquired;
            video.srcObject = stream;
            await video.play();
            if (request !== generation) return;
            if (!visible()) {
                suspend();
                return;
            }
            report(config.paused ? "WAITING" : "READY");
            frame();
        } catch (error) {
            if (request !== generation) return;
            release();
            suspended = true;
            report(error.name === "NotAllowedError" ? "DENIED" : "UNAVAILABLE");
        } finally {
            if (request === generation) acquiring = false;
        }
    }
    return widget;
}
