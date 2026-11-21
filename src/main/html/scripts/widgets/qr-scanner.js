// Состояние предъявления отделено от камеры для детерминированной проверки повторов.
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

// Никаких переходов и запросов по содержимому QR: принимается только точная ссылка магазина.
function qrProductCode(value, prefix) {
    if (typeof value !== "string" || !value.startsWith(prefix)) return null;
    const id = value.slice(prefix.length);
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id)
        ? value
        : null;
}
// Инвертируем также исходные пиксели: цветной фон требует повторной адаптивной бинаризации.
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
    const status = document.createElement("p");
    status.setAttribute("role", "status");
    const start = document.createElement("button");
    start.type = "button";
    widget.append(video, status, start);
    const canvas = document.createElement("canvas"),
        ctx = canvas.getContext("2d", { willReadFrequently: true });
    const gate = new QrPresentationGate();
    let config = null,
        stream = null,
        timer = null,
        stopped = false,
        acquiring = false,
        sequence = 0,
        ack = 0;
    const message = key => {
        status.textContent = config?.messages[key] || "";
    };
    const stop = () => {
        stopped = true;
        clearTimeout(timer);
        stream?.getTracks().forEach(track => track.stop());
        stream = null;
        video.srcObject = null;
        observer.disconnect();
        document.removeEventListener("visibilitychange", visibility);
        window.removeEventListener("pagehide", stop);
        qrScanners.delete(scanner);
    };
    const visibility = () => {
        if (document.hidden) {
            stop();
            message("closed");
        }
    };
    const scanner = { stop };
    qrScanners.add(scanner);
    // Сохранённые страницы могут оставаться в DOM, но камера у скрытой страницы выключается.
    const observer = new MutationObserver(() => {
        if (!widget.isConnected || (stream && widget.getClientRects().length === 0)) stop();
    });
    observer.observe(document.documentElement, {
        subtree: true,
        childList: true,
        attributes: true,
        attributeFilter: ["style", "hidden"]
    });
    document.addEventListener("visibilitychange", visibility);
    window.addEventListener("pagehide", stop);
    widget.setText = text => {
        // Начальное пустое значение модели приходит перед конфигурацией сканера.
        if (!text) return false;
        const next = JSON.parse(text);
        config = next;
        start.textContent = config.messages.start;
        if (next.ack > ack) {
            ack = next.ack;
            gate.resume();
        }
        if (next.paused) gate.busy = true;
        else if (ack === sequence) gate.resume();
        if (stream) message(next.paused ? "waiting" : "ready");
        else if (!acquiring && !stopped) message("idle");
        return true;
    };
    function frame() {
        try {
            readFrame();
        } catch {
            stop();
            message("unavailable");
        }
    }
    function readFrame() {
        if (stopped) return;
        if (!widget.isConnected || document.hidden || widget.getClientRects().length === 0) {
            stop();
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
            if (decoded && !code) message("invalid");
            if (gate.frame(code, performance.now())) {
                sequence++;
                message("waiting");
                sendEventToServer(widget, "text input", { text: sequence + "\n" + code });
            }
        }
        timer = setTimeout(frame, 120);
    }
    start.addEventListener("click", async () => {
        if (stopped || acquiring || stream || !config) return;
        if (!window.isSecureContext) {
            message("https");
            return;
        }
        if (!navigator.mediaDevices?.getUserMedia || !ctx || !window.jsQR) {
            message("unavailable");
            return;
        }
        acquiring = true;
        start.disabled = true;
        message("opening");
        try {
            const acquired = await navigator.mediaDevices.getUserMedia({
                audio: false,
                video: { facingMode: { ideal: "environment" } }
            });
            if (stopped || !widget.isConnected) {
                acquired.getTracks().forEach(track => track.stop());
                return;
            }
            stream = acquired;
            video.srcObject = stream;
            await video.play();
            if (stopped) return;
            message("ready");
            frame();
        } catch (error) {
            stream?.getTracks().forEach(track => track.stop());
            stream = null;
            video.srcObject = null;
            message(error.name === "NotAllowedError" ? "denied" : "unavailable");
        } finally {
            acquiring = false;
            start.disabled = false;
        }
    });
    return widget;
}
