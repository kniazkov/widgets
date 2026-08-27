/*
 * Copyright (c) 2025 Ivan Kniazkov
 */

const DEFAULT_FONT_FACE = "Inter";
const MAX_ACTIVE_UPLOADS = 5;
const REQUEST_TIMEOUT = 10 * 1000;
const UPLOAD_RETRY_DELAY = 100;
let uploadProtocol = null;

// Receives upload limits from the server-rendered page bootstrap.
function configureUploadProtocol(chunkSize, maxFileSize) {
    if (
        !Number.isSafeInteger(chunkSize) ||
        chunkSize <= 0 ||
        !Number.isSafeInteger(maxFileSize) ||
        maxFileSize < 0
    ) {
        throw new Error("Invalid upload protocol configuration");
    }
    uploadProtocol = Object.freeze({ chunkSize, maxFileSize });
}
