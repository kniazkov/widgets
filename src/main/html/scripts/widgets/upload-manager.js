/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
function createUploadList() {
    const widget = document.createElement("section");
    widget.className = "widgets-upload-list";
    widget.setAttribute("aria-label", "Uploads");
    return widget;
}

function createUploadManager() {
    const widget = widgetsLibrary["file loader"]();
    widget._managedFiles = new Map();
    widget._localUploadId = 0;
    const disabledProperty = Object.getOwnPropertyDescriptor(
        HTMLButtonElement.prototype,
        "disabled"
    );
    Object.defineProperty(widget, "disabled", {
        get: () => disabledProperty.get.call(widget),
        set: flag => {
            disabledProperty.set.call(widget, flag);
            if (flag) widget._closeFilePicker();
            renderManagedUploads(widget);
        }
    });
    widget._managedSelected = file => {
        const record = widget._selectingRecord;
        if (!record) return;
        record.id = file.id;
        record.transfer = file;
        record.state = "queued";
        widget._managedFiles.set(file.id, record);
        if (record.previous) file.retryOf = record.previous;
        renderManagedUploads(widget);
    };
    widget._managedProgress = (file, state, message = "") => {
        const record = widget._managedFiles.get(file.id);
        if (!record || ["done", "cancelled", "error"].includes(record.state)) return;
        record.percent = Math.round((file.nextChunk * 100) / file.totalChunks);
        // The server may already have completed processing in the chunk response.
        if (record.state !== "processing" || state === "error") record.state = state;
        if (state === "error") record.retry = true;
        record.message = message;
        renderManagedUploads(widget);
    };
    widget._managedLoad = files => {
        widget._managerNotice = "";
        for (const source of files) {
            if (widget._managedFiles.size >= widget._managerConfig.maxFiles) {
                widget._managerNotice = "File count limit reached";
                break;
            }
            const record = {
                source,
                name: source.name,
                size: source.size,
                percent: 0,
                state: "queued"
            };
            const invalidName =
                !source.name || source.name.length > 255 || /[/\\\0]/.test(source.name);
            if (
                invalidName ||
                source.size > Math.min(uploadProtocol.maxFileSize, widget._managerConfig.maxBytes)
            ) {
                record.id = --widget._localUploadId;
                record.state = "error";
                record.message = invalidName ? "Invalid file name" : "File is too large";
                record.source = null;
                widget._managedFiles.set(record.id, record);
            } else {
                widget._selectingRecord = record;
                loadFiles(widget, [source]);
                widget._selectingRecord = null;
            }
        }
        renderManagedUploads(widget);
    };
    widget._clearManagedUploads = () => {
        for (const record of widget._managedFiles.values()) {
            record.source = null;
            record.transfer = null;
        }
        widget._managedFiles.clear();
    };
    return widget;
}

function stopManagedTransfer(record) {
    const file = record.transfer;
    if (!file) return;
    const index = pendingUploads.indexOf(file);
    if (index >= 0) pendingUploads.splice(index, 1);
    removeActiveUpload(file);
    file.source = null;
    record.transfer = null;
}

function renderManagedUploads(widget) {
    const list = widgets[widget._managerConfig?.list];
    if (!list) return;
    if (!list._uploadSummary) {
        list._uploadSummary = document.createElement("div");
        list._uploadSummary.setAttribute("role", "status");
        list.append(list._uploadSummary);
    }
    const summary = list._uploadSummary;
    const records = [...widget._managedFiles.values()];
    const total = records.reduce((sum, record) => sum + record.size, 0);
    const received = records.reduce((sum, record) => sum + (record.size * record.percent) / 100, 0);
    summary.textContent =
        (widget._managerNotice || "Uploads") +
        ": " +
        records.filter(r => r.state === "done").length +
        "/" +
        records.length +
        " · " +
        (total
            ? Math.round((received * 100) / total)
            : records.length && records.every(r => r.state === "done")
              ? 100
              : 0) +
        "%";
    const retained = new Set(records.map(record => record.row).filter(Boolean));
    for (const row of list.querySelectorAll(".widgets-upload-row")) {
        if (!retained.has(row)) row.remove();
    }
    for (const record of records) {
        if (!record.row) {
            record.row = document.createElement("div");
            record.row.className = "widgets-upload-row";
            record.label = document.createElement("span");
            record.progress = document.createElement("progress");
            record.progress.max = 100;
            record.progress.setAttribute("aria-label", record.name);
            record.row.append(record.label, record.progress);
            record.actions = {};
            for (const action of ["cancel", "retry", "remove"]) {
                const button = document.createElement("button");
                button.type = "button";
                button.textContent = action;
                button.setAttribute("aria-label", action + " " + record.name);
                button.addEventListener("click", () => {
                    if (widget.disabled) return;
                    if (record.id < 0) {
                        widget._managedFiles.delete(record.id);
                        renderManagedUploads(widget);
                    } else
                        sendEventToServer(widget, "upload command", { action, fileId: record.id });
                });
                record.actions[action] = button;
                record.row.append(button);
            }
        }
        record.row.dataset.uploadState = record.state;
        record.label.textContent =
            record.name + " — " + record.state + (record.message ? ": " + record.message : "");
        if (record.state === "processing") record.progress.removeAttribute("value");
        else record.progress.value = record.percent;
        record.actions.cancel.hidden = ["done", "cancelled", "error"].includes(record.state);
        record.actions.retry.hidden = !record.retry || !record.source;
        for (const button of Object.values(record.actions)) button.disabled = widget.disabled;
        if (record.row.parentNode !== list) list.append(record.row);
    }
}

function setUploadManager(data) {
    const widget = widgets[data.widget];
    if (!widget?._managedFiles || !data.snapshot) return false;
    const state = data.snapshot;
    widget._managerConfig = state;
    const record = widget._managedFiles.get(state.fileId);
    if (record) {
        if (state.message === "retry" && record.source) {
            stopManagedTransfer(record);
            widget._managedFiles.delete(record.id);
            record.previous = record.id;
            record.retry = false;
            record.percent = 0;
            record.message = "";
            widget._selectingRecord = record;
            loadFiles(widget, [record.source]);
            widget._selectingRecord = null;
        } else {
            if (state.state !== "queued" || record.state === "queued") record.state = state.state;
            record.retry = state.retry;
            record.message = ["cancel", "remove"].includes(state.message) ? "" : state.message;
            if (["cancelled", "error", "done"].includes(state.state)) stopManagedTransfer(record);
            if (state.state === "done") {
                record.percent = 100;
                record.source = null;
            }
            if (state.message === "remove") {
                record.source = null;
                widget._managedFiles.delete(record.id);
            }
        }
    }
    renderManagedUploads(widget);
    return true;
}
