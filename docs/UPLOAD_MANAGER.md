# UploadManager and UploadList

See the [full Russian contract and example](UPLOAD_MANAGER.ru.md).

`UploadManager(caption, list, maxFiles, maxBytes, processor)` extends `FileLoader`;
`UploadList` is its separate block presentation. Attach both to the same live root,
using one list per manager. Set accepted extensions/MIME patterns with `setAcceptedFiles`.

Files transition through queued, uploading, processing, done, error and cancelled.
Each row has progress and cancel/retry/remove actions. Overall progress measures confirmed
transfer bytes; the done counter measures completed processing. Processing is indeterminate.

Retries keep a stable operation key. The application processor **must be idempotent by key**;
cancellation cannot roll back external writes. Retrying a cancelled running processor waits
for it to exit. The temporary file is valid only during the callback. Copy its bytes before
returning to retain them. Do not mutate widgets directly from the background processor.

Limits include retained rows (1–100), per-file bytes and the existing Options upload quotas.
MIME/extension matching is not content validation. Completed/failed rows count until removed.
Removal does not delete application results. Browser files are released on done/remove/disposal;
reload persistence and cross-session resume are not provided. `onSelect` cannot replace the
manager's internal admission callback. Existing FileLoader usage remains compatible.

AllWidgets accepts .txt/.bin files, up to 10 files of 2 MiB each. A name starting with `retry`
fails once to demonstrate recovery. Desktop/mobile gallery capture and Java/JS/browser tests
are described in the linked document.
