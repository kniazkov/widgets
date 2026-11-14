/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

let lastFileId = 0;

const pendingUploads = [];

const activeUploads = [];

let uploadRequestInFlight = false;

let nextUploadIndex = 0;
let uploadNetworkFailures = 0;
let uploadNetworkRetry = null;
let uploadErrorNotice = null;

function clearUploadError() {
    uploadErrorNotice?.remove();
    uploadErrorNotice = null;
}

function showUploadError(code, limitBytes) {
    const host = (typeof pageContext !== "undefined" && pageContext?.root) || document.body;
    if (!host) return;
    if (!uploadErrorNotice) {
        uploadErrorNotice = document.createElement("div");
        uploadErrorNotice.className = "widgets-upload-error";
        uploadErrorNotice.setAttribute("role", "status");
        host.append(uploadErrorNotice);
    }
    uploadErrorNotice.dataset.uploadError = code;
    const limit = Number.isSafeInteger(limitBytes) && limitBytes >= 0 ? limitBytes : null;
    if (code === "fileTooLarge" && limit !== null) {
        uploadErrorNotice.textContent =
            "File is too large. Maximum file size: " + limit + " bytes.";
    } else if (code === "chunkTooLarge" && limit !== null) {
        uploadErrorNotice.textContent =
            "Upload chunk is too large. Maximum chunk size: " + limit + " bytes.";
    } else if (code === "requestTooLarge") {
        uploadErrorNotice.textContent =
            "The server rejected this upload because the request is too large.";
    } else if (code === "processingBusy") {
        uploadErrorNotice.textContent = "File processing is busy. Select the file again later.";
    } else if (code === "invalidFileName") {
        uploadErrorNotice.textContent =
            "File name is not supported. Use at most 255 characters without path separators.";
    } else {
        uploadErrorNotice.textContent = "The server rejected this upload.";
    }
}

function scheduleUploadNetworkRetry() {
    if (uploadNetworkRetry !== null) return;
    uploadNetworkRetry = setTimeout(
        () => {
            uploadNetworkRetry = null;
            sendNextUploadChunk();
        },
        transportRetryDelay(++uploadNetworkFailures, UPLOAD_RETRY_DELAY)
    );
}

const uploadRetryTasks = new Set();

function scheduleUploadRetry(callback) {
    if (
        (typeof clientDisposed !== "undefined" && clientDisposed) ||
        (typeof clientFailed !== "undefined" && clientFailed)
    )
        return;
    const task = setTimeout(() => {
        uploadRetryTasks.delete(task);
        if (typeof clientDisposed === "undefined" || !clientDisposed) callback();
    }, UPLOAD_RETRY_DELAY);
    uploadRetryTasks.add(task);
}

function disposeUploads() {
    for (const task of uploadRetryTasks) clearTimeout(task);
    uploadRetryTasks.clear();
    clearTimeout(uploadNetworkRetry);
    clearUploadError();
    uploadNetworkRetry = null;
    uploadNetworkFailures = 0;
    for (const file of [...pendingUploads, ...activeUploads]) {
        file.source = null;
        file.widget = null;
    }
    pendingUploads.length = 0;
    activeUploads.length = 0;
    uploadRequestInFlight = false;
}

function discardWidgetUploads(widget) {
    for (let index = pendingUploads.length - 1; index >= 0; index--) {
        const file = pendingUploads[index];
        if (file.widget !== widget) continue;
        pendingUploads.splice(index, 1);
        file.source = null;
        file.widget = null;
    }
    // Remove through the scheduler so the round-robin index stays valid.
    for (const file of activeUploads.slice()) {
        if (file.widget !== widget) continue;
        removeActiveUpload(file);
        file.widget = null;
    }
}

function setMultipleInput(data) {
    const widget = widgets[data.widget];
    const flag = data["multiple input"];
    if (widget && typeof flag == "boolean") {
        widget._multiple = flag;
        log(
            "The multiple input flag has been " +
                (flag ? "set" : "cleared") +
                " on the widget " +
                data.widget +
                "."
        );
        return true;
    }
    return false;
}

function setAcceptedFiles(data) {
    const widget = widgets[data.widget];
    const files = data["accepted files"];
    if (widget && typeof files == "string") {
        widget._accept = files;
        if (files == "") {
            log("The widget " + data.widget + " can accept all files");
        } else {
            log("The widget " + data.widget + " can accept files: '" + files + "'.");
        }
        return true;
    }
    return false;
}

// Registers all selected files before the binary scheduler starts sending their chunks.
function loadFiles(widget, descriptions) {
    const selected = [];
    if (descriptions.length) clearUploadError();
    for (let index = 0; index < descriptions.length; index++) {
        const descr = descriptions[index];
        if (
            !Number.isSafeInteger(descr.size) ||
            descr.size < 0 ||
            descr.size > uploadProtocol.maxFileSize
        ) {
            showUploadError("fileTooLarge", uploadProtocol.maxFileSize);
            continue;
        }
        if (
            typeof descr.name !== "string" ||
            descr.name.length === 0 ||
            descr.name.length > 255 ||
            descr.name.includes("/") ||
            descr.name.includes("\\") ||
            descr.name.includes("\0")
        ) {
            showUploadError("invalidFileName");
            continue;
        }
        const file = {
            id: ++lastFileId,
            name: descr.name,
            type: descr.type,
            size: descr.size,
            source: descr,
            totalChunks: Math.max(1, Math.ceil(descr.size / uploadProtocol.chunkSize)),
            nextChunk: 0,
            ready: false,
            widget
        };
        pendingUploads.push(file);
        selected.push(file);
        createEvent(widget, "upload", {
            fileId: file.id,
            name: file.name,
            type: file.type,
            size: file.size,
            totalChunks: file.totalChunks
        });
    }
    if (selected.length > 0) {
        acknowledgeSelections(selected);
    }
}

// Waits for the reliable event stream to register descriptors before sending binary data.
function acknowledgeSelections(selected) {
    if (typeof clientDisposed !== "undefined" && clientDisposed) {
        return;
    }
    selected = selected.filter(file => file.source !== null);
    if (!selected.length) return;
    sendSynchronizeRequest(function (accepted) {
        if (!accepted) {
            scheduleUploadRetry(() => acknowledgeSelections(selected));
            return;
        }
        for (let index = 0; index < selected.length; index++) {
            selected[index].ready = true;
        }
        fillActiveUploads();
        sendNextUploadChunk();
    });
}

// Moves queued files into the five page-wide active upload slots in selection order.
function fillActiveUploads() {
    while (
        activeUploads.length < MAX_ACTIVE_UPLOADS &&
        pendingUploads.length > 0 &&
        pendingUploads[0].ready
    ) {
        activeUploads.push(pendingUploads.shift());
    }
}

// Removes a completed or rejected upload without skipping the next round-robin entry.
function removeActiveUpload(file) {
    const index = activeUploads.indexOf(file);
    if (index < 0) {
        return;
    }
    activeUploads.splice(index, 1);
    file.source = null;
    if (index < nextUploadIndex) {
        nextUploadIndex--;
    }
    if (nextUploadIndex >= activeUploads.length) {
        nextUploadIndex = 0;
    }
    fillActiveUploads();
}

// Sends one binary slice, then gives the next active file a turn.
function sendNextUploadChunk() {
    if (
        (typeof clientDisposed !== "undefined" && clientDisposed) ||
        (typeof clientFailed !== "undefined" && clientFailed) ||
        uploadRequestInFlight ||
        uploadNetworkRetry !== null
    ) {
        return;
    }
    fillActiveUploads();
    if (activeUploads.length == 0) {
        return;
    }
    if (navigator.onLine === false) {
        recordRequestFailure();
        scheduleUploadNetworkRetry();
        return;
    }
    if (nextUploadIndex >= activeUploads.length) {
        nextUploadIndex = 0;
    }
    const file = activeUploads[nextUploadIndex];
    nextUploadIndex = (nextUploadIndex + 1) % activeUploads.length;
    const offset = file.nextChunk * uploadProtocol.chunkSize;
    const chunk = file.source.slice(offset, Math.min(file.size, offset + uploadProtocol.chunkSize));
    uploadRequestInFlight = true;
    sendClientRequest(
        {
            action: "upload chunk",
            client: clientId,
            widget: file.widget._id,
            fileId: file.id,
            chunkIndex: file.nextChunk,
            lastUpdate: "#" + lastAppliedUpdateSequence
        },
        function (data) {
            if (
                (typeof clientDisposed !== "undefined" && clientDisposed) ||
                (typeof clientFailed !== "undefined" && clientFailed)
            ) {
                return;
            }
            uploadRequestInFlight = false;
            if (data == null) {
                recordRequestFailure();
                scheduleUploadNetworkRetry();
                return;
            }
            const receipt =
                typeof readProtocolResponse === "function"
                    ? readProtocolResponse(data)
                    : JSON.parse(data);
            if (
                !receipt ||
                (typeof responseHasClientError === "function" && responseHasClientError(receipt))
            ) {
                return;
            }
            uploadNetworkFailures = 0;
            if (receipt.result === true && !processUpdates(receipt)) return;
            recordRequestSuccess();
            if (file.source === null) {
                sendNextUploadChunk();
                return;
            }
            if (
                receipt.result === true &&
                Number.isInteger(receipt.nextChunk) &&
                receipt.nextChunk >= 0 &&
                receipt.nextChunk <= file.totalChunks &&
                typeof receipt.complete == "boolean" &&
                receipt.complete === (receipt.nextChunk == file.totalChunks)
            ) {
                file.nextChunk = receipt.nextChunk;
                if (receipt.complete === true) {
                    removeActiveUpload(file);
                }
                sendNextUploadChunk();
                return;
            }
            if (receipt.result === false) {
                const code = ["chunkTooLarge", "requestTooLarge", "processingBusy"].includes(
                    receipt.uploadError
                )
                    ? receipt.uploadError
                    : "rejected";
                showUploadError(code, receipt.limitBytes);
                removeActiveUpload(file);
                sendNextUploadChunk();
                return;
            }
            scheduleUploadRetry(sendNextUploadChunk);
        },
        "post",
        [
            {
                field: "chunk",
                name: "chunk.bin",
                data: chunk
            }
        ]
    );
}
