# Atomic batches and crash recovery

`Database.commit(drafts, removals)` commits new records, edits and deletions in one
persistence change set, even when they belong to different stores. The return value contains
canonical records in draft order. Existing record and field-model identities are preserved.
Existing `Draft.commit()`, `DataRecord.remove()` and direct `model.setData()` remain supported.

```java
Draft order = orders.createDraft();
order.model(total).setData(amount);
Draft line = lines.createDraft();
line.model(orderId).setData(order.getId());
List<DataRecord> basket = cart.getRecords();

database.commit(
    List.of(order, line),
    basket,
    () -> {
        if (!cart.getRecords().equals(basket)) {
            throw new IllegalStateException("Basket membership changed");
        }
        // Also check captured revisions, ownership, stock and business preconditions here.
    }
);
```

The optional third argument is read-only validation executed on the database dispatcher.
Throwing aborts the operation before persistence. Its reads cannot interleave with another
write. Validation and notifications must not perform network calls, acquire application locks,
wait for another thread, close the database, or mutate canonical records. Reentrant canonical
writes are rejected. Perform network calls outside the operation and validate their captured
inputs/revisions again before committing.

Draft revision checks, open-state checks, duplicate-target checks, ownership checks and value
conversion run before persistence. On validation/storage failure before a durable decision,
canonical records and live views remain unchanged and drafts remain usable. Successful batches
close their drafts. A record cannot be edited and removed in the same batch. Removing a record
means removing its current version: applications needing an optimistic delete must capture its
revision beforehand and check it in the validation callback. No implicit referential-integrity
or business rules are imposed by the generic database.

## Serialization and publication

The lock order is caller → database dispatcher → persistence backend → file/JDBC operations.
Canonical field models, record lookups, edits, removals, batch validation and commits all use
that dispatcher. Direct field writes cannot bypass a pending batch. No caller-owned model
monitor is acquired while waiting for the dispatcher; do not hold application locks while making
a database call. Drafts are private mutable inputs and must not be edited by another thread during
submission. Custom value types must provide deterministic, side-effect-free converters/models.

Only after durable commit are all canonical records, revisions, counts and live queries updated.
Then field/count/query notifications are delivered. Callbacks can read every participating store
and every existing live view and see the final state. Count notifications carry the final count,
including when several records were added to the same store. A view is reevaluated once per batch;
each event's indices refer to the previous and final snapshots, not a sequence of intermediate
lists. Consumers should identify records by UUID or read the final `getRecords()` snapshot.
Unaffected records whose positions changed receive MOVED events. Runtime exceptions from one
batch observer are logged and do not prevent the remaining observers from receiving updates.

A sequence of separate reads from an external thread is not a read transaction: a complete commit
may occur between them. Use the validation callback when several reads must be checked together.
The memory-only backend provides the same publication contract without disk durability.

## JSON commit protocol

The legacy `database.metadata` and per-store `*.json` formats remain readable. For every change
set, including single-record changes:

1. Serialize all affected stores without touching the live files.
2. Write complete replacement images to `database.prepare` and `FileChannel.force(true)` it.
3. Atomically rename it to `database.commit`, then flush the directory where supported. This is
   the commit decision. Atomic rename support is required; there is no non-atomic decision fallback.
4. For each store, write and force `database.install`, replace the store file, and flush the
   directory. The committed journal remains intact throughout installation.
5. Delete `database.commit` only after all targets are installed, and flush the directory again.

Startup replays a present `database.commit` **before reading/upgrading metadata or loading stores**.
Each replay replaces all its target files, so it is idempotent even if recovery itself crashes.
A leftover prepare/install file without a committed journal is not a transaction and is ignored;
subsequent operations reuse it. A malformed committed journal prevents opening the database;
it must not be silently deleted. Offline backups must capture a stopped database including any
journal. Old library versions do not understand journals: recover and shut down cleanly with
this version before any downgrade. Never edit files or run multiple database owners concurrently.

An I/O error once the decision rename has been attempted produces `RecoveryRequiredException`.
The database refuses further reads/writes; close it, repair the storage problem and reopen it.
The recovered result can be old or new depending on whether the decision survived. Retrying an
order/payment operation blindly can duplicate it; use application identifiers/idempotency rules.
Ordinary failures before the decision do not publish a partial result.

## SQLite/JDBC and physical durability

JDBC sends the complete change set through one existing SQL transaction. A failure before commit
rolls back; an uncertain commit or failure restoring auto-commit after commit requires reopening.
SQLite's durability depends on its journal/synchronous configuration, filesystem and device.
The application must provide an exclusively owned JDBC connection, not an externally managed
transaction, and must not alter the backing tables while the in-memory database is running.

JSON flushes files and, on Unix, directory entries. On Windows the standard Java API does not
provide directory flushing; the promise there covers **process crashes**, not sudden power loss.
On all platforms guarantees assume a local filesystem with the required rename semantics and a
device that honors flushes. Network filesystems, failing hardware, dishonest write caches,
external edits, and corruption are outside the guarantee. New database directory creation and
schema upgrades are not part of the multi-store data transaction; initialize schemas before
accepting business commands. After an uncertain storage outcome, no background jobs should resume
until reopening/recovery has succeeded.

## Verification

`AtomicBatchTest` covers JSON/SQLite round trips, coordinated live views/counts, stale/cancelled
and duplicate drafts, read-only validation, storage failure, and direct-model write/read contention.
`AtomicCrashTest` starts separate JVMs and uses `Runtime.halt` before/after the JSON decision,
between each store replacement, after cleanup, inside an SQLite transaction and immediately
before/after SQL commit. It also interrupts JSON recovery twice at the same point and verifies
two subsequent reopenings. Existing persistence, single-record, model and live-query tests remain
part of `mvn verify`. Tests never contact banking services or send messages.
