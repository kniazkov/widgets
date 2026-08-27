/*
 * Copyright (c) 2025 Ivan Kniazkov
 */

const DEFAULT_FONT_FACE = "Inter";
const MAX_UPLOAD_CHUNK_SIZE = 1024 * 64;
const MAX_UPLOAD_FILE_SIZE = 128 * 1024 * 1024;
const MAX_ACTIVE_UPLOADS = 5;
const REQUEST_TIMEOUT = 10 * 1000;
const UPLOAD_RETRY_DELAY = 100;
