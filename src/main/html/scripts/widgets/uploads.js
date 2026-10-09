/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

let lastFileId = 0;

const pendingUploads = [];

const activeUploads = [];

let uploadRequestInFlight = false;

let nextUploadIndex = 0;

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
    for (let index = 0; index < descriptions.length; index++) {
        const descr = descriptions[index];
        if (
            !Number.isSafeInteger(descr.size) ||
            descr.size < 0 ||
            descr.size > uploadProtocol.maxFileSize
        ) {
            log("The selected file '" + descr.name + "' is too large to upload.");
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
    sendSynchronizeRequest(function (accepted) {
        if (!accepted) {
            setTimeout(function () {
                acknowledgeSelections(selected);
            }, UPLOAD_RETRY_DELAY);
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
        uploadRequestInFlight
    ) {
        return;
    }
    fillActiveUploads();
    if (activeUploads.length == 0) {
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
    sendRequest(
        {
            action: "upload chunk",
            client: clientId,
            widget: file.widget._id,
            fileId: file.id,
            chunkIndex: file.nextChunk,
            lastUpdate: "#" + lastProcessedUpdateId
        },
        function (data) {
            if (typeof clientDisposed !== "undefined" && clientDisposed) {
                return;
            }
            uploadRequestInFlight = false;
            if (!data) {
                recordRequestFailure();
                setTimeout(sendNextUploadChunk, UPLOAD_RETRY_DELAY);
                return;
            }
            let receipt;
            try {
                receipt = JSON.parse(data);
            } catch (error) {
                recordRequestFailure();
                setTimeout(sendNextUploadChunk, UPLOAD_RETRY_DELAY);
                return;
            }
            if (typeof responseHasClientError === "function" && responseHasClientError(receipt)) {
                return;
            }
            recordRequestSuccess();
            processUpdates(receipt.updates);
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
                log("The server rejected upload '" + file.name + "'.");
                removeActiveUpload(file);
                sendNextUploadChunk();
                return;
            }
            setTimeout(sendNextUploadChunk, UPLOAD_RETRY_DELAY);
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
